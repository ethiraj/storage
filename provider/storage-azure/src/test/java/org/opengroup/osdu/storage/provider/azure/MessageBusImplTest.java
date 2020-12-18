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

import com.microsoft.azure.eventgrid.models.EventGridEvent;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.eventgrid.EventGridTopicStore;
import org.opengroup.osdu.azure.servicebus.ITopicClientFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.PubSubInfo;
import org.opengroup.osdu.storage.provider.azure.di.EventGridConfig;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class MessageBusImplTest {

    private static final String DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID = "data-partition-account-id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String PARTITION_ID = "partition-id";

    @Mock
    private ITopicClientFactory topicClientFactory;

    @Mock
    private TopicClient topicClient;

    @Mock
    private EventGridTopicStore eventGridTopicStore;

    @Captor
    ArgumentCaptor<List<EventGridEvent>> argCaptor;

    @Mock
    private EventGridConfig eventGridConfig;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private JaxRsDpsLog logger;

    @InjectMocks
    private MessageBusImpl sut;

    @Before
    public void init() throws ServiceBusException, InterruptedException {
        initMocks(this);

        doReturn(DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID).when(dpsHeaders).getPartitionIdWithFallbackToAccountId();
        doReturn(PARTITION_ID).when(dpsHeaders).getPartitionId();
        doReturn(CORRELATION_ID).when(dpsHeaders).getCorrelationId();
        doReturn(topicClient).when(topicClientFactory).getClient(eq(PARTITION_ID), any());
    }

    @Test
    public void should_publishToEventGrid_WhenFlagIsSet() {
        // Set Up
        String[] ids = {"id1", "id2", "id3", "id3", "id4", "id5", "id6", "id7", "id8", "id9", "id10", "id11"};
        String[] kinds = {"kind1", "kind2", "kind3", "kind4", "kind5", "kind6", "kind7", "kind8", "kind9", "kind10", "kind11"};

        PubSubInfo[] pubSubInfo = new PubSubInfo[12];
        for (int i = 0; i < ids.length; ++i) {
            pubSubInfo[i] = getPubsInfo(ids[0], kinds[0]);
        }
        when(this.eventGridConfig.isPublishingToEventGridEnabled()).thenReturn(true);
        when(this.eventGridConfig.getEventGridBatchSize()).thenReturn(5);

        // Act
        sut.publishMessage(this.dpsHeaders, pubSubInfo);

        // Asset
        verify(this.eventGridTopicStore, times(1)).publishToEventGridTopic(any(), any(), anyList());
        // The number of events that are being published is verified here.
        verify(this.eventGridTopicStore).publishToEventGridTopic(any(), any(), argCaptor.capture());
        assertEquals(3, argCaptor.getValue().size());
    }


    private PubSubInfo getPubsInfo(String id, String kind) {
        PubSubInfo pubSubInfo = new PubSubInfo();
        pubSubInfo.setId(id);
        pubSubInfo.setKind(kind);
        return pubSubInfo;
    }
}
