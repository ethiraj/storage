package org.opengroup.osdu.storage.provider.mongodb.database;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.DatastoreQueryResult;
import org.opengroup.osdu.core.common.model.storage.RecordMetadata;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.mongodb.entity.LegalTagAssociationDoc;
import org.opengroup.osdu.core.mongodb.entity.RecordMetadataDoc;
import org.opengroup.osdu.core.mongodb.entity.SchemaDoc;
import org.opengroup.osdu.storage.provider.mongodb.QueryRepositoryImplMongoDB;
import org.opengroup.osdu.storage.provider.mongodb.RecordsMetadataRepositoryImplMongoDB;
import org.opengroup.osdu.storage.provider.mongodb.SchemaRepositoryImplMongoDB;
import org.opengroup.osdu.storage.provider.mongodb.TestParent;
import org.opengroup.osdu.storage.provider.mongodb.config.MongoEmbeddedConfig;
import org.opengroup.osdu.storage.provider.mongodb.config.StorageTestAppContext;
import org.opengroup.osdu.storage.provider.mongodb.util.RecordGenerator;
import org.opengroup.osdu.storage.provider.mongodb.util.SchemaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opengroup.osdu.storage.provider.mongodb.config.TestVars.TENANT;
import static org.opengroup.osdu.storage.provider.mongodb.config.TestVars.USER;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {StorageTestAppContext.class})
public class QueryRepositoryMongoDBTest extends TestParent {

    private static final String KIND = "unit-test-storage:test:example:1.2.3";

    @Autowired
    protected QueryRepositoryImplMongoDB queryRepo;
    @Autowired
    protected SchemaRepositoryImplMongoDB schemaRepo;
    @Autowired
    protected RecordsMetadataRepositoryImplMongoDB recordsRepo;

    @Autowired
    DpsHeaders dpsHeaders;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeClass
    public static void init() {
        MongoEmbeddedConfig.init();
    }

    @After
    public void cleanup() {
        mongoTemplate.remove(new Query(), RecordMetadataDoc.class);
        mongoTemplate.remove(new Query(), LegalTagAssociationDoc.class);
        mongoTemplate.remove(new Query(), SchemaDoc.class);
    }

    @Test
    public void simpleTest() {
        Mockito.when(dpsHeaders.getPartitionId()).thenReturn(TENANT);
        Mockito.when(dpsHeaders.getUserEmail()).thenReturn(USER);
        Schema schema = SchemaGenerator.one(KIND);
        schemaRepo.add(schema, USER);
        DatastoreQueryResult result = queryRepo.getAllKinds(50, null);
        assertThat(result.getResults()).contains(KIND);
    }

    @Test
    public void testingCursor() {
        Mockito.when(dpsHeaders.getPartitionId()).thenReturn(TENANT);
        Mockito.when(dpsHeaders.getUserEmail()).thenReturn(USER);

        List<Schema> schemas = SchemaGenerator.withIds("test:1", "test:2", "test:3", "test:4");

        schemas.forEach(schema -> schemaRepo.add(schema, USER));

        DatastoreQueryResult result1 = queryRepo.getAllKinds(3, null);
        DatastoreQueryResult result2 = queryRepo.getAllKinds(3, result1.getCursor());

        assertThat(result1.getResults()).size().isEqualTo(3);
        assertThat(result1.getCursor()).isNotNull();
        assertThat(result2.getResults()).size().isEqualTo(schemas.size() - 3);
        assertThat(result1.getResults()).doesNotContainAnyElementsOf(result2.getResults());
        assertThat(result2.getCursor()).isNull();

        List<String> summary = new ArrayList<>();
        summary.addAll(result1.getResults());
        summary.addAll(result2.getResults());

        assertThat(summary).doesNotHaveDuplicates();
        assertThat(schemas.stream().map(Schema::getKind)).containsAll(summary);
    }

    @Test
    public void testGetFromKind() {

        List<RecordMetadata> records = RecordGenerator.withIds(KIND, USER, 1, 2, 3, 4, 5);
        recordsRepo.createOrUpdate(records);
        DatastoreQueryResult result1 = queryRepo.getAllRecordIdsFromKind(KIND, 3, null);
        DatastoreQueryResult result2 = queryRepo.getAllRecordIdsFromKind(KIND, 3, result1.getCursor());
        assertThat(result1.getResults()).size().isEqualTo(3);
        assertThat(result1.getCursor()).isNotNull();
        assertThat(result2.getResults()).size().isEqualTo(records.size() - 3);
        assertThat(result1.getResults()).doesNotContainAnyElementsOf(result2.getResults());
        assertThat(result2.getCursor()).isNull();

        List<String> summary = new ArrayList<>();
        summary.addAll(result1.getResults());
        summary.addAll(result2.getResults());

        assertThat(summary).doesNotHaveDuplicates();
        assertThat(records.stream().map(RecordMetadata::getId)).containsAll(summary);
    }

}
