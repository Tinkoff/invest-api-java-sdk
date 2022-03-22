package ru.tinkoff.piapi.core.exception;

public class ReadonlyModeViolationException extends RuntimeException {
  public ReadonlyModeViolationException() {
    super("Это действие нельзя совершить в режиме \"только для чтения\".");
  }
}
