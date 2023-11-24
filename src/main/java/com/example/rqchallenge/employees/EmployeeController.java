package com.example.rqchallenge.employees;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;

/**
 * {@inheritDoc}
 *
 * @see IEmployeeController
 */
@RestController
public class EmployeeController implements IEmployeeController {

    private final IEmployeeService service;

    public EmployeeController(@Autowired IEmployeeService employeeService) {
        this.service = employeeService;
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#getAllEmployees()
     */
    @Override
    public Flux<Employee> getAllEmployees() {
        return service.getAllEmployees();
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#getEmployeesByNameSearch(String)
     */
    @Override
    public Flux<Employee> getEmployeesByNameSearch(@RequestParam String searchString) {
        return service.getAllEmployees()
                      .filterWhen(e -> Mono.just(e.getName()
                                                  .contains(searchString)));
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#getEmployeeById(String)
     */
    @Override
    public Mono<Employee> getEmployeeById(@PathVariable String id) {
        int intId = Integer.parseInt(id);
        return service.getEmployeeById(intId)
                      .flatMap(x -> x.map(Mono::just)
                                     .orElseThrow(() -> new EmployeeIdNotFoundException(intId)));
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#getHighestSalaryOfEmployees()
     */
    @Override
    public Mono<Integer> getHighestSalaryOfEmployees() {
        return service.getAllEmployees()
                      .flatMap(e -> Mono.just(e.getSalary()))
                      .reduce((e1, e2) -> e1 > e2 ? e1 : e2);
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#getTopTenHighestEarningEmployeeNames()
     */
    @Override
    public Flux<String> getTopTenHighestEarningEmployeeNames() {
        return service.getAllEmployees()
                      .sort(Comparator.comparingInt(Employee::getSalary)
                                      .reversed())
                      .take(10)
                      .map(Employee::getName);
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#createEmployee(Map)
     */
    @Override
    public Mono<Employee> createEmployee(Map<String, Object> employeeInput) {
        return service.createEmployee(employeeInput);
    }

    /**
     * {@inheritDoc}
     *
     * @see IEmployeeController#deleteEmployeeById(String)
     */
    @Override
    public Mono<String> deleteEmployeeById(String id) {
        return service.deleteEmployeeById(Integer.parseInt(id));
    }

}
