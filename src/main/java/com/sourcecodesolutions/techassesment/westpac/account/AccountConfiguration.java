package com.sourcecodesolutions.techassesment.westpac.account;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AccountProperties.class)
class AccountConfiguration {

    static final String ACCOUNTS_CACHE = "accounts";

    /** Stores cached accounts as typed JSON (not JDK serialisation) with a configurable TTL. */
    @Bean
    RedisCacheManagerBuilderCustomizer accountsCacheCustomizer(AccountProperties properties) {
        RedisCacheConfiguration accountsCache = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.cacheTtl())
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(AccountResponse.class)));
        return builder -> builder.withCacheConfiguration(ACCOUNTS_CACHE, accountsCache);
    }
}
