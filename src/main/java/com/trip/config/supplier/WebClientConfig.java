package com.trip.config.supplier;

import com.trip.supplier.Supplier;
import com.trip.supplier.global.SupplierEndpoint;
import com.trip.supplier.global.SupplierProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
@EnableConfigurationProperties(SupplierProperties.class)
public class WebClientConfig {

    @Bean(destroyMethod = "dispose")
    public ConnectionProvider supplierAPool(SupplierProperties properties) {
        return pool(properties, Supplier.A);
    }

    @Bean(destroyMethod = "dispose")
    public ConnectionProvider supplierBPool(SupplierProperties properties) {
        return pool(properties, Supplier.B);
    }

    @Bean
    public WebClient supplierA(WebClient.Builder builder, SupplierProperties properties,
                               @Qualifier("supplierAPool") ConnectionProvider pool) {
        return build(builder, properties, Supplier.A, pool);
    }

    @Bean
    public WebClient supplierB(WebClient.Builder builder, SupplierProperties properties,
                               @Qualifier("supplierBPool") ConnectionProvider pool) {
        return build(builder, properties, Supplier.B, pool);
    }

    private ConnectionProvider pool(SupplierProperties properties, Supplier supplier) {
        return ConnectionProvider.builder("supplier-" + supplier)
                .maxConnections(properties.maxConnections())
                .pendingAcquireTimeout(properties.pendingAcquireTimeout())
                .maxIdleTime(properties.maxIdleTime())
                .build();
    }

    private WebClient build(WebClient.Builder builder, SupplierProperties properties, Supplier supplier,
                            ConnectionProvider pool) {
        SupplierEndpoint endpoint = properties.connectEndpoint(supplier);
        ClientHttpConnector connector = ClientHttpConnectorBuilder.reactor()
                .withHttpClientFactory(() -> HttpClient.create(pool))
                .build(HttpClientSettings.defaults()
                        .withTimeouts(properties.connectTimeout(), properties.responseTimeout()));
        return builder.clone()
                .baseUrl(endpoint.baseUrl())
                .defaultHeader("X-Api-Key", endpoint.apiKey())
                .clientConnector(connector)
                .build();
    }
}
