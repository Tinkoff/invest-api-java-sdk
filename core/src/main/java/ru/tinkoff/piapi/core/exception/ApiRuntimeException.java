package ru.tinkoff.piapi.core.exception;

public class ApiRuntimeException extends RuntimeException {

  private final Throwable throwable;
  private final String code;

  public ApiRuntimeException(String message, String code, Throwable throwable) {
    super(code + " " + message);
    this.code = code;
    this.throwable = throwable;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public String getCode() {
    return code;
  }
}
