package we.retail.core.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.search.PredicateConverter;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.TagConstants;

import we.retail.core.services.EsService;

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
import static we.retail.core.util.SearchHelpers.prepareQuery;

@Component
public class EsServiceImpl implements EsService
{
    private static final Logger LOG = LoggerFactory.getLogger(EsServiceImpl.class);

    private static final String PROP_SEARCH_ID = "id";
    private static final String PROP_SEARCH_TYPE = "type";

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public List<XContentBuilder> crawlContent(Session session) throws IOException, RepositoryException
    {
        Map<String, String> predicatesMap = new HashMap<>();
        prepareQuery(predicatesMap);

        PredicateGroup predicates = PredicateConverter.createPredicates(predicatesMap);
        Query query = this.queryBuilder.createQuery(predicates, session);
        SearchResult searchResults = query.getResult();

        LOG.info("Found '{}' matches in CRXDE for query. Indexing process continues.", searchResults.getTotalMatches());
        return createResultsList(searchResults);
    }

    private List<XContentBuilder> createResultsList(SearchResult searchResults) throws IOException, RepositoryException
    {
        ArrayList<XContentBuilder> builders = new ArrayList<>();

        for (Hit hit : searchResults.getHits())
        {
            Resource resource = hit.getResource();
            if (StringUtils.equals(resource.getResourceType(), NT_PAGE))
            {
                builders.add(createPageObject(resource, hit));
            }
            else if (StringUtils.equals(resource.getResourceType(), NT_DAM_ASSET))
            {
                builders.add(createAssetObject(resource));
            }
        }

        return builders;
    }

    private XContentBuilder createPageObject(Resource resource, Hit hit) throws RepositoryException, IOException
    {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        ValueMap vm = hit.getProperties();

        builder.startObject();
        builder.field(PROP_SEARCH_ID, resource.getPath());
        builder.field(PROP_SEARCH_TYPE, resource.getResourceType());
        builder.field("jcr_title", vm.get(JCR_TITLE));

        addFields(builder, vm, JCR_CREATED, PN_PAGE_LAST_MOD, "manualCreationDate", JCR_DESCRIPTION, JCR_NAME, JCR_PRIMARYTYPE, SLING_RESOURCE_TYPE_PROPERTY, TagConstants.PN_TAGS, "searchDescription", "pageImportanceRank");
        builder.endObject();
        return builder;
    }

    private XContentBuilder createAssetObject(Resource resource) throws IOException
    {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        ValueMap vm = getValueMapFromResource(resource);

        builder.startObject();
        builder.field(PROP_SEARCH_ID, resource.getPath());
        builder.field(PROP_SEARCH_TYPE, resource.getResourceType());
        builder.field("damAssetId", resource.getPath());

        addFields(builder, vm, JcrConstants.JCR_MIMETYPE, DamConstants.PN_NAME, DamConstants.PN_PARENT_PATH, DamConstants.DAM_ASSET_RELATIVE_PATH, TagConstants.PN_TAGS, "description", "sellingPriority", "creationDate");
        builder.endObject();

        return builder;
    }

    private ValueMap getValueMapFromResource(Resource resource)
    {
        Resource resourceChild = resource.getChild(JcrConstants.JCR_CONTENT);

        return resourceChild != null ? resourceChild.getValueMap() : resource.getValueMap();
    }

}