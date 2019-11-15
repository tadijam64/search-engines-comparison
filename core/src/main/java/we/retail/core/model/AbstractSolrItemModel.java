package we.retail.core.model;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Abstract model with all properties that both pages and assets contains
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public abstract class AbstractSolrItemModel
{
    @Self
    protected Resource resource;

    protected String id;
    protected String type;

    public String getId()
    {
        return this.resource.getPath();
    }

    public String getType()
    {
        return this.resource.getResourceType();
    }

}
