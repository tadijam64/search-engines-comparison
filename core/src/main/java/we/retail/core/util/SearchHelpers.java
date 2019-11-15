package we.retail.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RangeIterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

import we.retail.core.config.ElasticsearchServerConfiguration;
import we.retail.core.vo.AssetListItemImpl;
import we.retail.core.vo.PageListItemImpl;

import static com.day.cq.dam.api.DamConstants.NT_DAM_ASSET;
import static com.day.cq.wcm.api.NameConstants.NT_PAGE;

public class SearchHelpers
{
    private static final String PROP_SEARCH_ROOT_PAGES = "/content/we-retail";
    private static final String PROP_SEARCH_ROOT_ASSETS = "/content/dam";

    private SearchHelpers()
    {
    }

    /**
     * This method takes current page and search root to get search root page path
     * @param searchRoot
     * @param currentPage
     * @param languageManager
     * @param relationshipManager
     * @return
     */
    public static String getSearchRootPagePath(String searchRoot, Page currentPage, LanguageManager languageManager, LiveRelationshipManager relationshipManager)
    {
        String searchRootPagePath = null;
        PageManager pageManager = currentPage.getPageManager();
        if (StringUtils.isNotEmpty(searchRoot) && pageManager != null)
        {
            Page rootPage = pageManager.getPage(searchRoot);
            if (rootPage != null)
            {
                Page searchRootLanguageRoot = languageManager.getLanguageRoot(rootPage.getContentResource());
                Page currentPageLanguageRoot = languageManager.getLanguageRoot(currentPage.getContentResource());
                RangeIterator liveCopiesIterator = getLiveCopiesIterator(relationshipManager, currentPage);
                if (searchRootLanguageRoot != null && currentPageLanguageRoot != null && !searchRootLanguageRoot.equals(currentPageLanguageRoot))
                {
                    // check if there's a language copy of the search root
                    Page languageCopySearchRoot = pageManager.getPage(ResourceUtil.normalize(currentPageLanguageRoot.getPath() + "/" + getRelativePath(searchRootLanguageRoot, rootPage)));
                    if (languageCopySearchRoot != null)
                    {
                        rootPage = languageCopySearchRoot;
                    }
                }
                else if (liveCopiesIterator != null)
                {
                    rootPage = getCurrentLiveCopyRoot(liveCopiesIterator, currentPage, pageManager, rootPage);
                }
                searchRootPagePath = rootPage.getPath();
            }
        }
        return searchRootPagePath;
    }

    /**
     * This method takes current page to return current live copy root needed for lucene search
     * @param liveCopiesIterator
     * @param currentPage
     * @param pageManager
     * @param rootPage
     * @return
     */
    private static Page getCurrentLiveCopyRoot(RangeIterator liveCopiesIterator, Page currentPage, PageManager pageManager, Page rootPage)
    {
        Page result = rootPage;
        while (liveCopiesIterator.hasNext())
        {
            LiveRelationship relationship = (LiveRelationship) liveCopiesIterator.next();
            if (currentPage.getPath().startsWith(relationship.getTargetPath() + "/"))
            {
                Page liveCopySearchRoot = pageManager.getPage(relationship.getTargetPath());
                if (liveCopySearchRoot != null)
                {
                    result = liveCopySearchRoot;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * This method takes current page and relationship manager to create a live copies iterator
     * @param relationshipManager
     * @param currentPage
     * @return
     */
    private static RangeIterator getLiveCopiesIterator(LiveRelationshipManager relationshipManager, Page currentPage)
    {
        RangeIterator result = null;
        try
        {
            result = relationshipManager.getLiveRelationships(currentPage.adaptTo(Resource.class), null, null);
        }
        catch (WCMException e)
        {
            // ignore it
        }
        return result;
    }

    /**
     * This method takes root page and child page to return their relative path
     * @param root
     * @param child
     * @return
     */
    @Nullable
    private static String getRelativePath(@NotNull Page root, @NotNull Page child)
    {
        String relativePath = null;
        if (child.equals(root))
        {
            relativePath = ".";
        }
        else if ((child.getPath() + "/").startsWith(root.getPath()))
        {
            relativePath = child.getPath().substring(root.getPath().length() + 1);
        }
        return relativePath;
    }

    /**
     * This method takes search resoure to return content policy properties
     * @param searchResource
     * @return
     */
    public static ValueMap getContentPolicyProperties(Resource searchResource)
    {
        ValueMap contentPolicyProperties = new ValueMapDecorator(new HashMap<>());
        ResourceResolver resourceResolver = searchResource.getResourceResolver();
        ContentPolicyManager contentPolicyManager = resourceResolver.adaptTo(ContentPolicyManager.class);
        if (contentPolicyManager != null)
        {
            ContentPolicy policy = contentPolicyManager.getPolicy(searchResource);
            if (policy != null)
            {
                contentPolicyProperties = policy.getProperties();
            }
        }
        return contentPolicyProperties;
    }

    /**
     * This method returns asset from resource
     * @param resource
     * @return
     */
    public static Asset getAsset(Resource resource)
    {
        return DamUtil.resolveToAsset(resource);
    }

    /**
     * This method returns page from resource
     * @param resource
     * @return
     */
    public static Page getPage(Resource resource)
    {
        Page page = null;

        ResourceResolver resourceResolver = resource.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        if (pageManager != null)
        {
            page = pageManager.getContainingPage(resource);
        }

        return page;
    }

    /**
     * This method adds page to result list
     * @param results
     * @param request
     * @param hitRes
     */
    public static void addPageToResultsList(List<ListItem> results, SlingHttpServletRequest request, Resource hitRes)
    {
        Page page = getPage(hitRes);

        if (page != null)
        {
            results.add(new PageListItemImpl(request, page));
        }
    }

    /**
     * This method adds asset to result list
     * @param results
     * @param request
     * @param hitRes
     */
    public static void addAssetToResultsList(List<ListItem> results, SlingHttpServletRequest request, Resource hitRes)
    {
        Asset asset = getAsset(hitRes);

        if (asset != null)
        {
            results.add(new AssetListItemImpl(request, asset));
        }
    }

    /**
     * This method gets an elasticsearch client
     * @param esConfigService
     * @return
     */
    public static RestHighLevelClient getEsClient(ElasticsearchServerConfiguration esConfigService)
    {
        String server = esConfigService.getElasticsearchServerName();
        int port = Integer.parseInt(esConfigService.getElasticsearchServerPort());
        int secondPort = Integer.parseInt(esConfigService.getElasticsearchSecondServerPort());
        String protocol = esConfigService.getElasticsearchProtocol();

        return new RestHighLevelClient(RestClient.builder(new HttpHost(server, port, protocol), new HttpHost(server, secondPort, protocol)));
    }

    /**
     * This method prepares query for multiple condition search (pages and assets)
     * @param predicatesMap
     */
    public static void prepareQuery(Map<String, String> predicatesMap)
    {
        predicatesMap.put("group.p.or", "true"); // combine these groups with OR operator
        predicatesMap.put("group.1_group.path", PROP_SEARCH_ROOT_PAGES);
        predicatesMap.put("group.1_group.type", NT_PAGE);
        predicatesMap.put("group.2_group.path", PROP_SEARCH_ROOT_ASSETS);
        predicatesMap.put("group.2_group.type", NT_DAM_ASSET);
        predicatesMap.put("p.offset", "0");
        predicatesMap.put("p.limit", "10000");
    }
}
