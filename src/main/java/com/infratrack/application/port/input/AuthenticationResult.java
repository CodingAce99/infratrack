package com.infratrack.application.port.input;

public record AuthenticationResult(String token, String username, String role) {}