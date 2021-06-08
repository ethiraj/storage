package org.opengroup.osdu.storage.provider.mongodb.database;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.mongodb.entity.LegalTagAssociationDoc;
import org.opengroup.osdu.core.mongodb.entity.RecordMetadataDoc;
import org.opengroup.osdu.core.mongodb.entity.SchemaDoc;
import org.opengroup.osdu.storage.provider.mongodb.SchemaRepositoryImplMongoDB;
import org.opengroup.osdu.storage.provider.mongodb.config.MongoEmbeddedConfig;
import org.opengroup.osdu.storage.provider.mongodb.config.StorageTestAppContext;
import org.opengroup.osdu.storage.provider.mongodb.util.SchemaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opengroup.osdu.storage.provider.mongodb.config.TestVars.USER;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {StorageTestAppContext.class})
public class SchemaRepositoryMongoDBTest {

    private static final String KIND = "unit-test-storage:test:example:1.2.3";

    @Autowired
    private SchemaRepositoryImplMongoDB repo;
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
    public void addAndGetSchema() {
        Schema schema = SchemaGenerator.one(KIND);
        repo.add(schema, USER);
        Schema resultSchema = repo.get(KIND);
        assertThat(schema).isEqualTo(resultSchema);
    }

    @Test
    public void addGetDeleteWorkflow() {
        Schema schema = SchemaGenerator.one(KIND);
        repo.add(schema, USER);
        Schema firstGet = repo.get(KIND);
        repo.delete(KIND);
        Schema lastGet = repo.get(KIND);

        assertThat(firstGet).isEqualTo(schema);
        assertThat(lastGet).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void errorOnDuplication() {
        Schema schema = SchemaGenerator.one(KIND);
        repo.add(schema, USER);
        repo.add(schema, USER);
    }
}
