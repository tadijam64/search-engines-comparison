package we.retail.core.vo;

import java.util.Calendar;

import javax.annotation.Nonnull;

import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.dam.api.Asset;

public class AssetListItemImpl implements ListItem
{
    protected SlingHttpServletRequest request;
    protected Asset asset;

    public AssetListItemImpl(@Nonnull SlingHttpServletRequest request, @Nonnull Asset asset)
    {
        this.request = request;
        this.asset = asset;
    }

    @Override
    public String getURL()
    {
        return this.request.getScheme() + "://" + this.request.getServerName() + ":" + this.request.getServerPort() + "/assetdetails.html" + this.asset.getPath();
    }

    @Override
    public Calendar getLastModified()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(this.asset.getLastModified());

        return cal;
    }

    @Override
    public String getTitle()
    {
        String title = this.asset.getName();
        if (title == null)
        {
            title = this.asset.getPath();
        }

        return title;
    }

    @Override
    public String getDescription()
    {
        return "Asset: " + this.asset.getName() + "path: " + this.asset.getPath();
    }

    @Override
    public String getPath()
    {
        return this.asset.getPath();
    }

}
