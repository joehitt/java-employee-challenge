package com.example.rqchallenge.employees;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(NumberFormatException.class)
    public String numberFormatException(NumberFormatException e) {
        String message = "Request contained invalid numeric format.";
        log.warn(message, e);
        return message;
    }

    @ResponseStatus(NOT_FOUND)
    @ResponseBody
    @ExceptionHandler(EmployeeIdNotFoundException.class)
    public String employeeIdNotFoundException(EmployeeIdNotFoundException e) {
        String message = "The requested employee ID could not be found.";
        log.warn(message, e);
        return message;
    }

    /**
     * Back-end API returned a value of the 'status' field that was not 'success'.
     */
    @ResponseStatus(BAD_GATEWAY)
    @ResponseBody
    @ExceptionHandler(ApiResponseException.class)
    public String apiResponseException(ApiResponseException e) {
        log.error("API returned as status code that was not 'success'.", e);
        return e.getMessage();
    }

    /**
     * Back-end API incurred some other unexpected problem.
     * Note that caching should mitigate this problem generally, however there
     * may be cases (such as on startup) where we simply cannot obtain data.
     */
    @ResponseStatus(BAD_GATEWAY)
    @ResponseBody
    @ExceptionHandler(ServiceException.class)
    public String serviceException(ServiceException e) {
        log.error("Back-end API not available.", e);
        return "The service is unavailable, please try again later.";
    }

}
