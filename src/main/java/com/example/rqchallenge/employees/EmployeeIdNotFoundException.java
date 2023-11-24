package com.example.rqchallenge.employees;

import static java.lang.String.format;

/**
 * Custom exception thrown when a supplied Employee ID is not found in the back-end service.
 */
public class EmployeeIdNotFoundException extends RuntimeException {

    public EmployeeIdNotFoundException(int id) {
        super(format("Employee ID %s could not be found.", id));
    }

}
