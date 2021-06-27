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

package org.opengroup.osdu.storage.provider.azure.di;

import lombok.Getter;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.storage.provider.azure.util.MDCContextMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Named;


@Component
@Getter
public class AzureBootstrapConfig {

    @Value("${azure.keyvault.url}")
    private String keyVaultURL;

    @Value("${azure.cosmosdb.database}")
    private String cosmosDBName;

    @Bean
    @Named("STORAGE_CONTAINER_NAME")
    public String containerName() {
        return "opendes";
    }

    @Bean
    @Primary
    public DpsHeaders DpsHeaders(){
        return new DpsHeaders();
    }
    
    @Bean
    @Named("KEY_VAULT_URL")
    public String keyVaultURL() {
        return keyVaultURL;
    }

    @Bean
    public MDCContextMap mdcContextMap() {
        return new MDCContextMap();
    }

    @Bean
    public String cosmosDBName() {
        return cosmosDBName;
    }

    /**
     * @return How large the batch size has to be for the bulk executor to be used instead of uploading record in serial.
     */
    @Bean
    public int minBatchSizeToUseBulkUpload() {
        if (System.getenv("MIN_BATCH_SIZE_TO_USE_BULK_UPLOAD") == null) return 50;
        else return Integer.parseInt(System.getenv("MIN_BATCH_SIZE_TO_USE_BULK_UPLOAD"));
    }

    /**
     * @return The maximum degree of concurrency per partition key range. The default value in the SDK is 20.
     */
    @Bean
    public int bulkImportMaxConcurrencyPePartitionRange() {
        if (System.getenv("BULK_IMPORT_MAX_CONCURRENCY_PER_PARTITION_RANGE") == null) return 20;
        else return Integer.parseInt(System.getenv("BULK_IMPORT_MAX_CONCURRENCY_PER_PARTITION_RANGE"));
    }
}
