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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService appUserService;
    private final AuthenticationSuccessHandler customLoginSuccessHandler;
    private final OAuthCustomSuccessHandler oAuthCustomSuccessHandler;
    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;
    private final CustomAuthenticationConverter authenticationConverter;
    private final CustomLogoutSuccessHandler logoutSuccessHandler;

    public SecurityConfig(
            UserService appUserService, 
            AuthenticationSuccessHandler customLoginSuccessHandler, 
            OAuthCustomSuccessHandler oAuthCustomSuccessHandler,
            CustomAuthenticationFilter customAuthenticationFilter,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
            CustomAuthenticationConverter authenticationConverter,
            CustomLogoutSuccessHandler logoutSuccessHandler) {
        this.appUserService = appUserService;
        this.customLoginSuccessHandler = customLoginSuccessHandler;
        this.oAuthCustomSuccessHandler = oAuthCustomSuccessHandler;
        this.customAuthenticationFilter = customAuthenticationFilter;
        this.oauth2UserService = oauth2UserService;
        this.authenticationConverter = authenticationConverter;
        this.logoutSuccessHandler = logoutSuccessHandler;
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
                .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/api/**") // Disable CSRF for API endpoints
                )
                .authorizeHttpRequests(registry -> {
                    registry.requestMatchers("/", "/signup", "/req/signup", "/select-role", "/assign-role", "/debug/**", "/css/**", "/js/**", "/login", "/oauth2/**", "/error/**", "/error").permitAll();
                    // API endpoints should be authenticated but accessible
                    registry.requestMatchers("/api/teams/**", "/api/auth/**").authenticated();
                    registry.requestMatchers("/api/**").authenticated();
                    // Application routes
                    registry.requestMatchers("/dashboard/**", "/settings/**", "/notifications/**").authenticated();
                    registry.requestMatchers("/test-course-creation").authenticated();
                    registry.anyRequest().authenticated();
                })
                .formLogin(httpForm -> {
                    httpForm.loginPage("/login").permitAll();
                    httpForm.successHandler(customLoginSuccessHandler);
                })
                .oauth2Login(oauth2 -> {
                    oauth2
                     .loginPage("/login")
                     .successHandler(oAuthCustomSuccessHandler)
                     .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService));
                })
                .sessionManagement(session -> {
                    // Increase session timeout to 2 hours
                    session.maximumSessions(2) // Allow two sessions per user
                           .expiredUrl("/login?expired");
                    
                    session.invalidSessionUrl("/login?invalid")
                           .sessionFixation().changeSessionId() // Generate a new session ID when a user authenticates
                           .sessionCreationPolicy(SessionCreationPolicy.ALWAYS); // Always create a session
                })
                .rememberMe(remember -> { // Add remember-me functionality
                    remember.key("uniqueAndSecretKey123456789")
                            .tokenValiditySeconds(86400) // 1 day
                            .rememberMeParameter("remember-me")
                            .userDetailsService(userDetailsService());
                })
                .logout(logout -> {
                    logout.logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll();
                })
                .addFilterAfter(customAuthenticationFilter, BasicAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
