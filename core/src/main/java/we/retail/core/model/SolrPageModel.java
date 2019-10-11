package we.retail.core.model;

import java.util.Calendar;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import we.retail.core.util.SolrUtils;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SolrPageModel extends AbstractSolrItemModel
{
    @Inject
    @Named("jcr:content/jcr:title")
    private String jcrTitle;

    @Inject
    @Named("jcr:content/jcr:created")
    private Calendar jcrCreated;

    @Inject
    @Named("jcr:content/jcr:description")
    private String jcrDescription;

    @Inject
    @Named("jcr:content/cq:lastModified")
    private Calendar cqLastModified;

    @Inject
    @Named("jcr:content/jcr:name")
    private String jcrName;

    @Inject
    @Named("jcr:content/jcr:primaryType")
    private String jcrPrimaryType;

    @Inject
    @Named("jcr:content/sling:resourceType")
    private String slingResourceType;

    @Inject
    @Named("jcr:content/cq:tags")
    private String[] cqTags;

    @Inject
    @Named("jcr:content/searchDescription")
    private String searchDescription;

    @Inject
    @Named("jcr:content/pageImportanceRank")
    private String pageImportanceRank;

    @Inject
    @Named("jcr:content/manualCreationDate")
    private Calendar manualCreationDate;

    public String getJcrTitle()
    {
        return this.jcrTitle;
    }

    public String getJcrCreated()
    {
        return SolrUtils.castToSolrDate(this.jcrCreated);
    }

    public String getJcrDescription()
    {
        return this.jcrDescription;
    }

    public String getCqLastModified()
    {
        return SolrUtils.castToSolrDate(this.cqLastModified);
    }

    public String getJcrName()
    {
        return this.jcrName;
    }

    public String getJcrPrimaryType()
    {
        return this.jcrPrimaryType;
    }

    public String getSlingResourceType()
    {
        return this.slingResourceType;
    }

    public String[] getCqTags()
    {
        return this.cqTags;
    }

    public String getSearchDescription()
    {
        return this.searchDescription;
    }

    public String getPageImportanceRank()
    {
        return this.pageImportanceRank;
    }

    public String getManualCreationDate()
    {
        return SolrUtils.castToSolrDate(this.manualCreationDate);
    }
}
