package org.opengroup.osdu.storage.provider.mongodb.config;

import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.mongodb.StorageContext;
import org.opengroup.osdu.core.mongodb.config.MongoConfig;
import org.opengroup.osdu.core.mongodb.helper.QueryHelper;
import org.opengroup.osdu.core.mongodb.helper.RecordHelper;
import org.opengroup.osdu.core.mongodb.helper.SchemaHelper;
import org.opengroup.osdu.storage.provider.mongodb.QueryRepositoryImplMongoDB;
import org.opengroup.osdu.storage.provider.mongodb.RecordsMetadataRepositoryImplMongoDB;
import org.opengroup.osdu.storage.provider.mongodb.SchemaRepositoryImplMongoDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@Import({MongoConfig.class, StorageContext.class})
@ContextConfiguration
public class StorageTestAppContext {

    @Bean
    public DpsHeaders dpsHeaders() {
        DpsHeaders dpsHeaders = Mockito.mock(DpsHeaders.class);
        Mockito.when(dpsHeaders.getUserEmail()).thenReturn(TestVars.USER);
        Mockito.when(dpsHeaders.getPartitionId()).thenReturn(TestVars.TENANT);
        return dpsHeaders;
    }

    @Autowired
    @Bean
    public QueryRepositoryImplMongoDB queryRepo(DpsHeaders dpsHeaders, QueryHelper queryHelper) {
        return new QueryRepositoryImplMongoDB(dpsHeaders, queryHelper);
    }

    @Autowired
    @Bean
    public RecordsMetadataRepositoryImplMongoDB recordsMetadataRepositoryImplMongoDB(RecordHelper recordHelper) {
        return new RecordsMetadataRepositoryImplMongoDB(recordHelper);
    }

    @Autowired
    @Bean
    public SchemaRepositoryImplMongoDB schemaRepositoryImplMongoDB(DpsHeaders dpsHeaders, SchemaHelper schemaHelper) {
        return new SchemaRepositoryImplMongoDB(dpsHeaders, schemaHelper);
    }

}
