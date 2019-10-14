package we.retail.core.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.search.result.Hit;

import we.retail.core.config.SolrServerConfiguration;

import static com.day.cq.dam.api.DamConstants.NT_DAM_ASSET;
import static com.day.cq.wcm.api.NameConstants.NT_PAGE;
import static we.retail.core.util.SearchHelpers.addAssetToResultsList;
import static we.retail.core.util.SearchHelpers.addPageToResultsList;

public class SolrUtils
{
    static final String PROP_SEARCH_ROOT_ASSETS = "/content/dam/we-retail";
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrUtils.class);

    // private constructor to avoid unnecessary instantiation of the class
    private SolrUtils()
    {
    }

    /**
     * This method converts jcr formatted date to Solr specification format
     * @param cal Takes input as Calendar
     * @return Solr formatted date of type string
     */
    public static String castToSolrDate(Calendar cal)
    {
        if (cal != null)
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
            return dateFormat.format(cal.getTime()) + "Z";
        }
        else
        {
            return null;
        }
    }

    public static String getSolrServerUrl(SolrServerConfiguration solrConfigurationService)
    {
        String protocol = solrConfigurationService.getSolrProtocol();
        String serverName = solrConfigurationService.getSolrServerName();
        String serverPort = solrConfigurationService.getSolrServerPort();
        String coreName = solrConfigurationService.getSolrCoreName();
        return protocol + "://" + serverName + ":" + serverPort + "/solr/" + coreName;
    }

    public static void addHitsToResultsListItem(List<Hit> hits, List<ListItem> results, SlingHttpServletRequest request)
    {
        for (Hit hit : hits)
        {
            try
            {
                Resource hitRes = hit.getResource();

                if (hitRes.getResourceType().equals(NT_DAM_ASSET))
                {
                    addAssetToResultsList(results, request, hitRes);
                }
                else if (hitRes.getResourceType().equals(NT_PAGE))
                {
                    addPageToResultsList(results, request, hitRes);
                }
            }
            catch (RepositoryException e)
            {
                LOGGER.error("Unable to retrieve search results for query.", e);
            }
        }
    }
}
