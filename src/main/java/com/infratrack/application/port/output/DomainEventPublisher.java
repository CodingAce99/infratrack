package com.infratrack.application.port.output;

public interface DomainEventPublisher {

    void publish(Object event);
}
