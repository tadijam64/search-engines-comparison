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

/**
 *
 * This servlet acts as a bulk update to index content pages and assets to the configured Solr server
 *
 */

@Component(service = Servlet.class, property = { "sling.servlet.selectors=indexpages", "sling.servlet.resourceTypes=cq/Page",
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
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws IOException
    {

        this.doPost(request, response);
    }

    @Override
    protected void doPost(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws IOException
    {
        response.setContentType(CT_TEXT_HTML);

        String indexType = request.getParameter(PARAM_INDEX_TYPE);
        String protocol = this.solrConfigurationService.getSolrProtocol();
        String serverName = this.solrConfigurationService.getSolrServerName();
        String serverPort = this.solrConfigurationService.getSolrServerPort();
        String coreName = this.solrConfigurationService.getSolrCoreName();
        String url = protocol + "://" + serverName + ":" + serverPort + "/solr/" + coreName;

        if (indexType.equalsIgnoreCase(PROP_INDEX_TYPE_INDEX))
        {
            Resource resource = request.getResource();
            try (HttpSolrClient server = new HttpSolrClient(url); ResourceResolver resourceResolver = resource.getResourceResolver())
            {
                Session session = resourceResolver.adaptTo(Session.class);
                List<AbstractSolrItemModel> indexPageData = this.solrSearchService.crawlContent(session);
                boolean resultIndexingPages = this.solrSearchService.indexPagesToSolr(indexPageData, server);

                if (resultIndexingPages)
                {
                    response.getWriter().write("<h3>Successfully indexed content pages to Solr server </h3>");
                }
                else
                {
                    response.getWriter().write("<h3>Something went wrong</h3>");
                }
            }
            catch (Exception e)
            {
                LOG.error("Exception due to ", e);
                response.getWriter().write("<h3>Something went wrong. Please make sure Solr server is configured properly in Felix</h3>");
            }
        }
        else if (indexType.equalsIgnoreCase(PROP_INDEX_TYPE_DELETE))
        {
            try (HttpSolrClient server = new HttpSolrClient(url))
            {
                server.deleteByQuery("*:*");
                server.commit();
                response.getWriter().write("<h3>Deleted all the indexes from solr server </h3>");
            }
            catch (SolrServerException e)
            {
                LOG.error("Exception due to ", e);
            }
        }
        else
        {
            response.getWriter().write("<h3>Something went wrong</h3>");
        }
    }
}