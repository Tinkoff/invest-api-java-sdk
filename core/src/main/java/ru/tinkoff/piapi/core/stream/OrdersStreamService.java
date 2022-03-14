package ru.tinkoff.piapi.core.stream;

import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.TradesStreamRequest;
import ru.tinkoff.piapi.contract.v1.TradesStreamResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class OrdersStreamService {
  private final OrdersStreamServiceGrpc.OrdersStreamServiceStub stub;

  public OrdersStreamService(@Nonnull OrdersStreamServiceGrpc.OrdersStreamServiceStub stub) {
    this.stub = stub;
  }

  public void subscribeTrades(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor,
                              @Nullable Consumer<Throwable> onErrorCallback) {
    tradesStream(streamProcessor, onErrorCallback);
  }

  public void subscribeTrades(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor) {
    tradesStream(streamProcessor, null);
  }

  private void tradesStream(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor,
                            @Nullable Consumer<Throwable> onErrorCallback) {
    var request = TradesStreamRequest
      .newBuilder()
      .build();
    stub.tradesStream(request, new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback));
  }
}
