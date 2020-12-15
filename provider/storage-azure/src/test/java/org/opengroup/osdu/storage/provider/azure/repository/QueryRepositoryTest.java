package org.opengroup.osdu.storage.provider.azure.repository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.query.CosmosStorePageRequest;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.storage.*;
import org.opengroup.osdu.storage.provider.azure.RecordMetadataDoc;
import org.opengroup.osdu.storage.provider.azure.SchemaDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueryRepositoryTest {

    private static final String KIND1 = "ztenant:source:type:1.0.0";
    private static final String KIND2 = "atenant:source:type:1.0.0";
    private static final Sort SORT = Sort.by(Sort.Direction.ASC, "kind");
    private static final Integer limit = 10;
    private static final String cursor = "cursor";

    @Mock
    private RecordMetadataRepository record;

    @Mock
    private SchemaRepository dbSchema;

    @Mock
    private JaxRsDpsLog logger;

    @InjectMocks
    private QueryRepository sut;

    @Test
    public void testGetAllKindsNoRecords() {
        // No records found
        ArgumentCaptor<Sort> sortArgumentCaptor = ArgumentCaptor.forClass(Sort.class);
        List<SchemaDoc> schemaDocs = new ArrayList<>();
        when(dbSchema.findAll(sortArgumentCaptor.capture())).thenReturn(schemaDocs);
        DatastoreQueryResult datastoreQueryResult = sut.getAllKinds(null, null);
        assertEquals(datastoreQueryResult.getResults(), schemaDocs);
        assertEquals(sortArgumentCaptor.getValue(), SORT);
    }

    @Test
    public void testGetAllKindsOneRecord() {
        ArgumentCaptor<Sort> sortArgumentCaptor = ArgumentCaptor.forClass(Sort.class);
        List<SchemaDoc> schemaDocs = new ArrayList<>();
        schemaDocs.add(getSchemaDoc(KIND1));
        when(dbSchema.findAll(sortArgumentCaptor.capture())).thenReturn(schemaDocs);
        DatastoreQueryResult datastoreQueryResult = sut.getAllKinds(null, null);
        // Expected one kind
        assertEquals(datastoreQueryResult.getResults().size(), schemaDocs.size());
        assertEquals(sortArgumentCaptor.getValue(), SORT);
    }

    @Test
    public void testGetAllKindsMultipleRecord() {
        ArgumentCaptor<Sort> sortArgumentCaptor = ArgumentCaptor.forClass(Sort.class);
        List<SchemaDoc> schemaDocs = new ArrayList<>();
        schemaDocs.add(getSchemaDoc(KIND2));
        schemaDocs.add(getSchemaDoc(KIND1));
        when(dbSchema.findAll(sortArgumentCaptor.capture())).thenReturn(schemaDocs);
        DatastoreQueryResult datastoreQueryResult = sut.getAllKinds(null, null);
        // expected 2 kinds and they will be sorted ASC by kind name.
        List<String> results = datastoreQueryResult.getResults();
        assertEquals(results.size(), schemaDocs.size());
        assertEquals(results.get(0), KIND2);
        assertEquals(results.get(1), KIND1);
        assertEquals(sortArgumentCaptor.getValue(), SORT);
    }

    @Test
    public void testGetAllKinds() {
        Pageable pageable = mock(Pageable.class);
        Page<RecordMetadataDoc> docPage = mock(Page.class);
        SchemaDoc schemaDoc = mock(SchemaDoc.class);
        List<SchemaDoc> docs = Arrays.asList(schemaDoc);

        doReturn(docPage).when(dbSchema).findAll(any(CosmosStorePageRequest.class));
        doReturn(KIND1).when(schemaDoc).getKind();
        doReturn(pageable).when(docPage).getPageable();
        doReturn(docs).when(docPage).getContent();

        DatastoreQueryResult datastoreQueryResult = sut.getAllKinds(limit, cursor);

        Sort sort = Sort.by(Sort.Direction.ASC, "kind");
        CosmosStorePageRequest cosmosStorePageRequest = CosmosStorePageRequest.of(0, limit, cursor, sort);
        ArgumentCaptor<CosmosStorePageRequest> cosmosStorePageRequestArgumentCaptor = ArgumentCaptor.forClass(CosmosStorePageRequest.class);
        verify(dbSchema).findAll(cosmosStorePageRequestArgumentCaptor.capture());

        CosmosStorePageRequest obtainedCosmosStorePageRequest = cosmosStorePageRequestArgumentCaptor.getValue();

        assertEquals(cosmosStorePageRequest, obtainedCosmosStorePageRequest);
        assertEquals(0, obtainedCosmosStorePageRequest.getPageNumber());
        assertEquals(cursor, obtainedCosmosStorePageRequest.getRequestContinuation());
        assertEquals(limit.intValue(), obtainedCosmosStorePageRequest.getPageSize());
        assertEquals(sort, obtainedCosmosStorePageRequest.getSort());

        List<String> results = datastoreQueryResult.getResults();
        assertEquals(docs.size(), results.size());
        assertEquals(KIND1, results.get(0));
    }

    @Test
    public void testGetAllRecordIdsFromKind() {
        Pageable pageable = mock(Pageable.class);
        Page<RecordMetadataDoc> docPage = mock(Page.class);
        RecordMetadataDoc recordMetadataDoc = mock(RecordMetadataDoc.class);
        List<RecordMetadataDoc> recordMetadataDocs = Arrays.asList(recordMetadataDoc);
        String status = RecordState.active.toString();
        String id = "id";

        doReturn(id).when(recordMetadataDoc).getId();
        doReturn(docPage).when(record).findByMetadata_kindAndMetadata_status(eq(KIND1), eq(status), any());
        doReturn(pageable).when(docPage).getPageable();
        doReturn(recordMetadataDocs).when(docPage).getContent();

        DatastoreQueryResult datastoreQueryResult = sut.getAllRecordIdsFromKind(KIND1, limit, cursor);

        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        CosmosStorePageRequest cosmosStorePageRequest = CosmosStorePageRequest.of(0, limit, cursor, sort);

        ArgumentCaptor<CosmosStorePageRequest> cosmosStorePageRequestArgumentCaptor = ArgumentCaptor.forClass(CosmosStorePageRequest.class);
        verify(record).findByMetadata_kindAndMetadata_status(eq(KIND1), eq(status), cosmosStorePageRequestArgumentCaptor.capture());

        CosmosStorePageRequest obtainedCosmosStorePageRequest = cosmosStorePageRequestArgumentCaptor.getValue();

        assertEquals(cosmosStorePageRequest, obtainedCosmosStorePageRequest);
        assertEquals(0, obtainedCosmosStorePageRequest.getPageNumber());
        assertEquals(cursor, obtainedCosmosStorePageRequest.getRequestContinuation());
        assertEquals(limit.intValue(), obtainedCosmosStorePageRequest.getPageSize());
        assertEquals(sort, obtainedCosmosStorePageRequest.getSort());

        List<String> results = datastoreQueryResult.getResults();
        assertEquals(recordMetadataDocs.size(), results.size());
        assertEquals(id, results.get(0));
    }

    private SchemaDoc getSchemaDoc(String kind) {
        SchemaDoc doc = new SchemaDoc();
        doc.setKind(kind);
        SchemaItem item = new SchemaItem();
        item.setKind(kind);
        item.setPath("schemaPath");
        SchemaItem[] schemaItems = new SchemaItem[1];
        schemaItems[0] = item;
        doc.setSchemaItems(schemaItems);
        return doc;
    }
}
