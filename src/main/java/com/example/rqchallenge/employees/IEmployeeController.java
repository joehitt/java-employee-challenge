package com.example.rqchallenge.employees;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public interface IEmployeeController {

    /**
     * Return all employees
     *
     * @return Response list of employees
     * @throws IOException Upon input/output problem
     */
    @GetMapping()
    ResponseEntity<List<Employee>> getAllEmployees() throws IOException;

    /**
     * Return all employees whose name contains or matches the string input provided
     *
     * @param searchString Case-sensitive name search
     * @return List of matching employees
     */
    @GetMapping("/search/{searchString}")
    ResponseEntity<List<Employee>> getEmployeesByNameSearch(@PathVariable String searchString);

    /**
     * Get an employee by ID
     *
     * @param id Desired employee's ID
     * @return Single matching employee
     */
    @GetMapping("/{id}")
    ResponseEntity<Employee> getEmployeeById(@PathVariable String id);

    /**
     * Get the highest salary
     *
     * @return Single integer indicating the highest salary of all employees
     */
    @GetMapping("/highestSalary")
    ResponseEntity<Integer> getHighestSalaryOfEmployees();

    /**
     * Get a list of the top 10 employees based off of their salaries
     *
     * @return List of the top 10 employees
     */
    @GetMapping("/topTenHighestEarningEmployeeNames")
    ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames();

    /**
     * Create an Employee
     *
     * @param employeeInput Request with body containing a map of intended employee data.
     * @return Response with status of success or failed based on if an employee was created
     */
    @PostMapping()
    ResponseEntity<Employee> createEmployee(@RequestBody Map<String, Object> employeeInput);

    /**
     * Delete an Employee
     *
     * @param id ID of employee to delete
     * @return Response containing name of the employee that was deleted
     */
    @DeleteMapping("/{id}")
    ResponseEntity<String> deleteEmployeeById(@PathVariable String id);

}
