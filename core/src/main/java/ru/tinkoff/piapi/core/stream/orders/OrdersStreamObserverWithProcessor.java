package ru.tinkoff.piapi.core.stream.orders;

import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.contract.v1.TradesStreamResponse;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import java.util.function.Consumer;

public class OrdersStreamObserverWithProcessor implements StreamObserver<TradesStreamResponse> {

  private final StreamProcessor<TradesStreamResponse> streamProcessor;
  private final Consumer<Throwable> onErrorCallback;

  public OrdersStreamObserverWithProcessor(StreamProcessor<TradesStreamResponse> streamProcessor,
                                           Consumer<Throwable> onErrorCallback) {
    this.streamProcessor = streamProcessor;
    this.onErrorCallback = onErrorCallback;
  }

  @Override
  public void onNext(TradesStreamResponse value) {
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
