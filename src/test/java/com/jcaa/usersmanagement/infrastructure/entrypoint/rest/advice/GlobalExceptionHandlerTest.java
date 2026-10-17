package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.domain.exception.InvalidCredentialsException;
import com.jcaa.usersmanagement.domain.exception.InvalidUserEmailException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.exception.PersistenceException;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("maneja UserNotFoundException devolviendo 404")
  void shouldHandleUserNotFound() {
    final ApiErrorResponse response =
        handler.handleUserNotFound(UserNotFoundException.becauseIdWasNotFound("123"));
    assertEquals(404, response.status());
  }

  @Test
  @DisplayName("maneja UserAlreadyExistsException devolviendo 409")
  void shouldHandleUserAlreadyExists() {
    final ApiErrorResponse response =
        handler.handleUserAlreadyExists(
            UserAlreadyExistsException.becauseEmailAlreadyExists("a@b.com"));
    assertEquals(409, response.status());
  }

  @Test
  @DisplayName("maneja InvalidCredentialsException devolviendo 401")
  void shouldHandleInvalidCredentials() {
    final ApiErrorResponse response =
        handler.handleInvalidCredentials(
            InvalidCredentialsException.becauseCredentialsAreInvalid());
    assertEquals(401, response.status());
  }

  @Test
  @DisplayName("maneja ConstraintViolationException devolviendo 400")
  void shouldHandleConstraintViolation() {
    @SuppressWarnings("unchecked")
    final ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    final Path path = mock(Path.class);
    when(path.toString()).thenReturn("email");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("must not be blank");

    final ApiErrorResponse response =
        handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)));
    assertEquals(400, response.status());
    assertEquals("email: must not be blank", response.message());
  }

  @Test
  @DisplayName("maneja MethodArgumentNotValidException devolviendo 400")
  void shouldHandleMethodArgumentNotValid() {
    final MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
    final BindingResult bindingResult = mock(BindingResult.class);
    when(bindingResult.getFieldErrors())
        .thenReturn(List.of(new FieldError("user", "name", "must not be empty")));
    when(exception.getBindingResult()).thenReturn(bindingResult);

    final ApiErrorResponse response = handler.handleMethodArgumentNotValid(exception);
    assertEquals(400, response.status());
    assertEquals("name: must not be empty", response.message());
  }

  @Test
  @DisplayName("maneja DomainException devolviendo 400")
  void shouldHandleDomainException() {
    final ApiErrorResponse response =
        handler.handleDomain(InvalidUserEmailException.becauseValueIsEmpty());
    assertEquals(400, response.status());
  }

  @Test
  @DisplayName("maneja PersistenceException devolviendo 500")
  void shouldHandlePersistenceException() {
    final ApiErrorResponse response =
        handler.handlePersistence(
            PersistenceException.becauseFindAllFailed(new RuntimeException("db error")));
    assertEquals(500, response.status());
    assertEquals("Error de persistencia.", response.message());
  }

  @Test
  @DisplayName("maneja Exception general devolviendo 500")
  void shouldHandleGeneralException() {
    final ApiErrorResponse response = handler.handleGeneral(new RuntimeException("unexpected"));
    assertEquals(500, response.status());
    assertEquals("Error interno del servidor.", response.message());
  }
}
