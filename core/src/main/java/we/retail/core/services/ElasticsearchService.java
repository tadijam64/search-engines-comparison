package we.retail.core.services;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.elasticsearch.common.xcontent.XContentBuilder;

public interface ElasticsearchService
{
    XContentBuilder crawlContent(Session session) throws IOException, RepositoryException;
}
