package com.shrishailacademy.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.time.Instant;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final UserDetailsService userDetailsService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final RateLimitFilter rateLimitFilter;
        private final CsrfProtectionFilter csrfProtectionFilter;
        private final SecurityHeaderFilter securityHeaderFilter;
        private final AuthEntryPointJwt unauthorizedHandler;

        @Value("${cors.allowed.origins:http://localhost:3000,http://localhost:8080}")
        private String corsAllowedOrigins;

        @Value("${https.required:false}")
        private boolean httpsRequired;

        public SecurityConfig(UserDetailsService userDetailsService,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        RateLimitFilter rateLimitFilter,
                        CsrfProtectionFilter csrfProtectionFilter,
                        SecurityHeaderFilter securityHeaderFilter,
                        AuthEntryPointJwt unauthorizedHandler) {
                this.userDetailsService = userDetailsService;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.rateLimitFilter = rateLimitFilter;
                this.csrfProtectionFilter = csrfProtectionFilter;
                this.securityHeaderFilter = securityHeaderFilter;
                this.unauthorizedHandler = unauthorizedHandler;
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder);
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                List<String> allowedOriginPatterns = Arrays.stream(corsAllowedOrigins.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.toList());
                configuration.setAllowedOriginPatterns(allowedOriginPatterns);
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(
                                Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With",
                                                "X-CSRF-Token"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authProvider)
                        throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .logout(AbstractHttpConfigurer::disable)
                                .headers(headers -> headers
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000)
                                                                .preload(true))
                                                .xssProtection(
                                                                xss -> xss.headerValue(
                                                                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                                .contentTypeOptions(opt -> {
                                                })
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives(
                                                                                "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; form-action 'self'; base-uri 'self'"))
                                                .frameOptions(frame -> frame.deny())
                                                .referrerPolicy(referrer -> referrer
                                                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                                                .permissionsPolicy(permissions -> permissions
                                                                .policy("camera=(), microphone=(), geolocation=(), payment=(self)")))
                                .requiresChannel(channel -> {
                                        if (httpsRequired) {
                                                channel.requestMatchers(request -> {
                                                        String proto = request.getHeader("X-Forwarded-Proto");
                                                        if (proto != null) {
                                                                return !"https".equalsIgnoreCase(proto);
                                                        }
                                                        return !request.isSecure();
                                                }).requiresSecure();
                                        }
                                })
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/index.html", "/index-premium.html",
                                                                "/about.html", "/courses.html",
                                                                "/contact.html",
                                                                "/login.html", "/register.html",
                                                                "/team.html", "/faq.html", "/demo.html", "/blog.html",
                                                                "/blog-post.html",
                                                                "/sanskrit.html", "/hindi.html", "/english.html",
                                                                "/kannada.html", "/french.html",
                                                                "/maths.html", "/science.html", "/german.html",
                                                                "/privacy-policy.html", "/terms-conditions.html",
                                                                "/course-delivery.html",
                                                                "/fee-payment.html", "/pricing-cancellation.html",
                                                                "/qrcode.html",
                                                                "/careers.html",
                                                                "/css/**", "/js/**", "/images/**")
                                                .permitAll()
                                                .requestMatchers("/student-dashboard.html")
                                                .hasAnyRole("STUDENT", "ADMIN")
                                                .requestMatchers("/admin-dashboard.html").hasRole("ADMIN")
                                                .requestMatchers("/health", "/actuator/health", "/actuator/info")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/auth/login",
                                                                "/api/auth/register",
                                                                "/api/auth/refresh",
                                                                "/api/auth/logout")
                                                .permitAll()
                                                .requestMatchers("/actuator/**").hasRole("ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/contact")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/demo-booking")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/counseling")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/teacher-applications")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/courses", "/api/courses/**")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/testimonials")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/blog",
                                                                "/api/blog/categories",
                                                                "/api/blog/{slug}")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(unauthorizedHandler)
                                                .accessDeniedHandler((req, res, e) -> {
                                                        String accept = req.getHeader("Accept");
                                                        if (req.getRequestURI().endsWith(".html")
                                                                        || (accept != null && accept
                                                                                        .contains("text/html"))) {
                                                                res.sendRedirect("/index.html");
                                                                return;
                                                        }
                                                        res.setStatus(403);
                                                        res.setContentType("application/json");
                                                        String body = "{"
                                                                        + "\"timestamp\":\"" + Instant.now().toString()
                                                                        + "\","
                                                                        + "\"status\":403,"
                                                                        + "\"error\":\"Forbidden\","
                                                                        + "\"message\":\"Access Denied\""
                                                                        + "}";
                                                        res.getWriter().write(body);
                                                }));

                http.authenticationProvider(authProvider);
                http.addFilterBefore(securityHeaderFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterAfter(csrfProtectionFilter, JwtAuthenticationFilter.class);

                return http.build();
        }
}
