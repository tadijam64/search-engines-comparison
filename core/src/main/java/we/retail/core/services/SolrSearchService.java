package we.retail.core.services;

import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import com.day.cq.search.result.SearchResult;

import we.retail.core.model.AbstractSolrItemModel;

public interface SolrSearchService
{
    List<AbstractSolrItemModel> crawlContent(Session session) throws RepositoryException;

    List<AbstractSolrItemModel> createItemsMetadataArray(SearchResult results) throws RepositoryException;

    boolean indexPagesToSolr(List<AbstractSolrItemModel> indexPageData, HttpSolrClient server) throws IOException, SolrServerException;
}
