package org.opengroup.osdu.storage.provider.azure;

import com.azure.cosmos.models.SqlQuerySpec;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.azure.query.CosmosStorePageRequest;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.storage.RecordMetadata;
import org.opengroup.osdu.storage.provider.azure.repository.SimpleCosmosStoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class SimpleCosmosStoreRepositoryTest {
    private Acl acl;
    private static final String KIND = "ztenant:source:type:1.0.0";
    private static final String PATH_1 = "kind/id/123";
    private static final String CURSOR = "TestCursor";
    private static final String PATH_2 = "kind/id/456";
    private static final String PATH_3 = "kind/id/789";
    private static final String DATA_PARTITION_ID = "TESTPARTITION";
    private static final String COSMOS_DB = "TESTCOSMOS";
    private static final String PARTITION_KEY = "TESTPARTITION_KEY";
    private static final String COLLECTION = "TestCollection";
    private static final String PRIMARY_KEY = "TestPrimary";
    private static final String ID_MUST_NOT_BE_NULL = "id must not be null";
    private static final String ENTITY_MUST_NOT_BE_NULL = "entity must not be null";
    private static final String SORT_MUST_NOT_BE_NULL = "sort of findAll should not be null";
    private static final String ID_LIST_SHOULD_NOT_BE_NULL="Id list should not be null";
    private static final String ID1 = "Id1";
    private static final String ID2 = "Id2";
    private static final String ID3 = "Id3";
    @InjectMocks
    SimpleCosmosStoreRepository repository = new SimpleCosmosStoreRepository(RecordMetadataDoc.class);


    @Mock
    private CosmosStore operation;


    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void upsertItemTest() {
        Mockito.doNothing().when(operation).upsertItem(anyString(), anyString(), anyString(), anyString(), anyObject());
        repository.upsertItem(DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY, this.getRecordMetadataDoc());
    }

    @Test
    public void upsertItemTest_shouldThrowIfObjectNull() {
        try {
            repository.upsertItem(DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY, null);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), ENTITY_MUST_NOT_BE_NULL);
        }
    }

    @Test
    public void deleteItemTest() {
        doNothing().when(operation).deleteItem(anyString(), anyString(), anyString(), anyString(), anyString());
        repository.deleteItem(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void deleteItemTest_shouldThrowIfIDNull() {
        try {
            repository.deleteItem(DATA_PARTITION_ID, COSMOS_DB, COLLECTION, null, PARTITION_KEY);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), ID_MUST_NOT_BE_NULL);
        }
    }

    @Test
    public void findAllTest() {
        when(operation.queryItems(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyObject())).thenReturn(Collections.singletonList(this.getRecordMetadataDocList()));
        Sort sort = Sort.by(Sort.Direction.ASC, KIND);
        Iterable<RecordMetadataDoc> list = repository.findAll(sort, PARTITION_KEY, COSMOS_DB, COLLECTION);
        assertNotNull(list);
        assertTrue(Iterables.contains(list, this.getRecordMetadataDocList()));

    }

    @Test
    public void findAllTest_shouldThrowExceptionIfSortNull() {
        try {
            Sort sort = null;
            repository.findAll(sort, PARTITION_KEY, COSMOS_DB, COLLECTION);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), SORT_MUST_NOT_BE_NULL);
        }

    }

    @Test
    public void getOneTest() {
        when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.of(this.getRecordMetadataDocList()));
        Object doc = repository.getOne("ID", DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        assertNotNull(doc);
        assertEquals(doc, this.getRecordMetadataDocList());
    }

    @Test
    public void getOneTest_shouldReturn_NullResult() {
        when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.empty());
        Object doc = repository.getOne("ID", DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        assertNull(doc);


    }

    @Test
    public void findTest() {
        when(operation.queryItems(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyObject())).thenReturn(Collections.singletonList(this.getRecordMetadataDocList()));
        List<RecordMetadataDoc> data = repository.find(DATA_PARTITION_ID, COSMOS_DB, COLLECTION, new SqlQuerySpec());
        assertNotNull(data);
        assertTrue(data.contains(this.getRecordMetadataDocList()));

    }

    @Test(expected = UnsupportedOperationException.class)
    public void findByIdsTest_shouldThrowsException() {
        List<String> list = new ArrayList<>();
        repository.findByIds(list, DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);

    }

    @Test
    public void findByIdsTest_shouldThrowsExceptionIfIdNull() {
        try {
            repository.findByIds(null, DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), ID_LIST_SHOULD_NOT_BE_NULL);
        }


    }

    @Test(expected = UnsupportedOperationException.class)
    public void findAllByIdTest_shouldThrowsException() {
        List<String> list = new ArrayList<>();
        list.add("Test");
        repository.findAllById(list, DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);

    }

    @Test
    public void existsByIdTest() {
        when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.of(this.getRecordMetadataDoc()));
        boolean flag = repository.existsById(PRIMARY_KEY, DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        assertNotNull(flag);
        assertEquals(flag, true);
    }

    @Test
    public void existsByIdTest_shouldReturnFalse_IfRecordNotExists() {
        when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.empty());
        boolean flag = repository.existsById(PRIMARY_KEY, DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        assertNotNull(flag);
        assertEquals(flag, false);
    }

    @Test
    public void existsByIdTest_shouldThrowExceptionIfPrimaryKeyNull() {

        try {
            repository.existsById(null, DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "primaryKey should not be null");
        }

    }

    @Test
    public void existsByIdTest_shouldThrowException_IfIDNull() {

        try {
            repository.existsById(PRIMARY_KEY, DATA_PARTITION_ID, COSMOS_DB, null, PARTITION_KEY);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), ID_MUST_NOT_BE_NULL);
        }

    }

    @Test
    public void deleteByIdTest() {
        doNothing().when(operation).deleteItem(anyString(), anyString(), anyString(), anyString(), anyString());
        repository.deleteById(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void deleteByIdTest_shouldThrowExceptionIfIdNull() {
        try {
            repository.deleteById(null, DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), ID_MUST_NOT_BE_NULL);
        }
    }

    @Test
    public void findByIdTest() {
        Mockito.when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.of(this.getRecordMetadataDoc()));
        Optional<RecordMetadataDoc> doc = repository.findById("ID", DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        assertNotNull(doc);
        assertEquals(doc.isPresent(), true);


    }

    @Test(expected = IllegalArgumentException.class)
    public void findByIdTestThrowsException() {
        repository.findById(null, DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
    }

    @Test
    public void findByIdTestReturnsEmpty() {
        Mockito.when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.of(this.getRecordMetadataDoc()));
        Optional<RecordMetadataDoc> doc = repository.findById("", DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        assertNotNull(doc);
        assertEquals(doc, Optional.empty());
    }

    @Test
    public void findAllStringsTest() {
        Mockito.when(operation.findAllItems(anyString(), anyString(), anyString(), anyObject())).thenReturn(Collections.singletonList(this.getRecordMetadataDocList()));
        Iterable<RecordMetadataDoc> docs = repository.findAll(DATA_PARTITION_ID, COSMOS_DB, COLLECTION);
        assertNotNull(docs);
        assertTrue(Iterables.contains(docs, this.getRecordMetadataDocList()));
    }

    @Test
    public void saveTest() {
        Mockito.doNothing().when(operation).upsertItem(anyString(), anyString(), anyString(), anyString(), anyObject());
        RecordMetadataDoc doc = (RecordMetadataDoc) repository.save(getRecordMetadataDoc(), DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        assertNotNull(doc);
        assertEquals(doc, this.getRecordMetadataDoc());
    }

    @Test
    public void saveTest_shouldThrowException_IfEntityNull() {
        try {
            repository.save(null, DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), ENTITY_MUST_NOT_BE_NULL);
        }
    }

    @Test
    public void createItemTest() {
        Mockito.doNothing().when(operation).createItem(anyString(), anyString(), anyString(), anyString(), anyObject());
        repository.createItem(DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY, getRecordMetadataDoc());
    }

    @Test
    public void createItemTest_shouldThrowException_IfEntityNull() {
        try {
            repository.createItem(DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY, null);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), ENTITY_MUST_NOT_BE_NULL);
        }
    }

    @Test
    public void queryItemsPageTest() {
        when(operation.queryItemsPage(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyInt(), anyString())).thenReturn(getPageImpl());
        SqlQuerySpec query = new SqlQuerySpec();
        Page<RecordMetadataDoc> doc = repository.queryItemsPage(DATA_PARTITION_ID, COSMOS_DB, COLLECTION, query, RecordMetadataDoc.class, 10, "token");
        assertNotNull(doc);
        assertEquals(doc.getContent().size(), this.getRecordMetadataDocList().size());
    }

    private PageImpl getPageImpl() {
        CosmosStorePageRequest pageRequest = CosmosStorePageRequest.of(1, 10, CURSOR);

        return new PageImpl(this.getRecordMetadataDocList(), pageRequest, 1999L);

    }

    private RecordMetadataDoc getRecordMetadataDoc() {
        return new RecordMetadataDoc(ID1,getRecordMetadata(ID1,Lists.newArrayList(PATH_1)));

    }
    RecordMetadata getRecordMetadata(String id,List<String> path) {
        RecordMetadata metadata = new RecordMetadata();
        metadata.setId(id);
        metadata.setKind(KIND);
        metadata.setAcl(this.acl);
        metadata.setGcsVersionPaths(path);
        return metadata;
    }
    private List<RecordMetadataDoc> getRecordMetadataDocList() {
        RecordMetadataDoc doc1 = new RecordMetadataDoc(ID1,getRecordMetadata(ID1,Lists.newArrayList(PATH_1)));
        RecordMetadataDoc doc2 = new RecordMetadataDoc(ID2,getRecordMetadata(ID2,Lists.newArrayList(PATH_2)));
        RecordMetadataDoc doc3 = new RecordMetadataDoc(ID3,getRecordMetadata(ID3,Lists.newArrayList(PATH_3)));
        List<RecordMetadataDoc> recordMetadataDocList = new ArrayList<>();
        recordMetadataDocList.add(doc1);
        recordMetadataDocList.add(doc2);
        recordMetadataDocList.add(doc3);
        return recordMetadataDocList;

    }
}
