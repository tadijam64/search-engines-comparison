package we.retail.core.services.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateConverter;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;

import we.retail.core.services.ElasticsearchService;

import static com.day.cq.commons.jcr.JcrConstants.JCR_CREATED;
import static com.day.cq.commons.jcr.JcrConstants.JCR_DESCRIPTION;
import static com.day.cq.commons.jcr.JcrConstants.JCR_NAME;
import static com.day.cq.commons.jcr.JcrConstants.JCR_PRIMARYTYPE;
import static com.day.cq.commons.jcr.JcrConstants.JCR_TITLE;
import static com.day.cq.dam.api.DamConstants.NT_DAM_ASSET;
import static com.day.cq.wcm.api.NameConstants.NT_PAGE;
import static com.day.cq.wcm.api.NameConstants.PN_PAGE_LAST_MOD;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static we.retail.core.util.ElasticsearchUtils.addFields;

@Component
public class ElasticsearchServiceImpl implements ElasticsearchService
{
    private static final Logger LOG = LoggerFactory.getLogger(SolrSearchServiceImpl.class);

    private static final String PROP_SEARCH_ROOT_PAGES = "/content/we-retail";
    private static final String PROP_SEARCH_ROOT_ASSETS = "/content/dam";

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public XContentBuilder crawlContent(Session session) throws IOException, RepositoryException
    {
        Map<String, String> predicatesMap = new HashMap<>();

        predicatesMap.put("group.p.or", "true"); //combine this group with OR
        predicatesMap.put("group.1_group.path", PROP_SEARCH_ROOT_PAGES);
        predicatesMap.put("group.1_group.type", NT_PAGE);
        predicatesMap.put("group.2_group.path", PROP_SEARCH_ROOT_ASSETS);
        predicatesMap.put("group.2_group.type", NT_DAM_ASSET);
        predicatesMap.put("p.offset", "0");
        predicatesMap.put("p.limit", "10000");

        PredicateGroup predicates = PredicateConverter.createPredicates(predicatesMap);
        Query query = this.queryBuilder.createQuery(predicates, session);
        SearchResult searchResults = query.getResult();

        LOG.info("Found '{}' matches for query", searchResults.getTotalMatches());

        return createResultsMap(searchResults);
    }

    private XContentBuilder createResultsMap(SearchResult searchResults) throws IOException, RepositoryException
    {
        XContentBuilder builder = builder = XContentFactory.jsonBuilder();

        for (Hit hit : searchResults.getHits())
        {
            Resource resource = hit.getResource();
            if (StringUtils.equals(resource.getResourceType(), NT_PAGE))
            {
                createPageObject(resource, builder, hit);
            }
            else if (StringUtils.equals(resource.getResourceType(), NT_DAM_ASSET))
            {
                createAssetObject(resource, builder, hit);
            }
        }

        return builder;
    }

    private void createPageObject(Resource resource, XContentBuilder builder, Hit hit) throws RepositoryException, IOException
    {
        ValueMap vm = hit.getProperties();

        builder.startObject();
        builder.field("id", resource.getPath());
        builder.field("type", resource.getResourceType());
        builder.field("jcr_title", vm.get(JCR_TITLE));

        addFields(builder, vm, JCR_CREATED, PN_PAGE_LAST_MOD, "manualCreationDate", JCR_DESCRIPTION, JCR_NAME, JCR_PRIMARYTYPE, SLING_RESOURCE_TYPE_PROPERTY, "cqTags", "searchDescription", "pageImportanceRank");
        builder.endObject();
    }

    private void createAssetObject(Resource resource, XContentBuilder builder, Hit hit) throws RepositoryException, IOException
    {
        ValueMap vm = hit.getProperties();
        builder = XContentFactory.jsonBuilder();

        builder.startObject();
        builder.field("id", resource.getPath());
        builder.field("type", resource.getResourceType());
        builder.field("damAssetId", resource.getPath());

        addFields(builder, vm, "jcr:mimeType", "cq:name", "cq:parentPath", "dam:relativePath", "cq:tags", "description", "sellingPriotiry", "creationDate");
        builder.endObject();
    }

}