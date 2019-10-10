package we.retail.core.config.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import we.retail.core.config.SolrServerConfiguration;
import we.retail.core.services.SolrConfigurationService;

@Component(immediate = true, service = SolrServerConfiguration.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = SolrConfigurationService.class)
public class SolrServerConfigurationImpl implements SolrServerConfiguration
{
    private String solrProtocol;
    private String solrServerName;
    private String solrServerPort;
    private String solrCoreName;
    private String contentPagePath;

    @Activate
    @Modified
    public void activate(SolrConfigurationService config)
    {
        //Populate the solrProtocol data member
        this.solrProtocol = config.protocolValue();
        this.solrServerName = config.serverName();
        this.solrServerPort = config.serverPort();
        this.solrCoreName = config.serverCollection();
        this.contentPagePath = config.serverPath();
    }

    @Override
    public String getSolrProtocol()
    {
        return this.solrProtocol;
    }

    @Override
    public String getSolrServerName()
    {
        return this.solrServerName;
    }

    @Override
    public String getSolrServerPort()
    {
        return this.solrServerPort;
    }

    @Override
    public String getSolrCoreName()
    {
        return this.solrCoreName;
    }

    @Override
    public String getContentPagePath()
    {
        return this.contentPagePath;
    }
}
