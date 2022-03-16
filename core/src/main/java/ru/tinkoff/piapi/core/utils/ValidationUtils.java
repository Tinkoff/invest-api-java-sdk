package ru.tinkoff.piapi.core.utils;

import ru.tinkoff.piapi.core.exception.ReadonlyModeViolationException;

import java.time.Instant;

public class ValidationUtils {
  public static final String TO_IS_NOT_AFTER_FROM_MESSAGE = "Окончание периода не может быть раньше начала.";


  public static void checkPage(int page) {
    if (page < 0) {
      throw new IllegalArgumentException("Номерами страниц могут быть только положительные числа.");
    }
  }

  public static void checkFromTo(Instant from, Instant to) {
    if (from.isAfter(to)) {
      throw new IllegalArgumentException(TO_IS_NOT_AFTER_FROM_MESSAGE);
    }
  }

  public static void checkReadonly(boolean readonlyMode) {
    if (readonlyMode) {
      throw new ReadonlyModeViolationException();
    }
  }
}
