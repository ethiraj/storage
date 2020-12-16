// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.storage.provider.azure;

import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.runners.MockitoJUnitRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.azure.eventgrid.EventGridTopicStore;
import org.opengroup.osdu.azure.eventgrid.TopicName;
import org.opengroup.osdu.azure.servicebus.ITopicClientFactory;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.DatastoreQueryResult;
import org.opengroup.osdu.core.common.model.storage.PubSubInfo;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.storage.provider.azure.di.EventGridConfig;
import org.opengroup.osdu.storage.provider.azure.repository.QueryRepository;
import org.opengroup.osdu.storage.provider.azure.repository.SchemaRepository;
import org.springframework.data.domain.Sort;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;


@RunWith(MockitoJUnitRunner.class)

@ExtendWith(MockitoExtension.class)
public class MessageBusImplTest {
    
    private static final String DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID = "data-partition-account-id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String PARTITION_ID = "partition-id";

    @Mock
    private ITopicClientFactory topicClientFactory;
    /*@Mock
    private TopicClient topicClient;
*/

    @Mock
    private EventGridTopicStore eventGridTopicStore;

    @Mock
    private EventGridConfig eventGridConfig;

    @Mock
    private DpsHeaders dpsHeaders;

    @InjectMocks
    private MessageBusImpl sut;

    @Before
    public void init() throws ServiceBusException, InterruptedException {
        initMocks(this);

       // TopicClient concreteTopicClient = new TopicClient(new ConnectionStringBuilder("connectionString"));

        TopicClient mocktopicClient = mock(TopicClient.class);



   doReturn(DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID).when(dpsHeaders).getPartitionIdWithFallbackToAccountId();
        doReturn(PARTITION_ID).when(dpsHeaders).getPartitionId();
        doReturn(CORRELATION_ID).when(dpsHeaders).getCorrelationId();
        doReturn(mocktopicClient).when(topicClientFactory).getClient(eq(PARTITION_ID), any());
    }


    @Test
    public void should_publishToEventGrid_WhenFlagIsSet() {
   // Set Up
        String[] ids = {"id1", "id2"};
        String[] kinds = {"kind1", "kind2"};

        PubSubInfo[] pubSubInfo = new PubSubInfo[2];
        for (int i = 0; i < ids.length; ++i) {
            pubSubInfo[i] = getPubsInfo(ids[0], kinds[0]);
        }
        when(this.eventGridConfig.isPublishingToEventGridEnabled()).thenReturn(true);

        // Act
        sut.publishMessage(this.dpsHeaders, pubSubInfo);

        // Asset
        verify(this.eventGridTopicStore, times(1)).publishToEventGridTopic(PARTITION_ID, TopicName.RECORDS_CHANGED, any());

    }

    private PubSubInfo getPubsInfo(String id, String kind) {
        PubSubInfo pubSubInfo = new PubSubInfo();
        pubSubInfo.setId(id);
        pubSubInfo.setKind(kind);
        return pubSubInfo;
    }
}
