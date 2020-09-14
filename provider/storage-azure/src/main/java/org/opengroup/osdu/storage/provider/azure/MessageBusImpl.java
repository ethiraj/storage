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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.eventgrid.implementation.EventGridClientImpl;
import com.microsoft.azure.eventgrid.models.EventGridEvent;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.TopicClient;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Guid.GUID;
import org.joda.time.DateTime;
import org.opengroup.osdu.core.common.model.storage.PubSubInfo;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.storage.provider.interfaces.IMessageBus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.microsoft.azure.eventgrid.EventGridClient;
import com.microsoft.azure.eventgrid.TopicCredentials;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class MessageBusImpl implements IMessageBus {
    @Autowired
    private TopicClient topicClient;


    @Autowired
    private JaxRsDpsLog logger;


    @Override
    public void publishMessage(DpsHeaders headers, PubSubInfo... messages) {
        final int BATCH_SIZE = 50;
        Gson gson = new Gson();

        for (int i = 0; i < messages.length; i += BATCH_SIZE) {
            Message message = new Message();
            Map<String, Object> properties = new HashMap<>();

            // properties
            properties.put(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
            properties.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
            headers.addCorrelationIdIfMissing();
            properties.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
            message.setProperties(properties);

            // data
            PubSubInfo[] batch = Arrays.copyOfRange(messages, i, Math.min(messages.length, i + BATCH_SIZE));

            // add all to body {"message": {"data":[], "id":...}}
            JsonObject jo = new JsonObject();
            jo.add("data", gson.toJsonTree(batch));
            jo.addProperty(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
            jo.addProperty(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
            jo.addProperty(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
            JsonObject jomsg = new JsonObject();
            jomsg.add("message", jo);

            message.setBody(jomsg.toString().getBytes(StandardCharsets.UTF_8));
            message.setContentType("application/json");

            try {
                logger.info("Storage publishes message " + headers.getCorrelationId());
                topicClient.send(message);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
            }
        }
    }


    /*@Override
    public void publishMessage(DpsHeaders headers, PubSubInfo... messages)  {
        final int BATCH_SIZE = 50;
        Gson gson = new Gson();

        for (int i = 0; i < messages.length; i += BATCH_SIZE) {

            Map<String, Object> properties = new HashMap<>();

            // properties
            properties.put(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
            properties.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
            headers.addCorrelationIdIfMissing();
            properties.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
            //message.setProperties(properties);

            // data
            PubSubInfo[] batch = Arrays.copyOfRange(messages, i, Math.min(messages.length, i + BATCH_SIZE));

            // add all to body {"message": {"data":[], "id":...}}
            JsonObject jo = new JsonObject();
            jo.add("data", gson.toJsonTree(batch));
            jo.addProperty(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
            jo.addProperty(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
            jo.addProperty(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
            JsonObject jomsg = new JsonObject();
            jomsg.add("message", jo);

            //message.setBody(jomsg.toString().getBytes(StandardCharsets.UTF_8));
            //message.setContentType("application/json");
            TopicCredentials topicCredentials = new TopicCredentials("jUrTK5wrk3rozBjWkZFlLr45DqL5zsTN+6MYtge9l94=");
            //https://github.com/Azure-Samples/event-grid-java-publish-consume-events/blob/master/eventgrid-function-apps-producer-consumer/pom.xml
            EventGridClient client = new EventGridClientImpl(topicCredentials);
            String eventGridEndpoint = null;
            try {
                eventGridEndpoint = String.format("https://%s/", new URI("https://recordchanged.westus2-1.eventgrid.azure.net/api/events").getHost());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            //"https://recordchanged.westus2-1.eventgrid.azure.net/api/events";

            List<EventGridEvent> eventsList = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                eventsList.add(new EventGridEvent(
                        UUID.randomUUID().toString(),
                        String.format("Door%d", i),
                        jomsg,
                        "Contoso.Items.ItemReceived",
                        DateTime.now(),
                        "2.0"
                ));
            }

            try {
                logger.info("Storage publishes message " + headers.getCorrelationId());
                //topicClient.send(message);
                client.publishEvents(eventGridEndpoint, eventsList);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
            }
        }
    }*/
}
