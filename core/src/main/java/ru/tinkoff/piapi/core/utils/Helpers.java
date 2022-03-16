package ru.tinkoff.piapi.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.subscription.MultiEmitter;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Helpers {

  private static final Map<String, HashMap<String, String>> errorsMap = new HashMap<>();
  private static final String DEFAULT_ERROR_ID = "70001";

  public static <T> T unaryCall(Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (StatusRuntimeException exception) {
      var id = getErrorId(exception);
      var description = getErrorDescription(id);
      throw new ApiRuntimeException(description, id, exception);
    }
  }

  private static String getErrorId(StatusRuntimeException exception) {
    if ("RESOURCE_EXHAUSTED".equals(exception.getStatus().getCode().name())) {
      return "80002";
    }
    var error = exception.getStatus().getDescription();
    return Objects.requireNonNullElse(error, DEFAULT_ERROR_ID);
  }

  /**
   * Связывание асинхронного Unary-вызова с {@link CompletableFuture}.
   *
   * @param callPerformer Асинхронный Unary-вызов.
   * @param <T>           Тип результата вызова.
   * @return {@link CompletableFuture} с результатом вызова.
   */
  public static <T> CompletableFuture<T> unaryAsyncCall(Consumer<StreamObserver<T>> callPerformer) {
    var cf = new CompletableFuture<T>();
    callPerformer.accept(mkStreamObserverWithFuture(cf));
    return cf;
  }

  /**
   * Создание StreamObserver, который связывает свой результат с CompletableFuture.
   * <p>
   * Только для Unary-вызовов!
   */
  private static <T> StreamObserver<T> mkStreamObserverWithFuture(CompletableFuture<T> cf) {
    return new StreamObserver<>() {
      @Override
      public void onNext(T value) {
        cf.complete(value);
      }

      @Override
      public void onError(Throwable t) {
        var throwable = t;
        if (t instanceof StatusRuntimeException) {
          var statusRuntimeException = (StatusRuntimeException) t;
          var id = getErrorId(statusRuntimeException);
          var description = getErrorDescription(id);
          throwable = new ApiRuntimeException(description, id, statusRuntimeException);
        }
        cf.completeExceptionally(throwable);
      }

      @Override
      public void onCompleted() {
      }
    };
  }

  /**
   * Связывание {@link MultiEmitter} со {@link StreamObserver}.
   *
   * @param emitter Экземпляр {@link MultiEmitter}.
   * @param <T>     Тип оперируемый {@link MultiEmitter}.
   * @return Связанный {@link StreamObserver}.
   */
  public static <T> StreamObserver<T> wrapEmitterWithStreamObserver(MultiEmitter<? super T> emitter) {
    return new StreamObserver<>() {
      @Override
      public void onNext(T value) {
        emitter.emit(value);
      }

      @Override
      public void onError(Throwable t) {
        emitter.fail(t);
      }

      @Override
      public void onCompleted() {
        emitter.complete();
      }
    };
  }

  /**
   * Проведение необходимых преобразований для пользовательского идентификатора поручения.
   *
   * @param orderId Пользовательский идентификатор поручения.
   * @return Преобразованный идентификатор поручения.
   */
  public static String preprocessInputOrderId(String orderId) {
    var maxLength = Math.min(orderId.length(), 36);
    return orderId.isBlank() ? orderId.trim() : orderId.substring(0, maxLength);
  }

  private static String getErrorDescription(String id) {
    if (errorsMap.isEmpty()) {
      try {
        var json = new File(Helpers.class.getClassLoader().getResource("errors.json").getFile());
        errorsMap.putAll(new ObjectMapper().readValue(json, new TypeReference<Map<String, HashMap<String, String>>>() {
        }));
      } catch (IOException e) {
        throw new RuntimeException("Не найден файл errors.json");
      }
    }

    var errorData = errorsMap.get(id);
    return errorData.get("description");
  }
}
