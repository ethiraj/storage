package org.opengroup.osdu.storage.provider.mongodb.database;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.model.storage.RecordMetadata;
import org.opengroup.osdu.core.mongodb.entity.LegalTagAssociationDoc;
import org.opengroup.osdu.core.mongodb.entity.RecordMetadataDoc;
import org.opengroup.osdu.core.mongodb.entity.SchemaDoc;
import org.opengroup.osdu.storage.provider.mongodb.RecordsMetadataRepositoryImplMongoDB;
import org.opengroup.osdu.storage.provider.mongodb.config.MongoEmbeddedConfig;
import org.opengroup.osdu.storage.provider.mongodb.config.StorageTestAppContext;
import org.opengroup.osdu.storage.provider.mongodb.util.RecordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opengroup.osdu.storage.provider.mongodb.config.TestVars.USER;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {StorageTestAppContext.class})
public class RecordsMetadataRepositoryMongoDBTest {

    private static final String KIND = "unit-test-storage:test:example:1.2.3";

    @Autowired
    private RecordsMetadataRepositoryImplMongoDB repo;
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
    public void insertSomeRecords() {
        RecordMetadata record = RecordGenerator.one(KIND, USER, 1);
        repo.createOrUpdate(Collections.singletonList(record));
        RecordMetadata result = repo.get(record.getId());
        assertThat(result).isEqualTo(record);
    }

    @Test
    public void getByIdAllTheInsertedRecords() {
        List<RecordMetadata> records = RecordGenerator.withIds(KIND, USER, 1, 2, 3, 4, 5, 6);
        repo.createOrUpdate(records);
        List<String> ids = records.stream().map(RecordMetadata::getId).collect(Collectors.toList());
        Map<String, RecordMetadata> result = repo.get(ids);
        assertThat(result.size()).isEqualTo(records.size());
        assertThat(result.entrySet()).noneMatch(entry -> entry.getValue() == null);
        assertThat(ids).containsAll(result.keySet());
        assertThat(records).containsAll(result.values());
    }

    @Test
    public void deleting() {
        List<RecordMetadata> records = RecordGenerator.withIds(KIND, USER, 0, 1, 2, 3);
        repo.createOrUpdate(records);
        repo.delete(records.get(1).getId());
        repo.delete(records.get(2).getId());
        List<String> ids = records.stream().map(RecordMetadata::getId).collect(Collectors.toList());
        Map<String, RecordMetadata> result = repo.get(ids);

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(records.get(0).getId())).isNotNull();
        assertThat(result.get(records.get(1).getId())).isNull();
        assertThat(result.get(records.get(2).getId())).isNull();
        assertThat(result.get(records.get(3).getId())).isNotNull();
    }

    @Test
    public void findByLegalTag() {
        String legalTag = "test-marker";

        List<RecordMetadata> records = RecordGenerator.withIds(KIND, USER, 0, 1, 2, 3, 4, 5);
        records.get(1).getLegal().getLegaltags().add(legalTag);
        records.get(2).getLegal().getLegaltags().add(legalTag);
        records.get(3).getLegal().getLegaltags().add(legalTag);
        repo.createOrUpdate(records);

        AbstractMap.SimpleEntry<String, List<RecordMetadata>> result1 = repo.queryByLegalTagName(legalTag, 2, null);
        AbstractMap.SimpleEntry<String, List<RecordMetadata>> result2 = repo.queryByLegalTagName(legalTag, 2, result1.getKey());

        List<RecordMetadata> total = new LinkedList<>();
        total.addAll(result1.getValue());
        total.addAll(result2.getValue());

        assertThat(result1.getKey()).isNotNull();
        assertThat(result2.getKey()).isNull();

        assertThat(result1.getValue()).size().isEqualTo(2);
        assertThat(result2.getValue()).size().isEqualTo(1);

        assertThat(records).filteredOn(record -> record.getLegal().getLegaltags().contains(legalTag)).containsAll(total);
        assertThat(total).doesNotHaveDuplicates();
    }

}
