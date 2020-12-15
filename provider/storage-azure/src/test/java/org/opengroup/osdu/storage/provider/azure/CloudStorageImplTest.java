package org.opengroup.osdu.storage.provider.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.blobstorage.IBlobContainerClientFactory;
import org.opengroup.osdu.azure.multitenancy.TenantInfoDoc;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.storage.*;
import org.opengroup.osdu.storage.provider.azure.repository.GroupsInfoRepository;
import org.opengroup.osdu.storage.provider.interfaces.IRecordsMetadataRepository;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CloudStorageImplTest {

    private static final String partitionId = "partitionId";
    private static final String userEmail = "user@email.com";

    public static class Results {

        private ExecutorService executor = Executors.newSingleThreadExecutor();

        public Future<Boolean> getResults(boolean status) {
            return executor.submit(() -> {
                Thread.sleep(100);
                return status;
            });
        }
    }

    private static class DummyObject {
        private String id;

        public DummyObject(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private EntitlementsAndCacheServiceAzure dataEntitlementsService;

    @Mock
    private DpsHeaders headers;

    @Mock
    private ExecutorService threadPool;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Mock
    private IRecordsMetadataRepository recordRepository;

    @Mock
    private IBlobContainerClientFactory blobContainerClientFactory;

    @Mock
    private GroupsInfoRepository groupsInfoRepository;

    @InjectMocks
    private CloudStorageImpl sut;

    @Before
    public void init() {
        doReturn(partitionId).when(headers).getPartitionId();
        doReturn(userEmail).when(headers).getUserEmail();
    }

    @Test(expected = AppException.class)
    public void testWrite_whenGroupsIsEmptyOrNull_thenThrowsException() {
        String[] owners = {"owner1", "owner2"};
        RecordProcessing recordProcessing = getRecordProcessingWithAclOwners(owners);

        try {
            sut.write(recordProcessing);
        } catch (AppException e) {
            int errorCode = 500;
            String errorMessage = "Tenant was not found";
            validateAppException(e, errorCode, errorMessage);
            throw(e);
        }
    }

    @Test
    public void testWrite_whenProvidedValidAclRecordsAndInvokedTasksReturnsTrue() throws InterruptedException, ExecutionException {
        String[] groups = {"group1", "group2"};
        TenantInfoDoc doc = getTenantInfoDoc("id", groups);
        String[][] owners = new String[2][];
        owners[0] = new String[]{"group1@user1", "group1@user2", "group2@user1"};
        owners[1] = new String[]{"group1@user3", "group2@user2"};
        String[] kinds = {"kind1", "kind2"};
        String[] ids = {"id1", "id2"};
        Long[] latestVersions = {1L, 2L, 3L};
        RecordProcessing[] recordProcessings = new RecordProcessing[2];
        for (int i = 0; i < 2; ++i) {
            recordProcessings[i] = getRecordProcessingWithAclOwners(owners[i]);
            RecordMetadata recordMetadata = recordProcessings[i].getRecordMetadata();
            doReturn(kinds[i]).when(recordMetadata).getKind();
            doReturn(ids[i]).when(recordMetadata).getId();
            doReturn(latestVersions[i]).when(recordMetadata).getLatestVersion();
        }

        Future<Boolean> future = mock(Future.class);
        List<Future<Boolean>> results = Arrays.asList(future);

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        BlockBlobClient blockBlobClient = mock(BlockBlobClient.class);

        doReturn(results).when(threadPool).invokeAll(any());
        doReturn(Optional.of(doc)).when(groupsInfoRepository).findById(eq(partitionId));
        doReturn(blobContainerClient).when(blobContainerClientFactory).getClient(eq(partitionId), any());
        doReturn(blobClient).when(blobContainerClient).getBlobClient(anyString());
        doReturn(blockBlobClient).when(blobClient).getBlockBlobClient();

        sut.write(recordProcessings[0], recordProcessings[1]);

        ArgumentCaptor<List> tasksArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(threadPool, times(1)).invokeAll(tasksArgumentCaptor.capture());

        List<Callable<Boolean>> obtainedTasks = tasksArgumentCaptor.getValue();

        for (Future<Boolean> result: executorService.invokeAll(obtainedTasks)) {
            assertTrue(result.get());
        }
    }

    @Test(expected = AppException.class)
    public void testWrite_whenCannotGetFutureResults_throwsException() throws InterruptedException, ExecutionException {
        ExecutionException executionException = mock(ExecutionException.class);

        String[] groups = {"group1", "group2"};
        TenantInfoDoc doc = getTenantInfoDoc("id", groups);
        String[][] owners = new String[2][];
        owners[0] = new String[]{"group1@user1", "group1@user2", "group2@user1"};
        owners[1] = new String[]{"group1@user3", "group2@user2"};
        RecordProcessing[] recordProcessings = new RecordProcessing[2];
        for (int i = 0; i < 2; ++i) {
            recordProcessings[i] = getRecordProcessingWithAclOwners(owners[i]);
        }

        Future<Boolean> future = mock(Future.class);
        List<Future<Boolean>> results = Arrays.asList(future);
        doReturn(results).when(threadPool).invokeAll(any());
        doThrow(executionException).when(future).get();
        doReturn(Optional.of(doc)).when(groupsInfoRepository).findById(eq(partitionId));

        try {
            sut.write(recordProcessings[0], recordProcessings[1]);
        } catch (AppException e) {
            int errorCode = 500;
            String errorMessage = "An unexpected error on writing the record has occurred";
            validateAppException(e, errorCode, errorMessage);
            throw(e);
        }
    }

    @Test(expected = AppException.class)
    public void testWrite_whenProvidedInvalidAclRecords() {
        String[] groups = {"group1", "group2"};
        TenantInfoDoc doc = getTenantInfoDoc("id", groups);
        String[][] owners = new String[2][];
        owners[0] = new String[]{"group1@user1", "group1@user2", "group2@user1"};
        owners[1] = new String[]{"non-existing-group@user3", "group2@user2"};
        RecordProcessing[] recordProcessings = new RecordProcessing[2];
        for (int i = 0; i < 2; ++i) {
            recordProcessings[i] = getRecordProcessingWithAclOwners(owners[i]);
        }

        doReturn(Optional.of(doc)).when(groupsInfoRepository).findById(eq(partitionId));

        try {
            sut.write(recordProcessings[0], recordProcessings[1]);
        } catch (AppException e) {
            int errorCode = 403;
            String errorMessage = "Record ACL is not one of";
            validateAppException(e, errorCode, errorMessage);
            assertEquals(e.getError().getReason(), "Invalid ACL");
            throw(e);
        }
    }

    @Test
    public void testGetHash_isRecordOwner() {
        String[][] viewers = new String[2][];
        viewers[0] = new String[]{"viewer1-1", "viewer1-2"};
        viewers[1] = new String[]{"viewer1-1", "viewer2-2"};
        String[] kinds = {"kind1", "kind2"};
        String[] ids = {"id1", "id2"};
        String[] users = {userEmail, "user1"};
        Long[] latestVersions = {1L, 2L};
        RecordMetadata[] recordMetadatas = new RecordMetadata[2];
        for (int i = 0; i < 2; ++i) {
            recordMetadatas[i] = getRecordMetadataWithAcl(null, viewers[i]);
            doReturn(latestVersions[i]).when(recordMetadatas[i]).getLatestVersion();
            doReturn(kinds[i]).when(recordMetadatas[i]).getKind();
            doReturn(ids[i]).when(recordMetadatas[i]).getId();
            doReturn(users[i]).when(recordMetadatas[i]).getUser();
        }
        doReturn(false).doReturn(true).when(dataEntitlementsService).hasAccessToData(eq(headers), any());

        List<RecordMetadata> records = Arrays.asList(recordMetadatas);

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        doReturn(blobContainerClient).when(blobContainerClientFactory).getClient(eq(partitionId), any());

        BlobClient[] blobClients = new BlobClient[2];
        BlockBlobClient[] blockBlobClients = new BlockBlobClient[2];

        for (int i = 0; i < 2; ++i) {
            blobClients[i] = mock(BlobClient.class);
            blockBlobClients[i] = mock(BlockBlobClient.class);
            String path = buildPath(kinds[i], ids[i], latestVersions[i]);
            doReturn(blobClients[i]).when(blobContainerClient).getBlobClient(eq(path));
            doReturn(blockBlobClients[i]).when(blobClients[i]).getBlockBlobClient();
        }

        Map<String, String> hashes = sut.getHash(records);

        assertEquals(hashes.size(), ids.length);
        assertTrue(hashes.containsKey(ids[0]));
        assertTrue(hashes.containsKey(ids[1]));
    }

    @Test
    public void testisDuplicateRecord_whenRecordHashMatches_thenReturnsTrue() {
        TransferInfo transfer = mock(TransferInfo.class);
        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        Map.Entry<RecordMetadata, RecordData> kv = mock(Map.Entry.class);
        List<String> skippedRecords = mock(List.class);

        Map<String, String> hashMap = new HashMap<>();
        String id = "id";
        String recordHash = "hL02Ew=="; // hash obtained for the given recordData with current implementation
        hashMap.put(id, recordHash);
        Record record = new Record();
        Map<String, Object> data = getObjectMap("key-data", "id-data");
        Map<String, Object>[] meta = new HashMap[2];
        meta[0] = getObjectMap("key-data1", "id-meta1");
        meta[1] = getObjectMap("key-data2", "id-meta2");
        record.setData(data);
        record.setMeta(meta);
        RecordData recordData = new RecordData(record);

        doReturn(skippedRecords).when(transfer).getSkippedRecords();
        doReturn(recordMetadata).when(kv).getKey();
        doReturn(recordData).when(kv).getValue();
        doReturn(id).when(recordMetadata).getId();

        boolean status = sut.isDuplicateRecord(transfer, hashMap, kv);

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(skippedRecords).add(stringArgumentCaptor.capture());

        assertTrue(status);
        assertEquals(stringArgumentCaptor.getValue(), id);
    }

    @Test
    public void testisDuplicateRecord_whenRecordHashDoesNotMatch_thenReturnsFalse() {
        TransferInfo transfer = mock(TransferInfo.class);
        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        Map.Entry<RecordMetadata, RecordData> kv = mock(Map.Entry.class);

        Map<String, String> hashMap = new HashMap<>();
        String id = "id";
        String recordHash = "random-non-matching-hash"; // hash obtained for the given recordData with current implementation
        hashMap.put(id, recordHash);
        Record record = new Record();
        Map<String, Object> data = getObjectMap("key-data", "id-data");
        Map<String, Object>[] meta = new HashMap[1];
        meta[0] = getObjectMap("key-data1", "id-meta1");
        record.setData(data);
        record.setMeta(meta);
        RecordData recordData = new RecordData(record);

        doReturn(recordMetadata).when(kv).getKey();
        doReturn(recordData).when(kv).getValue();
        doReturn(id).when(recordMetadata).getId();

        boolean status = sut.isDuplicateRecord(transfer, hashMap, kv);

        assertFalse(status);
    }

    @Test
    public void testDelete_whenBuildPathContainsLatestVersion_thenDeleteRecordForLatestVersion() {
        String[] owners = {"group1@user1", "group1@user2", "group2@user1"};
        RecordMetadata recordMetadata = getRecordMetadataWithAcl(owners, null);

        String kind = "kind";
        String id = "id";
        Long latestVersion = 1L;
        doReturn(latestVersion).when(recordMetadata).getLatestVersion();
        doReturn(kind).when(recordMetadata).getKind();
        doReturn(id).when(recordMetadata).getId();

        String path = buildPath(kind, id, latestVersion);

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        BlockBlobClient blockBlobClient = mock(BlockBlobClient.class);

        doReturn(true).when(recordMetadata).hasVersion();
        doReturn(true).when(dataEntitlementsService).hasAccessToData(eq(headers), any());
        doReturn(blobContainerClient).when(blobContainerClientFactory).getClient(eq(partitionId), any());
        doReturn(blobClient).when(blobContainerClient).getBlobClient(anyString());
        doReturn(blockBlobClient).when(blobClient).getBlockBlobClient();

        sut.delete(recordMetadata);

        verify(blockBlobClient, times(1)).delete();
        ArgumentCaptor<HashSet> hashSetArgumentCaptor = ArgumentCaptor.forClass(HashSet.class);
        ArgumentCaptor<String> pathArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(dataEntitlementsService).hasAccessToData(eq(headers), hashSetArgumentCaptor.capture());
        verify(blobContainerClient).getBlobClient(pathArgumentCaptor.capture());
        HashSet<String> obtainedOwnersHashSet = hashSetArgumentCaptor.getValue();
        String obtainedPath = pathArgumentCaptor.getValue();

        for (String owner: owners) {
            assertTrue(obtainedOwnersHashSet.contains(owner));
        }
        assertEquals(path, obtainedPath);
    }

    @Test
    public void testDeleteVersion_whenBuildPathContainsGivenVersion_thenDeleteRecordForGivenVersion() {
        String[] owners = {"group1@user1", "group1@user2", "group2@user1"};
        RecordMetadata recordMetadata = getRecordMetadataWithAcl(owners, null);

        String kind = "kind";
        String id = "id";
        Long latestVersion = 1L;
        Long versionToDelete = 2L;
        doReturn(kind).when(recordMetadata).getKind();
        doReturn(id).when(recordMetadata).getId();

        String latestVersionPath = buildPath(kind, id, latestVersion);
        String versionToDeletePath = buildPath(kind, id, versionToDelete);

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        BlockBlobClient blockBlobClient = mock(BlockBlobClient.class);

        doReturn(true).when(dataEntitlementsService).hasAccessToData(eq(headers), any());
        doReturn(blobContainerClient).when(blobContainerClientFactory).getClient(eq(partitionId), any());
        doReturn(blobClient).when(blobContainerClient).getBlobClient(anyString());
        doReturn(blockBlobClient).when(blobClient).getBlockBlobClient();

        sut.deleteVersion(recordMetadata, versionToDelete);

        verify(blockBlobClient, times(1)).delete();
        ArgumentCaptor<HashSet> hashSetArgumentCaptor = ArgumentCaptor.forClass(HashSet.class);
        ArgumentCaptor<String> pathArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(dataEntitlementsService).hasAccessToData(eq(headers), hashSetArgumentCaptor.capture());
        verify(blobContainerClient).getBlobClient(pathArgumentCaptor.capture());
        HashSet<String> obtainedOwnersHashSet = hashSetArgumentCaptor.getValue();
        String obtainedPath = pathArgumentCaptor.getValue();

        for (String owner: owners) {
            assertTrue(obtainedOwnersHashSet.contains(owner));
        }
        assertEquals(versionToDeletePath, obtainedPath);
        assertNotEquals(latestVersionPath, obtainedPath);
    }

    @Test(expected = AppException.class)
    public void testDelete_whenDoesNotHaveOwnerAccessToRecord_thenThrowsException() {
        String[] owners = {"group1@user1", "group1@user2", "group2@user1"};
        RecordMetadata recordMetadata = getRecordMetadataWithAcl(owners, null);

        doReturn(true).when(recordMetadata).hasVersion();
        doReturn(false).when(dataEntitlementsService).hasAccessToData(eq(headers), any());

        try {
            sut.delete(recordMetadata);
        } catch (AppException e) {
            int errorCode = 403;
            String errorMessage = "The user is not authorized to perform this action";
            validateAppException(e, errorCode, errorMessage);

            throw(e);
        }
    }

    @Test
    public void testHasAccess() {
        String[][] owners = new String[3][];
        String[][] viewers = new String[3][];
        owners[0] = new String[]{"group1@user1", "group1@user2", "group2@user1"};
        owners[1] = new String[]{"group1@user3", "group2@user2"};
        owners[2] = new String[]{"group1@user4", "group2@user3", "group3@user4"};
        viewers[0] = new String[]{"viewer1-1", "viewer1-2"};
        viewers[1] = new String[]{"viewer2-1", "viewer2-2"};
        viewers[2] = new String[]{"viewer3-1", "viewer2-2"};

        RecordMetadata[] recordMetadatas = new RecordMetadata[3];
        String[] ids = new String[3];
        for (int i = 0; i < 3; ++i) {
            ids[i] = "id" + i;
            recordMetadatas[i] = getRecordMetadataWithAcl(owners[i], viewers[i]);
            doReturn(ids[i]).when(recordMetadatas[i]).getId();
        }

        // record meta data with non-active status
        doReturn(RecordState.deleted).when(recordMetadatas[0]).getStatus();

        // record meta data with active status but has no version
        doReturn(RecordState.active).when(recordMetadatas[1]).getStatus();
        doReturn(false).when(recordMetadatas[1]).hasVersion();

        // record meta data with active status, has version and has viewer access to record (record.getUser() == headers.getUserEmail())
        doReturn(RecordState.active).when(recordMetadatas[2]).getStatus();
        doReturn(true).when(recordMetadatas[2]).hasVersion();
        doReturn(userEmail).when(recordMetadatas[2]).getUser();

        boolean status = sut.hasAccess(recordMetadatas[0], recordMetadatas[1], recordMetadatas[2]);

        assertTrue(status);

        ArgumentCaptor<HashSet> hashSetArgumentCaptor = ArgumentCaptor.forClass(HashSet.class);
        verify(dataEntitlementsService, times(1)).hasAccessToData(eq(headers), hashSetArgumentCaptor.capture());
        HashSet<String> obtainedOwnersHashSet = hashSetArgumentCaptor.getValue();

        for (String viewer: viewers[2]) {
            assertTrue(obtainedOwnersHashSet.contains(viewer));
        }
    }

    @Test
    public void testHasAccess_whenGivenVariousRecordMetadatas_returnsTrueForRecordMetadataHavingActiveStatusAndHasVersionAndViewerAccess() {
        String[][] owners = new String[3][];
        String[][] viewers = new String[3][];
        owners[0] = new String[]{"group1@user1", "group1@user2", "group2@user1"};
        owners[1] = new String[]{"group1@user3", "group2@user2"};
        owners[2] = new String[]{"group1@user4", "group2@user3", "group3@user4"};
        viewers[0] = new String[]{"viewer1-1", "viewer1-2"};
        viewers[1] = new String[]{"viewer2-1", "viewer2-2"};
        viewers[2] = new String[]{"viewer3-1", "viewer2-2"};

        RecordMetadata[] recordMetadatas = new RecordMetadata[3];
        String[] ids = new String[3];
        for (int i = 0; i < 3; ++i) {
            ids[i] = "id" + i;
            recordMetadatas[i] = getRecordMetadataWithAcl(owners[i], viewers[i]);
            doReturn(ids[i]).when(recordMetadatas[i]).getId();
        }

        // record meta data with non-active status
        doReturn(RecordState.deleted).when(recordMetadatas[0]).getStatus();

        // record meta data with active status but has no version
        doReturn(RecordState.active).when(recordMetadatas[1]).getStatus();
        doReturn(false).when(recordMetadatas[1]).hasVersion();

        // record meta data with active status, has version and has viewer access to record (record.getUser() == headers.getUserEmail())
        doReturn(RecordState.active).when(recordMetadatas[2]).getStatus();
        doReturn(true).when(recordMetadatas[2]).hasVersion();
        doReturn(userEmail).when(recordMetadatas[2]).getUser();

        boolean status = sut.hasAccess(recordMetadatas[0], recordMetadatas[1], recordMetadatas[2]);

        assertTrue(status);

        ArgumentCaptor<HashSet> hashSetArgumentCaptor = ArgumentCaptor.forClass(HashSet.class);
        verify(dataEntitlementsService, times(1)).hasAccessToData(eq(headers), hashSetArgumentCaptor.capture());
        HashSet<String> obtainedOwnersHashSet = hashSetArgumentCaptor.getValue();

        for (String viewer: viewers[2]) {
            assertTrue(obtainedOwnersHashSet.contains(viewer));
        }
    }

    @Test
    public void testHasAccess_whenGivenRecordMetadataWithActiveStatusHasVersionButNoViewerAccessToRecord_thenReturnsFalse() {
        String[] owners = {"group1@user1", "group1@user2", "group2@user1"};
        String[] viewers = {"viewer1-1", "viewer1-2"};
        RecordMetadata recordMetadata = getRecordMetadataWithAcl(owners, viewers);
        doReturn(RecordState.active).when(recordMetadata).getStatus();
        doReturn(true).when(recordMetadata).hasVersion();
        doReturn("userWithNoAccess").when(recordMetadata).getUser();
        boolean status = sut.hasAccess(recordMetadata);
        assertFalse(status);
    }

    @Test
    public void testHasAccess_whenGivenRecordMetadataWithNonActiveStatus_thenReturnsTrue() {
        RecordMetadata recordMetadata = getRecordMetadataWithAcl(null, null);
        doReturn(RecordState.purged).when(recordMetadata).getStatus();
        boolean status = sut.hasAccess(recordMetadata);
        assertTrue(status);
    }

    @Test
    public void testHasAccess_whenGivenNoRecordMetadata() {
        boolean status = sut.hasAccess();
        assertTrue(status);
    }

    @Test
    public void testRead() throws InterruptedException, ExecutionException {
        Map<String, String> objects = new HashMap();
        Map<String, RecordMetadata> recordsMetadata = new HashMap();

        String[][] viewers = new String[3][];
        viewers[0] = new String[]{"viewer1-1", "viewer1-2"};
        viewers[1] = new String[]{"viewer2-1", "viewer2-2"};
        viewers[2] = new String[]{"viewer3-1", "viewer2-2"};
        String[] users = {userEmail, "user", "user"};
        boolean[] hasAccesses = {false, true, false};
        RecordMetadata[] recordMetadatas = new RecordMetadata[3];
        for (int i = 0; i < 3; ++i) {
            String id = "id" + i;
            String val = "path" + i;
            objects.put(id, val);
            recordMetadatas[i] = getRecordMetadataWithAcl(null, viewers[i]);
            doReturn(users[i]).when(recordMetadatas[i]).getUser();
            doReturn(hasAccesses[i]).when(dataEntitlementsService).hasAccessToData(eq(headers), eq(new HashSet<>(Arrays.asList(viewers[i]))));
            recordsMetadata.put(id, recordMetadatas[i]);
        }

        List<String> recordIds = new ArrayList<>(objects.keySet());

        doReturn(recordsMetadata).when(recordRepository).get(eq(recordIds));

        Future<Boolean> future = mock(Future.class);
        List<Future<Boolean>> results = Arrays.asList(future);
        doReturn(results).when(threadPool).invokeAll(any());

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        BlockBlobClient blockBlobClient = mock(BlockBlobClient.class);

        doReturn(blobContainerClient).when(blobContainerClientFactory).getClient(eq(partitionId), any());
        doReturn(blobClient).when(blobContainerClient).getBlobClient(anyString());
        doReturn(blockBlobClient).when(blobClient).getBlockBlobClient();

        Map<String, String> map = sut.read(objects);

        assertEquals(map.size(), 1);
        assertTrue(map.containsKey("id2"));

        ArgumentCaptor<List> tasksArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(threadPool).invokeAll(tasksArgumentCaptor.capture());

        List<Callable<Boolean>> obtainedTasks = tasksArgumentCaptor.getValue();

        for (Future<Boolean> result: executorService.invokeAll(obtainedTasks)) {
            assertTrue(result.get());
        }

        verify(dataEntitlementsService, times(3)).hasAccessToData(eq(headers), any());
        assertEquals(map.size(), 3);
        for (int i = 0; i < 3; ++i) {
            assertTrue(map.containsKey("id" + i));
        }
    }

    @Test(expected = AppException.class)
    public void testRead_whenDoesNotHaveViewerAccessToRecord_thenThrowsException() throws InterruptedException, ExecutionException {
        String[] viewers = {"viewer1-1", "viewer1-2"};
        Long version = 1L;
        boolean checkDataInconsistency = false;
        RecordMetadata recordMetadata = getRecordMetadataWithAcl(null, viewers);
        doReturn("user").when(recordMetadata).getUser();
        doReturn(false).when(dataEntitlementsService).hasAccessToData(eq(headers), eq(new HashSet<>(Arrays.asList(viewers))));

        try {
            sut.read(recordMetadata, version, checkDataInconsistency);
        } catch (AppException e) {
            int errorCode = 403;
            String errorMessage = "The user is not authorized to perform this action";

            validateAppException(e, errorCode, errorMessage);
            throw(e);
        }
    }

    @Test(expected = AppException.class)
    public void testRead_whenCannotGetFutureResults_throwsException() throws InterruptedException, ExecutionException {
        ExecutionException executionException = mock(ExecutionException.class);
        Map<String, String> objects = new HashMap();
        Map<String, RecordMetadata> recordsMetadata = new HashMap();
        String[] viewers = {"viewer1-1", "viewer1-2"};

        String user = userEmail;
        objects.put("id", "val");
        RecordMetadata recordMetadata = getRecordMetadataWithAcl(null, viewers);
        doReturn(user).when(recordMetadata).getUser();
        doReturn(false).when(dataEntitlementsService).hasAccessToData(eq(headers), any());
        recordsMetadata.put("id", recordMetadata);

        List<String> recordIds = new ArrayList<>(objects.keySet());

        doReturn(recordsMetadata).when(recordRepository).get(eq(recordIds));

        Future<Boolean> future = mock(Future.class);
        List<Future<Boolean>> results = Arrays.asList(future);
        doReturn(results).when(threadPool).invokeAll(any());
        doThrow(executionException).when(future).get();

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);

        doReturn(blobContainerClient).when(blobContainerClientFactory).getClient(eq(partitionId), any());

        try {
            sut.read(objects);
        } catch (AppException e) {
            int errorCode = 500;
            String errorMessage = "Unable to process parallel blob download";

            assertThat(e.getError().getReason(), containsString(errorMessage));
            assertEquals(e.getError().getCode(), errorCode);
            throw(e);
        }
    }

    Map<String, Object> getObjectMap(String key, String objId) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, new DummyObject(objId));
        return map;
    }

    private RecordProcessing getRecordProcessingWithAclOwners(String[] owners) {
        RecordData recordData = mock(RecordData.class);
        RecordMetadata recordMetadata = getRecordMetadataWithAcl(owners, null);
        OperationType operationType = mock(OperationType.class);
        RecordProcessing recordProcessing = new RecordProcessing(recordData, recordMetadata, operationType);
        return recordProcessing;
    }

    private RecordMetadata getRecordMetadataWithAcl(String[] owners, String[] viewers) {
        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        Acl acl = Acl.builder().owners(owners).viewers(viewers).build();
        doReturn(acl).when(recordMetadata).getAcl();
        return recordMetadata;
    }

    private TenantInfoDoc getTenantInfoDoc(String id, String[] groups) {
        TenantInfoDoc tenantInfoDoc = new TenantInfoDoc(id, null, null, groups);
        return tenantInfoDoc;
    }

    private void validateAppException(AppException e, int errorCode, String errorMessage) {
        AppError error = e.getError();
        assertEquals(error.getCode(), errorCode);
        assertThat(error.getMessage(), containsString(errorMessage));
    }

    private String buildPath(String kind, String id, Long version) {
        return kind + "/" + id + "/" + version;
    }
}
