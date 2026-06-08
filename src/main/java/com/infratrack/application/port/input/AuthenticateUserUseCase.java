package com.infratrack.application.port.input;

public interface AuthenticateUserUseCase {

    AuthenticationResult login(LoginCommand command);

}
