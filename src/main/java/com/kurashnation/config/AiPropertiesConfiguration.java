package com.kurashnation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiIntegrationProperties.class)
public class AiPropertiesConfiguration {
}
