package com.example.blog.config;

import java.nio.ByteBuffer;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableCaching
class CacheConfig implements CachingConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    @ConfigurationProperties("spring.cache")
    CacheProperties cacheProperties() {
        return new CacheProperties();
    }

    @Bean
    CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            CacheProperties cacheProperties,
            tools.jackson.databind.ObjectMapper objectMapper) {
        var redisProperties = cacheProperties.getRedis();
        var valueSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);
        var valueSerializationPair = SerializationPair.just(
                buffer -> deserialize(valueSerializer, buffer),
                value -> ByteBuffer.wrap(valueSerializer.serialize(value)));
        var cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(redisProperties.getTimeToLive())
                .serializeValuesWith(valueSerializationPair);
        if (!redisProperties.isCacheNullValues()) {
            cacheConfiguration = cacheConfiguration.disableCachingNullValues();
        }
        if (redisProperties.isUseKeyPrefix()) {
            cacheConfiguration = cacheConfiguration.prefixCacheNameWith(redisProperties.getKeyPrefix());
        }
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .initialCacheNames(Set.copyOf(cacheProperties.getCacheNames()))
                .transactionAware()
                .build();
    }

    private static Object deserialize(GenericJacksonJsonRedisSerializer serializer, ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        var bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return serializer.deserialize(bytes);
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logCacheError("get", exception, cache);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logCacheError("put", exception, cache);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logCacheError("evict", exception, cache);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logCacheError("clear", exception, cache);
            }

            private void logCacheError(String operation, RuntimeException exception, Cache cache) {
                LOG.warn("Redis cache {} failed for '{}'", operation, cache.getName(), exception);
            }
        };
    }
}
