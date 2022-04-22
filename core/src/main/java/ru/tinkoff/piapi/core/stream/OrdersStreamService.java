package ru.tinkoff.piapi.core.stream;

import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.TradesStreamRequest;
import ru.tinkoff.piapi.contract.v1.TradesStreamResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.function.Consumer;

public class OrdersStreamService {
  private final OrdersStreamServiceGrpc.OrdersStreamServiceStub stub;

  public OrdersStreamService(@Nonnull OrdersStreamServiceGrpc.OrdersStreamServiceStub stub) {
    this.stub = stub;
  }

  public void subscribeTrades(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor,
                              @Nullable Consumer<Throwable> onErrorCallback) {
    tradesStream(streamProcessor, onErrorCallback, Collections.emptyList());
  }

  /**
   * Подписка на стрим сделок
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param onErrorCallback обработчик ошибок в стриме
   * @param accounts Идентификаторы счетов
   */
  public void subscribeTrades(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor,
                              @Nullable Consumer<Throwable> onErrorCallback,
                              @Nonnull Iterable<String> accounts) {
    tradesStream(streamProcessor, onErrorCallback, accounts);
  }

  /**
   * Подписка на стрим сделок
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   */
  public void subscribeTrades(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor) {
    tradesStream(streamProcessor, null, Collections.emptyList());
  }

  /**
   * Подписка на стрим сделок
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param accounts Идентификаторы счетов
   */
  public void subscribeTrades(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor,
                              @Nonnull Iterable<String> accounts) {
    tradesStream(streamProcessor, null, accounts);
  }

  private void tradesStream(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor,
                            @Nullable Consumer<Throwable> onErrorCallback,
                            @Nonnull Iterable<String> accounts) {
    var request = TradesStreamRequest
      .newBuilder()
      .addAllAccounts(accounts)
      .build();
    stub.tradesStream(request, new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback));
  }
}
