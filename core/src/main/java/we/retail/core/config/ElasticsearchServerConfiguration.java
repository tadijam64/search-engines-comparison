package we.retail.core.config;

public interface ElasticsearchServerConfiguration
{
    String getElasticsearchProtocol();

    String getElasticsearchServerName();

    String getElasticsearchServerPort();

    String getElasticsearchSecondServerPort();

    String getElasticsearchIndexName();
}