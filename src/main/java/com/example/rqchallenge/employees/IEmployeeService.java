package com.example.rqchallenge.employees;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * Service Layer interface for accessing Employee data - a wrapper for the back-end API capabilities.
 * Use of an interface here allows easier mocking of the back-end API calls.
 */
public interface IEmployeeService {

    /**
     * Return a list of all employees.
     *
     * @return Employee list
     */
    Flux<Employee> getAllEmployees();

    /**
     * Get a specific employee by ID
     *
     * @param id ID of desired Employee
     * @return Optional employee, Empty if the ID was not located.
     */
    Mono<Optional<Employee>> getEmployeeById(int id);

    /**
     * Create a new Employee in the back-end
     *
     * @param employee Employee to create
     * @return Created employee object
     */
    Mono<Employee> createEmployee(Map<String, Object> employee);

    /**
     * Delete an Employee, returning the name of the deleted employee
     *
     * @param id ID of Employee to delete
     * @return Name of deleted employee
     */
    Mono<String> deleteEmployeeById(int id);

}
