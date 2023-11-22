package com.example.rqchallenge.employees;

import java.io.IOException;
import java.util.List;
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
     * @throws IOException      Required to be propagated by IEmployeeController.getAllEmployees()
     * @throws ServiceException If some problem occurred
     */
    List<Employee> getAllEmployees() throws IOException, ServiceException;

    /**
     * Get a specific employee by ID
     *
     * @param id ID of desired Employee
     * @return Optional employee, Empty if the ID was not located.
     * @throws ServiceException If some problem occurred
     */
    Optional<Employee> getEmployeeById(int id) throws ServiceException;

    /**
     * Create a new Employee in the back-end
     *
     * @param employee Employee to create
     * @return Created employee object
     * @throws ServiceException If some problem occurred
     */
    Employee createEmployee(Employee employee) throws ServiceException;

    /**
     * Delete an Employee, returning the name of the deleted employee
     *
     * @param id ID of Employee to delete
     * @return Name of deleted employee
     * @throws ServiceException If some problem occurred
     */
    String deleteEmployeeById(int id) throws ServiceException;

}
