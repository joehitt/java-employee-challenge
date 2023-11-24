package com.example.rqchallenge.employees;

/**
 * Exception used for any condition such that the API returns HTTP OK, but the "status" field is not "success".
 */
public class ApiResponseException extends RuntimeException {

    public ApiResponseException(String message) {
        super(message);
    }

}
