package we.retail.core.model;

import java.util.Calendar;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import we.retail.core.util.SolrUtils;

/**
 * Extension of AbstractSolrItemModel class with page-specific properties
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SolrAssetModel extends AbstractSolrItemModel
{
    @Inject
    @Named("jcr:content/jcr:mimeType")
    private String jcrMimeType;

    @Inject
    @Named("jcr:content/dam:mimeType")
    private String damMimeType;

    @Inject
    @Named("jcr:content/cq:name")
    private String cqName;

    @Inject
    @Named("jcr:content/cq:parentPath")
    private String cqParentPath;

    @Inject
    @Named("jcr:content/dam:relativePath")
    private String damRelativePath;

    @Inject
    @Named("jcr:content/description")
    private String description;

    @Inject
    @Named("jcr:content/sellingPriority")
    private String sellingPriority;

    @Inject
    @Named("jcr:content/creationDate")
    private Calendar creationDate;

    @Inject
    @Named("jcr:content/cq:tags")
    private String[] cqTags;

    /**
     * In this case it is not important which mime type to save.
     * In some other cases these could be two different index fields.
     * @return any mime type available
     */
    public String getJcrMimeType()
    {
        if (this.jcrMimeType == null)
        {
            return this.damMimeType;
        }
        else
        {
            return this.jcrMimeType;
        }
    }

    public String getCqName()
    {
        return this.cqName;
    }

    public String getCqParentPath()
    {
        return this.cqParentPath;
    }

    public String getDamRelativePath()
    {
        return this.damRelativePath;
    }

    public String getDescription()
    {
        return this.description;
    }

    public String getSellingPriority()
    {
        return this.sellingPriority;
    }

    /**
     * @return creation date in solr format
     */
    public String getCreationDate()
    {
        return SolrUtils.castToSolrDate(this.creationDate);
    }

    public String[] getCqTags()
    {
        return this.cqTags;
    }
}
