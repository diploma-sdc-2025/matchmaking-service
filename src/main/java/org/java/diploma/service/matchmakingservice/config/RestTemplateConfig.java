package org.java.diploma.service.matchmakingservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

    private static final String REST_TEMPLATE_CONFIGURED = "RestTemplate bean configured for HTTP communication";

    @Bean
    public RestTemplate restTemplate() {
        logger.info(REST_TEMPLATE_CONFIGURED);
        return new RestTemplate();
    }
}
