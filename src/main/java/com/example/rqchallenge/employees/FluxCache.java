package com.example.rqchallenge.employees;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Spring @Cacheable does not work with reactive WebFlux, so we implement a solution that meets the use case here.
 * To enable the cache to work within the reactive framework, the cache is called in 2 phases: upstream and downstream.
 * Upstream phase implements throttling and checks the cache, while downstream will update the cache based on data
 * returned from the WebClient (or other expensive operation).
 *
 * @see <a href="https://www.baeldung.com/spring-webflux-cacheable">Spring Webflux and @Cacheable Annotation</a>
 * @see <a href="https://copyprogramming.com/howto/spring-webflux-and-cacheable-annotation">Using @Cacheable Annotation with Spring Webflux</a>
 * @see <a href="https://stackoverflow.com/questions/71532361/spring-boot-reactive-caching">Spring boot Reactive caching</a>
 * @see <a href="https://github.com/ben-manes/caffeine/discussions/500">Integration Support with Spring Webflux?</a>
 * @see <a href="https://github.com/reactor/reactor-addons/issues/237">Deprecating cache utilities (removal in 3.6.0)</a>
 */
@Component
public class FluxCache<K, V> {

    private final ConcurrentHashMap<K, Mono<V>> data;
    private long throttleTimestamp = System.currentTimeMillis();
    /**
     * Minimum amount of time (in milliseconds) to wait between each request to the cache supplier.
     */
    @Value("${employee.cache.throttle-time-ms:7200000}")
    private long throttleTime;

    public FluxCache() {
        this.data = new ConcurrentHashMap<>();
    }

    private synchronized boolean checkThrottleExpired() {
        long now = System.currentTimeMillis();
        if (now > this.throttleTimestamp) {
            this.throttleTimestamp = now + this.throttleTime;
            return true;
        } else {
            return false;
        }
    }

    public Mono<Optional<V>> cacheUpstream(K key) {
        if (checkThrottleExpired()) {
            // intentionally do not evict here: the stale object is still last known good
            // and thus may be needed during cacheDownstream
            return Mono.just(Optional.empty());
        }
        Mono<V> maybeNull = data.get(key);
        if (maybeNull == null) {
            return Mono.just(Optional.empty());
        } else {
            return maybeNull.map(Optional::of);
        }
    }

    public Mono<Optional<V>> cacheDownstream(K key,
                                             Mono<Optional<V>> value) {
        return value.flatMap(optional -> {
            if (optional.isPresent()) {
                data.put(key, Mono.just(optional.get()));
            } else {
                data.remove(key);
            }
            return value;
        });
    }

    public Optional<Flux<V>> cacheUpstreamBulk() {
        if (checkThrottleExpired() || data.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Flux.concat(data.values()));
    }

    public Mono<List<V>> cacheDownstreamBulk(Mono<List<V>> monoList,
                                             Function<V, K> mapping) {
        return monoList.flatMap(list -> {
            data.clear();
            list.forEach(value -> data.put(mapping.apply(value), Mono.just(value)));
            return monoList;
        });
    }

    /**
     * A synchronous deletion.
     * @param key Key of entry to delete from cache.
     */
    public void deleteFromCache(K key) {
        data.remove(key);
    }

    /**
     * A synchronous addition to cache.
     * @param key Key of entry to add to the cache.
     * @param value Value to be mapped against the key.
     */
    public void put(K key, V value) {
        data.put(key, Mono.just(value));
    }

}
