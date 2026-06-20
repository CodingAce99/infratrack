package com.infratrack.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MdcCorrelationFilter — Single Registration")
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "infratrack.encryption.key=yt1+CDm1+7KdsybbxWrFcunLl8hnMTROPrEdi2daEuc="
})
class MdcFilterRegistrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("Exactly one FilterRegistrationBean<MdcCorrelationFilter> exists — no duplicate servlet registration")
    void singleFilterRegistrationBeanExists() {
        // The MdcCorrelationFilter is created inline in SecurityConfig (new MdcCorrelationFilter())
        // and never registered as a standalone Spring bean. The only bean is the FilterRegistrationBean.
        var regBeans = context.getBeansOfType(FilterRegistrationBean.class);
        long mdcRegCount = regBeans.values().stream()
                .filter(reg -> reg.getFilter() instanceof MdcCorrelationFilter)
                .count();
        assertEquals(1L, mdcRegCount,
                "Exactly one FilterRegistrationBean<MdcCorrelationFilter> should exist");

        // Also verify the filter was found at all
        assertTrue(mdcRegCount > 0,
                "At least one FilterRegistrationBean wrapping MdcCorrelationFilter must exist");
    }

    @Test
    @DisplayName("MdcCorrelationFilter is NOT added to any SecurityFilterChain — no security-chain double-registration")
    void mdcFilterNotInSecurityFilterChain() {
        // The JWT filter's registration pattern: it has a FilterRegistrationBean
        // (setEnabled(false)) AND is added via addFilterBefore in the filter chain.
        // The MDC filter MUST NOT follow this pattern — it must be servlet-only.
        var chains = context.getBeansOfType(SecurityFilterChain.class);

        for (var entry : chains.entrySet()) {
            SecurityFilterChain chain = entry.getValue();
            List<?> filters = chain.getFilters();
            boolean mdcFound = filters.stream()
                    .anyMatch(f -> f instanceof MdcCorrelationFilter);
            assertFalse(mdcFound,
                    "MdcCorrelationFilter MUST NOT appear in SecurityFilterChain '"
                    + entry.getKey() + "'. It should be servlet-only via FilterRegistrationBean.");
        }
    }
}
