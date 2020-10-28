package org.opengroup.osdu.storage.provider.azure;

import com.azure.cosmos.CosmosException;
import com.google.common.collect.Lists;
import org.apache.http.HttpStatus;
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
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.RecordMetadata;
import org.opengroup.osdu.storage.provider.azure.repository.RecordMetadataRepository;
import org.opengroup.osdu.storage.provider.azure.repository.SimpleCosmosStoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class RecordMetadataRepositoryTest {
    private static final String ID_MUST_NOT_BE_NULL = "id must not be null";
    private static final String RECORDS_METADATA_MUST_NOT_BE_NULL= "recordsMetadata must not be null";
    private Acl acl;
    private static final String KIND = "ztenant:source:type:1.0.0";
    private static final String PATH_1 = "kind/id/123";
    private static final String PATH_2 = "kind/id/456";
    private static final String PATH_3 = "kind/id/789";
    private static final String CURSOR = "TestCursor";
    private static final String RECOREDMETADATACOLLECTION = "TestCollection";


    @Mock
    private DpsHeaders dpsHeaders;

    @InjectMocks
    RecordMetadataRepository recordMetadataRepository = new RecordMetadataRepository();

    @Mock
    private SimpleCosmosStoreRepository repo;
    @Mock
    private CosmosStore operation;

    @Mock
    private Page page;


    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void createOrUpdateTest() {
        when(repo.save(anyObject(), anyString(), anyString(), anyString(), anyString())).thenReturn(this.createRecordMetadata());
        doNothing().when(repo).upsertItem(anyString(), anyString(), anyString(), anyString(), anyObject());
        List<RecordMetadata> recordMetadata = recordMetadataRepository.createOrUpdate(this.createRecordMetadata());
        assertEquals(recordMetadata.size(), this.createRecordMetadata().size());
    }
    @Test
    public void createOrUpdateTest_shouldThrowExceptionWhenDataIsNull() {

        try{
            recordMetadataRepository.createOrUpdate(null);
        }catch(IllegalArgumentException ex){
            assertEquals(ex.getMessage(),RECORDS_METADATA_MUST_NOT_BE_NULL);
        }

    }

    @Test
    public void getTestWithValidResult() {
        when(repo.getOne(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(this.getRecordMetadataDoc());
        when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.of(this.getRecordMetadataDoc()));
        when(repo.findItem(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.of(this.getRecordMetadataDoc()));
        RecordMetadata data = recordMetadataRepository.get("id1");
        assertEquals(data.getKind(), KIND);
        assertEquals(data.getId(), "id1");

    }

    @Test
    public void getTestWithEmptyResult() {
        when(repo.getOne(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(this.getRecordMetadataDoc());
        when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.empty());
        when(repo.findItem(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        RecordMetadata data = recordMetadataRepository.get("id1");
        assertNull(data);

    }
    @Test
    public void getTest_shouldThrowsException_whenIDIsNull() {
        String id =null;
             try{
                 recordMetadataRepository.get(id);
             }catch(IllegalArgumentException ex){
                 assertEquals(ex.getMessage(),ID_MUST_NOT_BE_NULL);
             }


    }

    @Test
    public void queryByLegalTagNameTest() {
        String compliantTagName = "compliant-test-tag";
        Page<RecordMetadataDoc> docPage = Mockito.mock(PageImpl.class);
        when(repo.paginationQuery(anyObject(), anyObject(), anyObject(), anyString(), anyString(), anyString())).thenReturn(docPage);
        when(repo.queryItemsPage(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyInt(), anyString())).thenReturn(docPage);
        when(operation.queryItemsPage(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyInt(), anyString())).thenReturn(getPageImpl());
        ReflectionTestUtils.setField(recordMetadataRepository, "recordMetadataCollection", RECOREDMETADATACOLLECTION);
        when(repo.find(anyObject(), anyString(), anyString(), anyString(), anyObject())).thenReturn(docPage);

        AbstractMap.SimpleEntry<String, List<RecordMetadata>> results = recordMetadataRepository.queryByLegalTagName(compliantTagName, 100, CURSOR);
        assertNotNull(results);
        assertEquals(results.getKey(), CURSOR);
        assertEquals(results.getValue().size(), this.getRecordMetadataDocList().size());


    }


    @Test
    public void queryByLegalTagNameTest_shouldThrowException_WhenPageSizeLessThanZero() {
        String compliantTagName = "compliant-test-tag";
      try {
          recordMetadataRepository.queryByLegalTagName(compliantTagName, 0, CURSOR);
      }catch (IllegalArgumentException ex)
      {
          assertEquals(ex.getMessage(),"Page size must not be less than one!");
      }
    }

    @Test(expected = CosmosException.class)
    public void queryByLegalTagNameTestThrowsException() {
        String compliantTagName = "compliant-test-tag";
        Page<RecordMetadataDoc> docPage = Mockito.mock(PageImpl.class);
        when(repo.paginationQuery(anyObject(), anyObject(), anyObject(), anyString(), anyString(), anyString())).thenReturn(docPage);
        when(repo.queryItemsPage(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyInt(), anyString())).thenReturn(docPage);
        when(operation.queryItemsPage(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyInt(), anyString())).thenThrow(CosmosException.class);
        ReflectionTestUtils.setField(recordMetadataRepository, "recordMetadataCollection", RECOREDMETADATACOLLECTION);
        when(repo.find(anyObject(), anyString(), anyString(), anyString(), anyObject())).thenThrow(CosmosException.class);

        recordMetadataRepository.queryByLegalTagName(compliantTagName, 10, CURSOR);

    }

    @Test(expected = AppException.class)
    public void queryByLegalTagNameTestThrowsAppException() {
        String compliantTagName = "compliant-test-tag";
        Page<RecordMetadataDoc> docPage = Mockito.mock(PageImpl.class);
        when(repo.paginationQuery(anyObject(), anyObject(), anyObject(), anyString(), anyString(), anyString())).thenReturn(docPage);
        when(repo.queryItemsPage(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyInt(), anyString())).thenReturn(docPage);
        CosmosException ex = Mockito.mock(CosmosException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(ex.getMessage()).thenReturn("INVALID JSON in continuation token");
        when(operation.queryItemsPage(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyInt(), anyString())).thenThrow(ex);
        ReflectionTestUtils.setField(recordMetadataRepository, "recordMetadataCollection", RECOREDMETADATACOLLECTION);
        when(repo.find(anyObject(), anyString(), anyString(), anyString(), anyObject())).thenThrow(CosmosException.class);

        recordMetadataRepository.queryByLegalTagName(compliantTagName, 10, CURSOR);

    }


    @Test
    public void getList() {

        List<String> recordsId = new ArrayList<>();
        recordsId.add("id1");
        recordsId.add("id2");
        recordsId.add("id3");
        when(repo.getOne(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(this.getRecordMetadataDoc());
        when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.of(this.getRecordMetadataDoc()));
        when(repo.findItem(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.of(this.getRecordMetadataDoc()));
        Map<String, RecordMetadata> list = this.recordMetadataRepository.get(recordsId);
        assertNotNull(list);
        assertEquals(list.size(), recordsId.size());

    }

    @Test
    public void findByMetadata_kindAndMetadata_statusTest() {
        when(operation.queryItems(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyObject())).thenReturn(Collections.singletonList(this.getRecordMetadataDocList()));
        when(repo.queryItems(anyString(), anyString(), anyString(), anyObject(), anyObject())).thenReturn(this.getRecordMetadataDocList());
        List<RecordMetadataDoc> records = recordMetadataRepository.findByMetadata_kindAndMetadata_status("TestKind", "active");
        assertNotNull(records);
    }
    @Test
    public void findByMetadata_kindAndMetadata_status_shouldThrowsException_WhenKindIsNull() {

      try {
          recordMetadataRepository.findByMetadata_kindAndMetadata_status(null, "active");
      }catch(IllegalArgumentException ex)
      {
          assertEquals(ex.getMessage(),"kind must not be null");
      }
    }
    @Test
    public void findByMetadata_kindAndMetadata_status_shouldThrowsException_WhenStatusIsNull() {

        try {
            recordMetadataRepository.findByMetadata_kindAndMetadata_status("TestKind", null);
        }catch(IllegalArgumentException ex)
        {
            assertEquals(ex.getMessage(),"status must not be null");
        }
    }
    @Test
    public void findByMetadata_kindAndMetadata_statusPageableTest() {
        String compliantTagName = "compliant-test-tag";
        Page<RecordMetadataDoc> docPage = Mockito.mock(PageImpl.class);
        when(repo.paginationQuery(anyObject(), anyObject(), anyObject(), anyString(), anyString(), anyString())).thenReturn(docPage);
        when(repo.queryItemsPage(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyInt(), anyString())).thenReturn(docPage);
        when(operation.queryItemsPage(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyInt(), anyString())).thenReturn(getPageImpl());
        ReflectionTestUtils.setField(recordMetadataRepository, "recordMetadataCollection", RECOREDMETADATACOLLECTION);
        when(repo.find(anyObject(), anyString(), anyString(), anyString(), anyObject())).thenReturn(docPage);
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        Page<RecordMetadataDoc> doc = recordMetadataRepository.findByMetadata_kindAndMetadata_status(KIND, "active",
                CosmosStorePageRequest.of(0, 10, "test", sort));
        assertNotNull(doc);

    }

    @Test
    public void deletetest() {
        doNothing().when(repo).deleteItem(anyString(), anyString(), anyString(), anyString(), anyString());
        doNothing().when(operation).deleteItem(anyString(), anyString(), anyString(), anyString(), anyString());
        recordMetadataRepository.delete("id1");

    }
    @Test
    public void deleteTest_shouldThrowException_whenIdIsNull() {
       try{
           recordMetadataRepository.delete(null);
       }catch(IllegalArgumentException ex)
       {
        assertEquals(ex.getMessage(),ID_MUST_NOT_BE_NULL);
       }

    }

    private PageImpl getPageImpl() {
        CosmosStorePageRequest pageRequest = CosmosStorePageRequest.of(1, 10, CURSOR);
        List<String> results = new ArrayList();
        return new PageImpl(this.getRecordMetadataDocList(), pageRequest, 1999L);

    }

    private List<RecordMetadataDoc> getRecordMetadataDocList() {
        RecordMetadataDoc doc1 = new RecordMetadataDoc();
        RecordMetadata metadata = new RecordMetadata();
        metadata.setId("id1");
        metadata.setKind(KIND);
        metadata.setAcl(this.acl);
        metadata.setGcsVersionPaths(Lists.newArrayList(PATH_1));

        doc1.setId("ID1");
        doc1.setMetadata(metadata);

        RecordMetadataDoc doc2 = new RecordMetadataDoc();
        RecordMetadata metadata2 = new RecordMetadata();
        metadata.setId("id2");
        metadata.setKind(KIND);
        metadata.setAcl(this.acl);
        metadata.setGcsVersionPaths(Lists.newArrayList(PATH_2));

        doc2.setId("ID2");
        doc2.setMetadata(metadata2);
        RecordMetadataDoc doc3 = new RecordMetadataDoc();
        RecordMetadata metadata3 = new RecordMetadata();
        metadata.setId("id2");
        metadata.setKind(KIND);
        metadata.setAcl(this.acl);
        metadata.setGcsVersionPaths(Lists.newArrayList(PATH_3));

        doc2.setId("ID3");
        doc2.setMetadata(metadata2);
        List<RecordMetadataDoc> recordMetadataDocList = new ArrayList<>();
        recordMetadataDocList.add(doc1);
        recordMetadataDocList.add(doc2);
        recordMetadataDocList.add(doc3);
        return recordMetadataDocList;

    }

    private RecordMetadataDoc getRecordMetadataDoc() {
        RecordMetadataDoc doc = new RecordMetadataDoc();
        RecordMetadata metadata = new RecordMetadata();
        metadata.setId("id1");
        metadata.setKind(KIND);
        metadata.setAcl(this.acl);
        metadata.setGcsVersionPaths(Lists.newArrayList(PATH_1));

        doc.setId("ID1");
        doc.setMetadata(metadata);
        return doc;
    }

    private List<RecordMetadata> createRecordMetadata() {
        RecordMetadata metadata1 = new RecordMetadata();
        metadata1.setId("id1");
        metadata1.setKind(KIND);
        metadata1.setAcl(this.acl);
        metadata1.setGcsVersionPaths(Lists.newArrayList(PATH_1));

        RecordMetadata metadata2 = new RecordMetadata();
        metadata2.setId("id2");
        metadata2.setKind(KIND);
        metadata2.setAcl(this.acl);
        metadata2.setGcsVersionPaths(Lists.newArrayList(PATH_2));

        RecordMetadata metadata3 = new RecordMetadata();
        metadata3.setId("id3");
        metadata3.setKind(KIND);
        metadata3.setAcl(this.acl);
        metadata3.setGcsVersionPaths(Lists.newArrayList(PATH_3));

        List<RecordMetadata> recordMetadataList = new ArrayList<>();
        recordMetadataList.add(metadata1);
        recordMetadataList.add(metadata2);
        recordMetadataList.add(metadata3);

        return recordMetadataList;

    }
}
