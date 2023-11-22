package com.example.rqchallenge.employees;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Interface to centralize back-end HTTP/REST API access while hiding the underlying http client
 * implementation from callers.
 */
public interface IApiClient {

    /**
     * Send a request to the HttpClient.
     *
     * @param request HttpRequest to send
     * @throws ServiceException Chained exception indicating that a problem occurred
     */
    HttpResponse<String> send(HttpRequest request) throws IOException, ServiceException;

    /**
     * Construct HttpRequest with default properties set
     *
     * @return HttpRequest.Builder that can be modified
     */
    HttpRequest.Builder requestBuilder();


}
