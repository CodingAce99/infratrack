# Delta for Angular Auth UI

## ADDED Requirements

### Requirement: Login Page

The system MUST provide a `/login` page with username and password inputs, submit authentication, redirect to `/` after successful login, and show an inline error for invalid credentials.

#### Scenario: Login succeeds

- GIVEN the user is on `/login` with valid credentials
- WHEN the form is submitted and authentication succeeds
- THEN the user is redirected to `/`
- AND the dashboard may load authenticated data

#### Scenario: Invalid credentials show inline error

- GIVEN the user is on `/login`
- WHEN authentication returns 401
- THEN an inline login error is shown
- AND the user remains on `/login`

### Requirement: Protected Dashboard Route

The system MUST protect the dashboard route so unauthenticated users are redirected to `/login`. The guard MUST allow authenticated users to access `/`.

#### Scenario: Unauthenticated user is redirected

- GIVEN no auth token exists
- WHEN the user navigates to `/`
- THEN navigation redirects to `/login`

#### Scenario: Authenticated user reaches dashboard

- GIVEN a valid auth session exists
- WHEN the user navigates to `/`
- THEN the dashboard route is allowed

### Requirement: Header Auth Controls

The system MUST show authenticated user context in the header and provide logout. Logout MUST clear auth state and return the user to `/login`.

#### Scenario: Authenticated header shows logout

- GIVEN an authenticated user is viewing the dashboard
- WHEN the header renders
- THEN the username is visible
- AND a logout control is available

#### Scenario: Header logout ends session

- GIVEN an authenticated user clicks logout
- WHEN logout completes
- THEN auth state is cleared
- AND the user is routed to `/login`

### Requirement: Role-Driven Management Affordances

The system MUST derive dashboard management affordances from the authenticated role. `ADMIN` users MUST be allowed to manage assets; non-admin roles MUST NOT see management controls.

#### Scenario: Admin can manage assets

- GIVEN the authenticated role is `ADMIN`
- WHEN the dashboard renders
- THEN create, edit, and delete affordances are available

#### Scenario: Viewer cannot manage assets

- GIVEN the authenticated role is not `ADMIN`
- WHEN the dashboard renders
- THEN create, edit, and delete affordances are hidden

### Requirement: UI Coverage

The system MUST include Karma/Jasmine coverage for the login component, route guard redirects, header logout behavior, and role-driven management visibility.

#### Scenario: UI auth behavior is tested

- GIVEN auth UI behavior is changed
- WHEN frontend tests run
- THEN the login, guard, logout, and role-affordance expectations pass

### Requirement: UI Scope Boundaries

The system MUST NOT introduce registration, password reset, visual redesign, chart or metrics polish, fine-grained permissions, or advanced 403 UX beyond minimal unauthorized handling.

#### Scenario: Out-of-scope UI work stays absent

- GIVEN the auth UI change is reviewed
- WHEN the UI behavior is inspected
- THEN only minimal login, logout, guard, and role-affordance behavior is required
