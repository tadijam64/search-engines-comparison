package we.retail.core.servlets;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import we.retail.core.config.SolrServerConfiguration;
import we.retail.core.model.AbstractSolrItemModel;
import we.retail.core.services.SolrSearchService;

import static com.adobe.granite.rest.Constants.CT_TEXT_HTML;
import static we.retail.core.util.SolrUtils.getSolrServerUrl;

/**
 *
 * This servlet acts as a bulk update to index content pages and assets to the configured Solr server
 *
 */

@Component(service = Servlet.class, property = { "sling.servlet.selectors=indexSolrPages", "sling.servlet.resourceTypes=cq/Page",
  "sling.servlet.extensions=json", "sling.servlet.methods=" + HttpConstants.METHOD_GET })
public class SolrIndexingServlet extends SlingAllMethodsServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(SolrIndexingServlet.class);

    private static final String PARAM_INDEX_TYPE = "indexType";
    private static final String PROP_INDEX_TYPE_INDEX = "indexpages";
    private static final String PROP_INDEX_TYPE_DELETE = "deleteindexdata";

    @Reference
    SolrServerConfiguration solrConfigurationService;

    @Reference
    SolrSearchService solrSearchService;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
    {
        this.doPost(request, response);
    }

    /**
     * This method calls other methods for indexing content or deleting indexed content from Solr server.
     * This is based on user's choice provided in given request.
     * This method writes response of successful or unsuccessful indexing/deleting.
     * @param request
     * @param response
     */
    @Override
    protected void doPost(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
    {
        response.setContentType(CT_TEXT_HTML);
        String indexType = request.getParameter(PARAM_INDEX_TYPE);
        String url = getSolrServerUrl(this.solrConfigurationService);

        if (indexType.equalsIgnoreCase(PROP_INDEX_TYPE_INDEX))
        {
            indexData(request, response, url);
        }
        else if (indexType.equalsIgnoreCase(PROP_INDEX_TYPE_DELETE))
        {
            deleteData(url, response);
        }
    }

    /**
     * This method performs indexing.
     * It is done by taking Solr server URL to create client, index data and return response
     * @param request
     * @param response
     * @param url
     */
    private void indexData(SlingHttpServletRequest request, SlingHttpServletResponse response, String url)
    {
        Resource resource = request.getResource();
        try (HttpSolrClient server = new HttpSolrClient(url); ResourceResolver resourceResolver = resource.getResourceResolver())
        {
            Session session = resourceResolver.adaptTo(Session.class);
            List<AbstractSolrItemModel> indexPageData = this.solrSearchService.crawlContent(session);
            boolean dataIndexed = this.solrSearchService.indexPagesToSolr(indexPageData, server);

            writeResponse(dataIndexed, response);
        }
        catch (Exception e)
        {
            LOG.error("Exception due to ", e);
        }
    }

    /**
     * This method takes Boolean parameter dataIndex and writes response based on it's value.
     * @param dataIndexed
     * @param response
     * @throws IOException
     */
    private void writeResponse(boolean dataIndexed, SlingHttpServletResponse response) throws IOException
    {
        if (dataIndexed)
        {
            response.getWriter().write("<h3>Successfully indexed content pages to Solr server </h3>");
        }
        else
        {
            response.getWriter().write("<h3>Something went wrong</h3>");
        }
    }

    /**
     * This method takes URL of Solr client to write info response about index data deletion.
     * @param url
     * @param response
     */
    private void deleteData(String url, SlingHttpServletResponse response)
    {
        try (HttpSolrClient server = new HttpSolrClient(url))
        {
            server.deleteByQuery("*:*");
            server.commit();
            response.getWriter().write("<h3>Deleted all the indexes from solr server </h3>");
        }
        catch (SolrServerException | IOException e)
        {
            LOG.error("Exception due to ", e);
        }
    }
}