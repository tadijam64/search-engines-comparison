package we.retail.core.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "AEM Elasticsearch Search - Elasticsearch Configuration Service", description = "Service Configuration")
public @interface EsConfigService
{
    @AttributeDefinition(name = "Protocol", description = "Configuration value") //
      String protocolValue() default "http";

    @AttributeDefinition(name = "Elasticsearch Server Name", description = "Server name or IP address") //
      String serverName() default "localhost";

    @AttributeDefinition(name = "Elasticsearch Server Port", description = "Server Port") //
      String serverPort() default "9200";

    @AttributeDefinition(name = "Elasticsearch Second Server Port", description = "Second Server Port") //
      String secondServerPort() default "9201";

    @AttributeDefinition(name = "Elasticsearch Index Name", description = "Core name in Elasticsearch server") //
      String serverIndex() default "gettingstarted";

    @AttributeDefinition(name = "Elasticsearch Index Id", description = "Id of elasticsearch server index") //
      String serverIndexId() default "1";
}