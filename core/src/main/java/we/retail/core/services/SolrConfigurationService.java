package we.retail.core.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "AEM Solr Search - Solr Configuration Service", description = "Service Configuration")
public @interface SolrConfigurationService
{
    @AttributeDefinition(name = "Protocol", description = "Configuration value") //
      String protocolValue() default "http";

    @AttributeDefinition(name = "Solr Server Name", description = "Server name or IP address") //
      String serverName() default "localhost";

    @AttributeDefinition(name = "Solr Server Port", description = "Server Port") //
      String serverPort() default "8983";

    /**
     * This is a collection name in Solr terminology
     */
    @AttributeDefinition(name = "Solr Core Name", description = "Core name in Solr server") //
      String serverCollection() default "gettingstarted";
}
