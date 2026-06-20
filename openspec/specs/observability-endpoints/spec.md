# Observability Endpoints Specification

## Purpose

Expose safe runtime health and metrics endpoints so operators can verify application liveness and scrape observability data without SSH access.

## Requirements

### Requirement: Actuator Endpoint Exposure

The system MUST expose only the Actuator endpoints required for runtime observability: health, info, and Prometheus metrics.

#### Scenario: Health endpoint reports liveness

- GIVEN the application is running with the demo profile
- WHEN an operator requests `GET /actuator/health`
- THEN the response MUST be HTTP 200
- AND the health status MUST be `UP`

#### Scenario: Prometheus endpoint is available

- GIVEN the application is running with Actuator and Prometheus enabled
- WHEN a metrics scraper requests `GET /actuator/prometheus`
- THEN the response MUST be HTTP 200
- AND the body MUST use Prometheus text exposition format

#### Scenario: Unsafe actuator endpoints stay unavailable

- GIVEN management endpoint exposure is configured
- WHEN a requester attempts to access an endpoint outside health, info, or prometheus
- THEN the system MUST NOT expose sensitive operational endpoints such as env or configprops

### Requirement: Actuator Security Compatibility

The system MUST keep observability endpoints reachable according to the existing security rules without weakening asset or authentication endpoints.

#### Scenario: Actuator access does not require asset authentication

- GIVEN no bearer token is supplied
- WHEN a requester calls an allowed `/actuator/**` endpoint
- THEN the request MUST be handled by the actuator endpoint policy
- AND existing `/api/v1/assets/**` authorization MUST remain unchanged
