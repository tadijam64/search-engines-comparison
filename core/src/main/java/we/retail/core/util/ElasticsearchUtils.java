package we.retail.core.util;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.tagging.TagConstants;

import static com.day.cq.commons.jcr.JcrConstants.JCR_CREATED;
import static com.day.cq.dam.api.DamConstants.NT_DAM_ASSET;
import static com.day.cq.wcm.api.NameConstants.NT_PAGE;
import static com.day.cq.wcm.api.NameConstants.PN_PAGE_LAST_MOD;
import static we.retail.core.util.SearchHelpers.addAssetToResultsList;
import static we.retail.core.util.SearchHelpers.addPageToResultsList;

public class ElasticsearchUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchUtils.class);

    private static final String PROP_SEARCH_ASSETS = "assets";
    private static final String PROP_SEARCH_PAGES = "pages";
    private static final String PROP_SEARCH_TAGS = "tags";
    private static final String PROP_SEARCH_TYPE = "type";

    private ElasticsearchUtils()
    {
    }

    /**
     * This method takes indexResponse from Elasticsearch index to write info of indexing process success or failure
     * @param indexResponse
     * @param response
     * @throws IOException
     */
    public static void writeResponse(BulkResponse indexResponse, SlingHttpServletResponse response) throws IOException
    {
        if (indexResponse.hasFailures())
        {
            for (BulkItemResponse item : indexResponse.getItems())
            {
                LOG.error(item.getFailureMessage());
                response.getWriter().write("<h3>Unable to index given data</h3>");
            }
        }
        else
        {
            LOG.info("Documents are successfully updated.");
            response.getWriter().write("<h3>Successfully indexed content pages to Elasticsearch server </h3>");
        }

    }

    /**
     * This method takes Boolean acknowledged parameter and index name to write info of deleting index process success or failure
     * @param acknowledged
     * @param response
     * @param indexName
     * @throws IOException
     */
    public static void writeResponse(Boolean acknowledged, SlingHttpServletResponse response, String indexName) throws IOException
    {
        if (Boolean.FALSE.equals(acknowledged))
        {
            LOG.error("Unable to delete index with the name: {}", indexName);
            response.getWriter().write("<h3> Something went wrong! Index does not exist.</h3>");
        }
        else
        {
            response.getWriter().write("<h3>Index is successfully deleted </h3>");
        }
    }

    /**
     * This method adds property to JSON builder if it is contained in search results
     * @param builder
     * @param vm
     * @param keys
     * @throws IOException
     */
    public static void addFields(XContentBuilder builder, ValueMap vm, String... keys) throws IOException
    {
        for (String key : keys)
        {
            if (vm.containsKey(key))
            {
                addField(key, vm, builder);
            }
        }
    }

    /**
     * This method adds property to JSON builder based on it's type
     * @param key
     * @param vm
     * @param builder
     * @throws IOException
     */
    private static void addField(String key, ValueMap vm, XContentBuilder builder) throws IOException
    {
        if (StringUtils.equals(key, TagConstants.PN_TAGS))
        {
            addTagArray(vm, builder, key);
        }
        else if (key.equals(JCR_CREATED) || key.equals("manualCreationDate") || key.equals("creationDate") || key.equals(PN_PAGE_LAST_MOD))
        {
            addDateField(builder, key, vm);
        }
        else
        {
            builder.field(key, vm.get(key).toString());
        }
    }

    /**
     * This method adds date property to JSON builder
     * @param builder
     * @param key
     * @param vm
     * @throws IOException
     */
    private static void addDateField(XContentBuilder builder, String key, ValueMap vm) throws IOException
    {
        Calendar cal = vm.get(key, Calendar.class);
        Date d = new Date();
        if (cal != null)
        {
            d = cal.getTime();
        }
        builder.field(key, d.toString());
    }

    /**
     * This method adds array property to JSON builder
     * @param vm
     * @param builder
     * @param key
     * @throws IOException
     */
    private static void addTagArray(ValueMap vm, XContentBuilder builder, String key) throws IOException
    {
        String[] tags = vm.get(key, String[].class);
        builder.array("tags", tags);
    }

    /**
     * This method takes ElasticsearchException to write it's status to Logger file
     * @param ex
     */
    public static void handleElasticsearchException(ElasticsearchException ex)
    {
        if (ex.status().equals(RestStatus.CONFLICT))
        {
            LOG.error("There was version conflict on added index. Check index versions", ex);
        }
        else if (ex.status() == RestStatus.NOT_FOUND)
        {
            LOG.error("Unable to find index", ex);
        }
        else
        {
            LOG.error("Elasticsearch exception", ex);
        }
    }

    /**
     * This method takes request for index creation to set it's properties
     * @param createIndexRequest
     */
    public static void setIndexRequestOptions(CreateIndexRequest createIndexRequest)
    {
        createIndexRequest.settings(Settings.builder() //
                                      .put("index.number_of_shards", 3) //
                                      .put("index.number_of_replicas", 1) //
                                      .put("index.mapping.total_fields.limit", 100000) //
                                      .build());
    }

    /**
     * This method takes user's search input (fulltext) parameter and search type (searchContentType) parameter
     * to return query based on these parameters
     * @param fulltext
     * @param searchContentType
     * @return
     */
    public static BoolQueryBuilder getBoolQuery(String fulltext, String searchContentType)
    {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (searchContentType.equalsIgnoreCase(PROP_SEARCH_TAGS))
        {
            boolQueryBuilder.should(QueryBuilders.matchQuery("tags", "*" + fulltext + "*"));
        }
        else
        {
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(fulltext));

            if (searchContentType.equalsIgnoreCase(PROP_SEARCH_ASSETS))
            {
                boolQueryBuilder.must(QueryBuilders.matchPhraseQuery(PROP_SEARCH_TYPE, NT_DAM_ASSET));
            }
            else if (searchContentType.equalsIgnoreCase(PROP_SEARCH_PAGES))
            {
                boolQueryBuilder.must(QueryBuilders.matchPhraseQuery(PROP_SEARCH_TYPE, NT_PAGE));
            }
        }
        return boolQueryBuilder;
    }

    /**
     * This method takes index name and source builder to create search request for Elasticsearch server
     * @param sourceBuilder
     * @param indexName
     * @return
     */
    public static SearchRequest getSearchRequest(SearchSourceBuilder sourceBuilder, String indexName)
    {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(indexName);
        searchRequest.source(sourceBuilder);

        return searchRequest;
    }

    /**
     * This method takes query to create source builder
     * @param boolQueryBuilder
     * @return
     */
    public static SearchSourceBuilder getSourceBuilder(BoolQueryBuilder boolQueryBuilder)
    {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.from(0);
        sourceBuilder.size(10);
        sourceBuilder.query(boolQueryBuilder);
        return sourceBuilder;
    }

    /**
     * This method takes search results and request to convert each result to data format needed for indexing
     * @param results
     * @param request
     * @param searchResponse
     */
    public static void addResultsToResultItemList(List<ListItem> results, SlingHttpServletRequest request, SearchResponse searchResponse)
    {
        long numberOfHits = searchResponse.getHits().getTotalHits().value;
        LOG.info("Number of hits recieved from index: {}", numberOfHits);

        ResourceResolver resourceResolver = request.getResourceResolver();
        SearchHits hits = searchResponse.getHits();

        for (SearchHit hit : hits)
        {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            String path = (String) sourceAsMap.get("id");
            Resource res = resourceResolver.getResource(path);

            if (res != null)
            {
                if (res.getResourceType().equals(NT_PAGE))
                {
                    addPageToResultsList(results, request, res);
                }
                else if (res.getResourceType().equals(NT_DAM_ASSET))
                {
                    addAssetToResultsList(results, request, res);
                }
            }
        }
    }
}
