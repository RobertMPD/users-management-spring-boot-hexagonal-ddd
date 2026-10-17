package com.jcaa.usersmanagement.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.jcaa.usersmanagement.infrastructure.adapter.email.BrevoConfig;
import java.time.Duration;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

@DisplayName("InfrastructureConfig")
class InfrastructureConfigTest {

  @Test
  @DisplayName("BrevoSpringConfig construye BrevoConfig y RestClient correctamente")
  void shouldBuildBrevoConfigAndRestClient() {
    final BrevoSpringConfig springConfig = new BrevoSpringConfig();
    final BrevoConfig config =
        springConfig.brevoConfig(
            "https://api.brevo.com",
            "xkeysib-test-123456",
            "noreply@example.com",
            "Gestion de Usuarios",
            Duration.ofSeconds(2),
            Duration.ofSeconds(5),
            3,
            Duration.ofMillis(500),
            2.0,
            50,
            10,
            5,
            Duration.ofSeconds(30));

    assertNotNull(config);
    assertEquals("https://api.brevo.com", config.baseUrl());
    assertEquals("xkeysib-test-123456", config.apiKey());

    final RestClient restClient = springConfig.brevoRestClient(config);
    assertNotNull(restClient);
  }

  @Test
  @DisplayName("AsyncNotificationConfig construye el ThreadPoolTaskExecutor")
  void shouldBuildNotificationExecutor() {
    final AsyncNotificationConfig asyncConfig = new AsyncNotificationConfig();
    final Executor executor = asyncConfig.notificationExecutor();
    assertNotNull(executor);
    if (executor instanceof ThreadPoolTaskExecutor poolExecutor) {
      poolExecutor.shutdown();
    }
  }

  @Test
  @DisplayName("ConfigurationException encapsula la causa del error")
  void shouldCreateConfigurationException() {
    final RuntimeException cause = new RuntimeException("io error");
    final ConfigurationException exception = ConfigurationException.becauseLoadFailed(cause);
    assertEquals("Failed to load the application configuration.", exception.getMessage());
    assertSame(cause, exception.getCause());
  }
}
