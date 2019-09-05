package we.retail.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.models.Search;
import com.day.cq.search.PredicateConverter;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import we.retail.core.model.PageListItemImpl;
import we.retail.core.util.StringUtils;

@Component(
  service = Servlet.class,
  property = {
    "sling.servlet.selectors=" + SearchServlet.DEFAULT_SELECTOR,
    "sling.servlet.resourceTypes=cq/Page",
    "sling.servlet.extensions=json",
    "sling.servlet.methods=" + HttpConstants.METHOD_GET
  }
)
public class SearchServlet extends SlingSafeMethodsServlet {

    static final String DEFAULT_SELECTOR = "mysearchresults";

    private static final String PARAM_RESULTS_OFFSET = "resultsOffset";
    private static final String PREDICATE_FULLTEXT = "fulltext";
    private static final String PREDICATE_TYPE = "type";
    private static final String PREDICATE_PATH = "path";
    private static final String NN_STRUCTURE = "structure";

    private static final int PROP_RESULTS_SIZE_DEFAULT = 10;
    private static final int PROP_SEARCH_TERM_MINIMUM_LENGTH_DEFAULT = 3;
    private static final String PROP_SEARCH_ROOT_DEFAULT = "/content";

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchServlet.class);

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private LanguageManager languageManager;

    @Reference
    private LiveRelationshipManager relationshipManager;

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response) throws IOException
    {
        try{
            Page currentPage = getCurrentPage(request);
            if (currentPage != null) {
                Resource searchResource = getSearchContentResource(request, currentPage);
                List<ListItem> results = getResults(request, searchResource, currentPage);
                writeJson(results, response);
            }
        } catch (Exception ex){
            LOGGER.error("Error in SearchServlet - ", ex);
            response.getWriter().write(ex.getLocalizedMessage());
        }
    }

    private Page getCurrentPage(SlingHttpServletRequest request) {
        Page currentPage = null;
        Resource currentResource = request.getResource();
        ResourceResolver resourceResolver = currentResource.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        if (pageManager != null) {
            currentPage = pageManager.getContainingPage(currentResource.getPath());
        }
        return currentPage;
    }

    private void writeJson(List<ListItem> results, SlingHttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(response.getWriter(), results);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private Resource getSearchContentResource(SlingHttpServletRequest request, Page currentPage) {
        Resource searchContentResource = null;
        RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        Resource resource = request.getResource();
        String relativeContentResource = StringUtils.removeStartingSlash(requestPathInfo.getSuffix());

        if (StringUtils.isNotEmpty(relativeContentResource)) {
            searchContentResource = resource.getChild(relativeContentResource);
            if (searchContentResource == null) {
                PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
                if (pageManager != null) {
                    Template template = currentPage.getTemplate();
                    if (template != null) {
                        Resource templateResource = request.getResourceResolver().getResource(template.getPath());
                        if (templateResource != null) {
                            searchContentResource = templateResource.getChild(NN_STRUCTURE + "/" + relativeContentResource);
                        }
                    }
                }
            }
        }
        return searchContentResource;
    }

    private List<ListItem> getResults(SlingHttpServletRequest request, Resource searchResource, Page currentPage) {
        int searchTermMinimumLength = PROP_SEARCH_TERM_MINIMUM_LENGTH_DEFAULT;
        int resultsSize = PROP_RESULTS_SIZE_DEFAULT;
        String searchRootPagePath;
        if (searchResource != null) {
            ValueMap valueMap = searchResource.getValueMap();
            ValueMap contentPolicyMap = getContentPolicyProperties(searchResource, request.getResource());
            searchTermMinimumLength = valueMap.get(Search.PN_SEARCH_TERM_MINIMUM_LENGTH, contentPolicyMap.get(Search
                                                                                                                .PN_SEARCH_TERM_MINIMUM_LENGTH, PROP_SEARCH_TERM_MINIMUM_LENGTH_DEFAULT));
            resultsSize = valueMap.get(Search.PN_RESULTS_SIZE, contentPolicyMap.get(Search.PN_RESULTS_SIZE,
              PROP_RESULTS_SIZE_DEFAULT));
            String searchRoot = valueMap.get(Search.PN_SEARCH_ROOT, contentPolicyMap.get(Search.PN_SEARCH_ROOT, PROP_SEARCH_ROOT_DEFAULT));
            searchRootPagePath = getSearchRootPagePath(searchRoot, currentPage);
        } else {
            String languageRoot = languageManager.getLanguageRoot(currentPage.getContentResource()).getPath();
            searchRootPagePath = getSearchRootPagePath(languageRoot, currentPage);
        }
        if (StringUtils.isEmpty(searchRootPagePath)) {
            searchRootPagePath = currentPage.getPath();
        }
        List<ListItem> results = new ArrayList<>();
        String fulltext = request.getParameter(PREDICATE_FULLTEXT);
        if (fulltext == null || fulltext.length() < searchTermMinimumLength) {
            return results;
        }
        long resultsOffset = 0;
        if (request.getParameter(PARAM_RESULTS_OFFSET) != null) {
            resultsOffset = Long.parseLong(request.getParameter(PARAM_RESULTS_OFFSET));
        }
        Map<String, String> predicatesMap = new HashMap<>();
        predicatesMap.put(PREDICATE_FULLTEXT, fulltext);
        predicatesMap.put(PREDICATE_PATH, searchRootPagePath);
        predicatesMap.put(PREDICATE_TYPE, NameConstants.NT_PAGE);
        PredicateGroup predicates = PredicateConverter.createPredicates(predicatesMap);
        ResourceResolver resourceResolver = request.getResource().getResourceResolver();
        Query query = queryBuilder.createQuery(predicates, resourceResolver.adaptTo(Session.class));
        if (resultsSize != 0) {
            query.setHitsPerPage(resultsSize);
        }
        if (resultsOffset != 0) {
            query.setStart(resultsOffset);
        }
        SearchResult searchResult = query.getResult();

        List<Hit> hits = searchResult.getHits();
        addHitsToResultsListItem(hits, results, request);

        return results;
    }

    private void addHitsToResultsListItem(List<Hit> hits, List<ListItem> results, SlingHttpServletRequest request){
        if (hits != null) {
            for (Hit hit : hits) {
                try {
                    Resource hitRes = hit.getResource();
                    Page page = getPage(hitRes);
                    if (page != null) {
                        results.add(new PageListItemImpl(request, page));
                    }
                } catch (RepositoryException e) {
                    LOGGER.error("Unable to retrieve search results for query.", e);
                }
            }
        }
    }

    private String getSearchRootPagePath(String searchRoot, Page currentPage) {
        String searchRootPagePath = null;
        PageManager pageManager = currentPage.getPageManager();
        if (StringUtils.isNotEmpty(searchRoot) && pageManager != null) {
            Page rootPage = pageManager.getPage(searchRoot);
            if (rootPage != null) {
                Page searchRootLanguageRoot = languageManager.getLanguageRoot(rootPage.getContentResource());
                Page currentPageLanguageRoot = languageManager.getLanguageRoot(currentPage.getContentResource());
                RangeIterator liveCopiesIterator = null;
                try {
                    liveCopiesIterator = relationshipManager.getLiveRelationships(currentPage.adaptTo(Resource.class), null, null);
                } catch (WCMException e) {
                    // ignore it
                }
                if (searchRootLanguageRoot != null && currentPageLanguageRoot != null && !searchRootLanguageRoot.equals
                                                                                                                   (currentPageLanguageRoot)) {
                    // check if there's a language copy of the search root
                    Page languageCopySearchRoot = pageManager.getPage(ResourceUtil.normalize(currentPageLanguageRoot.getPath() + "/" +
                                                                                               getRelativePath(searchRootLanguageRoot, rootPage)));
                    if (languageCopySearchRoot != null) {
                        rootPage = languageCopySearchRoot;
                    }
                } else if (liveCopiesIterator != null) {
                    while (liveCopiesIterator.hasNext()) {
                        LiveRelationship relationship = (LiveRelationship) liveCopiesIterator.next();
                        if (currentPage.getPath().startsWith(relationship.getTargetPath() + "/")) {
                            Page liveCopySearchRoot = pageManager.getPage(relationship.getTargetPath());
                            if (liveCopySearchRoot != null) {
                                rootPage = liveCopySearchRoot;
                                break;
                            }
                        }
                    }
                }
                searchRootPagePath = rootPage.getPath();
            }
        }
        return searchRootPagePath;
    }

    private ValueMap getContentPolicyProperties(Resource searchResource, Resource requestedResource) {
        ValueMap contentPolicyProperties = new ValueMapDecorator(new HashMap<>());
        ResourceResolver resourceResolver = searchResource.getResourceResolver();
        ContentPolicyManager contentPolicyManager = resourceResolver.adaptTo(ContentPolicyManager.class);
        if (contentPolicyManager != null) {
            ContentPolicy policy = contentPolicyManager.getPolicy(searchResource);
            if (policy != null) {
                contentPolicyProperties = policy.getProperties();
            }
        }
        return contentPolicyProperties;
    }

    @Nullable
    private String getRelativePath(@NotNull Page root, @NotNull Page child) {
        if (child.equals(root)) {
            return ".";
        } else if ((child.getPath() + "/").startsWith(root.getPath())) {
            return child.getPath().substring(root.getPath().length() + 1);
        }
        return null;
    }

    private Page getPage(Resource resource) {
        if (resource != null) {
            ResourceResolver resourceResolver = resource.getResourceResolver();
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            if (pageManager != null) {
                return pageManager.getContainingPage(resource);
            }
        }
        return null;
    }
}