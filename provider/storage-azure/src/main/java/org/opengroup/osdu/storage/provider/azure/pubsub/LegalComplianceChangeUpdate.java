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

package org.opengroup.osdu.storage.provider.azure.pubsub;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.microsoft.azure.servicebus.IMessage;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.legal.LegalCompliance;
import org.opengroup.osdu.core.common.model.legal.jobs.ComplianceMessagePushReceiver;
import org.opengroup.osdu.core.common.model.legal.jobs.ComplianceUpdateStoppedException;
import org.opengroup.osdu.core.common.model.legal.jobs.LegalTagChangedCollection;
import org.opengroup.osdu.storage.logging.StorageAuditLogger;
import org.opengroup.osdu.storage.provider.azure.config.ThreadDpsHeaders;
import org.opengroup.osdu.storage.provider.azure.config.ThreadScopeContext;
import org.opengroup.osdu.storage.provider.azure.config.ThreadScopeContextHolder;
import org.opengroup.osdu.storage.provider.azure.util.MDCContextMap;
import org.opengroup.osdu.storage.provider.interfaces.IRecordsMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class LegalComplianceChangeUpdate extends ComplianceMessagePushReceiver   {
    private final static Logger LOGGER = LoggerFactory.getLogger(LegalComplianceChangeUpdate.class);

    @Autowired
    private IRecordsMetadataRepository recordsRepo;
    @Autowired
    private StorageAuditLogger auditLogger;
    @Autowired
    private DpsHeaders headers;
    @Autowired
    private MDCContextMap mdcContextMap;
    @Autowired
    private ComplianceMessagePullReceiver complianceMessagePullReceiver;


    public Map<String, LegalCompliance> updateCompliance(IMessage message) throws ComplianceUpdateStoppedException , Exception{
        Map<String, LegalCompliance> output = new HashMap<>();
        Gson gson = new Gson();
        try {
            String messageBody = new String(message.getMessageBody().getBinaryData().get(0), UTF_8);
            JsonElement jsonRoot = JsonParser.parseString(messageBody);
            JsonElement messageData = jsonRoot.getAsJsonObject().get("data");
            String messageId = jsonRoot.getAsJsonObject().get("id").getAsString();
            message.setMessageId(messageId);

            String dataPartitionId = messageData.getAsJsonObject().get(DpsHeaders.DATA_PARTITION_ID).getAsString();
            String correlationId = messageData.getAsJsonObject().get(DpsHeaders.CORRELATION_ID).getAsString();
            String user = messageData.getAsJsonObject().get(DpsHeaders.USER_EMAIL).getAsString();
            //headers.setThreadContext(dataPartitionId, correlationId, user);
            MDC.setContextMap(mdcContextMap.getContextMap(headers.getCorrelationId(), headers.getCorrelationId()));

            LegalTagChangedCollection tags = gson.fromJson(messageData.getAsJsonObject().get("data"), LegalTagChangedCollection.class);
            complianceMessagePullReceiver.receiveMessage(tags, headers);
        } finally {
            ThreadScopeContextHolder.getContext().clear();
            MDC.clear();
        }
        return output;
    }
}