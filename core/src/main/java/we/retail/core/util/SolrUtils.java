package we.retail.core.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.solr.client.solrj.SolrQuery;
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
    private static final String PROP_SEARCH_ASSETS = "assets";
    private static final String PROP_SOLR_QUERY_FULLTEXT = "text:";
    private static final String PROP_SEARCH_PAGES = "pages";
    private static final String PROP_SEARCH_TAGS = "tags";
    private static final String PROP_SEARCH_TYPE = "type";
    private static final String PROP_SEARCH_TAGS_ON_PAGES = "cqTags";
    private static final String PROP_SEARCH_TAGS_ON_ASSETS = "cqTags_asset";

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

    /**
     * This method creates solr server url from AEM configuration
     * @param solrConfigurationService
     * @return
     */
    public static String getSolrServerUrl(SolrServerConfiguration solrConfigurationService)
    {
        String protocol = solrConfigurationService.getSolrProtocol();
        String serverName = solrConfigurationService.getSolrServerName();
        String serverPort = solrConfigurationService.getSolrServerPort();
        String coreName = solrConfigurationService.getSolrCoreName();
        return protocol + "://" + serverName + ":" + serverPort + "/solr/" + coreName;
    }

    /**
     * This method adds different type of results as the same object in result list
     * @param hits
     * @param results
     * @param request
     */
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

    /**
     * This method takes searchContentType to add different query parameters
     * @param searchContentType
     * @param query
     * @param fulltext
     */
    public static void addPredicatesToQuery(String searchContentType, SolrQuery query, String fulltext)
    {
        if (searchContentType.equalsIgnoreCase(PROP_SEARCH_ASSETS))
        {
            query.setFilterQueries(PROP_SEARCH_TYPE + ":\"" + NT_DAM_ASSET + "\"");
        }
        else if (searchContentType.equalsIgnoreCase(PROP_SEARCH_PAGES))
        {
            query.setFilterQueries(PROP_SEARCH_TYPE + ":\"" + NT_PAGE + "\"");
        }

        if (searchContentType.equalsIgnoreCase(PROP_SEARCH_TAGS))
        {
            query.setQuery("(" + PROP_SEARCH_TAGS_ON_PAGES + ":*" + fulltext + ") OR (" + PROP_SEARCH_TAGS_ON_ASSETS + ":*" + fulltext + ")");
        }
        else
        {
            query.setQuery(PROP_SOLR_QUERY_FULLTEXT + fulltext);
        }
        query.setStart(0);
        query.setRows(10);
    }
}
