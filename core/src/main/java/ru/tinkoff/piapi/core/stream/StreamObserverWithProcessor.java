package ru.tinkoff.piapi.core.stream;

import io.grpc.stub.StreamObserver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class StreamObserverWithProcessor<T> implements StreamObserver<T> {

  private final StreamProcessor<T> streamProcessor;
  private final Consumer<Throwable> onErrorCallback;

  public StreamObserverWithProcessor(@Nonnull StreamProcessor<T> streamProcessor,
                                     @Nullable Consumer<Throwable> onErrorCallback) {
    this.streamProcessor = streamProcessor;
    this.onErrorCallback = onErrorCallback;
  }

  @Override
  public void onNext(T value) {
    streamProcessor.process(value);
  }

  @Override
  public void onError(Throwable t) {
    if (onErrorCallback != null) {
      onErrorCallback.accept(t);
    }
  }

  @Override
  public void onCompleted() {
  }
}
