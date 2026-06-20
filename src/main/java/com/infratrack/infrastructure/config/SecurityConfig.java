package com.infratrack.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infratrack.application.port.output.TokenValidator;
import com.infratrack.infrastructure.security.JwtAuthenticationFilter;
import com.infratrack.infrastructure.security.MdcCorrelationFilter;
import com.infratrack.infrastructure.security.RestAccessDeniedHandler;
import com.infratrack.infrastructure.security.RestAuthenticationEntryPoint;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Must stay public so @WebMvcTest slices can @Import(SecurityConfig.class).
@Configuration
public class SecurityConfig {

    // --- Dev chain ---

    // @Profile("dev"): only active in dev. Permits everything so existing fast
    // tests run without auth tokens. No JWT filter, no TokenValidator needed.
    @Bean
    @Profile("dev")
    SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // --- Security infrastructure beans (demo/prod only) ---

    // @Profile({"demo","prod"}): mirrors the repository/collector split in BeanConfiguration.
    // Dev has no TokenValidator bean, so the JWT filter must not be wired there.
    @Bean
    @Profile({"demo", "prod"})
    JwtAuthenticationFilter jwtAuthenticationFilter(TokenValidator tokenValidator) {
        return new JwtAuthenticationFilter(tokenValidator);
    }

    @Bean
    @Profile({"demo", "prod"})
    RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @Profile({"demo", "prod"})
    RestAccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }

    // Spring Boot auto-registers every Filter bean as a servlet filter.
    // Without this, JwtAuthenticationFilter would run once via the servlet container
    // AND once via the Spring Security chain — two executions per request.
    // setEnabled(false) keeps the bean available for the security chain while
    // preventing the duplicate servlet registration.
    @Bean
    @Profile({"demo", "prod"})
    FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    // MdcCorrelationFilter: registered as a servlet filter (NOT in the
    // SecurityFilterChain) so it applies to every request, including
    // actuator endpoints that bypass the security chain entirely.
    // Order -100 ensures it runs before Spring Security filters,
    // putting the correlation id in MDC before any auth or controller work.
    // Available in ALL profiles — correlation ids are useful during development too.
    @Bean
    FilterRegistrationBean<MdcCorrelationFilter> mdcCorrelationFilterRegistration() {
        FilterRegistrationBean<MdcCorrelationFilter> reg =
                new FilterRegistrationBean<>(new MdcCorrelationFilter());
        reg.setOrder(-100);
        return reg;
    }

    // --- Secured chain (demo/prod) ---

    @Bean
    @Profile({"demo", "prod"})
    SecurityFilterChain securedFilterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           RestAuthenticationEntryPoint entryPoint,
                                           RestAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        http
                // csrf: disabled — stateless API uses tokens in headers, not cookies.
                // A CSRF attack relies on the browser sending cookies automatically;
                // Bearer tokens in Authorization headers are not sent by browsers automatically.
                .csrf(csrf -> csrf.disable())

                // STATELESS: Spring must not create or use an HttpSession.
                // Every request must carry its own JWT — no session-based auth state.
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Disable browser-facing auth mechanisms — this is a JSON API.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                // Authorization rules — first match wins, so ORDER MATTERS:
                // 1. Login is public (no token required to obtain a token).
                // 2. Actuator endpoints are public — Prometheus scrapes and Docker
                //    healthchecks are unauthenticated machine requests. Note: in a real
                //    deployment these would sit on a separate management port or network.
                // 3. GET on assets/** is allowed to any authenticated role.
                // 4. All other verbs on assets/** (POST/PUT/DELETE) require ADMIN.
                //    hasRole("ADMIN") checks for the authority ROLE_ADMIN — the filter
                //    sets "ROLE_" + claims.role(), so "ADMIN" → "ROLE_ADMIN". Consistent.
                //    Rules 3 and 4 are an intentional pair — do not reorder them.
                // 5. Anything else requires authentication (future endpoints stay protected).
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/assets/**").hasAnyRole("ADMIN", "VIEWER")
                        .requestMatchers("/api/v1/assets/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                // addFilterBefore: our filter runs before Spring's UsernamePasswordAuthenticationFilter.
                // It populates the SecurityContext from the Bearer token so that the
                // authorization rules above can evaluate the caller's role.
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // Custom handlers write {"error":"..."} JSON instead of Spring's default
                // empty or HTML responses. Filter-level rejections never reach
                // @RestControllerAdvice, so these handlers are the only interception point.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)   // 401
                        .accessDeniedHandler(accessDeniedHandler) // 403
                );

        return http.build();
    }
}
