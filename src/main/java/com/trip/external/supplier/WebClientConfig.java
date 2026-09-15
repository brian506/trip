package com.trip.external.supplier;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(SupplierProperties.class)
public class WebClientConfig {

    @Bean
    public WebClient supplierA(WebClient.Builder builder, SupplierProperties properties) {
        return build(builder, properties, Supplier.A);
    }

    @Bean
    public WebClient supplierB(WebClient.Builder builder, SupplierProperties properties) {
        return build(builder, properties, Supplier.B);
    }

    private WebClient build(WebClient.Builder builder, SupplierProperties properties, Supplier supplier) {
        SupplierEndpoint endpoint = properties.connectEndpoint(supplier);
        ClientHttpConnector connector = ClientHttpConnectorBuilder.detect()
                .build(HttpClientSettings.defaults()
                        .withTimeouts(properties.connectTimeout(), properties.responseTimeout()));
        return builder.clone()
                .baseUrl(endpoint.baseUrl())
                .defaultHeader("X-Api-Key", endpoint.apiKey())
                .clientConnector(connector)
                .build();
    }
}
