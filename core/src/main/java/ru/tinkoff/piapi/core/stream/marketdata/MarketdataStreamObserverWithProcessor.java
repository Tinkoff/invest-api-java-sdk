package ru.tinkoff.piapi.core.stream.marketdata;

import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import java.util.function.Consumer;

public class MarketdataStreamObserverWithProcessor implements StreamObserver<MarketDataResponse> {

  private final StreamProcessor<MarketDataResponse> streamProcessor;
  private final Consumer<Throwable> onErrorCallback;

  public MarketdataStreamObserverWithProcessor(StreamProcessor<MarketDataResponse> streamProcessor,
                                               Consumer<Throwable> onErrorCallback) {
    this.streamProcessor = streamProcessor;
    this.onErrorCallback = onErrorCallback;
  }

  @Override
  public void onNext(MarketDataResponse value) {
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
