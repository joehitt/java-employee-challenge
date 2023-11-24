package com.example.rqchallenge.employees;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring configuration, primarily used to construct the WebClient.
 */
@Configuration
public class ApplicationConfiguration {

    @Value("${employee.base-uri}")
    private String baseUrl;

    @Bean
    public WebClient webClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(baseUrl)
                               .defaultHeader(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON.toString())
                               .build();
    }

}
