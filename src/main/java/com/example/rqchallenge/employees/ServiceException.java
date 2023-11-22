package com.example.rqchallenge.employees;

import org.springframework.http.HttpStatus;

/**
 * Custom exception allowing propagation of status codes from the service layer.
 * This can allow the caller to choose which action to take, such as retrying a failed operation.
 */
public class ServiceException extends Exception {

    private final int httpStatus;

    public ServiceException(String message,
                            Throwable cause) {
        super(message, cause);
        // default to the blanket "something went wrong on our end" code
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    public ServiceException(String message,
                            int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /**
     * Code based on HTTP semantics which may or may not be interpreted by the caller to take corrective action,
     * generally should be propagated back to a calling client.  Note that this might not be the same code returned by
     * a dependent API.
     *
     * @return HTTP status code, with HttpStatus.INTERNAL_SERVER_ERROR as the default value
     * @see HttpStatus#INTERNAL_SERVER_ERROR
     */
    public int getHttpStatus() {
        return httpStatus;
    }
}