package com.example.rqchallenge.employees;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    private final WebClient client;

    private final FluxCache<Integer, Employee> cache;

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
                                                                         .map(ServiceException::new))
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
                                                                       .map(ServiceException::new))
                                         .bodyToMono(ApiResponse.<Employee>type())
                                         .map(response -> Optional.of(response.getData()))
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
                                                   .map(ServiceException::new))
                     .bodyToMono(ApiResponse.<Employee>type())
                     .map(ApiResponse::getData)
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
                                     return client.get()
                                          .uri(format("/delete%s", id))
                                          .retrieve()
                                          .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                                                    response -> response.bodyToMono(String.class)
                                                                        .map(ServiceException::new))
                                          .bodyToMono(ApiResponse.<String>type());
                        })
                        .orElseThrow(() -> new IdNotFoundException(id))
                        .map(ApiResponse::getData));
        // @formatter:on
    }

}



