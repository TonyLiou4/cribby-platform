package com.santosh.roommate_marketplace_api.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.santosh.roommate_marketplace_api.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<RateLimitedRoute> routes = List.of(
            new RateLimitedRoute("/api/auth/login", "login", SubjectType.IP),
            new RateLimitedRoute("/api/auth/register", "register", SubjectType.IP),
            new RateLimitedRoute("/api/auth/refresh", "refresh", SubjectType.USER_OR_IP),
            new RateLimitedRoute("/api/auth/send-verification-email", "verification-email", SubjectType.USER_OR_IP),
            new RateLimitedRoute("/api/auth/resend-verification-email", "verification-email", SubjectType.USER_OR_IP),
            new RateLimitedRoute("/api/reports", "reports", SubjectType.USER_OR_IP),
            new RateLimitedRoute("/api/conversations", "conversations", SubjectType.USER_OR_IP),
            new RateLimitedRoute("/api/conversations/*/messages", "messages", SubjectType.USER_OR_IP)
    );

    public RateLimitFilter(
            RateLimitProperties properties,
            RateLimiter rateLimiter,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<RateLimitedRoute> route = findRoute(request);
        if (route.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitedRoute rateLimitedRoute = route.get();
        RateLimitResult result = rateLimiter.tryConsume(
                rateLimitedRoute.policyName(),
                subjectKey(request, rateLimitedRoute.subjectType())
        );

        RateLimitProperties.Limit limit = properties.getPolicy(rateLimitedRoute.policyName());
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit.getCapacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));

        if (!result.allowed()) {
            writeTooManyRequests(response, request, result.retryAfterSeconds());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Optional<RateLimitedRoute> findRoute(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return Optional.empty();
        }

        String path = request.getRequestURI();
        return routes.stream()
                .filter(route -> pathMatcher.match(route.pattern(), path))
                .findFirst();
    }

    private String subjectKey(HttpServletRequest request, SubjectType subjectType) {
        if (subjectType == SubjectType.IP) {
            return "ip:" + clientIp(request);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return "user:" + authentication.getName().toLowerCase();
        }

        return "ip:" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(
            HttpServletResponse response,
            HttpServletRequest request,
            long retryAfterSeconds
    ) throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Too many requests. Please try again later.",
                request.getRequestURI(),
                LocalDateTime.now()
        )));
    }

    private record RateLimitedRoute(
            String pattern,
            String policyName,
            SubjectType subjectType
    ) {
    }

    private enum SubjectType {
        IP,
        USER_OR_IP
    }
}
