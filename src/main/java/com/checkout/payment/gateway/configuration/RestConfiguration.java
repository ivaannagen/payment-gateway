package com.checkout.payment.gateway.configuration;

import com.checkout.payment.gateway.client.InternalRequestInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestConfiguration {

  private final InternalRequestInterceptor internalRequestInterceptor;

  public static final String INTERNAL_REST_TEMPLATE = "internalRestTemplate";

  public RestConfiguration(InternalRequestInterceptor internalRequestInterceptor) {
    this.internalRequestInterceptor = internalRequestInterceptor;
  }

  @Bean(name = INTERNAL_REST_TEMPLATE)
  @Primary
  public RestTemplate internalRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setInterceptors(List.of(internalRequestInterceptor));

    ObjectMapper mapper = new ObjectMapper();
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setObjectMapper(mapper);

    List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
    messageConverters.add(converter);
    messageConverters.addAll(restTemplate.getMessageConverters());
    restTemplate.setMessageConverters(messageConverters);

    return restTemplate;
  }

}