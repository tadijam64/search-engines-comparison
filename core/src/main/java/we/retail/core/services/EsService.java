package we.retail.core.services;

import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.elasticsearch.common.xcontent.XContentBuilder;

public interface EsService
{
    List<XContentBuilder> crawlContent(Session session) throws IOException, RepositoryException;
}
