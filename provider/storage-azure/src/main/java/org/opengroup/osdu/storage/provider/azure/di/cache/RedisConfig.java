// Copyright 2017-2021, Schlumberger
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

package org.opengroup.osdu.storage.provider.azure.di.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

  @Configuration
  @ConditionalOnExpression(value = "'${cache.provider}' == 'redis' && '${redis.ssl.enabled:false}'")
  static class SslConfig {

    @Value("${redis.host}")
    private String host;

    @Value("${redis.port}")
    private int port;

    @Value("${redis.expiration}")
    private int expiration;

    @Value("${redis.database}")
    private int database;

    @Value("${redis.password}")
    private String password;

    @Bean
    public RedisCache<String, Groups> groupCache() {
      return new RedisCache<>(host, port, password, expiration, database, String.class, Groups.class);
    }

    @Bean("legalTagCache")
    public RedisCache<String, String> legalTagCache() {
      return new RedisCache<>(host, port, password, expiration, database, String.class, String.class);
    }

    @Bean
    public RedisCache<String, Schema> schemaCache() {
      return new RedisCache<>(host, port, password, expiration, database, String.class, Schema.class);
    }
  }

  @Configuration
  @ConditionalOnExpression(value = "'${cache.provider}' == 'redis' && !'${redis.ssl.enabled:true}'")
  static class NoSslConfig {

    @Value("${redis.host}")
    private String host;

    @Value("${redis.port}")
    private int port;

    @Value("${redis.database}")
    private int database;

    @Value("${redis.expiration}")
    private int expiration;

    @Bean
    public RedisCache<String, Groups> groupCache() {
      return new RedisCache<>(host, port, expiration, database, String.class, Groups.class);
    }

    @Bean("legalTagCache")
    public RedisCache<String, String> legalTagCache() {
      return new RedisCache<>(host, port, expiration, database, String.class, String.class);
    }

    @Bean
    public RedisCache<String, Schema> schemaCache() {
      return new RedisCache<>(host, port, expiration, database, String.class, Schema.class);
    }
  }

}
