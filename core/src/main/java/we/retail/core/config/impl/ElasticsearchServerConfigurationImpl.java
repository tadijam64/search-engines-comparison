package we.retail.core.config.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import we.retail.core.config.ElasticsearchServerConfiguration;
import we.retail.core.services.EsConfigService;

/**
 * Configuration impl class.
 * Values can be set using AEM Web Console - OSGi Configuration
 */
@Component(immediate = true, service = ElasticsearchServerConfiguration.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = EsConfigService.class)
public class ElasticsearchServerConfigurationImpl implements ElasticsearchServerConfiguration
{
    private String elasticsearchProtocol;
    private String elasticsearchServerName;
    private String elasticsearchServerPort;
    private String elasticsearchSecondServerPort;
    private String elasticsearchIndexName;

    @Activate
    @Modified
    public void activate(EsConfigService config)
    {
        this.elasticsearchProtocol = config.protocolValue();
        this.elasticsearchServerName = config.serverName();
        this.elasticsearchServerPort = config.serverPort();
        this.elasticsearchSecondServerPort = config.secondServerPort();
        this.elasticsearchIndexName = config.serverIndex();
    }

    @Override
    public String getElasticsearchProtocol()
    {
        return this.elasticsearchProtocol;
    }

    @Override
    public String getElasticsearchServerName()
    {
        return this.elasticsearchServerName;
    }

    @Override
    public String getElasticsearchServerPort()
    {
        return this.elasticsearchServerPort;
    }

    @Override
    public String getElasticsearchSecondServerPort()
    {
        return this.elasticsearchSecondServerPort;
    }

    @Override
    public String getElasticsearchIndexName()
    {
        return this.elasticsearchIndexName;
    }
}