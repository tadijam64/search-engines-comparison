package we.retail.core.config;

public interface SolrServerConfiguration
{
    String getSolrProtocol();

    String getSolrServerName();

    String getSolrServerPort();

    String getSolrCoreName();

    String getContentPagePath();
}
