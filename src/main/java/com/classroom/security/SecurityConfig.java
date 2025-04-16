package com.classroom.security;

import com.classroom.service.OAuthCustomSuccessHandler;
import com.classroom.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService appUserService;
    private final AuthenticationSuccessHandler customLoginSuccessHandler;
    private final OAuthCustomSuccessHandler oAuthCustomSuccessHandler;


    public SecurityConfig(UserService appUserService, AuthenticationSuccessHandler customLoginSuccessHandler, OAuthCustomSuccessHandler oAuthCustomSuccessHandler) {
        this.appUserService = appUserService;
        this.customLoginSuccessHandler = customLoginSuccessHandler;
        this.oAuthCustomSuccessHandler = oAuthCustomSuccessHandler;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return appUserService;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(appUserService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(registry -> {
                    registry.requestMatchers("/req/signup", "/css/**", "/js/**", "/login", "/oauth2/**").permitAll();
                    registry.anyRequest().authenticated();
                })
                .formLogin(httpForm -> {
                    httpForm.loginPage("/login").permitAll();
                    httpForm.successHandler(customLoginSuccessHandler);
                })
                .oauth2Login(oauth2 -> {
                    oauth2
                            .loginPage("/login")
                            .successHandler(oAuthCustomSuccessHandler);
                })
                .build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
