# Async Events with Virtual Threads

This document describes how to implement internal async processing in the customer-api using Spring Application Events
and Java 21 Virtual Threads.

## Overview

### Problem

HTTP requests need to perform secondary tasks (statistics tracking, event storage, notifications) that shouldn't block
the response. We want to avoid external message queues like Kafka for simplicity.

### Solution

Use Spring's built-in Application Events with async listeners running on virtual threads:

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP Request                            │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller/Service                         │
│  1. Process request (fast path)                                 │
│  2. Publish event with captured context                         │
│  3. Return response immediately                                 │
└─────────────────────────┬───────────────────────────────────────┘
                          │ ApplicationEventPublisher
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Internal Event Queue                         │
│              (Spring's event infrastructure)                    │
└─────────────────────────┬───────────────────────────────────────┘
                          │ @Async @EventListener
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Background Handler                            │
│              (runs on virtual thread)                           │
│  - Increment statistics                                         │
│  - Store events                                                 │
│  - Send notifications                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Configuration

### 1. Enable Virtual Threads

Add to `application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

This configures Spring Boot to use virtual threads for `@Async` methods via `SimpleAsyncTaskExecutor`.

### 2. Enable Async Processing

Create `AsyncConfiguration.java`:

```java
package io.hephaistos.flagforge.customerapi.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfiguration {
    // Virtual threads are auto-configured when spring.threads.virtual.enabled=true
    // No custom TaskExecutor needed unless you want additional customization
}
```

### 3. Optional: Custom TaskExecutor with Context Propagation

If you need to propagate ThreadLocal data (not just event payload), add a custom executor:

```java
package io.hephaistos.flagforge.customerapi.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean
    public Executor taskExecutor() {
        TaskExecutorAdapter executor = new TaskExecutorAdapter(
            Executors.newVirtualThreadPerTaskExecutor()
        );
        executor.setTaskDecorator(securityContextDecorator());
        return executor;
    }

    private TaskDecorator securityContextDecorator() {
        return runnable -> {
            SecurityContext context = SecurityContextHolder.getContext();
            return () -> {
                try {
                    SecurityContextHolder.setContext(context);
                    runnable.run();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
        };
    }
}
```

---

## Defining Events

### Recommended Pattern: Capture Context in Event

Instead of propagating `SecurityContext` via ThreadLocal, capture needed data directly in the event. This is cleaner and
more explicit.

### Example: Statistics Event

```java
package io.hephaistos.flagforge.customerapi.event;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Event for tracking statistics in the background.
 * All security context data is captured at publish time.
 */
public record StatisticsEvent(
    Long applicationId,
    Long companyId,
    String eventType,
    Map<String, Object> data,
    OffsetDateTime timestamp
) {
    public static StatisticsEvent create(
            Long applicationId,
            Long companyId,
            String eventType,
            Map<String, Object> data) {
        return new StatisticsEvent(
            applicationId,
            companyId,
            eventType,
            data,
            OffsetDateTime.now()
        );
    }
}
```

### Example: Flag Evaluation Event

```java
package io.hephaistos.flagforge.customerapi.event;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Event published when a feature flag is evaluated.
 */
public record FlagEvaluationEvent(
    Long applicationId,
    Long companyId,
    String flagKey,
    String evaluationContext,
    Object resultValue,
    String reason,
    OffsetDateTime timestamp
) {
    public static FlagEvaluationEvent create(
            Long applicationId,
            Long companyId,
            String flagKey,
            String evaluationContext,
            Object resultValue,
            String reason) {
        return new FlagEvaluationEvent(
            applicationId,
            companyId,
            flagKey,
            evaluationContext,
            resultValue,
            reason,
            OffsetDateTime.now()
        );
    }
}
```

### Example: SDK Event Ingestion Event

```java
package io.hephaistos.flagforge.customerapi.event;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Event for SDK events received via the /v1/events endpoint.
 */
public record SdkIngestionEvent(
    Long applicationId,
    Long companyId,
    String eventType,
    Map<String, Object> data,
    OffsetDateTime clientTimestamp,
    OffsetDateTime serverTimestamp
) {
    public static SdkIngestionEvent from(
            Long applicationId,
            Long companyId,
            String eventType,
            Map<String, Object> data,
            OffsetDateTime clientTimestamp) {
        return new SdkIngestionEvent(
            applicationId,
            companyId,
            eventType,
            data,
            clientTimestamp,
            OffsetDateTime.now()
        );
    }
}
```

---

## Publishing Events

### Inject ApplicationEventPublisher

```java
package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.customerapi.event.FlagEvaluationEvent;
import io.hephaistos.flagforge.customerapi.security.CustomerApiSecurityContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

    private final ApplicationEventPublisher eventPublisher;

    public FeatureFlagService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public FlagEvaluationResponse evaluateFlag(String flagKey, String context) {
        // 1. Fast path - process the request
        var result = doEvaluation(flagKey, context);

        // 2. Publish event for background processing
        //    Capture security context NOW, before returning
        eventPublisher.publishEvent(FlagEvaluationEvent.create(
            CustomerApiSecurityContext.getApplicationId(),
            CustomerApiSecurityContext.getCompanyId(),
            flagKey,
            context,
            result.getValue(),
            result.getReason()
        ));

        // 3. Return immediately - event processed async
        return result;
    }
}
```

### Example: EventsController Integration

Update the existing `EventsController` to publish events:

```java
package io.hephaistos.flagforge.customerapi.controller;

import io.hephaistos.flagforge.customerapi.controller.request.EventRequest;
import io.hephaistos.flagforge.customerapi.event.SdkIngestionEvent;
import io.hephaistos.flagforge.customerapi.security.CustomerApiSecurityContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/events")
public class EventsController {

    private final ApplicationEventPublisher eventPublisher;

    public EventsController(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    public ResponseEntity<Void> ingestEvent(@RequestBody EventRequest request) {
        // Publish event for async processing
        eventPublisher.publishEvent(SdkIngestionEvent.from(
            CustomerApiSecurityContext.getApplicationId(),
            CustomerApiSecurityContext.getCompanyId(),
            request.eventType(),
            request.data(),
            request.clientTimestamp()
        ));

        // Return immediately
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<Void> ingestEvents(@RequestBody List<EventRequest> requests) {
        Long applicationId = CustomerApiSecurityContext.getApplicationId();
        Long companyId = CustomerApiSecurityContext.getCompanyId();

        // Publish all events
        requests.forEach(request ->
            eventPublisher.publishEvent(SdkIngestionEvent.from(
                applicationId,
                companyId,
                request.eventType(),
                request.data(),
                request.clientTimestamp()
            ))
        );

        return ResponseEntity.accepted().build();
    }
}
```

---

## Event Listeners

### Basic Async Listener

```java
package io.hephaistos.flagforge.customerapi.listener;

import io.hephaistos.flagforge.customerapi.event.SdkIngestionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EventStorageListener {

    private static final Logger log = LoggerFactory.getLogger(EventStorageListener.class);

    private final EventRepository eventRepository;

    public EventStorageListener(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Async
    @EventListener
    public void handleSdkEvent(SdkIngestionEvent event) {
        log.debug("Storing SDK event: type={}, appId={}",
            event.eventType(), event.applicationId());

        try {
            eventRepository.save(toEntity(event));
        } catch (Exception e) {
            log.error("Failed to store event: {}", event, e);
            // Consider: retry logic, dead letter queue, alerting
        }
    }

    private EventEntity toEntity(SdkIngestionEvent event) {
        // Map to your entity
        return new EventEntity(
            event.applicationId(),
            event.companyId(),
            event.eventType(),
            event.data(),
            event.clientTimestamp(),
            event.serverTimestamp()
        );
    }
}
```

### Statistics Listener

```java
package io.hephaistos.flagforge.customerapi.listener;

import io.hephaistos.flagforge.customerapi.event.FlagEvaluationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class StatisticsListener {

    private static final Logger log = LoggerFactory.getLogger(StatisticsListener.class);

    private final StatisticsService statisticsService;

    public StatisticsListener(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Async
    @EventListener
    public void handleFlagEvaluation(FlagEvaluationEvent event) {
        log.debug("Incrementing stats for flag: {}", event.flagKey());

        try {
            statisticsService.incrementEvaluationCount(
                event.applicationId(),
                event.flagKey()
            );
        } catch (Exception e) {
            log.error("Failed to update statistics for flag: {}", event.flagKey(), e);
        }
    }
}
```

### Multiple Listeners for Same Event

Spring supports multiple listeners for the same event type:

```java
@Component
public class AuditListener {

    @Async
    @EventListener
    public void auditFlagEvaluation(FlagEvaluationEvent event) {
        // Write to audit log
    }
}

@Component
public class AnalyticsListener {

    @Async
    @EventListener
    public void trackFlagEvaluation(FlagEvaluationEvent event) {
        // Send to analytics
    }
}
```

---

## Best Practices

### 1. When to Use Sync vs Async Events

| Scenario                           | Recommendation                          |
|------------------------------------|-----------------------------------------|
| Statistics, counters, metrics      | Async                                   |
| Audit logging                      | Async                                   |
| Sending notifications              | Async                                   |
| Result affects response            | Sync (don't use @Async)                 |
| Transactional consistency required | Sync or use @TransactionalEventListener |

### 2. Error Handling

Async listener exceptions are **not propagated** to the publisher. Always:

```java
@Async
@EventListener
public void handleEvent(MyEvent event) {
    try {
        processEvent(event);
    } catch (Exception e) {
        log.error("Failed to process event: {}", event, e);
        // Options:
        // 1. Log and continue (fire-and-forget)
        // 2. Publish to a "failed events" topic for retry
        // 3. Send alert
    }
}
```

### 3. Configure Async Exception Handler (Optional)

```java
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Async method {} threw exception: {}", method.getName(), ex.getMessage(), ex);
            // Add alerting, metrics, etc.
        };
    }
}
```

### 4. Testing Async Events

```java
@SpringBootTest
class EventListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldStoreEventAsync() throws Exception {
        // Given
        var event = SdkIngestionEvent.from(1L, 1L, "test", Map.of(), null);

        // When
        eventPublisher.publishEvent(event);

        // Then - wait for async processing
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() ->
                assertThat(eventRepository.findAll()).hasSize(1)
            );
    }
}
```

Add test dependency:

```kotlin
testImplementation("org.awaitility:awaitility:4.2.0")
```

### 5. Monitoring

Add logging to track event processing:

```java
@Async
@EventListener
public void handleEvent(MyEvent event) {
    long start = System.currentTimeMillis();
    try {
        processEvent(event);
        log.info("Processed event in {}ms: type={}",
            System.currentTimeMillis() - start, event.getClass().getSimpleName());
    } catch (Exception e) {
        log.error("Failed to process event after {}ms: {}",
            System.currentTimeMillis() - start, event, e);
    }
}
```

Consider adding metrics:

```java
@Async
@EventListener
public void handleEvent(MyEvent event) {
    meterRegistry.counter("events.processed", "type", event.getClass().getSimpleName()).increment();
    // ...
}
```

---

## References

- [Spring Events - Baeldung](https://www.baeldung.com/spring-events)
- [Working with Virtual Threads in Spring - Baeldung](https://www.baeldung.com/spring-6-virtual-threads)
- [Spring Boot Task Execution and Scheduling - Official Docs](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html)
- [Spring Security Context Propagation with @Async - Baeldung](https://www.baeldung.com/spring-security-async-principal-propagation)
