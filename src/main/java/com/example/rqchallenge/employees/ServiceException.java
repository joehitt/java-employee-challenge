package com.example.rqchallenge.employees;

/**
 * Some problem occurred that is blocking service, generally in a dependent API.
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }

}
