package org.opengroup.osdu.storage.provider.azure.di.cache;

import javax.inject.Named;

import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.security.keyvault.secrets.SecretClient;

@Configuration
public class RedisConfig {

  @Value("${redis.host}")
  private String redisHost;

  @Bean
  @Named("REDIS_HOST")
  public String redisHost(SecretClient kv) {
    if (redisHost == null || redisHost.isEmpty()) {
      redisHost = KeyVaultFacade.getSecretWithValidation(kv, "redis-hostname");
    }
    return redisHost;
  }

  @Configuration
  @ConditionalOnExpression(value = "'${cache.provider}' == 'redis' && '${redis.ssl.enabled:false}'")
  static class SslConfig {

    @Value("${redis.port}")
    private int port;

    @Value("${redis.expiration}")
    private int expiration;

    @Value("${redis.database}")
    private int database;

    @Value("${redis.password}")
    private String password;

    @Bean
    public RedisCache<String, Groups> groupCache(@Named("REDIS_HOST") String host) {
      return new RedisCache<>(host, port, password, expiration, database, String.class, Groups.class);
    }

    @Bean("legalTagCache")
    public RedisCache<String, String> legalTagCache(@Named("REDIS_HOST") String host) {
      return new RedisCache<>(host, port, password, expiration, database, String.class, String.class);
    }

    @Bean
    public RedisCache<String, Schema> schemaCache(@Named("REDIS_HOST") String host) {
      return new RedisCache<>(host, port, password, expiration, database, String.class, Schema.class);
    }
  }

  @Configuration
  @ConditionalOnExpression(value = "'${cache.provider}' == 'redis' && !'${redis.ssl.enabled:true}'")
  static class NoSslConfig {

    @Value("${redis.port}")
    private int port;

    @Value("${redis.database}")
    private int database;

    @Value("${redis.expiration}")
    private int expiration;

    @Bean
    public RedisCache<String, Groups> groupCache(@Named("REDIS_HOST") String host) {
      return new RedisCache<>(host, port, expiration, database, String.class, Groups.class);
    }

    @Bean("legalTagCache")
    public RedisCache<String, String> legalTagCache(@Named("REDIS_HOST") String host) {
      return new RedisCache<>(host, port, expiration, database, String.class, String.class);
    }

    @Bean
    public RedisCache<String, Schema> schemaCache(@Named("REDIS_HOST") String host) {
      return new RedisCache<>(host, port, expiration, database, String.class, Schema.class);
    }
  }

}
