package org.opengroup.osdu.storage.provider.azure.repository;

import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlQuerySpec;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.storage.provider.azure.DistinctKinds;
import org.opengroup.osdu.storage.provider.azure.di.AzureBootstrapConfig;
import org.opengroup.osdu.storage.provider.azure.di.CosmosContainerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.util.List;

@Repository
public class DistinctKindsRepository extends SimpleCosmosStoreRepository<DistinctKinds> {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private AzureBootstrapConfig azureBootstrapConfig;

    @Autowired
    private CosmosContainerConfig cosmosContainerConfig;

    @Autowired
    private String recordMetadataCollection;

    @Autowired
    private String cosmosDBName;


    public DistinctKindsRepository() {
        super(DistinctKinds.class);
    }


    public List<DistinctKinds> findAllDistinctKinds() {
        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        SqlQuerySpec query = findAllDistinctKinds_Query();
        return this.queryItems(headers.getPartitionId(), cosmosDBName, recordMetadataCollection, query, options);
    }

    public Page<DistinctKinds>  findAllDistinctKinds(Pageable pageable) {
        SqlQuerySpec query = findAllDistinctKinds_Query();
        return  this.find(pageable, headers.getPartitionId(), cosmosDBName, recordMetadataCollection, query);

    }

    private static SqlQuerySpec findAllDistinctKinds_Query() {
        String queryText = String.format("SELECT distinct c.metadata.kind FROM StorageRecord c");
        SqlQuerySpec query = new SqlQuerySpec(queryText);
        return query;
    }

}
