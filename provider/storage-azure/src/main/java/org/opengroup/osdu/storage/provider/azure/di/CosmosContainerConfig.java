package org.opengroup.osdu.storage.provider.azure.di;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosmosContainerConfig {

    @Value("${cosmos.container.name}")
    private String containerName;

    @Bean
    public String cosmosContainer(){
        return containerName;
    }

}
