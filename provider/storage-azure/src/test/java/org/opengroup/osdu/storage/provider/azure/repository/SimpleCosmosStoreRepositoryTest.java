package org.opengroup.osdu.storage.provider.azure.repository;

import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlQuerySpec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SimpleCosmosStoreRepositoryTest {

    private static final String DATA_PARTITION_ID = "data-partition-id";
    private static final String cosmosDBName = "cosmosDBName";
    private static final String partitionKey = "partition-key";
    private static final String id = "id";
    private static final String collection = "collection";
    private static final int pageSize = 50;
    private static final String continuationToken = "continuationToken";

    private Class<String> domainClass = String.class;
    @Mock
    private CosmosStore operation;

    @InjectMocks
    private SimpleCosmosStoreRepository<String> sut = new SimpleCosmosStoreRepository(String.class);

    @Test
    public void testDeleteById() {
        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection, id, partitionKey};

        sut.deleteById(args[3], args[0], args[1], args[2], args[4]);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(operation, times(1)).deleteItem(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture());
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);
        validateArgs(args, obtainedArgs);
    }

    @Test
    public void testFindById() {
        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection, id, partitionKey};

        sut.findById(args[3], args[0], args[1], args[2], args[4]);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class<String>> classArgumentCaptor = ArgumentCaptor.forClass(Class.class);

        verify(operation, times(1)).findItem(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), classArgumentCaptor.capture());

        assertEquals(domainClass, classArgumentCaptor.getValue());
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);
        validateArgs(args, obtainedArgs);
    }

    @Test
    public void testExistsById() {
        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection, id, partitionKey};

        Optional<String> item = Optional.of("item");
        doReturn(item).when(operation).findItem(eq(args[0]), eq(args[1]), eq(args[2]), eq(args[3]), eq(args[4]), eq(domainClass));

        boolean status = sut.existsById(args[0], args[1], args[2], args[3], args[4]);

        ArgumentCaptor<Class<String>> classArgumentCaptor = ArgumentCaptor.forClass(Class.class);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(operation, times(1)).findItem(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), classArgumentCaptor.capture());
        assertEquals(domainClass, classArgumentCaptor.getValue());
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);

        validateArgs(args, obtainedArgs);
        assertTrue(status);
    }

    @Test
    public void testFindAll() {
        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection};

        List<String> items = Arrays.asList("item1", "item2");

        doReturn(items).when(operation).findAllItems(eq(args[0]), eq(args[1]), eq(args[2]), eq(domainClass));

        List<String> obtainedItems = (List<String>) sut.findAll(args[0], args[1], args[2]);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class<String>> classArgumentCaptor = ArgumentCaptor.forClass(Class.class);

        verify(operation, times(1)).findAllItems(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), classArgumentCaptor.capture());
        assertEquals(domainClass, classArgumentCaptor.getValue());
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);

        validateArgs(args, obtainedArgs);
        assertEquals(items, obtainedItems);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFindAllById() {

        Iterable<String> ids = Arrays.asList("id1", "id2");

        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection, partitionKey};

        try {
            sut.findAllById(ids, args[0], args[1], args[2], args[3]);
        } catch (UnsupportedOperationException e) {
            throw(e);
        }
    }

    @Test
    public void testFind() {
        SqlQuerySpec query = mock(SqlQuerySpec.class);

        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection};

        List<String> items = Arrays.asList("item1", "item2");

        doReturn(items).when(operation).queryItems(eq(args[0]), eq(args[1]), eq(args[2]), eq(query), any(CosmosQueryRequestOptions.class), eq(domainClass));

        List<String> obtainedItems = sut.find(args[0], args[1], args[2], query);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class<String>> classArgumentCaptor = ArgumentCaptor.forClass(Class.class);

        verify(operation, times(1)).queryItems(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), eq(query), any(CosmosQueryRequestOptions.class), classArgumentCaptor.capture());
        assertEquals(domainClass, classArgumentCaptor.getValue());
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);

        validateArgs(args, obtainedArgs);
        assertEquals(items, obtainedItems);
    }

    @Test
    public void testQueryItemsPage() {
        SqlQuerySpec query = mock(SqlQuerySpec.class);
        CosmosQueryRequestOptions options = mock(CosmosQueryRequestOptions.class);

        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection};

        sut.queryItemsPage(args[0], args[1], args[2], query, domainClass, pageSize, continuationToken);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(operation, times(1)).queryItemsPage(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), eq(query), eq(domainClass), eq(pageSize), eq(continuationToken));
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);
        validateArgs(args, obtainedArgs);
    }

    @Test
    public void testCreateItem() {
        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection, partitionKey, "item"};

        sut.createItem(args[0], args[1], args[2], args[3], args[4]);
        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(operation, times(1)).createItem(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture());
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);
        validateArgs(args, obtainedArgs);
    }

    @Test
    public void testSave() {
        String entity = "entity";
        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection, partitionKey};

        String obtainedEntity = sut.save(entity, args[0], args[1], args[2], args[3]);
        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(operation, times(1)).upsertItem(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), eq(entity));
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);
        validateArgs(args, obtainedArgs);
        assertEquals(entity, obtainedEntity);
    }

    @Test
    public void testGetOne() {
        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection, id, partitionKey};

        String doc = "doc";
        Optional<String> optionalDoc = Optional.of(doc);
        doReturn(optionalDoc).when(operation).findItem(eq(args[0]), eq(args[1]), eq(args[2]), eq(args[3]), eq(args[4]), eq(domainClass));

        String obtainedDoc = sut.getOne(args[3], args[0], args[1], args[2], args[4]);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(operation, times(1)).findItem(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), eq(domainClass));

        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);
        validateArgs(args, obtainedArgs);
        assertEquals(doc, obtainedDoc);
    }

    @Test
    public void testPaginationQuery() {
        Pageable pageable = mock(Pageable.class);
        SqlQuerySpec query = mock(SqlQuerySpec.class);
        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection};

        doReturn(pageSize).when(pageable).getPageSize();

        sut.paginationQuery(pageable, query, domainClass, args[0], args[1], args[2]);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(operation, times(1)).queryItemsPage(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), eq(query), eq(domainClass), eq(pageSize), eq(null));
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);
        validateArgs(args, obtainedArgs);
    }

    @Test
    public void testFindAll_paginationQuery() {
        Pageable pageable = mock(Pageable.class);
        Sort sort = mock(Sort.class);

        doReturn(pageSize).when(pageable).getPageSize();
        doReturn(sort).when(pageable).getSort();
        doReturn(false).when(sort).isSorted();

        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection};

        Page<String> page = sut.findAll(pageable, args[0], args[1], args[2]);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(operation, times(1)).queryItemsPage(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), any(SqlQuerySpec.class), eq(domainClass), eq(pageSize), eq(null));
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);
        validateArgs(args, obtainedArgs);
    }

    @Test
    public void testFind_paginationQuery() {
        Pageable pageable = mock(Pageable.class);
        SqlQuerySpec query = mock(SqlQuerySpec.class);
        Sort sort = mock(Sort.class);

        doReturn("query test").when(query).getQueryText();
        doReturn(pageSize).when(pageable).getPageSize();
        doReturn(sort).when(pageable).getSort();
        doReturn(false).when(sort).isSorted();

        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection};

        Page<String> page = sut.find(pageable, args[0], args[1], args[2], query);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(operation, times(1)).queryItemsPage(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), any(SqlQuerySpec.class), eq(domainClass), eq(pageSize), eq(null));
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);
        validateArgs(args, obtainedArgs);
    }

    @Test
    public void testFindAllSort() {
        Sort sort = mock(Sort.class);

        String[] args = {DATA_PARTITION_ID, cosmosDBName, collection};

        List<String> items = Arrays.asList("item1", "item2");

        doReturn(items).when(operation).queryItems(eq(args[0]), eq(args[1]), eq(args[2]), any(SqlQuerySpec.class), any(CosmosQueryRequestOptions.class), eq(domainClass));

        List<String> obtainedItems = (List<String>) sut.findAll(sort, args[0], args[1], args[2]);

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class<String>> classArgumentCaptor = ArgumentCaptor.forClass(Class.class);

        verify(operation, times(1)).queryItems(argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), any(SqlQuerySpec.class), any(CosmosQueryRequestOptions.class), classArgumentCaptor.capture());
        assertEquals(domainClass, classArgumentCaptor.getValue());
        String[] obtainedArgs = new String[args.length];
        obtainedArgs = argCaptor.getAllValues().toArray(obtainedArgs);

        validateArgs(args, obtainedArgs);
        assertEquals(items, obtainedItems);
    }

    void validateArgs(String[] args, String[] obtainedArgs) {
        for (int i = 0; i < args.length; ++i) {
            assertEquals(args[i], obtainedArgs[i]);
        }
    }
}
