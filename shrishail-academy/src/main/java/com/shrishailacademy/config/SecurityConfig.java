package com.shrishailacademy.config;

import com.shrishailacademy.security.CsrfProtectionFilter;
import com.shrishailacademy.security.JwtAuthenticationFilter;
import com.shrishailacademy.security.RateLimitFilter;
import com.shrishailacademy.security.HttpsRedirectFilter;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private HttpsRedirectFilter httpsRedirectFilter;

    @Autowired
    private CsrfProtectionFilter csrfProtectionFilter;

    @Value("${cors.allowed.origins:http://localhost:3000,http://localhost:8080}")
    private String corsAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "X-CSRF-Token"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .xssProtection(
                                xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentTypeOptions(opt -> {
                        })
                        .frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // Public static pages
                        .requestMatchers("/", "/index.html", "/index-premium.html", "/about.html", "/courses.html",
                                "/contact.html",
                                "/login.html", "/register.html",
                                "/team.html", "/faq.html", "/demo.html", "/blog.html", "/blog-post.html",
                                "/sanskrit.html", "/hindi.html", "/english.html", "/kannada.html", "/french.html",
                                "/maths.html", "/science.html", "/german.html",
                                "/privacy-policy.html", "/terms-conditions.html", "/course-delivery.html",
                                "/fee-payment.html", "/pricing-cancellation.html", "/qrcode.html",
                                "/css/**", "/js/**", "/images/**")
                        .permitAll()
                        .requestMatchers("/student-dashboard.html").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/admin-dashboard.html").hasRole("ADMIN")
                        .requestMatchers("/health", "/api/auth/**", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/contact").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/demo-booking").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/teacher-applications")
                        .permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/courses", "/api/courses/**")
                        .permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/testimonials").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/blog", "/api/blog/categories",
                                "/api/blog/{slug}")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            String accept = req.getHeader("Accept");
                            if (req.getRequestURI().endsWith(".html")
                                    || (accept != null && accept.contains("text/html"))) {
                                res.sendRedirect("/login.html");
                                return;
                            }
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Unauthorized - please login\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            String accept = req.getHeader("Accept");
                            if (req.getRequestURI().endsWith(".html")
                                    || (accept != null && accept.contains("text/html"))) {
                                res.sendRedirect("/index.html");
                                return;
                            }
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Access Denied\"}");
                        }));

        http.authenticationProvider(authenticationProvider());
        // HTTPS redirect runs first, then rate limit, then JWT auth
        http.addFilterBefore(httpsRedirectFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(csrfProtectionFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
