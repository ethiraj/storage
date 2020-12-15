package org.opengroup.osdu.storage.provider.azure;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.servicebus.ITopicClientFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.PubSubInfo;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MessageBusImplTest {

    private static final String DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID = "data-partition-account-id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String PARTITION_ID = "partition-id";

    @Mock
    private ITopicClientFactory topicClientFactory;

    @Mock
    private TopicClient topicClient;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private DpsHeaders headers;

    @InjectMocks
    private MessageBusImpl sut;

    @Before
    public void init() throws ServiceBusException, InterruptedException {
        doReturn(DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID).when(headers).getPartitionIdWithFallbackToAccountId();
        doReturn(PARTITION_ID).when(headers).getPartitionId();
        doReturn(CORRELATION_ID).when(headers).getCorrelationId();
        doReturn(topicClient).when(topicClientFactory).getClient(eq(PARTITION_ID), any());
    }

    @Test
    public void testPublishMessage() throws ServiceBusException, InterruptedException {
        String[] ids = {"id1", "id2"};
        String[] kinds = {"kind1", "kind2"};

        PubSubInfo[] messages = new PubSubInfo[2];
        for (int i = 0; i < ids.length; ++i) {
            messages[i] = getPubsInfo(ids[0], kinds[0]);
        }

        ArgumentCaptor<Message> msg = ArgumentCaptor.forClass(Message.class);

        sut.publishMessage(headers, messages[0], messages[1]);
        verify(topicClient).send(msg.capture());
        Map<String, Object> properties = msg.getValue().getProperties();

        assertEquals(DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID, properties.get(DpsHeaders.ACCOUNT_ID));
        assertEquals(DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID, properties.get(DpsHeaders.DATA_PARTITION_ID));
        assertEquals(CORRELATION_ID, properties.get(DpsHeaders.CORRELATION_ID));

        MessageBody messageBody = msg.getValue().getMessageBody();
        Gson gson = new Gson();
        String messageKey = "message";
        String dataKey = "data";
        JsonObject jsonObjectMessage = gson.fromJson(new String(messageBody.getBinaryData().get(0)), JsonObject.class);
        JsonObject jsonObject = (JsonObject) jsonObjectMessage.get(messageKey);
        assertEquals(DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID, jsonObject.get(DpsHeaders.ACCOUNT_ID).getAsString());
        assertEquals(DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID, jsonObject.get(DpsHeaders.DATA_PARTITION_ID).getAsString());
        assertEquals(CORRELATION_ID, jsonObject.get(DpsHeaders.CORRELATION_ID).getAsString());
        assertEquals(gson.toJsonTree(messages), jsonObject.get(dataKey));
    }

    private PubSubInfo getPubsInfo(String id, String kind) {
        PubSubInfo pubSubInfo = new PubSubInfo();
        pubSubInfo.setId(id);
        pubSubInfo.setKind(kind);
        return pubSubInfo;
    }
}
