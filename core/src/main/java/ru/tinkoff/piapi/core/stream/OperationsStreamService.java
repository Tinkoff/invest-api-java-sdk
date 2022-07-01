package ru.tinkoff.piapi.core.stream;

import ru.tinkoff.piapi.contract.v1.OperationsStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.PortfolioStreamRequest;
import ru.tinkoff.piapi.contract.v1.PortfolioStreamResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class OperationsStreamService {

  private final OperationsStreamServiceGrpc.OperationsStreamServiceStub stub;

  public OperationsStreamService(OperationsStreamServiceGrpc.OperationsStreamServiceStub stub) {
    this.stub = stub;
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
    var request = PortfolioStreamRequest
      .newBuilder()
      .addAllAccounts(accounts)
      .build();
    stub.portfolioStream(request, new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback));
  }
}
