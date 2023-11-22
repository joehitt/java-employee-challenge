package com.example.rqchallenge.employees;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Default (non-caching API client).
 * @see IApiClient
 */
@Component
public class ApiClient implements IApiClient {

    private final HttpClient httpClient;

    ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .followRedirects(HttpClient.Redirect.NEVER)
                                    .connectTimeout(Duration.ofSeconds(20))
                                    .build();
    }

    /**
     * {@inheritDoc}
     * @see IApiClient#send(HttpRequest) 
     */
    @Override
    public HttpResponse<String> send(HttpRequest request) throws IOException, ServiceException {
        try {
            return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interruptedException) {
            throw new ServiceException("Thread was interrupted while calling HttpClient.", interruptedException);
        }
    }

    /**
     * {@inheritDoc}
     * @see IApiClient#requestBuilder() 
     */
    @Override
    public HttpRequest.Builder requestBuilder() {
        return HttpRequest.newBuilder()
                          .timeout(Duration.ofMinutes(2))
                          .header(ACCEPT, APPLICATION_JSON_VALUE)
                          .header(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

}