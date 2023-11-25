package com.example.rqchallenge.employees;

import static java.lang.String.format;

/**
 * Custom exception thrown when an expected ID was not found.
 */
public class IdNotFoundException extends RuntimeException {

    public <T> IdNotFoundException(T id) {
        super(format("ID %s could not be found.", id));
    }

}
