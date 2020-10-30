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
import java.util.stream.Collectors;

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
    private static final String ID1 = "Id1";
    private static final String ID2 = "Id2";
    private static final String ID3 = "Id3";

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
        assertEquals(recordMetadata, this.createRecordMetadata());
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
        RecordMetadata data = recordMetadataRepository.get(ID1);
        assertEquals(data.getKind(), KIND);
        assertEquals(data.getId(), ID1);

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
        assertEquals(results.getValue().get(0).getKind(),KIND);

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
        List<String> result = new ArrayList(list.keySet().stream().sorted().collect(Collectors.toList()));
        assertEquals(result,recordsId);
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
        recordMetadataRepository.delete(ID1);

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

    private RecordMetadataDoc getRecordMetadataDoc() {
        RecordMetadataDoc doc = new RecordMetadataDoc();
        doc.setId(ID1);
        doc.setMetadata(getRecordMetadata(ID1,Lists.newArrayList(PATH_1)));
        return doc;
    }

    private List<RecordMetadata> createRecordMetadata() {
        List<RecordMetadata> recordMetadataList = new ArrayList<>();
        recordMetadataList.add(getRecordMetadata(ID1,Lists.newArrayList(PATH_1)));
        recordMetadataList.add(getRecordMetadata(ID2,Lists.newArrayList(PATH_2)));
        recordMetadataList.add(getRecordMetadata(ID3,Lists.newArrayList(PATH_3)));

        return recordMetadataList;

    }
}
