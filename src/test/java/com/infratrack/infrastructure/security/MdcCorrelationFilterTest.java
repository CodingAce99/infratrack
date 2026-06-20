package com.infratrack.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("MdcCorrelationFilter")
class MdcCorrelationFilterTest {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "correlationId";

    private MdcCorrelationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new MdcCorrelationFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Nested
    @DisplayName("Correlation id generation and propagation")
    class CorrelationIdGeneration {

        @Test
        @DisplayName("should generate a UUID when no X-Request-ID header is present")
        void generatesUuidWhenNoHeader() throws ServletException, IOException {
            when(request.getHeader(REQUEST_ID_HEADER)).thenReturn(null);
            AtomicReference<String> capturedId = new AtomicReference<>();

            doAnswer(invocation -> {
                capturedId.set(MDC.get(MDC_KEY));
                return null;
            }).when(chain).doFilter(request, response);

            filter.doFilterInternal(request, response, chain);

            assertNotNull(capturedId.get(), "MDC should contain a correlation id during chain execution");
            assertDoesNotThrow(() -> UUID.fromString(capturedId.get()),
                    "generated id should be a valid UUID");
            verify(response).setHeader(REQUEST_ID_HEADER, capturedId.get());
        }

        @Test
        @DisplayName("should preserve existing X-Request-ID header value")
        void preservesExistingHeader() throws ServletException, IOException {
            String existingId = "caller-supplied-id-123";
            when(request.getHeader(REQUEST_ID_HEADER)).thenReturn(existingId);
            AtomicReference<String> capturedId = new AtomicReference<>();

            doAnswer(invocation -> {
                capturedId.set(MDC.get(MDC_KEY));
                return null;
            }).when(chain).doFilter(request, response);

            filter.doFilterInternal(request, response, chain);

            assertEquals(existingId, capturedId.get(),
                    "MDC should contain the caller-supplied id during chain execution");
            verify(response).setHeader(REQUEST_ID_HEADER, existingId);
        }

        @Test
        @DisplayName("should forward request to the filter chain")
        void forwardsToFilterChain() throws ServletException, IOException {
            filter.doFilterInternal(request, response, chain);

            verify(chain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("MDC cleanup")
    class MdcCleanup {

        @Test
        @DisplayName("should clear MDC after successful request processing")
        void clearsMdcAfterSuccess() throws ServletException, IOException {
            filter.doFilterInternal(request, response, chain);

            assertNull(MDC.get(MDC_KEY),
                    "MDC should be cleared after filter completes");
        }

        @Test
        @DisplayName("should clear MDC when filter chain throws exception")
        void clearsMdcAfterException() throws IOException, ServletException {
            doThrow(new ServletException("downstream failure"))
                    .when(chain).doFilter(request, response);

            assertThrows(ServletException.class,
                    () -> filter.doFilterInternal(request, response, chain));

            assertNull(MDC.get(MDC_KEY),
                    "MDC should be cleared even when chain throws");
        }
    }

    @Nested
    @DisplayName("No cross-request id leakage")
    class NoIdLeakage {

        @Test
        @DisplayName("second request should not inherit first request's correlation id")
        void secondRequestGetsNewId() throws ServletException, IOException {
            // First request — capture MDC during chain execution
            when(request.getHeader(REQUEST_ID_HEADER)).thenReturn("first-request-id");
            AtomicReference<String> firstCapturedId = new AtomicReference<>();
            doAnswer(invocation -> {
                firstCapturedId.set(MDC.get(MDC_KEY));
                return null;
            }).when(chain).doFilter(request, response);
            filter.doFilterInternal(request, response, chain);

            // MDC should be cleared after first request
            assertNull(MDC.get(MDC_KEY),
                    "MDC should be clear after first request completes");

            // Second request — should get its own id
            HttpServletRequest request2 = mock(HttpServletRequest.class);
            HttpServletResponse response2 = mock(HttpServletResponse.class);
            FilterChain chain2 = mock(FilterChain.class);
            when(request2.getHeader(REQUEST_ID_HEADER)).thenReturn("second-request-id");
            AtomicReference<String> secondCapturedId = new AtomicReference<>();
            doAnswer(invocation -> {
                secondCapturedId.set(MDC.get(MDC_KEY));
                return null;
            }).when(chain2).doFilter(request2, response2);

            filter.doFilterInternal(request2, response2, chain2);

            assertEquals("second-request-id", secondCapturedId.get(),
                    "second request's id should be its own, not the first's");
            assertNotEquals(firstCapturedId.get(), secondCapturedId.get(),
                    "each request should get a distinct id");
        }
    }
}
