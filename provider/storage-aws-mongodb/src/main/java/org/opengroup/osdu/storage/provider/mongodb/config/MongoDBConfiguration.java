package org.opengroup.osdu.storage.provider.mongodb.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan({"org.opengroup.osdu.storage.provider", "org.opengroup.osdu.core.mongodb"})
@Configuration
public class MongoDBConfiguration {

}
