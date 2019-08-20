package we.retail.core.model;

import com.adobe.cq.wcm.core.components.models.ListItem;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

public class PageListItemImpl implements ListItem
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PageListItemImpl.class);

    public static final String PN_REDIRECT_TARGET = "cq:redirectTarget";

    protected SlingHttpServletRequest request;
    protected Page page;

    public PageListItemImpl(@Nonnull SlingHttpServletRequest request, @Nonnull Page page) {
        this.request = request;
        this.page = page;
        Page redirectTarget = getRedirectTarget(page);
        if (redirectTarget != null && !redirectTarget.equals(page)) {
            this.page = redirectTarget;
        }
    }

    @Override
    public String getURL() {
        return getURL(request, page);
    }

    @Nonnull
    public static String getURL(@Nonnull SlingHttpServletRequest request, @Nonnull Page page) {
        String vanityURL = page.getVanityUrl();
        return StringUtils.isEmpty(vanityURL) ? request.getContextPath() + page.getPath() + ".html" : request.getContextPath() + vanityURL;
    }

    @Override
    public String getTitle() {
        String title = page.getNavigationTitle();
        if (title == null) {
            title = page.getPageTitle();
        }
        if (title == null) {
            title = page.getTitle();
        }
        if (title == null) {
            title = page.getName();
        }
        return title;
    }

    @Override
    public String getDescription() {
        return page.getDescription();
    }

    @Override
    public Calendar getLastModified() {
        return page.getLastModified();
    }

    @Override
    public String getPath() {
        return page.getPath();
    }


    private Page getRedirectTarget(@Nonnull Page page) {
        Page result = page;
        String redirectTarget;
        PageManager pageManager = page.getPageManager();
        Set<String> redirectCandidates = new LinkedHashSet<>();
        redirectCandidates.add(page.getPath());
        while (result != null && StringUtils.isNotEmpty((redirectTarget = result.getProperties().get(PN_REDIRECT_TARGET, String.class)))) {
            result = pageManager.getPage(redirectTarget);
            if (result != null) {
                if (!redirectCandidates.add(result.getPath())) {
                    LOGGER.warn("Detected redirect loop for the following pages: {}.", redirectCandidates.toString());
                    break;
                }
            }
        }
        return result;
    }
}
