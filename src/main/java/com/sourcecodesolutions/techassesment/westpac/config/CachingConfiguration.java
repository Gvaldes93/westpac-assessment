package com.sourcecodesolutions.techassesment.westpac.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Caching runs outermost (before the transaction interceptor) so a cache hit never opens a database
 * transaction. Cache failures are logged and the call falls through to the database: Redis is an
 * optimisation, not a dependency the API should fail on.
 *
 * <p>TODO: add a circuit breaker around Redis so an outage does not cost every request a timeout.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
class CachingConfiguration implements CachingConfigurer {

    private static final boolean LOG_STACK_TRACES = true;

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(CachingConfiguration.class.getName(), LOG_STACK_TRACES);
    }
}
