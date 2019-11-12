package we.retail.core.util;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

import static com.day.cq.dam.api.DamConstants.NT_DAM_ASSET;
import static com.day.cq.wcm.api.NameConstants.NT_PAGE;
import static we.retail.core.util.SearchHelpers.addAssetToResultsList;
import static we.retail.core.util.SearchHelpers.addPageToResultsList;

public class LuceneUtils
{
    private static final String PREDICATE_TYPE = "type";
    private static final String PREDICATE_PATH = "path";
    private static final String PREDICATE_TAG_ID = "tagid";
    public static final String PREDICATE_FULLTEXT = "fulltext";
    private static final String PARAM_RESULTS_OFFSET = "resultsOffset";
    private static final String PROP_SEARCH_ASSETS = "assets";
    private static final String PROP_SEARCH_ROOT_ASSETS = "/content/dam/we-retail";
    private static final String PROP_SEARCH_PAGES = "pages";
    private static final String PROP_SEARCH_CONTENT_TYPE = "searchContent";
    private static final String PROP_SEARCH_TAGS = "tags";

    private LuceneUtils()
    {
    }

    public static void addQueryConditions(Map<String, String> predicatesMap, String propRootPath, String propItemType)
    {
        predicatesMap.put(PREDICATE_PATH, propRootPath);
        predicatesMap.put(PREDICATE_TYPE, propItemType);
    }

    public static void addTagQueryConditions(SlingHttpServletRequest request, Map<String, String> predicatesMap)
    {
        String fulltext = request.getParameter(PREDICATE_FULLTEXT);

        Tag[] tags = getTagsFromRequest(request, fulltext);
        predicatesMap.clear();

        if (tags != null && tags.length >= 1)
        {
            predicatesMap.put(PREDICATE_TAG_ID, tags[0].getTagID());
        }
        else
        {
            predicatesMap.put(PREDICATE_TAG_ID, fulltext);
        }

        predicatesMap.put(PREDICATE_TAG_ID + ".property", "jcr:content/cq:tags");
    }

    private static Tag[] getTagsFromRequest(SlingHttpServletRequest request, String fulltext)
    {
        Tag[] results = null;
        TagManager tagManager = request.getResource().getResourceResolver().adaptTo(TagManager.class);

        if (tagManager != null)
        {
            results = tagManager.findTagsByTitle("*" + fulltext, null);
        }

        return results;
    }

    public static void prepareAllContentQuery(String searchRootPagePath, Map<String, String> predicatesMap)
    {
        predicatesMap.put("group.p.or", "true"); //combine these groups with OR operator
        predicatesMap.put("group.1_group.path", searchRootPagePath);
        predicatesMap.put("group.1_group.type", NT_PAGE);
        predicatesMap.put("group.2_group.path", PROP_SEARCH_ROOT_ASSETS);
        predicatesMap.put("group.2_group.type", NT_DAM_ASSET);
    }

    public static void addResultDocsToResultItemList(SolrDocumentList resultDocs, SlingHttpServletRequest request, List<ListItem> results)
    {
        ResourceResolver resourceResolver = request.getResourceResolver();

        for (SolrDocument solrDocument : resultDocs)
        {
            String path = solrDocument.getFieldValue("id").toString();
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

    public static long getResultOffset(SlingHttpServletRequest request)
    {
        long resultsOffset = 0;
        if (request.getParameter(PARAM_RESULTS_OFFSET) != null)
        {
            resultsOffset = Long.parseLong(request.getParameter(PARAM_RESULTS_OFFSET));
        }

        return resultsOffset;
    }

    public static void setQueryPredicates(SlingHttpServletRequest request, Map<String, String> predicatesMap, String fulltext, String searchRootPagePath)
    {
        predicatesMap.put(PREDICATE_FULLTEXT, "*" + fulltext + "*");
        String searchContentType = request.getParameter(PROP_SEARCH_CONTENT_TYPE);

        if (searchContentType.equalsIgnoreCase(PROP_SEARCH_ASSETS))
        {
            addQueryConditions(predicatesMap, PROP_SEARCH_ROOT_ASSETS, NT_DAM_ASSET);
        }
        else if (searchContentType.equalsIgnoreCase(PROP_SEARCH_PAGES))
        {
            addQueryConditions(predicatesMap, searchRootPagePath, NT_PAGE);
        }
        else
        {
            if (searchContentType.equalsIgnoreCase(PROP_SEARCH_TAGS))
            {
                addTagQueryConditions(request, predicatesMap);
            }

            prepareAllContentQuery(searchRootPagePath, predicatesMap);
        }
    }
}
