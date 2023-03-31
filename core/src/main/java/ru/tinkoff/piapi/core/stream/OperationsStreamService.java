package ru.tinkoff.piapi.core.stream;

import io.grpc.Context;
import ru.tinkoff.piapi.contract.v1.OperationsStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.PortfolioStreamRequest;
import ru.tinkoff.piapi.contract.v1.PortfolioStreamResponse;
import ru.tinkoff.piapi.contract.v1.PositionsStreamRequest;
import ru.tinkoff.piapi.contract.v1.PositionsStreamResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class OperationsStreamService {

  private final OperationsStreamServiceGrpc.OperationsStreamServiceStub stub;
  private final AtomicReference<Context.CancellableContext> positionsCtx = new AtomicReference<>();
  private final AtomicReference<Context.CancellableContext> portfolioCtx = new AtomicReference<>();

  public OperationsStreamService(OperationsStreamServiceGrpc.OperationsStreamServiceStub stub) {
    this.stub = stub;
  }

  /**
   * Подписка на стрим позиций
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param account         Идентификатор счета
   */
  public void subscribePositions(@Nonnull StreamProcessor<PositionsStreamResponse> streamProcessor,
                                 @Nonnull String account) {
    subscribePositions(streamProcessor, null, List.of(account));
  }

  /**
   * Подписка на стрим позиций
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param onErrorCallback обработчик ошибок в стриме
   * @param account         Идентификатор счета
   */
  public void subscribePositions(@Nonnull StreamProcessor<PositionsStreamResponse> streamProcessor,
                                 @Nullable Consumer<Throwable> onErrorCallback,
                                 @Nonnull String account) {
    subscribePositions(streamProcessor, onErrorCallback, List.of(account));
  }

  /**
   * Подписка на стрим позиций
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param accounts        Идентификаторы счетов
   */
  public void subscribePositions(@Nonnull StreamProcessor<PositionsStreamResponse> streamProcessor,
                                 @Nonnull Iterable<String> accounts) {
    subscribePositions(streamProcessor, null, accounts);
  }

  /**
   * Подписка на стрим позиций
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param onErrorCallback обработчик ошибок в стриме
   * @param accounts        Идентификаторы счетов
   */
  public void subscribePositions(@Nonnull StreamProcessor<PositionsStreamResponse> streamProcessor,
                                 @Nullable Consumer<Throwable> onErrorCallback,
                                 @Nonnull Iterable<String> accounts) {
    cancelPositionSubscription();
    var request = PositionsStreamRequest
      .newBuilder()
      .addAllAccounts(accounts)
      .build();
    var context = Context.current().fork().withCancellation();
    var ctx = context.attach();
    try {
      stub.positionsStream(request, new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback));
      positionsCtx.set(context);
    } finally {
      context.detach(ctx);
    }
  }

  /**
   * Подписка на стрим портфеля
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param account         Идентификатор счета
   */
  public void subscribePortfolio(@Nonnull StreamProcessor<PortfolioStreamResponse> streamProcessor,
                                 @Nonnull String account) {
    subscribePortfolio(streamProcessor, null, List.of(account));
  }

  /**
   * Подписка на стрим портфеля
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param onErrorCallback обработчик ошибок в стриме
   * @param account         Идентификатор счета
   */
  public void subscribePortfolio(@Nonnull StreamProcessor<PortfolioStreamResponse> streamProcessor,
                                 @Nullable Consumer<Throwable> onErrorCallback,
                                 @Nonnull String account) {
    subscribePortfolio(streamProcessor, onErrorCallback, List.of(account));
  }

  /**
   * Подписка на стрим портфеля
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param accounts        Идентификаторы счетов
   */
  public void subscribePortfolio(@Nonnull StreamProcessor<PortfolioStreamResponse> streamProcessor,
                                 @Nonnull Iterable<String> accounts) {
    subscribePortfolio(streamProcessor, null, accounts);
  }

  /**
   * Подписка на стрим портфеля
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param onErrorCallback обработчик ошибок в стриме
   * @param accounts        Идентификаторы счетов
   */
  public void subscribePortfolio(@Nonnull StreamProcessor<PortfolioStreamResponse> streamProcessor,
                                 @Nullable Consumer<Throwable> onErrorCallback,
                                 @Nonnull Iterable<String> accounts) {
    cancelPortfolioSubscription();
    var request = PortfolioStreamRequest
      .newBuilder()
      .addAllAccounts(accounts)
      .build();
    var context = Context.current().fork().withCancellation();
    var ctx = context.attach();
    try {
      stub.portfolioStream(request, new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback));
      portfolioCtx.set(context);
    } finally {
      context.detach(ctx);
    }
  }

  public void cancelPortfolioSubscription() {
    cancelContext(portfolioCtx.get());
  }

  public void cancelPositionSubscription() {
    cancelContext(positionsCtx.get());
  }

  void cancelContext(Context.CancellableContext context) {
    if (context != null)
      context.cancel(new RuntimeException("canceled by user"));
  }
}
