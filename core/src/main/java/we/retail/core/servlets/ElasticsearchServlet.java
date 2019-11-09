package we.retail.core.servlets;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import we.retail.core.config.ElasticsearchServerConfiguration;
import we.retail.core.services.EsService;

import static com.adobe.granite.rest.Constants.CT_TEXT_HTML;
import static we.retail.core.util.ElasticsearchUtils.handleElasticsearchException;
import static we.retail.core.util.ElasticsearchUtils.setIndexRequestOptions;
import static we.retail.core.util.ElasticsearchUtils.writeResponse;
import static we.retail.core.util.SearchHelpers.getEsClient;

@Component(service = Servlet.class, property = { "sling.servlet.selectors=indexElasticsearchPages", "sling.servlet.resourceTypes=cq/Page",
  "sling.servlet.extensions=json", "sling.servlet.methods=" + HttpConstants.METHOD_GET })
public class ElasticsearchServlet extends SlingAllMethodsServlet
{
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchServlet.class);
    private static final String PARAM_INDEX_TYPE = "indexType";
    private static final String PROP_INDEX_TYPE_INDEX = "indexpages";
    private static final String PROP_INDEX_TYPE_DELETE = "deleteindexdata";

    @Reference
    ElasticsearchServerConfiguration esConfigService;

    @Reference
    EsService elasticsearchService;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
    {
        this.doPost(request, response);
    }

    @Override
    protected void doPost(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
    {
        response.setContentType(CT_TEXT_HTML);
        String indexType = request.getParameter(PARAM_INDEX_TYPE);

        try (RestHighLevelClient client = getEsClient(this.esConfigService); ResourceResolver resourceResolver = request.getResourceResolver())
        {
            if (indexType.equalsIgnoreCase(PROP_INDEX_TYPE_INDEX))
            {
                indexData(response, client, resourceResolver);
            }
            else if (indexType.equalsIgnoreCase(PROP_INDEX_TYPE_DELETE))
            {
                deleteData(client, response);
            }
        }
        catch (ElasticsearchException ex)
        {
            handleElasticsearchException(ex);
        }
        catch (IOException | RepositoryException ex)
        {
            LOG.error("Elasticsearch exception: {}", ex);
        }
    }

    private void indexData(SlingHttpServletResponse response, RestHighLevelClient client, ResourceResolver resourceResolver) throws IOException, RepositoryException
    {
        Session session = resourceResolver.adaptTo(Session.class);
        String indexName = this.esConfigService.getElasticsearchIndexName();

        GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
        boolean indesNameExists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);

        if (indesNameExists)
        {
            response.getWriter().write("Index already exists. Choose different index name.");
        }
        else
        {
            List<XContentBuilder> builders = this.elasticsearchService.crawlContent(session);

            CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
            setIndexRequestOptions(createIndexRequest);
            client.indices().create(createIndexRequest, RequestOptions.DEFAULT);

            BulkRequest bulkRequest = new BulkRequest();
            addDocsToRequest(bulkRequest, builders, indexName);

            BulkResponse indexResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            writeResponse(indexResponse, response);
        }
    }

    private void addDocsToRequest(BulkRequest bulkRequest, List<XContentBuilder> builders, String indexName)
    {
        for (XContentBuilder builder : builders)
        {
            String id = Integer.toString(builders.indexOf(builder));
            IndexRequest indexRequest = new IndexRequest(indexName).id(id).source(builder);
            bulkRequest.add(indexRequest);
        }
    }

    private void deleteData(RestHighLevelClient client, SlingHttpServletResponse response) throws IOException
    {
        String indexName = this.esConfigService.getElasticsearchIndexName();

        DeleteIndexRequest deleteRequest = new DeleteIndexRequest(indexName);
        AcknowledgedResponse deleteIndexResponse = null;
        boolean acknowledged = false;

        try
        {
            deleteIndexResponse = client.indices().delete(deleteRequest, RequestOptions.DEFAULT);
            acknowledged = deleteIndexResponse.isAcknowledged();
        }
        catch (ElasticsearchException ex)
        {
            LOG.error("Index can not be found", ex);
        }
        finally
        {
            writeResponse(acknowledged, response, indexName);
        }
    }
}
