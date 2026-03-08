package com.shrishailacademy.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.web.access.AccessDeniedHandler;
import jakarta.servlet.http.HttpServletResponse;
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

        private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

        private final UserDetailsService userDetailsService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final com.shrishailacademy.tenant.TenantContextFilter tenantContextFilter;
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
                        com.shrishailacademy.tenant.TenantContextFilter tenantContextFilter,
                        RateLimitFilter rateLimitFilter,
                        CsrfProtectionFilter csrfProtectionFilter,
                        SecurityHeaderFilter securityHeaderFilter,
                        AuthEntryPointJwt unauthorizedHandler) {
                this.userDetailsService = userDetailsService;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.tenantContextFilter = tenantContextFilter;
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
        public AccessDeniedHandler accessDeniedHandler() {
                return (req, res, ex) -> {
                        String accept = req.getHeader("Accept");
                        boolean wantsHtml = req.getRequestURI().endsWith(".html")
                                        || (accept != null && accept.contains("text/html"));

                        if (wantsHtml) {
                                res.sendRedirect("/index.html");
                                return;
                        }

                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType("application/json");
                        String body = "{" +
                                        "\"timestamp\":\"" + Instant.now().toString() + "\"," +
                                        "\"status\":" + HttpServletResponse.SC_FORBIDDEN + "," +
                                        "\"error\":\"Forbidden\"," +
                                        "\"message\":\"Access denied\"" +
                                        "}";
                        res.getWriter().write(body);
                };
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.toList());
                if (allowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
                        throw new IllegalStateException(
                                        "CORS origins must be explicit when credentials are enabled. Remove wildcard origins.");
                }

                if (allowedOrigins.isEmpty()) {
                        log.warn("No CORS origins configured. Falling back to localhost development defaults.");
                        allowedOrigins = List.of("http://localhost:3000", "http://localhost:8080");
                }

                configuration.setAllowedOrigins(allowedOrigins);
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(
                                Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With",
                                                "X-CSRF-Token", "X-Request-Id", "X-Tenant-ID"));
                configuration.setExposedHeaders(List.of("X-Request-Id"));
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
                                                .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                                                .requestMatchers("/admin-dashboard.html").hasAnyRole("TEACHER", "ADMIN")
                                                .requestMatchers("/health", "/actuator/health", "/actuator/info")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/auth/login",
                                                                "/api/auth/register",
                                                                "/api/auth/refresh",
                                                                "/api/auth/logout",
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/register",
                                                                "/api/v1/auth/refresh",
                                                                "/api/v1/auth/logout")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/auth/verify-email",
                                                                "/api/v1/auth/verify-email")
                                                .permitAll()
                                                .requestMatchers("/actuator/**").hasRole("ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/contact")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/v1/contact")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/demo-booking")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/v1/demo-booking")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/counseling")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/v1/counseling")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/teacher-applications")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/v1/teacher-applications")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/courses", "/api/courses/**")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/courses", "/api/v1/courses/**")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/testimonials")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/testimonials")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/blog",
                                                                "/api/blog/categories",
                                                                "/api/blog/{slug}")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/blog",
                                                                "/api/v1/blog/categories",
                                                                "/api/v1/blog/{slug}")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/users/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                                                "/api/users/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                                                "/api/users/**")
                                                .hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(unauthorizedHandler)
                                                .accessDeniedHandler(accessDeniedHandler()));

                http.authenticationProvider(authProvider);
                http.addFilterBefore(securityHeaderFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterBefore(tenantContextFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterBefore(csrfProtectionFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
