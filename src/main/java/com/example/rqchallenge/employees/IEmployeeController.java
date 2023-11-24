package com.example.rqchallenge.employees;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Reactive controller interface for Employee REST service.
 */
@RestController
public interface IEmployeeController {

    /**
     * Return all employees
     *
     * @return All employees
     */
    @GetMapping()
    Flux<Employee> getAllEmployees();

    /**
     * Return all employees whose name contains or matches the string input provided
     *
     * @param searchString Case-sensitive name search
     * @return Matching employees
     */
    @GetMapping("/search/{searchString}")
    Flux<Employee> getEmployeesByNameSearch(@PathVariable String searchString);

    /**
     * Get an employee by ID
     *
     * @param id Desired employee's ID
     * @return Single matching employee
     */
    @GetMapping("/{id}")
    Mono<Employee> getEmployeeById(@PathVariable String id);

    /**
     * Get the highest salary
     *
     * @return Single integer indicating the highest salary of all employees
     */
    @GetMapping("/highestSalary")
    Mono<Integer> getHighestSalaryOfEmployees();

    /**
     * Get a list of the top 10 employees based off of their salaries
     *
     * @return Top 10 employees
     */
    @GetMapping("/topTenHighestEarningEmployeeNames")
    Flux<String> getTopTenHighestEarningEmployeeNames();

    /**
     * Create an Employee
     *
     * @param employeeInput Request with body containing a map of intended employee data.
     * @return Response with status of success or failed based on if an employee was created
     */
    @PostMapping()
    Mono<Employee> createEmployee(@RequestBody Map<String, Object> employeeInput);

    /**
     * Delete an Employee
     *
     * @param id ID of employee to delete
     * @return Response containing name of the employee that was deleted
     */
    @DeleteMapping("/{id}")
    Mono<String> deleteEmployeeById(@PathVariable String id);

}
