# Delta for Angular Dashboard

## MODIFIED Requirements

### Requirement: Angular Shell and API Compatibility

The system SHALL render the real Angular dashboard at `/` for PR3. It MUST keep using the existing backend REST contract and relative `/api` calls. It MUST NOT require frontend login, JWT storage, route guards, Docker/CI cutover, Next.js removal, or unrelated Angular CLI analytics changes in this slice.
(Previously: `/` only needed an Angular dashboard shell and broader migration compatibility.)

#### Scenario: Dashboard opens with real content
- GIVEN the Angular frontend is served
- WHEN a user navigates to `/`
- THEN the dashboard shows Header, asset area, and asset cards instead of the placeholder

#### Scenario: Backend unavailable
- GIVEN the API cannot be reached
- WHEN the dashboard loads assets
- THEN a user-visible error state is shown without crashing

#### Scenario: Out-of-slice concerns stay absent
- GIVEN PR3 is implemented
- WHEN the dashboard is reviewed
- THEN no login page, JWT interceptor, route guard, Docker/CI cutover, Next.js removal, or `cli.analytics` change is required

### Requirement: Shared Asset State and Interaction-Safe Refresh

The system SHALL provide shared asset-list state with replayed latest data. `Dashboard` SHALL own 60-second asset-list refresh. Refresh MUST NOT close an open create modal or edit panel and MUST NOT overwrite in-progress form input. The header connection indicator MUST mean the backend/API is responding now.
(Previously: shared state required 60-second refresh but did not define interaction safety or connection-indicator semantics.)

#### Scenario: Asset list is shared
- GIVEN multiple dashboard consumers need assets
- WHEN the asset list is loaded
- THEN each consumer observes the same latest list

#### Scenario: Timed refresh preserves interaction
- GIVEN a create modal or edit panel is open with in-progress input
- WHEN the 60-second refresh interval elapses
- THEN the interaction remains open and the in-progress input is unchanged

#### Scenario: Connection indicator reflects current API reachability
- GIVEN the dashboard checks the asset API
- WHEN the latest check succeeds or fails
- THEN the header shows connected only for the successful current response

### Requirement: Dashboard CRUD Workflows

The system MUST support create, status update, IP update, credentials update, and confirmed delete. Status, IP, and credentials saves SHALL remain separate actions. Successful create MUST close the modal, refresh the asset list, and show visible confirmation. Failed mutations MUST surface errors and preserve the last valid list.
(Previously: create only needed to close the modal and show the new asset.)

#### Scenario: Create asset succeeds
- GIVEN valid name, type, IP, username, and password
- WHEN the create form is submitted successfully
- THEN the modal closes, the list refreshes, and a visible confirmation is shown

#### Scenario: Duplicate IP is rejected
- GIVEN asset creation returns duplicate-IP conflict
- WHEN the form submits
- THEN a form-level error is shown and the previous asset list remains visible

#### Scenario: Delete requires confirmation
- GIVEN a delete action is requested
- WHEN the user cancels confirmation
- THEN the asset remains unchanged

### Requirement: Single Edit Card Invariant

The system MUST allow at most one asset card to be in editing mode at a time. Opening edit mode for another card MUST leave the previous card's edit mode before the new panel becomes active.
(Previously: each card could own editing state without a dashboard-level invariant.)

#### Scenario: Editing moves between cards
- GIVEN one asset card is already editing
- WHEN the user opens edit mode on another card
- THEN only the newly selected card remains in editing mode

### Requirement: Per-Asset Metrics and Sparklines

The system SHALL load each asset's metric history independently, poll every 60 seconds, show threshold colors, and render sparklines without an external charting dependency. Empty history MUST render a stable empty state.
(Previously: independent metrics and empty history were required without tying them to PR3 asset-card wiring.)

#### Scenario: Metrics update independently
- GIVEN two asset cards are visible
- WHEN one metric stream refreshes
- THEN that card updates without blocking the other

#### Scenario: Empty history is safe
- GIVEN an asset has no metric snapshots
- WHEN its sparkline renders
- THEN a stable empty state is displayed
