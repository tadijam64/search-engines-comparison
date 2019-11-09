package we.retail.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jetbrains.annotations.NotNull;
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
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import we.retail.core.config.ElasticsearchServerConfiguration;
import we.retail.core.config.SolrServerConfiguration;
import we.retail.core.util.ExtendedStringUtils;

import static com.adobe.aemds.guide.utils.GuideConstants.UTF_8;
import static com.adobe.cq.mcm.salesforce.SalesforceExportProcess.APPLICATION_JSON;
import static we.retail.core.util.ElasticsearchUtils.addResultsToResultItemList;
import static we.retail.core.util.ElasticsearchUtils.getBoolQuery;
import static we.retail.core.util.ElasticsearchUtils.getSearchRequest;
import static we.retail.core.util.ElasticsearchUtils.getSourceBuilder;
import static we.retail.core.util.LuceneUtils.addResultDocsToResultItemList;
import static we.retail.core.util.LuceneUtils.getResultOffset;
import static we.retail.core.util.LuceneUtils.setQueryPredicates;
import static we.retail.core.util.SearchHelpers.getContentPolicyProperties;
import static we.retail.core.util.SearchHelpers.getEsClient;
import static we.retail.core.util.SearchHelpers.getSearchRootPagePath;
import static we.retail.core.util.SolrUtils.addHitsToResultsListItem;
import static we.retail.core.util.SolrUtils.addPredicatesToQuery;
import static we.retail.core.util.SolrUtils.getSolrServerUrl;

@Component(service = Servlet.class, property = { "sling.servlet.selectors=" + SearchServlet.DEFAULT_SELECTOR, "sling.servlet.resourceTypes=cq/Page",
  "sling.servlet.extensions=json", "sling.servlet.methods=" + HttpConstants.METHOD_GET })
public class SearchServlet extends SlingSafeMethodsServlet
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchServlet.class);

    static final String DEFAULT_SELECTOR = "mysearchresults";
    public static final String PREDICATE_FULLTEXT = "fulltext";
    private static final String NN_STRUCTURE = "structure";
    private static final String PROP_SEARCH_ROOT_DEFAULT = "/content";
    private static final String PROP_SEARCH_CONTENT_TYPE = "searchContent";
    private static final String PARAM_SEARCH_ENGINE = "searchEngine";

    private static final int PROP_RESULTS_SIZE_DEFAULT = 10;
    private static final int PROP_SEARCH_TERM_MINIMUM_LENGTH_DEFAULT = 3;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private LanguageManager languageManager;

    @Reference
    private LiveRelationshipManager relationshipManager;

    @Reference
    private SolrServerConfiguration solrConfigurationService;

    @Reference
    private ElasticsearchServerConfiguration esConfigService;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
    {
        this.handleRequest(request, response);
    }

    private void handleRequest(SlingHttpServletRequest request, SlingHttpServletResponse response)
    {
        Page currentPage = getCurrentPage(request);

        if (currentPage != null)
        {
            Resource searchResource = getSearchContentResource(request, currentPage);
            List<ListItem> results = null;

            if (StringUtils.equalsIgnoreCase(request.getParameter(PARAM_SEARCH_ENGINE), "lucene"))
            {
                results = getLuceneResults(request, searchResource, currentPage);
            }
            else if (StringUtils.equalsIgnoreCase(request.getParameter(PARAM_SEARCH_ENGINE), "solr"))
            {
                results = getSolrResults(request);
            }
            else if (StringUtils.equalsIgnoreCase(request.getParameter(PARAM_SEARCH_ENGINE), "elasticsearch"))
            {
                results = getElasticsearchResults(request);
            }

            writeJson(results, response);
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
        String relativeContentResource = ExtendedStringUtils.removeStartingSlash(requestPathInfo.getSuffix());

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

    private List<ListItem> getLuceneResults(SlingHttpServletRequest request, Resource searchResource, Page currentPage)
    {
        int searchTermMinimumLength = PROP_SEARCH_TERM_MINIMUM_LENGTH_DEFAULT;
        int resultsSize = PROP_RESULTS_SIZE_DEFAULT;
        String searchRootPagePath;
        if (searchResource != null)
        {
            ValueMap valueMap = searchResource.getValueMap();
            ValueMap contentPolicyMap = getContentPolicyProperties(searchResource);
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

        long resultsOffset = getResultOffset(request);
        setQueryPredicates(request, predicatesMap, fulltext, searchRootPagePath);

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
            addHitsToResultsListItem(hits, results, request);
        }

        return results;
    }

    private List<ListItem> getSolrResults(SlingHttpServletRequest request)
    {
        List<ListItem> results = new ArrayList<>();
        String fulltext = request.getParameter(PREDICATE_FULLTEXT);
        String searchContentType = request.getParameter(PROP_SEARCH_CONTENT_TYPE);

        SolrQuery query = new SolrQuery();
        addPredicatesToQuery(searchContentType, query, fulltext);

        try (HttpSolrClient server = new HttpSolrClient(getSolrServerUrl(this.solrConfigurationService)))
        {
            QueryResponse response = server.query(query);
            SolrDocumentList resultDocs = response.getResults();

            addResultDocsToResultItemList(resultDocs, request, results);
        }
        catch (SolrServerException | IOException e)
        {
            LOGGER.error("Exception due to ", e);
        }

        return results;
    }

    private List<ListItem> getElasticsearchResults(SlingHttpServletRequest request)
    {
        List<ListItem> results = new ArrayList<>();
        String fulltext = request.getParameter(PREDICATE_FULLTEXT);
        String searchContentType = request.getParameter(PROP_SEARCH_CONTENT_TYPE);
        String indexName = this.esConfigService.getElasticsearchIndexName();

        try (RestHighLevelClient client = getEsClient(this.esConfigService))
        {
            BoolQueryBuilder boolQueryBuilder = getBoolQuery(fulltext, searchContentType);
            SearchSourceBuilder sourceBuilder = getSourceBuilder(boolQueryBuilder);
            SearchRequest searchRequest = getSearchRequest(sourceBuilder, indexName);

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            addResultsToResultItemList(results, request, searchResponse);
        }
        catch (ElasticsearchException | IOException e)
        {
            LOGGER.error("Exception due to ", e);
        }

        return results;
    }

    private void writeJson(List<ListItem> results, SlingHttpServletResponse response)
    {
        response.setContentType(APPLICATION_JSON);
        response.setCharacterEncoding(UTF_8);
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