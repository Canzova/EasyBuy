package com.easybuy.cart_order.configurations;

import com.easybuy.cart_order.HttpInterface.HttpInterface;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import javax.management.modelmbean.ModelMBean;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Bean
    public RestClient restClient(){
        return RestClient.builder()
                .baseUrl("http://localhost:8080/")
                .build();
    }

    @Bean
    public HttpInterface httpInterface(){
        // Step 1 : Create Rest Client
        RestClient restClient = RestClient.builder()
                .baseUrl("PRODUCT-CATEGORY-SERVICE")
                .build();

        // Step 2 : Create an adaptor for the client
        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        // Step 3 : Create a factory for generating HttpInterface proxy
        HttpServiceProxyFactory factory =  HttpServiceProxyFactory.builderFor(adapter).build();

        // 4. Create and return the proxy implementation of the interface
        return factory.createClient(HttpInterface.class);
    }

    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }
}
