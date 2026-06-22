# Angular Dashboard Specification

## Purpose
Angular SPA replacing the current dashboard while preserving asset CRUD, live metrics, sparklines, Docker delivery, and operational clarity. The backend REST API MUST remain unchanged.

## Requirements

### Requirement: Angular Shell and API Compatibility
The system SHALL serve an Angular dashboard at `/`. It MUST use relative `/api` calls in development and production and MUST NOT require backend contract changes.

#### Scenario: Dashboard opens
- GIVEN the frontend is served
- WHEN a user navigates to `/`
- THEN the Angular dashboard renders with header and asset area

#### Scenario: Backend unavailable
- GIVEN the API cannot be reached
- WHEN the dashboard loads assets
- THEN a user-visible error state is shown without crashing

### Requirement: Shared Asset State and Mutations
The system SHALL provide shared asset-list state with replayed latest data, 60-second refresh, and explicit revalidation after successful mutations. Failed mutations MUST propagate errors and preserve the last valid list.

#### Scenario: Asset list is shared
- GIVEN multiple dashboard consumers need assets
- WHEN the asset list is loaded
- THEN each consumer observes the same latest list

#### Scenario: Mutation refreshes list
- GIVEN an asset mutation succeeds
- WHEN the mutation completes
- THEN the asset list revalidates and emits updated data

#### Scenario: Duplicate IP is rejected
- GIVEN asset creation returns duplicate-IP conflict
- WHEN the form submits
- THEN the error is exposed and the previous asset list remains visible

### Requirement: Dashboard CRUD Workflows
The system MUST support create, status update, IP update, credentials update, and confirmed delete. Status, IP, and credentials saves SHALL remain separate actions.

#### Scenario: Create asset
- GIVEN valid name, type, IP, username, and password
- WHEN the create form is submitted
- THEN the modal closes and the new asset appears

#### Scenario: Delete requires confirmation
- GIVEN a delete action is requested
- WHEN the user cancels confirmation
- THEN the asset remains unchanged

### Requirement: Per-Asset Metrics and Sparklines
The system SHALL load each asset's metric history independently, poll every 60 seconds, show threshold colors, and render sparklines without an external charting dependency.

#### Scenario: Metrics update independently
- GIVEN two asset cards are visible
- WHEN one metric stream refreshes
- THEN that card updates without blocking the other

#### Scenario: Empty history is safe
- GIVEN an asset has no metric snapshots
- WHEN its sparkline renders
- THEN a stable empty state is displayed

### Requirement: Component Boundaries and Theme
The system SHALL separate state-owning dashboard/form components from presentational input/output components. The default theme MUST be dark and use consistent status and metric visual states.

#### Scenario: Presentational component renders from inputs
- GIVEN a status badge receives `ACTIVE`
- WHEN it renders
- THEN it displays the active visual state without API access

#### Scenario: Metric threshold color applies
- GIVEN a metric value of 75%
- WHEN the gauge renders
- THEN the metric is shown with the amber threshold state

### Requirement: Build, Docker, and Tests
The system MUST build as an Angular production app, be served by nginx in Docker, reverse proxy `/api/`, and include a runnable non-zero frontend test suite.

#### Scenario: Docker serves dashboard
- GIVEN `docker-compose up -d` completes
- WHEN `http://localhost:3000` is opened
- THEN the Angular dashboard loads and API calls reach the backend

#### Scenario: Frontend tests run
- GIVEN CI executes frontend verification
- WHEN the test command runs
- THEN at least one Angular test executes successfully
