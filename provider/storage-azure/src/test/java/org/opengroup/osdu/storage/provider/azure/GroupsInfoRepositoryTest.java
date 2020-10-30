package org.opengroup.osdu.storage.provider.azure;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.azure.multitenancy.TenantInfoDoc;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.storage.provider.azure.repository.GroupsInfoRepository;
import org.opengroup.osdu.storage.provider.azure.repository.SimpleCosmosStoreRepository;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class GroupsInfoRepositoryTest {

    @InjectMocks
    private GroupsInfoRepository repository = new GroupsInfoRepository();
    @Mock
    private DpsHeaders dpsHeaders;
    @Mock
    private SimpleCosmosStoreRepository repo;
    @Mock
    private CosmosStore operation;
    private static final String ID = "testID";
    private static final String SERVICE_PRINCIPLE_ID = "testServicePrincipleID";
    private static final String TEST_COMPLAINCE = "testComplaince";
    private static final String DATA_PARTITION_ID = "TESTPARTITION";
    private static final String COSMOS_DB = "TESTCOSMOS";
    private static final String PARTITION_KEY = "TESTPARTITION_KEY";
    private static final String COLLECTION = "TestCollection";
    private static final String ID_MUST_NOT_BE_NULL = "id must not be null";
    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void findByIdTestWithValidData() {


        Mockito.when(repo.findById(ID, DATA_PARTITION_ID, COSMOS_DB, COLLECTION, PARTITION_KEY)).thenReturn(Optional.of(this.getTenantInfoDoc()));
        Mockito.when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.of(this.getTenantInfoDoc()));
        Optional<TenantInfoDoc> doc = repository.findById(ID);
        assertNotNull(doc);
        assertEquals(doc.isPresent(), Boolean.TRUE);


    }
    @Test
    public void findByIdTest() {


        Mockito.when(repo.findById(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.of(this.getTenantInfoDoc()));
        Mockito.when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.of(this.getTenantInfoDoc()));
        Optional<TenantInfoDoc> doc = repository.findById(ID);
        assertNotNull(doc);
        assertEquals(doc.isPresent(), Boolean.TRUE);


    }

    @Test
    public void findByIdTestReturnsEmptyResult() {
        Mockito.when(repo.findById(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        Mockito.when(operation.findItem(anyString(), anyString(), anyString(), anyString(), anyString(), anyObject())).thenReturn(Optional.empty());
        Optional<TenantInfoDoc> doc = repository.findById(ID);
        assertNotNull(doc);
        assertEquals(doc.isPresent(), Boolean.FALSE);

    }

    @Test(expected = IllegalArgumentException.class)
    public void findByIdThrowsException() {
        repository.findById(null);
    }
    @Test
    public void findByIdThrowsException_When_IdIsNull() {
        try {
            repository.findById(null);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), ID_MUST_NOT_BE_NULL);
        }

    }
    private TenantInfoDoc getTenantInfoDoc() {
        String[] groups = {ID};
        TenantInfoDoc doc = new TenantInfoDoc(ID, SERVICE_PRINCIPLE_ID, TEST_COMPLAINCE, groups);
        return doc;
    }
}
