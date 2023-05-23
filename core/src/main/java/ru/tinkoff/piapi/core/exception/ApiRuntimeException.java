package ru.tinkoff.piapi.core.exception;

import io.grpc.Metadata;
import lombok.Getter;

@Getter
public class ApiRuntimeException extends RuntimeException {

  private final Throwable throwable;
  private final String code;
  private final String message;
  private final String trackingId;
  private final Metadata metadata;

  public ApiRuntimeException(String message, String code, String trackingId, Throwable throwable, Metadata metadata) {
    super(code + " " + message + " tracking_id " + trackingId);
    this.metadata = metadata;
    this.throwable = throwable;
    this.message = message;
    this.code = code;
    this.trackingId = trackingId;
  }
}
