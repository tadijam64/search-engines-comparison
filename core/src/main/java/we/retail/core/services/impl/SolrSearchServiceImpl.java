package we.retail.core.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
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

import we.retail.core.model.AbstractSolrItemModel;
import we.retail.core.model.SolrAssetModel;
import we.retail.core.model.SolrPageModel;
import we.retail.core.services.SolrSearchService;

import static com.day.cq.dam.api.DamConstants.NT_DAM_ASSET;
import static com.day.cq.wcm.api.NameConstants.NT_PAGE;

@Component
public class SolrSearchServiceImpl implements SolrSearchService
{
    private static final Logger LOG = LoggerFactory.getLogger(SolrSearchServiceImpl.class);

    private static final String PROP_SEARCH_ROOT_PAGES = "/content/we-retail";
    private static final String PROP_SEARCH_ROOT_ASSETS = "/content/dam";

    @Reference
    private QueryBuilder queryBuilder;

    /**
     * This method takes session of the resource to perform search in JCR
     *
     * @param session session from servlet's request
     * @return List<AbstractSolrItemModel> with all data to be indexed
     */
    @Override
    public List<AbstractSolrItemModel> crawlContent(Session session)
    {
        Map<String, String> predicatesMap = new HashMap<>();

        predicatesMap.put("group.p.or", "true"); //combine this group with OR
        predicatesMap.put("group.1_group.path", PROP_SEARCH_ROOT_PAGES);
        predicatesMap.put("group.1_group.type", NT_PAGE);
        predicatesMap.put("group.2_group.path", PROP_SEARCH_ROOT_ASSETS);
        predicatesMap.put("group.2_group.type", NT_DAM_ASSET);
        predicatesMap.put("p.offset", "0");
        predicatesMap.put("p.limit", "10000");

        try
        {
            PredicateGroup predicates = PredicateConverter.createPredicates(predicatesMap);
            Query query = this.queryBuilder.createQuery(predicates, session);
            SearchResult searchResult = query.getResult();

            LOG.info("Found '{}' matches for query", searchResult.getTotalMatches());

            return createItemsMetadataArray(searchResult);
        }
        catch (RepositoryException e)
        {
            LOG.error("Exception due to", e);
            session.logout();
        }

        return Collections.emptyList();
    }

    /**
     * This method takes search result of content pages and creates a ArrayList of page and asset objects
     * object with properties
     * @param searchResults
     * @return
     * @throws RepositoryException
     */
    @Override
    public List<AbstractSolrItemModel> createItemsMetadataArray(SearchResult searchResults) throws RepositoryException
    {
        List<AbstractSolrItemModel> solrItemList = new ArrayList<>();
        for (Hit hit : searchResults.getHits())
        {
            Resource hitRes = hit.getResource();
            if (hitRes.getResourceType().equals(NT_PAGE))
            {
                SolrPageModel solrPage = hitRes.adaptTo(SolrPageModel.class);
                addItemToArray(solrPage, solrItemList);
            }
            else if (hitRes.getResourceType().equals(NT_DAM_ASSET))
            {
                SolrAssetModel solrAsset = hitRes.adaptTo(SolrAssetModel.class);
                addItemToArray(solrAsset, solrItemList);
            }
        }

        return solrItemList;
    }

    /**
     * This method adds solr objects (pages and assets) to the same list
     * @param solrItem
     * @param solrItemList
     */
    private void addItemToArray(AbstractSolrItemModel solrItem, List<AbstractSolrItemModel> solrItemList)
    {
        if (solrItem != null)
        {
            solrItemList.add(solrItem);
        }
    }

    /**
     * This method connects to the Solr server and indexes page content using Solrj api. This is used by bulk update handler (servlet)
     * @param indexItemData
     * @param server
     * Takes Json array and iterates over each object and index solr
     * @return coolean true if it indexes successfully to solr server, else false.
     * @throws SolrServerException
     * @throws IOException
     */
    @Override
    public boolean indexPagesToSolr(List<AbstractSolrItemModel> indexItemData, HttpSolrClient server) throws SolrServerException, IOException
    {
        if (null != indexItemData)
        {
            for (int i = 0; i < indexItemData.size(); i++)
            {
                SolrInputDocument doc = null;
                if (indexItemData.get(i).getType().equals(NT_PAGE))
                {
                    SolrPageModel pageModel = (SolrPageModel) indexItemData.get(i);
                    doc = createPageSolrDoc(pageModel);
                }
                else if (indexItemData.get(i).getType().equals(NT_DAM_ASSET))
                {
                    SolrAssetModel assetModel = (SolrAssetModel) indexItemData.get(i);
                    doc = createAssetSolrDoc(assetModel);
                }
                server.add(doc);
            }
            server.commit();
            return true;
        }

        return false;
    }

    /**
     * This method gets metadata from page and converts it to Solr page object to create a document
     * @param solrPageModel
     * @return Solr document
     */
    private SolrInputDocument createPageSolrDoc(SolrPageModel solrPageModel)
    {
        SolrInputDocument doc = new SolrInputDocument();

        doc.addField("id", solrPageModel.getId());
        doc.addField("type", solrPageModel.getType());

        doc.addField("jcr_title", solrPageModel.getJcrTitle());
        doc.addField("jcr_created", solrPageModel.getJcrCreated());
        doc.addField("jcr_description", solrPageModel.getJcrDescription());
        doc.addField("cq_lastModified", solrPageModel.getCqLastModified());
        doc.addField("jcr_name", solrPageModel.getJcrName());
        doc.addField("jcr_primaryType", solrPageModel.getJcrPrimaryType());
        doc.addField("sling_resourceType", solrPageModel.getSlingResourceType());
        doc.addField("cqTags", solrPageModel.getCqTags());
        doc.addField("searchDescription", solrPageModel.getSearchDescription());
        doc.addField("pageImportanceRank", solrPageModel.getPageImportanceRank());
        doc.addField("manualCreationDate", solrPageModel.getManualCreationDate());

        return doc;
    }

    /**
     * This method gets metadata from asset and converts it to Solr asset object to create a document
     * @param solrAssetModel
     * @return
     */
    private SolrInputDocument createAssetSolrDoc(SolrAssetModel solrAssetModel)
    {
        SolrInputDocument doc = new SolrInputDocument();

        doc.addField("id", solrAssetModel.getId());
        doc.addField("type", solrAssetModel.getType());

        doc.addField("damAssetId", solrAssetModel.getId());
        doc.addField("jcr_mimeType", solrAssetModel.getJcrMimeType());
        doc.addField("cqName", solrAssetModel.getCqName());
        doc.addField("cqParentPath", solrAssetModel.getCqParentPath());
        doc.addField("damRelativePath", solrAssetModel.getDamRelativePath());
        doc.addField("cqTags_asset", solrAssetModel.getCqTags());
        doc.addField("description", solrAssetModel.getDescription());
        doc.addField("sellingPriotiry", solrAssetModel.getSellingPriority());
        doc.addField("creationDate", solrAssetModel.getCreationDate());

        return doc;
    }
}