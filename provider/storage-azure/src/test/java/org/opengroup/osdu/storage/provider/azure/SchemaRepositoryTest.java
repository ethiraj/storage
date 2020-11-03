package org.opengroup.osdu.storage.provider.azure;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.azure.query.CosmosStorePageRequest;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.storage.provider.azure.repository.SchemaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class SchemaRepositoryTest {
    private Acl acl;
    private static final String KIND = "ztenant:source:type:1.0.0";
    private static final String PATH_1 = "kind/id/123";
    private static final String CURSOR = "TestCursor";
    private static final String SCHEMAPATH = "SchemaPath";
    private static final String RECOREDMETADATACOLLECTION = "TestCollection";
    private static final String ID_MUST_NOT_BE_NULL = "id must not be null";
    private static final String SCHEMA_MUST_NOT_BE_NULL = "schema must not be null";
    private static final String USER_MUST_NOT_BE_NULL = "user must not be null";
    private static String SORT_OF_FINDALL_SHOULD_NOT_NULL="sort of findAll should not be null";
    private static String PAGE_INDEX_MUST_NOT_BE_LESS_THAN_ZERO="Page index must not be less than zero!";
    @Mock
    private DpsHeaders dpsHeaders;
    @InjectMocks
    SchemaRepository schemaRepository = new SchemaRepository();

    @Mock
    private CosmosStore operation;

    @Before
    public void setUp() {
        initMocks(this);
    }


    @Test
    public void addTest() {
        Schema schema = new Schema();
        schema.setKind("tenant:source:type:1.0.0");
        SchemaItem item = new SchemaItem();
        item.setKind("schemaKind");
        item.setPath("schemaPath");
        SchemaItem[] schemaItems = new SchemaItem[1];
        schemaItems[0] = item;
        schema.setSchema(schemaItems);
        String user = "test-user";
        when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.empty());
        schemaRepository.add(schema, user);
    }

    @Test
    public void addTest_shouldThrowsException_When_SchemaIsNull() {
        String user = "test-user";
        try {
            schemaRepository.add(null, user);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), SCHEMA_MUST_NOT_BE_NULL);
        }

    }

    @Test
    public void addTest_shouldThrowsException_When_UserIsNull() {
        Schema schema = new Schema();
        try {
            schemaRepository.add(schema, null);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), USER_MUST_NOT_BE_NULL);
        }

    }

    @Test
    public void getSchema() {
        Schema expectedSchema = new Schema();
        expectedSchema.setKind(KIND);
        SchemaItem item = new SchemaItem();
        item.setKind(KIND);
        item.setPath(SCHEMAPATH);
        SchemaItem[] schemaItems = new SchemaItem[1];
        schemaItems[0] = item;
        expectedSchema.setSchema(schemaItems);
        String user = "test-user";
        SchemaDoc expectedSd = new SchemaDoc();
        expectedSd.setKind(expectedSchema.getKind());
        expectedSd.setExtension(expectedSchema.getExt());
        expectedSd.setUser(user);
        when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.of(this.getSchemaDocRecord()));
        Schema schema = schemaRepository.get(KIND);
        assertEquals(schema, expectedSchema);
    }

    @Test
    public void getSchemaEmptyTest() {
        when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.empty());
        Schema schema = schemaRepository.get(KIND);
        assertNull(schema);
    }

    @Test
    public void getTest_shouldThrowException_WhenIdIsNull() {
        try {
            schemaRepository.get(null);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), ID_MUST_NOT_BE_NULL);
        }
    }

    @Test
    public void deleteTest() {
        String kind = "tenant:source:type:1.0.0";
        doNothing().when(operation).deleteItem(anyString(), anyString(), anyString(), anyString(), anyString());
        schemaRepository.delete(kind);
    }

    @Test
    public void deleteTest_shouldThrowException_whenIdIsNull() {
        try {
            schemaRepository.delete(null);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), ID_MUST_NOT_BE_NULL);
        }
    }

    @Test
    public void findAllTest() {
        when(operation.queryItems(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyObject())).thenReturn(Collections.singletonList(this.getSchemaDocRecord()));
        Sort sort = Sort.by(Sort.Direction.ASC, KIND);

        Iterable<SchemaDoc> docs = schemaRepository.findAll(sort);
        assertNotNull(docs);
        List<String> kinds = new ArrayList();
        docs.forEach(
                d -> kinds.add(d.getKind()));
        assertTrue(kinds.contains(KIND));
    }

    @Test
    public void findAllTest_shouldThrowException_whenSortIsNull() {

        Sort sort = null;
        try {
            Iterable<SchemaDoc> docs = schemaRepository.findAll(sort);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), SORT_OF_FINDALL_SHOULD_NOT_NULL);
        }
    }

    @Test
    public void findAllPageableTest() {

        when(operation.queryItemsPage(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyInt(), anyString())).thenReturn(getPageImpl());
        ReflectionTestUtils.setField(schemaRepository, "schemaCollection", RECOREDMETADATACOLLECTION);
        Sort sort = Sort.by(Sort.Direction.ASC, KIND);
        Page<SchemaDoc> docPage = schemaRepository.findAll(CosmosStorePageRequest.of(0, 10, "test", sort));
        assertNotNull(docPage);
        assertEquals(this.getSchemaDocRecordsList().size(), docPage.getContent().size());
        assertEquals(docPage.getTotalElements(),1999);

    }

    @Test
    public void findAllPageableTest_shouldThrowException_WhenPageIndexIsNegative() {

        Sort sort = Sort.by(Sort.Direction.ASC, KIND);
        try {
            schemaRepository.findAll(CosmosStorePageRequest.of(-2, 10, "test", sort));
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), PAGE_INDEX_MUST_NOT_BE_LESS_THAN_ZERO);
        }
    }

    @Test
    public void findAllPageableTest_shouldThrowException_WhenCollectionIsNull() {

        Sort sort = Sort.by(Sort.Direction.ASC, KIND);
        try {
            schemaRepository.findAll(CosmosStorePageRequest.of(1, 4, "test", sort));
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "collection should not be null, empty or only whitespaces");
        }
    }

    private PageImpl getPageImpl() {
        CosmosStorePageRequest pageRequest = CosmosStorePageRequest.of(1, 10, CURSOR);
        return new PageImpl(this.getSchemaDocRecordsList(), pageRequest, 1999L);

    }

    private SchemaDoc getSchemaDocRecord() {
        SchemaDoc doc = new SchemaDoc();
        SchemaItem item = new SchemaItem();
        item.setKind(KIND);
        item.setPath(SCHEMAPATH);
        SchemaItem[] schemaItems = new SchemaItem[1];
        schemaItems[0] = item;
        doc.setSchemaItems(schemaItems);
        doc.setKind(KIND);


        return doc;
    }

    private List<SchemaDoc> getSchemaDocRecordsList() {
        List<SchemaDoc> docs = new ArrayList<>();
        docs.add(this.getSchemaDocRecord());
        return docs;
    }


}
