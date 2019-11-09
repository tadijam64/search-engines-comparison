package we.retail.core.servlets;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.http.HttpHost;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import we.retail.core.config.ElasticsearchServerConfiguration;
import we.retail.core.services.ElasticsearchService;

import static com.adobe.granite.rest.Constants.CT_TEXT_HTML;
import static we.retail.core.util.ElasticsearchUtils.writeResponse;

@Component(service = Servlet.class, property = { "sling.servlet.selectors=indexElasticsearchPages", "sling.servlet.resourceTypes=cq/Page",
  "sling.servlet.extensions=json", "sling.servlet.methods=" + HttpConstants.METHOD_GET })
public class ElasticsearchServlet extends SlingAllMethodsServlet
{
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchServlet.class);
    private static final long serialVersionUID = 6406832539138310646L;
    private static final String PARAM_INDEX_TYPE = "indexType";
    private static final String PROP_INDEX_TYPE_INDEX = "indexpages";
    private static final String PROP_INDEX_TYPE_DELETE = "deleteindexdata";

    @Reference
    ElasticsearchServerConfiguration elasticsearchConfigurationService;

    @Reference
    ElasticsearchService elasticsearchService;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException
    {
        this.doPost(request, response);
    }

    @Override
    protected void doPost(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws IOException
    {
        response.setContentType(CT_TEXT_HTML);
        String indexType = request.getParameter(PARAM_INDEX_TYPE);
        String server = this.elasticsearchConfigurationService.getElasticsearchServerName();
        int port = Integer.parseInt(this.elasticsearchConfigurationService.getElasticsearchServerPort());
        int secondPort = Integer.parseInt(this.elasticsearchConfigurationService.getElasticsearchSecondServerPort());
        String protocol = this.elasticsearchConfigurationService.getElasticsearchProtocol();
        String indexName = this.elasticsearchConfigurationService.getElasticsearchIndexName();

        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(server, port, protocol),//
          new HttpHost(server, secondPort, protocol))); ResourceResolver resourceResolver = request.getResourceResolver())
        {
            if (indexType.equalsIgnoreCase(PROP_INDEX_TYPE_INDEX))
            {
                Session session = resourceResolver.adaptTo(Session.class);
                XContentBuilder builder = this.elasticsearchService.crawlContent(session);

                IndexRequest indexRequest = new IndexRequest("posts");
                indexRequest.id("2");
                try
                {
                    indexRequest.source(builder);
                    IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
                    writeResponse(indexResponse, response);
                }
                catch (Exception ex)
                {
                    LOG.error("Error - {}", ex);
                }
            }
            else if (indexType.equalsIgnoreCase(PROP_INDEX_TYPE_DELETE))
            {
                DeleteRequest deleteRequest = new DeleteRequest("posts", "1");
                DeleteResponse deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);

                writeResponse(deleteResponse, response, indexName);
            }
        }
        catch (ElasticsearchException ex)
        {
            if (ex.status().equals(RestStatus.CONFLICT))
            {
                LOG.error("There is version conflict on added index. Check index versions - {}", ex);
            }
            else
            {
                LOG.error("Elasticsearch exception in class: {}", ex);
            }
        }
        catch (IOException | RepositoryException ex)
        {
            LOG.error("Elasticsearch exception: {}", ex);
        }
    }
}
