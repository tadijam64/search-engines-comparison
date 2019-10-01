package we.retail.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.models.Search;
import com.day.cq.dam.api.Asset;
import com.day.cq.search.PredicateConverter;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import we.retail.core.util.StringUtils;
import we.retail.core.vo.AssetListItemImpl;
import we.retail.core.vo.PageListItemImpl;

import static com.day.cq.dam.api.DamConstants.NT_DAM_ASSET;
import static com.day.cq.wcm.api.NameConstants.NT_PAGE;
import static we.retail.core.util.SearchHelpers.getAsset;
import static we.retail.core.util.SearchHelpers.getContentPolicyProperties;
import static we.retail.core.util.SearchHelpers.getPage;
import static we.retail.core.util.SearchHelpers.getSearchRootPagePath;

@Component(service = Servlet.class, property = { "sling.servlet.selectors=" + SearchServlet.DEFAULT_SELECTOR, "sling.servlet.resourceTypes=cq/Page",
  "sling.servlet.extensions=json", "sling.servlet.methods=" + HttpConstants.METHOD_GET })
public class SearchServlet extends SlingSafeMethodsServlet
{

    static final String DEFAULT_SELECTOR = "mysearchresults";

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchServlet.class);
    private static final String PARAM_RESULTS_OFFSET = "resultsOffset";
    private static final String PREDICATE_FULLTEXT = "fulltext";
    private static final String PREDICATE_TYPE = "type";
    private static final String PREDICATE_PATH = "path";
    private static final String NN_STRUCTURE = "structure";

    private static final int PROP_RESULTS_SIZE_DEFAULT = 10;
    private static final int PROP_SEARCH_TERM_MINIMUM_LENGTH_DEFAULT = 3;
    private static final String PROP_SEARCH_ROOT_DEFAULT = "/content";
    private static final String PROP_SEARCH_ROOT_ASSETS = "/content/dam/we-retail";
    private static final String PROP_SEARCH_PAGES = "pages";
    private static final String PROP_SEARCH_ASSETS = "assets";
    private static final String PROP_SEARCH_CONTENT_TYPE = "searchContent";
    private static final long serialVersionUID = 5692888423980970123L;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private LanguageManager languageManager;

    @Reference
    private LiveRelationshipManager relationshipManager;

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response) throws IOException
    {
        try
        {
            Page currentPage = getCurrentPage(request);
            if (currentPage != null)
            {
                Resource searchResource = getSearchContentResource(request, currentPage);
                List<ListItem> results = getResults(request, searchResource, currentPage);
                writeJson(results, response);
            }
        }
        catch (Exception ex)
        {
            LOGGER.error("Error in SearchServlet - ", ex);
            response.getWriter().write(ex.getLocalizedMessage());
        }
    }

    private Page getCurrentPage(SlingHttpServletRequest request)
    {
        Page currentPage = null;
        Resource currentResource = request.getResource();
        ResourceResolver resourceResolver = currentResource.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        if (pageManager != null)
        {
            currentPage = pageManager.getContainingPage(currentResource.getPath());
        }
        return currentPage;
    }

    private Resource getSearchContentResource(SlingHttpServletRequest request, Page currentPage)
    {
        Resource searchContentResource = null;
        RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        Resource resource = request.getResource();
        String relativeContentResource = StringUtils.removeStartingSlash(requestPathInfo.getSuffix());

        if (StringUtils.isNotEmpty(relativeContentResource))
        {
            searchContentResource = resource.getChild(relativeContentResource);
            if (searchContentResource == null)
            {
                PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
                if (pageManager != null)
                {
                    Template template = currentPage.getTemplate();
                    if (template != null)
                    {
                        Resource templateResource = request.getResourceResolver().getResource(template.getPath());
                        if (templateResource != null)
                        {
                            searchContentResource = templateResource.getChild(NN_STRUCTURE + "/" + relativeContentResource);
                        }
                    }
                }
            }
        }
        return searchContentResource;
    }

    private List<ListItem> getResults(SlingHttpServletRequest request, Resource searchResource, Page currentPage)
    {
        int searchTermMinimumLength = PROP_SEARCH_TERM_MINIMUM_LENGTH_DEFAULT;
        int resultsSize = PROP_RESULTS_SIZE_DEFAULT;
        String searchRootPagePath;
        if (searchResource != null)
        {
            ValueMap valueMap = searchResource.getValueMap();
            ValueMap contentPolicyMap = getContentPolicyProperties(searchResource, request.getResource());
            searchTermMinimumLength = valueMap.get(Search.PN_SEARCH_TERM_MINIMUM_LENGTH, contentPolicyMap.get(Search.PN_SEARCH_TERM_MINIMUM_LENGTH, PROP_SEARCH_TERM_MINIMUM_LENGTH_DEFAULT));
            resultsSize = valueMap.get(Search.PN_RESULTS_SIZE, contentPolicyMap.get(Search.PN_RESULTS_SIZE, PROP_RESULTS_SIZE_DEFAULT));
            String searchRoot = valueMap.get(Search.PN_SEARCH_ROOT, contentPolicyMap.get(Search.PN_SEARCH_ROOT, PROP_SEARCH_ROOT_DEFAULT));
            searchRootPagePath = getSearchRootPagePath(searchRoot, currentPage, this.languageManager, this.relationshipManager);
        }
        else
        {
            String languageRoot = this.languageManager.getLanguageRoot(currentPage.getContentResource()).getPath();
            searchRootPagePath = getSearchRootPagePath(languageRoot, currentPage, this.languageManager, this.relationshipManager);
        }
        if (StringUtils.isEmpty(searchRootPagePath))
        {
            searchRootPagePath = currentPage.getPath();
        }
        List<ListItem> results = new ArrayList<>();
        Map<String, String> predicatesMap = new HashMap<>();

        String fulltext = request.getParameter(PREDICATE_FULLTEXT);
        if (fulltext == null || fulltext.length() < searchTermMinimumLength)
        {
            return results;
        }
        long resultsOffset = 0;
        if (request.getParameter(PARAM_RESULTS_OFFSET) != null)
        {
            resultsOffset = Long.parseLong(request.getParameter(PARAM_RESULTS_OFFSET));
        }

        predicatesMap.put(PREDICATE_FULLTEXT, fulltext);
        String searchContentType = request.getParameter(PROP_SEARCH_CONTENT_TYPE);

        if (searchContentType.equalsIgnoreCase(PROP_SEARCH_ASSETS))
        {
            predicatesMap.put(PREDICATE_PATH, PROP_SEARCH_ROOT_ASSETS);
            predicatesMap.put(PREDICATE_TYPE, NT_DAM_ASSET);
        }
        else if (searchContentType.equalsIgnoreCase(PROP_SEARCH_PAGES))
        {
            predicatesMap.put(PREDICATE_PATH, searchRootPagePath);
            predicatesMap.put(PREDICATE_TYPE, NT_PAGE);
        }
        else
        {
            predicatesMap.put("group.p.or", "true"); //combine this group with OR
            predicatesMap.put("group.1_group.path", searchRootPagePath);
            predicatesMap.put("group.1_group.type", NT_PAGE);
            predicatesMap.put("group.2_group.path", PROP_SEARCH_ROOT_ASSETS);
            predicatesMap.put("group.2_group.type", NT_DAM_ASSET);
        }

        PredicateGroup predicates = PredicateConverter.createPredicates(predicatesMap);
        ResourceResolver resourceResolver = request.getResource().getResourceResolver();
        Query query = this.queryBuilder.createQuery(predicates, resourceResolver.adaptTo(Session.class));
        if (resultsSize != 0)
        {
            query.setHitsPerPage(resultsSize);
        }
        if (resultsOffset != 0)
        {
            query.setStart(resultsOffset);
        }
        SearchResult searchResult = query.getResult();

        List<Hit> hits = searchResult.getHits();
        if (hits != null)
        {
            addHitsToResultsListItem(hits, results, request, searchContentType);
        }

        return results;
    }

    private void addHitsToResultsListItem(List<Hit> hits, List<ListItem> results, SlingHttpServletRequest request, String searchContentType)
    {
        for (Hit hit : hits)
        {
            try
            {
                Resource hitRes = hit.getResource();

                if (searchContentType.equals(PROP_SEARCH_ASSETS))
                {
                    addAssetToResultsList(results, request, hitRes);
                }
                else if (searchContentType.equals(PROP_SEARCH_PAGES))
                {
                    addPageToResultsList(results, request, hitRes);
                }
                else
                {
                    if (hitRes.getPath().contains("/content/dam"))
                    {
                        addAssetToResultsList(results, request, hitRes);
                    }
                    else if (hitRes.getPath().contains("/content/we-retail"))
                    {
                        addPageToResultsList(results, request, hitRes);
                    }
                }
            }
            catch (RepositoryException e)
            {
                LOGGER.error("Unable to retrieve search results for query.", e);
            }
        }
    }

    private void addPageToResultsList(List<ListItem> results, SlingHttpServletRequest request, Resource hitRes)
    {
        Page page = getPage(hitRes);

        if (page != null)
        {
            results.add(new PageListItemImpl(request, page));
        }
    }

    private void addAssetToResultsList(List<ListItem> results, SlingHttpServletRequest request, Resource hitRes)
    {
        Asset asset = getAsset(hitRes);

        if (asset != null)
        {
            results.add(new AssetListItemImpl(request, asset));
        }
    }

    private void writeJson(List<ListItem> results, SlingHttpServletResponse response)
    {
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            mapper.writeValue(response.getWriter(), results);
        }
        catch (IOException e)
        {
            LOGGER.error(e.getMessage());
        }
    }
}