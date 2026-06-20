# Observability MDC Specification

## Purpose

Attach a per-request correlation id to logs so operators can connect log lines to the HTTP request that produced them.

## Requirements

### Requirement: Request Correlation Id

The system MUST assign a correlation id to each HTTP request and store it in MDC for the duration of request processing.

#### Scenario: Request receives correlation id

- GIVEN an HTTP request enters the application
- WHEN the request is processed
- THEN the system MUST place a correlation id in MDC
- AND log lines emitted during processing SHOULD include that id

#### Scenario: Existing correlation id is preserved

- GIVEN a request already carries a supported correlation id header
- WHEN the request is processed
- THEN the system SHOULD use the supplied id rather than generating an unrelated one
- AND that id SHOULD appear in request-scoped logs

### Requirement: MDC Cleanup

The system MUST clear request correlation data from MDC after each request, including failure paths.

#### Scenario: Correlation id is removed after success

- GIVEN a request completes successfully
- WHEN processing exits
- THEN the system MUST remove the correlation id from MDC

#### Scenario: Correlation id is removed after failure

- GIVEN a request fails during processing
- WHEN processing exits through the error path
- THEN the system MUST remove the correlation id from MDC
- AND the next request MUST NOT inherit the previous request's id

### Requirement: Single Filter Registration

The system MUST register the MDC correlation filter exactly once.

#### Scenario: Filter does not run twice

- GIVEN the MDC filter is configured
- WHEN one request is handled
- THEN the filter MUST apply one correlation lifecycle for that request
- AND duplicate servlet/security-chain registration MUST NOT occur
