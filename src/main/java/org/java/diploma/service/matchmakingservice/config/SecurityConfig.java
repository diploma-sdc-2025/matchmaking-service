package org.java.diploma.service.matchmakingservice.config;

import org.java.diploma.service.matchmakingservice.security.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String ENDPOINT_MATCHMAKING_JOIN = "/api/matchmaking/join";
    private static final String ENDPOINT_MATCHMAKING_LEAVE = "/api/matchmaking/leave";  // just for testing, remove in prod
    private static final String ENDPOINT_ACTUATOR = "/actuator/**";
    private static final String ENDPOINT_API_DOCS = "/v3/api-docs/**";
    private static final String ENDPOINT_SWAGGER_UI = "/swagger-ui/**";
    private static final String ENDPOINT_SWAGGER_HTML = "/swagger-ui.html";

    private static final String SECURITY_FILTER_CHAIN_CONFIGURED = "Security filter chain configured with stateless session and JWT authentication";
    private static final String CONFIGURING_SECURITY = "Configuring security filter chain";

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        logger.info(CONFIGURING_SECURITY);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
//                                ENDPOINT_MATCHMAKING_JOIN,
//                                ENDPOINT_MATCHMAKING_LEAVE,
                                ENDPOINT_ACTUATOR,
                                ENDPOINT_API_DOCS,
                                ENDPOINT_SWAGGER_UI,
                                ENDPOINT_SWAGGER_HTML
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        logger.info(SECURITY_FILTER_CHAIN_CONFIGURED);
        return http.build();
    }
}