package com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.domain.exception.InvalidCredentialsException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.io.ConsoleIO;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.io.UserResponsePrinter;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.menu.MenuOption;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.controller.UserController;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.dto.UserResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserManagementCli & CLI Components")
class UserManagementCliTest {

  private static ConsoleIO consoleWithInput(
      final String input, final ByteArrayOutputStream outputBuffer) {
    final Scanner scanner = new Scanner(input);
    final PrintStream out = new PrintStream(outputBuffer, true, StandardCharsets.UTF_8);
    return new ConsoleIO(scanner, out);
  }

  @Test
  @DisplayName("ConsoleIO lee valores requeridos, opcionales y enteros validando entradas")
  void shouldReadAndWriteFromConsoleIO() {
    final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    final ConsoleIO console =
        consoleWithInput("   \nvalorRequerido\nopcional\nabc\n42\n", outBuffer);

    assertEquals("valorRequerido", console.readRequired("Prompt1: "));
    assertEquals("opcional", console.readOptional("Prompt2: "));
    assertEquals(42, console.readInt("Prompt3: "));
    console.println("Mensaje");
    console.println();
    console.printf("Formato %s%n", "ok");

    final String output = outBuffer.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("Value cannot be blank."));
    assertTrue(output.contains("Invalid input. Please enter a number."));
  }

  @Test
  @DisplayName("MenuOption resuelve opciones validas e invalidas")
  void shouldResolveMenuOptionFromNumber() {
    assertEquals(Optional.of(MenuOption.LIST_USERS), MenuOption.fromNumber(1));
    assertEquals(Optional.of(MenuOption.EXIT), MenuOption.fromNumber(0));
    assertEquals(Optional.empty(), MenuOption.fromNumber(99));
  }

  @Test
  @DisplayName("UserResponsePrinter imprime listas vacias y con elementos")
  void shouldPrintUserResponses() {
    final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    final ConsoleIO console = consoleWithInput("", outBuffer);
    final UserResponsePrinter printer = new UserResponsePrinter(console);

    printer.printList(List.of());
    printer.printList(
        List.of(new UserResponse("id-1", "Alice", "alice@example.com", "ADMIN", "ACTIVE")));

    final String output = outBuffer.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("No users found."));
    assertTrue(output.contains("Total: 1 user(s)"));
    assertTrue(output.contains("Alice"));
  }

  @Test
  @DisplayName("UserManagementCli ejecuta todas las operaciones exitosas del menu y sale")
  void shouldRunCliLoopWithSuccessfulOperations() {
    final UserController controller = mock(UserController.class);
    final UserResponse sampleUser =
        new UserResponse("id-1", "John", "john@example.com", "ADMIN", "ACTIVE");

    when(controller.listAllUsers()).thenReturn(List.of(sampleUser));
    when(controller.findUserById("id-1")).thenReturn(sampleUser);
    when(controller.createUser(any())).thenReturn(sampleUser);
    when(controller.updateUser(any())).thenReturn(sampleUser);
    when(controller.login(any())).thenReturn(sampleUser);

    final String input =
        String.join(
            "\n",
            "99", // Invalid option
            "1", // List users
            "2",
            "id-1", // Find user by ID
            "3",
            "id-2",
            "Jane",
            "jane@example.com",
            "Secret123!",
            "MEMBER", // Create user
            "4",
            "id-1",
            "John Updated",
            "john@example.com",
            "",
            "ADMIN",
            "ACTIVE", // Update user (blank password)
            "4",
            "id-1",
            "John Updated",
            "john@example.com",
            "NewPass123!",
            "ADMIN",
            "ACTIVE", // Update user (with password)
            "5",
            "id-1", // Delete user
            "6",
            "john@example.com",
            "Secret123!", // Login
            "0" // Exit
            ) + "\n";

    final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    final ConsoleIO console = consoleWithInput(input, outBuffer);
    final UserManagementCli cli = new UserManagementCli(controller, console);

    cli.start();

    verify(controller).deleteUser("id-1");
    final String output = outBuffer.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("Invalid option."));
    assertTrue(output.contains("User created successfully."));
    assertTrue(output.contains("User updated successfully."));
    assertTrue(output.contains("User deleted successfully."));
    assertTrue(output.contains("Login successful."));
    assertTrue(output.contains("Goodbye!"));
  }

  @Test
  @DisplayName("UserManagementCli maneja excepciones de dominio, validacion e inesperadas")
  void shouldHandleExceptionsInCliLoop() {
    final UserController controller = mock(UserController.class);

    when(controller.findUserById("missing"))
        .thenThrow(UserNotFoundException.becauseIdWasNotFound("missing"));
    when(controller.createUser(any()))
        .thenThrow(UserAlreadyExistsException.becauseEmailAlreadyExists("dup@example.com"));
    when(controller.updateUser(any()))
        .thenThrow(UserNotFoundException.becauseIdWasNotFound("missing"));
    doThrow(UserNotFoundException.becauseIdWasNotFound("missing"))
        .when(controller)
        .deleteUser("missing");
    when(controller.login(any()))
        .thenThrow(InvalidCredentialsException.becauseCredentialsAreInvalid());

    @SuppressWarnings("unchecked")
    final ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    when(violation.getMessage()).thenReturn("invalid field");
    final ConstraintViolationException cve =
        new ConstraintViolationException("validation failed", Set.of(violation));
    when(controller.listAllUsers()).thenThrow(cve).thenThrow(new RuntimeException("boom"));

    final String input =
        String.join(
            "\n",
            "1", // ConstraintViolationException
            "1", // RuntimeException
            "2",
            "missing", // FindUserById -> UserNotFoundException
            "3",
            "id-2",
            "Dup",
            "dup@example.com",
            "Pass123!",
            "MEMBER", // CreateUser -> UserAlreadyExistsException
            "4",
            "missing",
            "Name",
            "email@example.com",
            "",
            "ADMIN",
            "ACTIVE", // UpdateUser -> UserNotFoundException
            "5",
            "missing", // DeleteUser -> UserNotFoundException
            "6",
            "bad@example.com",
            "wrong", // Login -> InvalidCredentialsException
            "0" // Exit
            ) + "\n";

    final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    final ConsoleIO console = consoleWithInput(input, outBuffer);
    final UserManagementCli cli = new UserManagementCli(controller, console);

    cli.start();

    final String output = outBuffer.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("Validation errors:"));
    assertTrue(output.contains("Unexpected error: boom"));
    assertTrue(output.contains("Not found:"));
    assertTrue(output.contains("Error:"));
  }
}
