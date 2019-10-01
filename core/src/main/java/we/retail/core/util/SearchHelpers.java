package we.retail.core.util;

import java.util.HashMap;

import javax.jcr.RangeIterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

public class SearchHelpers
{
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
                RangeIterator liveCopiesIterator = null;
                try
                {
                    liveCopiesIterator = relationshipManager.getLiveRelationships(currentPage.adaptTo(Resource.class), null, null);
                }
                catch (WCMException e)
                {
                    // ignore it
                }
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
                    while (liveCopiesIterator.hasNext())
                    {
                        LiveRelationship relationship = (LiveRelationship) liveCopiesIterator.next();
                        if (currentPage.getPath().startsWith(relationship.getTargetPath() + "/"))
                        {
                            Page liveCopySearchRoot = pageManager.getPage(relationship.getTargetPath());
                            if (liveCopySearchRoot != null)
                            {
                                rootPage = liveCopySearchRoot;
                                break;
                            }
                        }
                    }
                }
                searchRootPagePath = rootPage.getPath();
            }
        }
        return searchRootPagePath;
    }

    @Nullable
    private static String getRelativePath(@NotNull Page root, @NotNull Page child)
    {
        if (child.equals(root))
        {
            return ".";
        }
        else if ((child.getPath() + "/").startsWith(root.getPath()))
        {
            return child.getPath().substring(root.getPath().length() + 1);
        }
        return null;
    }

    public static ValueMap getContentPolicyProperties(Resource searchResource, Resource requestedResource)
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

    public static Asset getAsset(Resource resource)
    {
        return DamUtil.resolveToAsset(resource);
    }

    public static Page getPage(Resource resource)
    {
        ResourceResolver resourceResolver = resource.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        if (pageManager != null)
        {
            return pageManager.getContainingPage(resource);
        }

        return null;
    }
}
