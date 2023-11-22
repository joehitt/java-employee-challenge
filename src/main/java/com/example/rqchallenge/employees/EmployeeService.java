package com.example.rqchallenge.employees;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;


/**
 * {@inheritDoc}
 *
 * @see com.example.rqchallenge.employees.IEmployeeService
 */
@Service
public class EmployeeService implements IEmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final IApiClient apiClient;

    @Value("${employee.base-uri}")
    private String baseUrl;

    public EmployeeService(@Autowired IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    private static <T> T parseResponseData(ApiResponse<T> apiResponse) {
        T data = apiResponse.getData();
        if (data instanceof LinkedHashMap) {
            log.debug("Converting LinkedHashMap data to object.");
            Employee employee = Employee.fromMapOutput(uncheckedCast(data));
            // we assume here that if we received a LinkedHaspMap, then T is Employee
            // (this could be improved)
            return uncheckedCast(employee);
        } else {
            log.info("Returning transformed data from API: {}", data);
            return data;
        }
    }

    private static <T> void checkApiSuccess(ApiResponse<T> apiResponse) throws ServiceException {
        if (!apiResponse.isSuccess()) {
            String errorMessage = format("Failed to call API due to: %s", apiResponse.getMessage());
            log.error(errorMessage);
            throw new ServiceException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private static void checkHttpOk(HttpRequest request,
                                    HttpResponse<String> response) throws ServiceException {
        int status = response.statusCode();
        log.info("Back-end HTTP API call returned status code={}", status);
        if (status < HttpStatus.OK.value() && status >= HttpStatus.MULTIPLE_CHOICES.value()) {
            // we choose not to log the body here, as it could contain sensitive information
            String errorMessage = format("%s %s returned unsuccessful HTTP status code=%s",
                                         request.method(),
                                         request.uri(),
                                         response.statusCode());
            log.error(errorMessage);
            throw new ServiceException(errorMessage, response.statusCode());
        }
    }

    /**
     * Private utility method to handle unchecked casting warnings
     */
    @SuppressWarnings({"unchecked"})
    private static <T> T uncheckedCast(Object obj) {
        return (T) obj;
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#getAllEmployees()
     */
    @Cacheable("employees")
    @Override
    public List<Employee> getAllEmployees() throws IOException, ServiceException {
        return callOrThrowIOException(apiClient.requestBuilder()
                                               .GET()
                                               .uri(URI.create(baseUrl + "/employees"))
                                               .build());
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#getEmployeeById(int)
     */
    @Cacheable(value = "employees",
               key = "new org.springframework.cache.interceptor.SimpleKey(#id)")
    @Override
    public Optional<Employee> getEmployeeById(int id) throws ServiceException {
        Employee employee = call(apiClient.requestBuilder()
                                          .GET()
                                          .uri(URI.create(baseUrl + format("/employee/%s", id)))
                                          .build());
        return Optional.of(employee);
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#createEmployee(Employee)
     */
    @Override
    @SuppressWarnings("SpringCacheableMethodCallsInspection")
    public Employee createEmployee(Employee employee) throws ServiceException {
        try {
            log.info("Method createEmployee(Employee) called with data: {}", employee);
            Map<String, Object> returnData = call(apiClient.requestBuilder()
                                                           .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                                                                   employee), StandardCharsets.UTF_8))
                                                           .uri(URI.create(baseUrl + "/create"))
                                                           .build());
            int id = Integer.parseInt((String) returnData.get("id"));
            log.info("Created new Employee in back-end with ID, {}", id);
            return this.getEmployeeById(id)
                       .orElseThrow(() -> new ServiceException(
                               "Create succeeded, but the created Employee object could not be verified.  Please note that the back-end state is now indeterminate.",
                               HttpStatus.BAD_GATEWAY.value()));
        } catch (JsonProcessingException jsonException) {
            throw new ServiceException("Failed to map employee object to JSON.", jsonException);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeService#deleteEmployeeById(int)
     */
    @Override
    @SuppressWarnings("SpringCacheableMethodCallsInspection")
    public String deleteEmployeeById(int id) throws ServiceException {
        // need to load Employee to get returned name
        Optional<String> name = this.getEmployeeById(id)
                                    .map(Employee::getName);
        if (name.isPresent()) {
            call(apiClient.requestBuilder()
                          .DELETE()
                          .uri(URI.create(baseUrl + format("/delete/%s", id)))
                          .build());
            return name.get();
        } else {
            throw new ServiceException("Employee name does not exist for given ID.", HttpStatus.NOT_FOUND.value());
        }

    }

    @CacheEvict(value = "employees")
    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.HOURS)
    public void evictMoviesFromCache() {
        // annotations handle the eviction
        log.info("Evicting Employees from cache.");
    }

    /**
     * Intercept IOException and throw a wrapped ServiceException
     * (for cases where it should not be propagated up the call stack).
     *
     * @param request The HttpRequest
     * @return T Generic return type from the response body
     * @throws ServiceException If there was an unexpected problem (including possible IOException)
     */
    private <T> T call(HttpRequest request) throws ServiceException {
        try {
            return callOrThrowIOException(request);
        } catch (IOException ioException) {
            throw new ServiceException("IOException thrown while calling external API", ioException);
        }
    }

    /**
     * Submit a request to the external API, parse and return results (while handling many edge cases).
     *
     * @param request HttpRequest object
     * @return Result parsed from external API response
     * @throws IOException      Underlying IOException allowed to bubble to
     * @throws ServiceException Other unexpected problem the caller needs to contend with such as HTTP failure codes
     */
    private <T> T callOrThrowIOException(HttpRequest request) throws IOException, ServiceException {

        log.info("Invoking URL: {}", request.uri());
        HttpResponse<String> response = apiClient.send(request);
        checkHttpOk(request, response);
        ApiResponse<T> apiResponse = objectMapper.readValue(response.body(), new TypeReference<>() {
        });
        checkApiSuccess(apiResponse);
        String requestMethod = request.method();
        log.info("{} {} status={}, message={}",
                 requestMethod,
                 request.uri(),
                 apiResponse.getStatus(),
                 apiResponse.getMessage());
        return parseResponseData(apiResponse);
    }

    /**
     * Container format returned from the back-end API containing status, message, and data.
     *
     * @param <T> Generic data parameter that can very based on which API call is invoked.
     */
    private static class ApiResponse<T> {

        private String status;
        private T data;
        private String message;

        public ApiResponse() {
            // intentionally empty
        }

        public boolean isSuccess() {
            return "success".equalsIgnoreCase(status);
        }

        public String getStatus() {
            return status;
        }

        @SuppressWarnings("unused")
        public void setStatus(String status) {
            this.status = status;
        }

        public T getData() {
            return data;
        }

        @SuppressWarnings("unused")
        public void setData(T data) {
            this.data = data;
        }

        public String getMessage() {
            return message;
        }

        @SuppressWarnings("unused")
        public void setMessage(String message) {
            this.message = message;
        }

    }

}



