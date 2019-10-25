package we.retail.core.util;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchUtils.class);

    public static void writeResponse(IndexResponse indexResponse, SlingHttpServletResponse response)
    {
        String index = indexResponse.getIndex();
        String id = indexResponse.getId();
        try
        {
            if (indexResponse.getResult() == DocWriteResponse.Result.CREATED)
            {
                LOG.info("Document is successfully created. Index: {}, id: {}", index, id);
                response.getWriter().write("<h3>Successfully indexed content pages to Elasticsearch server </h3>");
            }
            else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED)
            {
                LOG.info("Document is successfully updated. Index: {}, id: {}", index, id);
                response.getWriter().write("<h3>Successfully indexed content pages to Elasticsearch server </h3>");
            }

            writeShardInfo(indexResponse, response);
        }
        catch (IOException ex)
        {
            LOG.error("Exception while trying to write a site response: {}", ex);
        }
    }

    public static void writeShardInfo(IndexResponse indexResponse, SlingHttpServletResponse response) throws IOException
    {
        ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();

        if (shardInfo.getTotal() != shardInfo.getSuccessful())
        {
            LOG.error("Some shards are not successful. {} shards failed", shardInfo.getFailures());
            response.getWriter().write("<h3>Something went wrong</h3>");
        }
        if (shardInfo.getFailed() > 0)
        {
            for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures())
            {
                String reason = failure.reason();
                LOG.error("Some shards are not successful. {} shards failed. Reason: {}", shardInfo.getFailures(), reason);
                response.getWriter().write("<h3>Something went wrong</h3>");
            }
        }
    }

    public static void writeResponse(DeleteResponse deleteResponse, SlingHttpServletResponse response, String indexName)
    {
        try
        {
            if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND)
            {
                LOG.error("Unable to delete index with the name: {}", indexName);
                response.getWriter().write("<h3> Something went wrong!</h3>");
            }
            else if (deleteResponse.getResult() == DocWriteResponse.Result.DELETED)
            {
                response.getWriter().write("<h3>Index is successfully deleted </h3>");
            }
        }
        catch (IOException ex)
        {
            LOG.error("Exception while trying to write a site response: {}", ex);
        }
    }

    public static void addFields(XContentBuilder builder, ValueMap vm, String... keys) throws IOException
    {
        for (String key : keys)
        {
            if (vm.containsKey(key))
            {
                builder.field(key, vm.get(key).toString());
            }
        }
    }
}
