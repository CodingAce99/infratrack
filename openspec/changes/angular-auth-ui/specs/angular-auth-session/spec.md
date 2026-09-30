# Delta for Frontend Auth Session

## ADDED Requirements

### Requirement: Login Session Lifecycle

The system MUST authenticate with the existing login API, persist the received bearer token in `localStorage`, expose the authenticated user and role to the frontend, and clear all session state on logout.

#### Scenario: Successful login creates a session

- GIVEN valid credentials are submitted
- WHEN the login API returns a bearer token with user claims
- THEN the token is persisted in `localStorage`
- AND the authenticated user and role become available to the application

#### Scenario: Logout clears the session

- GIVEN an authenticated session exists
- WHEN logout is requested
- THEN the token and authenticated user state are cleared
- AND subsequent protected navigation requires login again

### Requirement: Bearer Header Injection

The system MUST add `Authorization: Bearer <token>` to `/api/*` HTTP requests when a token exists. It MUST NOT add an authorization header when no token exists.

#### Scenario: Token is attached to API requests

- GIVEN a token is stored
- WHEN the frontend sends a request to an `/api/*` URL
- THEN the request includes the matching bearer authorization header

#### Scenario: Missing token leaves request unchanged

- GIVEN no token is stored
- WHEN the frontend sends a request to an `/api/*` URL
- THEN no authorization header is added

### Requirement: Unauthorized Session Handling

The system MUST treat HTTP 401 from protected API requests, excluding the login request itself, as an invalid session by clearing auth state and redirecting to `/login`. HTTP 401 returned by the login request MUST remain a form-level invalid-credentials error. HTTP 403 handling MAY remain minimal but MUST NOT clear the session and MUST be distinguishable from successful responses.

#### Scenario: Unauthorized response returns to login

- GIVEN an authenticated session exists
- WHEN a protected API request returns 401
- THEN the session state is cleared
- AND the user is redirected to `/login`

#### Scenario: Login failure stays on login page

- GIVEN the user submits invalid credentials on `/login`
- WHEN the login API returns 401
- THEN the login form shows an inline error
- AND no protected-session redirect behavior is triggered

#### Scenario: Forbidden response is surfaced minimally

- GIVEN a valid token lacks permission for an action
- WHEN a protected API request returns 403
- THEN the response is not treated as success
- AND the authenticated session is not cleared
- AND minimal unauthorized feedback or state is available to the UI

### Requirement: Session Coverage

The system MUST include Karma/Jasmine coverage for login/logout state, token persistence, bearer header injection, 401 handling, and guard behavior.

#### Scenario: Auth behavior is tested

- GIVEN the auth session code is changed
- WHEN frontend tests run
- THEN the covered auth behaviors pass in Karma/Jasmine

### Requirement: Session Scope Boundaries

The system MUST NOT add refresh tokens, silent renewal, registration, password reset, or backend authentication changes in this capability.

#### Scenario: Out-of-scope session features stay absent

- GIVEN the auth session change is reviewed
- WHEN its behavior is inspected
- THEN only access-token login/logout session behavior is present
