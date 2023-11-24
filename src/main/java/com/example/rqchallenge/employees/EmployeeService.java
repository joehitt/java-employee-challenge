package com.example.rqchallenge.employees;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;


/**
 * {@inheritDoc}
 *
 * @see com.example.rqchallenge.employees.IEmployeeService
 *
 * Caveat: Need to build an aspect/decorator to provide a workable solution for sharing cache items between methods
 * that return a Collection and those that return a single-object.  In addition, the solution needs to work with
 * Mono/Flux return types.  Currently, this code can only cache the single objects separately and API calls that
 * return 429 - Too Many Requests will fail unless that specific ID has preveiously succeeded.
 * There are some disparate solutions here: https://github.com/qaware/collection-cacheable-for-spring
 * and here: https://github.com/shaikezr/async-cacheable
 * .. but neither of these appear to solve the combined use case.
 */
@Service
public class EmployeeService implements IEmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final WebClient client;

    @Autowired
    public EmployeeService(@Autowired WebClient client) {
        this.client = client;
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#getAllEmployees()
     */
    @Cacheable(value = "employees")
    @Override
    public Flux<Employee> getAllEmployees() {
        return client.get()
                     .uri("/employees")
                     .retrieve()
                     .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                               response -> response.bodyToMono(String.class)
                                                   .map(ServiceException::new))
                     .bodyToMono(ApiResponse.<List<Employee>>type())
                     .map(ApiResponse::getData)
                     .flatMapIterable(list -> list)
                     .cache(1000, Duration.ofHours(2));
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#getEmployeeById(int)
     */
    @Cacheable(value = "employee",
               key = "new org.springframework.cache.interceptor.SimpleKey(#id)")
    @Override
    public Mono<Optional<Employee>> getEmployeeById(int id) {
        return client.get()
                     .uri(format("/employee/%s", id))
                     .retrieve()
                     .onStatus(HttpStatus.NOT_FOUND::equals,
                               response -> response.bodyToMono(String.class)
                                                   .map(x -> new EmployeeIdNotFoundException(id)))
                     .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                               response -> response.bodyToMono(String.class)
                                                   .map(ServiceException::new))
                     .bodyToMono(ApiResponse.<Employee>type())
                     .map(response -> Optional.of(response.getData()))
                     .cache(Duration.ofHours(2));
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#createEmployee(Map)
     */
    @Override
    public Mono<Employee> createEmployee(Map<String, Object> employee) {
        return client.post()
                     .uri("/create")
                     .body(BodyInserters.fromValue(employee))
                     .retrieve()
                     .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                               response -> response.bodyToMono(String.class)
                                                   .map(ServiceException::new))
                     .bodyToMono(ApiResponse.<Employee>type())
                     .map(ApiResponse::getData);
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#deleteEmployeeById(int)
     */
    @Override
    @SuppressWarnings("SpringCacheableMethodCallsInspection")
    public Mono<String> deleteEmployeeById(int id) {
        // @formatter:off
        return getEmployeeById(id)
                .flatMap(optionalEmployee -> optionalEmployee
                        .map(employee -> client.get()
                                          .uri(format("/delete%s", id))
                                          .retrieve()
                                          .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                                                    response -> response.bodyToMono(String.class)
                                                                        .map(ServiceException::new))
                                          .bodyToMono(ApiResponse.<String>type()))
                        .orElseThrow(() -> new EmployeeIdNotFoundException(id))
                        .map(ApiResponse::getData));
        // @formatter:on
    }

    @CacheEvict(value = "employees")
    @Scheduled(fixedDelay = 1,
               timeUnit = TimeUnit.HOURS)
    public void evictFromEmployeesCache() {
        // annotations handle the eviction
        log.info("Evicting employees from cache.");
    }

    @CacheEvict(value = "employee")
    @Scheduled(fixedDelay = 1,
               timeUnit = TimeUnit.HOURS)
    public void evictFromEmployeeCache() {
        // annotations handle the eviction
        log.info("Evicting from employee cache.");
    }

}



