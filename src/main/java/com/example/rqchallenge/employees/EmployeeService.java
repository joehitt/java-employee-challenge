package com.example.rqchallenge.employees;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * {@inheritDoc}
 *
 * @see com.example.rqchallenge.employees.IEmployeeService
 */
@Service
public class EmployeeService implements IEmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    private final WebClient client;
    private final FluxCache<Integer, Employee> cache;

    /**
     * Maximum number of retries to attempt for a GET operation.
     */
    @Value("${employee.retry.get.max:3}")
    private int retryGetMax;

    /**
     * Duration (in ms) to wait on average between GET retries.
     */
    @Value("${employee.retry.get.duration.ms:2200}")
    private int retryGetDurationMs;

    /**
     * Duration (in ms) to wait on average between non-idempotent retries.
     */
    @Value("${employee.retry.change.duration.ms:2200}")
    private int retryChangeDurationMs;

    /**
     * Maximum number of retires to attempt for non-idempotent operations.
     */
    @Value("${employee.retry.change.max:5}")
    private int retryChangeMax;

    /**
     * Message to return on retry exhaustion.
     */
    @Value("${employee.retry.message:The operation could not be completed after several attempts.  We apologize for the inconvenience, please try again later.}")
    private String retryMessage;

    /**
     * Message to return on general service unavailability.
     */
    @Value("${employee.unavailable.message:The service is currently unavailable.  We apologize for the inconvenience, please try again later.}")
    private String unavailableMessage;

    @Autowired
    public EmployeeService(@Autowired WebClient client,
                           @Autowired FluxCache<Integer, Employee> cache) {
        this.client = client;
        this.cache = cache;
    }


    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#getAllEmployees()
     */
    @Override
    public Flux<Employee> getAllEmployees() {
        return cache.cacheUpstreamBulk() // upstream cache access
                    .orElseGet(() -> client.get()
                                           .uri("/employees")
                                           .retrieve()
                                           .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                                                     response -> response.bodyToMono(String.class)
                                                                         .map(b -> new ServiceException(
                                                                                 unavailableMessage)))
                                           .bodyToMono(ApiResponse.<List<Map<String, Object>>>type())
                                           .map(ApiResponse::getData)
                                           .flatMap(list -> Mono.just(list.stream()
                                                                          .map(Employee::fromMapOutput)
                                                                          .collect(Collectors.toList())))
                                           // downstream cache access
                                           .transform(list -> cache.cacheDownstreamBulk(list, Employee::getId))
                                           // convert Mono<List<T> to Flux<T>
                                           .flatMapIterable(list -> list));
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#getEmployeeById(int)
     */
    @Override
    public Mono<Optional<Employee>> getEmployeeById(int id) {
        return cache.cacheUpstream(id) // upstream cache access
                    .flatMap(optionalEmployee -> {
                        if (optionalEmployee.isEmpty()) {
                            return client.get()
                                         .uri(format("/employee/%s", id))
                                         .retrieve()
                                         .onStatus(HttpStatus.NOT_FOUND::equals,
                                                   response -> response.bodyToMono(String.class)
                                                                       .map(x -> new IdNotFoundException(id)))
                                         .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                                                   response -> response.bodyToMono(String.class)
                                                                       .map(b -> new ServiceException(unavailableMessage)))
                                         .bodyToMono(ApiResponse.<Map<String, Object>>type())
                                         .retryWhen(Retry.backoff(retryGetMax, Duration.ofMillis(retryGetDurationMs))
                                                         .jitter(0.35)
                                                         .filter(throwable -> throwable instanceof ServiceException)
                                                         .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                                             throw new ServiceException(retryMessage);
                                                         }))
                                         .map(response -> Optional.of(response.getData()))
                                         .map(opt -> opt.map(Employee::fromMapOutput))
                                         // downstream cache access
                                         .transform(mono -> cache.cacheDownstream(id, mono));
                        } else {
                            return Mono.just(optionalEmployee);
                        }
                    });
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#createEmployee(Map)
     */
    @Override
    public Mono<Employee> createEmployee(Map<String, Object> nameValuePairs) {
        return client.post()
                     .uri("/create")
                     .body(BodyInserters.fromValue(nameValuePairs))
                     .retrieve()
                     .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                               response -> response.bodyToMono(String.class)
                                                   .map(b -> new ServiceException(unavailableMessage)))
                     .bodyToMono(ApiResponse.<LinkedHashMap<String, Object>>type())
                     .retryWhen(Retry.backoff(retryChangeMax, Duration.ofMillis(retryChangeDurationMs))
                                     .jitter(0.35)
                                     .filter(throwable -> throwable instanceof ServiceException)
                                     .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                         throw new ServiceException(retryMessage);
                                     }))
                     .map(ApiResponse::getData)
                     .map(Employee::fromMapOutputCreate)
                     .flatMap(employee -> {
                         cache.put(employee.getId(), employee);
                         return Mono.just(employee);
                     });
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#deleteEmployeeById(int)
     */
    @Override
    public Mono<String> deleteEmployeeById(int id) {
        // @formatter:off
        return getEmployeeById(id)
                .flatMap(optionalEmployee -> optionalEmployee
                  .map(employee -> {
                    cache.deleteFromCache(id);
                    return client
                      .delete()
                      .uri(format("/delete/%s", id))
                      .retrieve()
                      .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                        response -> response.bodyToMono(String.class)
                                            .map(b -> new ServiceException(unavailableMessage)))
                      .bodyToMono(ApiResponse.<String>type())
                      .retryWhen(Retry.backoff(retryChangeMax, Duration.ofMillis(retryChangeDurationMs))
                                      .jitter(0.35)
                                      .filter(throwable -> throwable instanceof ServiceException)
                                      .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                          throw new ServiceException(retryMessage);
                                      })
                      );
                  })
                  .orElseThrow(() -> new IdNotFoundException(id))
                  .map(ApiResponse::getData));
        // @formatter:on
    }

    @PostConstruct
    public void init() {
        // attempt to load all employees on startup
        try {
            getAllEmployees().blockLast(Duration.ofSeconds(120));
        } catch (ServiceException e) {
            log.warn("Could not load employees on startup, e");
        }
    }

}



