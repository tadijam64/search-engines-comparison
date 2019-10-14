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
import static we.retail.core.servlets.SearchServlet.PREDICATE_FULLTEXT;
import static we.retail.core.util.SearchHelpers.addAssetToResultsList;
import static we.retail.core.util.SearchHelpers.addPageToResultsList;
import static we.retail.core.util.SolrUtils.PROP_SEARCH_ROOT_ASSETS;

public class LuceneUtils
{
    private static final String PREDICATE_TYPE = "type";
    private static final String PREDICATE_PATH = "path";
    private static final String PREDICATE_TAG_ID = "tagid";

    private LuceneUtils()
    {
    }

    public static void setPageAndPath(Map<String, String> predicatesMap, String propRootPath, String propItemType)
    {
        predicatesMap.put(PREDICATE_PATH, propRootPath);
        predicatesMap.put(PREDICATE_TYPE, propItemType);
    }

    public static void setPredicatesForTags(SlingHttpServletRequest request, Map<String, String> predicatesMap)
    {
        String fulltext = request.getParameter(PREDICATE_FULLTEXT);

        TagManager tagManager = request.getResource().getResourceResolver().adaptTo(TagManager.class);
        Tag[] tags = null;
        if (tagManager != null)
        {
            tags = tagManager.findTagsByTitle("*" + fulltext, null);
        }
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

    public static void searchAllContent(String searchRootPagePath, Map<String, String> predicatesMap)
    {
        predicatesMap.put("group.p.or", "true"); //combine this group with OR
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
}
