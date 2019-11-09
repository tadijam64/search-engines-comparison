package we.retail.core.config;

public interface ElasticsearchServerConfiguration
{
    String getElasticsearchIndexId();

    String getElasticsearchProtocol();

    String getElasticsearchServerName();

    String getElasticsearchServerPort();

    String getElasticsearchSecondServerPort();

    String getElasticsearchIndexName();
}