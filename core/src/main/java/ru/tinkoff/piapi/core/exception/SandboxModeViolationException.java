package ru.tinkoff.piapi.core.exception;

public class SandboxModeViolationException extends RuntimeException {
  public SandboxModeViolationException() {
    super("Это действие нельзя совершить в режиме \"песочницы\".");
  }
}
