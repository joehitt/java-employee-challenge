package com.example.rqchallenge.employees;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * {@inheritDoc}
 *
 * @see IEmployeeController
 */
@RestController
public class EmployeeController implements IEmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);
    private final IEmployeeService service;

    public EmployeeController(@Autowired IEmployeeService employeeService) {
        this.service = employeeService;
    }

    /**
     * Given a callable, wrap the result in a ResponseEntity
     *
     * @param supplier Callable supplier that returns a list
     * @param <T>      Generic type contained by the list
     * @return Wrapped ResponseEntity containing the list
     */
    private static <T> ResponseEntity<List<T>> wrapList(Callable<List<T>> supplier) {
        return wrap(supplier);
    }

    private static <T> ResponseEntity<T> wrap(Callable<T> supplier) {
        try {
            return wrapOrThrow(supplier);
        } catch (NumberFormatException nfe) {
            log.error("Invalid numeric format submitted.", nfe);
            return ResponseEntity.badRequest()
                                 .build();
        } catch (IOException ioe) {
            log.error("IOException while wrapping result in ResponseEntity.", ioe);
            return ResponseEntity.internalServerError()
                                 .build();
        } catch (Exception ex) {
            // although this is not expected to occur (Throwable is caught by wrapOrThrow),
            // we handle it here to ensure a correct error code is returned
            log.error("Unexpected error while wrapping result in ResponseEntity.", ex);
            return ResponseEntity.internalServerError()
                                 .build();
        }
    }

    /**
     * Given a callable, wrap the result list in a ResponseEntity.  The purpose is to serve getAllEmployees,
     * which expects (by the method signature) to propagate IOException up the call stack.
     *
     * @param supplier Callable supplier that returns a list
     * @param <T>      Generic type contained by the list
     * @return Wrapped ResponseEntity containing the list
     */
    private static <T> ResponseEntity<List<T>> wrapListOrThrow(Callable<List<T>> supplier) throws IOException {
        return wrapOrThrow(supplier);
    }

    /**
     * Given a callable, wrap the result in a ResponseEntity.  The purpose is to serve getAllEmployees,
     * which expects (by the method signature) to propagate IOException up the call stack.
     *
     * @param supplier Callable supplier that returns a list
     * @param <T>      Generic type contained by the list
     * @return Wrapped ResponseEntity containing the list
     */
    private static <T> ResponseEntity<T> wrapOrThrow(Callable<T> supplier) throws IOException {
        try {
            T result = supplier.call();
            return ResponseEntity.ok(result);
        } catch (ServiceException e) {
            log.error("ServiceException while invoking service layer", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                 .build();
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            log.error("Unexpected error while invoking service layer", e);
            return ResponseEntity.internalServerError()
                                 .build();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#getAllEmployees()
     */
    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() throws IOException {
        try {
            return wrapListOrThrow(service::getAllEmployees);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                                 .build();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#getEmployeesByNameSearch(String)
     */
    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(@RequestParam String searchString) {
        return wrapList(() -> service.getAllEmployees()
                                     .stream()
                                     .filter(employee -> employee.getName()
                                                                   .contains(searchString))
                                     .collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#getEmployeeById(String)
     */
    @Override
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String id) {
        try {
            return service.getEmployeeById(Integer.parseInt(id))
                          .map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound()
                                                .build());
        } catch (ServiceException e) {
            if (e.getHttpStatus() == HttpStatus.NOT_FOUND.value()) {
                log.warn("Service Exception (getEmployeeById)", e);
                // NOT_FOUND should be propagated to the caller
                return ResponseEntity.status(HttpStatus.NOT_FOUND.value())
                                     .build();
            } else {
                // other back-end API errors are hidden and should propagate BAD_GATEWAY
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                     .build();
            }
        } catch (NumberFormatException nfe) {
            log.warn("ID format for getEmployeeId(String) was non-numeric.", nfe);
            return ResponseEntity.badRequest()
                                 .build();
        } catch (Exception e) {
            log.error("Exception while calling getEmployeeById(String).", e);
            return ResponseEntity.internalServerError()
                                 .build();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#getHighestSalaryOfEmployees()
     */
    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        return wrap(() -> service.getAllEmployees()
                                 .stream()
                                 .map(Employee::getSalary)
                                 .max(Integer::compareTo)
                                 .orElse(0));
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#getTopTenHighestEarningEmployeeNames()
     */
    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        return wrap(() -> service.getAllEmployees()
                                 .stream()
                                 .sorted(Comparator.comparingInt(Employee::getSalary)
                                                   .reversed())
                                 .limit(10)
                                 .map(Employee::getName)
                                 .collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#createEmployee(Map)
     */
    @Override
    public ResponseEntity<Employee> createEmployee(Map<String, Object> employeeInput) {
        log.info("EmployeeController.createEmployee(Map) called with data: {}", employeeInput);
        return wrap(() -> {
            log.info("EmployeeController.createEmployee, executing callable.");
            Employee input = Employee.fromMapInput(employeeInput);
            log.info("EmployeeController.createEmployee, mapped input employee {}", input);
            Employee output = service.createEmployee(input);
            log.info("EmployeeController.createEmployee, service returned employee {}", output);
            return output;
        });
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#deleteEmployeeById(String)
     */
    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        log.info("EmployeeController.deleteEmployeeById(String) called with id={}", id);
        try {
            int intId = Integer.parseInt(id);
            return wrap(() -> service.deleteEmployeeById(intId));
        } catch (NumberFormatException nfe) {
            return ResponseEntity.badRequest()
                                 .body("Employee ID should be numeric.");
        }
    }

}
