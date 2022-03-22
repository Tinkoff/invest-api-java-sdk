package ru.tinkoff.piapi.core.exception;

public class ApiRuntimeException extends RuntimeException {

  private final Throwable throwable;
  private final String code;
  private final String trackingId;

  public ApiRuntimeException(String message, String code, Throwable throwable, String trackingId) {
    super(code + " " + message + ". tracking_id " + trackingId);
    this.code = code;
    this.throwable = throwable;
    this.trackingId = trackingId;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public String getCode() {
    return code;
  }

  public String getTrackingId() {
    return trackingId;
  }
}
