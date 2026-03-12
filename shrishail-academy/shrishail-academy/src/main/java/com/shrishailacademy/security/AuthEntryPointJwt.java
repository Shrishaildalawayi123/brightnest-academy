package com.shrishailacademy.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Authentication entrypoint for unauthorized requests.
 *
 * Returns JSON for API requests, and redirects for HTML browser requests.
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(AuthEntryPointJwt.class);
    private final ApplicationContext applicationContext;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthEntryPointJwt(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        String accept = request.getHeader("Accept");
        boolean wantsHtml = request.getRequestURI().endsWith(".html")
                || (accept != null && accept.contains("text/html"));

        if (wantsHtml) {
            response.sendRedirect("/login.html");
            return;
        }

        if (request.getRequestURI().startsWith("/api/")) {
            ApiRouteResolution routeResolution = resolveApiRoute(request);
            if (routeResolution.status == HttpServletResponse.SC_NOT_FOUND) {
                writeJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Not Found", "Resource not found.");
                return;
            }
            if (routeResolution.status == HttpServletResponse.SC_METHOD_NOT_ALLOWED) {
                if (!routeResolution.allowedMethods.isBlank()) {
                    response.setHeader("Allow", routeResolution.allowedMethods);
                }
                writeJsonError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed",
                        "Request method is not supported.");
                return;
            }
        }

        writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Unauthorized - please login");
    }

    private ApiRouteResolution resolveApiRoute(HttpServletRequest request) {
        RequestMappingHandlerMapping mapping;
        try {
            mapping = applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        } catch (Exception ex) {
            log.debug("Could not resolve requestMappingHandlerMapping bean: {}", ex.getMessage());
            return ApiRouteResolution.found();
        }

        String requestPath = request.getRequestURI();
        String requestMethod = request.getMethod();
        boolean anyPathMatched = false;
        Set<String> allowedMethods = new LinkedHashSet<>();

        for (RequestMappingInfo info : mapping.getHandlerMethods().keySet()) {
            if (!matchesPath(info, requestPath)) {
                continue;
            }

            anyPathMatched = true;

            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                return ApiRouteResolution.found();
            }
            if (methods.stream().anyMatch(m -> m.name().equalsIgnoreCase(requestMethod))) {
                return ApiRouteResolution.found();
            }
            methods.stream().map(RequestMethod::name).forEach(allowedMethods::add);
        }

        if (!anyPathMatched) {
            return ApiRouteResolution.notFound();
        }
        String allowHeader = String.join(", ", allowedMethods);
        return ApiRouteResolution.methodNotAllowed(allowHeader);
    }

    private boolean matchesPath(RequestMappingInfo info, String requestPath) {
        if (info.getPathPatternsCondition() != null
                && info.getPathPatternsCondition().getPatternValues().stream()
                        .anyMatch(pattern -> pathMatcher.match(pattern, requestPath))) {
            return true;
        }

        PatternsRequestCondition patterns = info.getPatternsCondition();
        if (patterns == null) {
            return false;
        }
        return patterns.getPatterns().stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    private void writeJsonError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        String body = "{"
                + "\"timestamp\":\"" + Instant.now().toString() + "\","
                + "\"status\":" + status + ","
                + "\"error\":\"" + error + "\","
                + "\"message\":\"" + message + "\""
                + "}";
        response.getWriter().write(body);
    }

    private static final class ApiRouteResolution {
        private final int status;
        private final String allowedMethods;

        private ApiRouteResolution(int status, String allowedMethods) {
            this.status = status;
            this.allowedMethods = allowedMethods;
        }

        static ApiRouteResolution found() {
            return new ApiRouteResolution(HttpServletResponse.SC_OK, "");
        }

        static ApiRouteResolution notFound() {
            return new ApiRouteResolution(HttpServletResponse.SC_NOT_FOUND, "");
        }

        static ApiRouteResolution methodNotAllowed(String allowedMethods) {
            return new ApiRouteResolution(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    allowedMethods == null ? "" : allowedMethods);
        }
    }
}
