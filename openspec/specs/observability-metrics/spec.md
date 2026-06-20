# Observability Metrics Specification

## Purpose

Record application-level monitoring metrics with Micrometer while preserving Infratrack's hexagonal architecture boundaries.

## Requirements

### Requirement: Monitoring Sweep Duration Metric

The system MUST record the duration of scheduled monitoring sweeps using the metric name `infratrack.monitoring.collection.duration`.

#### Scenario: Successful sweep records duration

- GIVEN active assets are collected by the monitoring scheduler
- WHEN a collection sweep completes
- THEN the system MUST record one timer sample for the sweep duration
- AND the metric MUST be visible through the Prometheus endpoint

#### Scenario: Failed sweep still records duration

- GIVEN a monitoring sweep starts
- WHEN collection exits with an error
- THEN the system MUST still record the elapsed duration
- AND normal error handling MUST remain responsible for the failure outcome

### Requirement: SSH Collection Outcome Counter

The system MUST count SSH metric collection attempts using the metric name `infratrack.ssh.collection` and distinguish success from failure.

#### Scenario: Successful SSH collection increments success count

- GIVEN an SSH asset collection succeeds
- WHEN the collector returns a metric snapshot
- THEN the system MUST increment the SSH collection counter with a success outcome

#### Scenario: Failed SSH collection increments failure count

- GIVEN an SSH asset collection fails
- WHEN the collector reports or throws the failure
- THEN the system MUST increment the SSH collection counter with a failure outcome

### Requirement: Metrics Stay in Infrastructure

The system MUST keep Micrometer dependencies out of `domain/` and `application/` packages.

#### Scenario: Architecture boundary remains clean

- GIVEN observability metrics are implemented
- WHEN source imports are inspected
- THEN `domain/` and `application/` MUST contain zero `io.micrometer` imports
- AND metrics wiring MUST remain an infrastructure concern
