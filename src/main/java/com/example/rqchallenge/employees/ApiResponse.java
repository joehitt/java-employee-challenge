package com.example.rqchallenge.employees;

import org.springframework.core.ParameterizedTypeReference;

import static java.lang.String.format;

/**
 * Container format returned from back-end API.
 */
@SuppressWarnings("unused")
class ApiResponse<T> {

    private static final String SUCCESS_STATUS = "success";

    private String status;
    private T data;
    private String message;

    public ApiResponse() {
        // intentionally empty
    }

    /**
     * Validate a given ApiResponse which may be null.
     *
     * @param response A given ApiResponse.
     * @param <T>      Generic type for 'data' field.
     * @return Object parsed from the 'data' field.
     * @throws ApiResponseException If the given response was null, or if the 'status' field was not 'success'.
     */
    public static <T> T validate(ApiResponse<T> response) throws ApiResponseException {
        if (response == null) {
            throw new ApiResponseException("API returned null response.");
        } else {
            return response.validate();
        }
    }

    public static <T> ParameterizedTypeReference<ApiResponse<T>> type() {
        return new ParameterizedTypeReference<>() {
        };
    }

    public boolean isSuccess() {
        return SUCCESS_STATUS.equalsIgnoreCase(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Validate this ApiResponse by checking the 'status' field.
     *
     * @return Value parsed from the 'data' field.
     * @throws ApiResponseException If the 'status' field was not 'success'.
     */
    public T validate() throws ApiResponseException {
        if (!this.isSuccess()) {
            throw new ApiResponseException(format("API returned a value for the 'status' field which was not '%s'",
                                                  SUCCESS_STATUS));
        }
        return data;
    }

}