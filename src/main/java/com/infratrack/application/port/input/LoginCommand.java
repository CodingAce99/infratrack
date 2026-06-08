package com.infratrack.application.port.input;

public record LoginCommand(
        String username,
        String rawPassword
) {
    @Override
    public String toString() {
        return "LoginCommand{username='" + username + "', rawPassword='[PROTECTED]'}";
    }
}