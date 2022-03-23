package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.contract.v1.BrokerReportRequest;
import ru.tinkoff.piapi.contract.v1.BrokerReportResponse;
import ru.tinkoff.piapi.contract.v1.GenerateBrokerReportRequest;
import ru.tinkoff.piapi.contract.v1.GenerateDividendsForeignIssuerReportRequest;
import ru.tinkoff.piapi.contract.v1.GetBrokerReportRequest;
import ru.tinkoff.piapi.contract.v1.GetBrokerReportResponse;
import ru.tinkoff.piapi.contract.v1.GetDividendsForeignIssuerReportRequest;
import ru.tinkoff.piapi.contract.v1.GetDividendsForeignIssuerReportResponse;
import ru.tinkoff.piapi.contract.v1.GetDividendsForeignIssuerRequest;
import ru.tinkoff.piapi.contract.v1.GetDividendsForeignIssuerResponse;
import ru.tinkoff.piapi.contract.v1.Operation;
import ru.tinkoff.piapi.contract.v1.OperationState;
import ru.tinkoff.piapi.contract.v1.OperationsRequest;
import ru.tinkoff.piapi.contract.v1.OperationsResponse;
import ru.tinkoff.piapi.contract.v1.OperationsServiceGrpc.OperationsServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.OperationsServiceGrpc.OperationsServiceStub;
import ru.tinkoff.piapi.contract.v1.PortfolioRequest;
import ru.tinkoff.piapi.contract.v1.PortfolioResponse;
import ru.tinkoff.piapi.contract.v1.PositionsRequest;
import ru.tinkoff.piapi.contract.v1.PositionsResponse;
import ru.tinkoff.piapi.contract.v1.WithdrawLimitsRequest;
import ru.tinkoff.piapi.contract.v1.WithdrawLimitsResponse;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.models.Positions;
import ru.tinkoff.piapi.core.models.WithdrawLimits;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.tinkoff.piapi.core.utils.Helpers.unaryCall;
import static ru.tinkoff.piapi.core.utils.ValidationUtils.checkFromTo;
import static ru.tinkoff.piapi.core.utils.ValidationUtils.checkPage;

/**
 * Сервис получения информации о портфеле по конкретному счёту.
 */
public class OperationsService {
  private final OperationsServiceBlockingStub operationsBlockingStub;
  private final OperationsServiceStub operationsStub;

  OperationsService(@Nonnull OperationsServiceBlockingStub operationsBlockingStub,
                    @Nonnull OperationsServiceStub operationsStub) {
    this.operationsBlockingStub = operationsBlockingStub;
    this.operationsStub = operationsStub;
  }

  /**
   * Получение (синхронное) списка операций всех типов по счёту в заданном периоде времени.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Список операций.
   */
  @Nonnull
  public List<Operation> getAllOperationsSync(@Nonnull String accountId,
                                              @Nonnull Instant from,
                                              @Nonnull Instant to) {
    checkFromTo(from, to);

    return unaryCall(() -> operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .getOperationsList());
  }

  /**
   * Получение (синхронное) списка исполненных операций по счёту в заданном периоде времени.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Список операций.
   */
  @Nonnull
  public List<Operation> getExecutedOperationsSync(@Nonnull String accountId,
                                                   @Nonnull Instant from,
                                                   @Nonnull Instant to) {
    checkFromTo(from, to);

    return unaryCall(() -> operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setState(OperationState.OPERATION_STATE_EXECUTED)
          .build())
      .getOperationsList());
  }

  /**
   * Получение (синхронное) списка отменённых операций по счёту в заданном периоде времени.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Список операций.
   */
  @Nonnull
  public List<Operation> getCancelledOperationsSync(@Nonnull String accountId,
                                                    @Nonnull Instant from,
                                                    @Nonnull Instant to) {
    checkFromTo(from, to);

    return unaryCall(() -> operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setState(OperationState.OPERATION_STATE_CANCELED)
          .build())
      .getOperationsList());
  }

  /**
   * Получение (синхронное) списка операций всех типов по счёту в заданном периоде времени в рамках указанного
   * инструмента.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @param figi      FIGI-идентификатор инструмента.
   * @return Список операций.
   */
  @Nonnull
  public List<Operation> getAllOperationsSync(@Nonnull String accountId,
                                              @Nonnull Instant from,
                                              @Nonnull Instant to,
                                              @Nonnull String figi) {
    checkFromTo(from, to);

    return unaryCall(() -> operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setFigi(figi)
          .build())
      .getOperationsList());
  }

  /**
   * Получение (синхронное) списка исполненных операций по счёту в заданном периоде времени в рамках указанного
   * инструмента.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @param figi      FIGI-идентификатор инструмента.
   * @return Список операций.
   */
  @Nonnull
  public List<Operation> getExecutedOperationsSync(@Nonnull String accountId,
                                                   @Nonnull Instant from,
                                                   @Nonnull Instant to,
                                                   @Nonnull String figi) {
    checkFromTo(from, to);

    return unaryCall(() -> operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setState(OperationState.OPERATION_STATE_EXECUTED)
          .setFigi(figi)
          .build())
      .getOperationsList());
  }

  /**
   * Получение (синхронное) списка отменённых операций по счёту
   * в заданном периоде времени в рамках указанного инструмента.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @param figi      FIGI-идентификатор инструмента.
   * @return Список операций.
   */
  @Nonnull
  public List<Operation> getCancelledOperationsSync(@Nonnull String accountId,
                                                    @Nonnull Instant from,
                                                    @Nonnull Instant to,
                                                    @Nonnull String figi) {
    checkFromTo(from, to);

    return unaryCall(() -> operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setState(OperationState.OPERATION_STATE_CANCELED)
          .setFigi(figi)
          .build())
      .getOperationsList());
  }

  /**
   * Получение (синхронное) портфеля по счёту.
   *
   * @param accountId Идентификатор счёта.
   * @return Состояние портфеля.
   */
  @Nonnull
  public Portfolio getPortfolioSync(@Nonnull String accountId) {
    var request = PortfolioRequest.newBuilder().setAccountId(accountId).build();
    return Portfolio.fromResponse(unaryCall(() -> operationsBlockingStub.getPortfolio(request)));
  }

  /**
   * Получение (синхронное) списка позиций по счёту.
   *
   * @param accountId Идентификатор счёта.
   * @return Список позиций.
   */
  @Nonnull
  public Positions getPositionsSync(@Nonnull String accountId) {
    var request = PositionsRequest.newBuilder().setAccountId(accountId).build();
    return Positions.fromResponse(unaryCall(() -> operationsBlockingStub.getPositions(request)));
  }

  /**
   * Получение (синхронное) доступного остатка для вывода средств.
   *
   * @param accountId Идентификатор счёта.
   * @return Доступного остаток для вывода средств.
   */
  @Nonnull
  public WithdrawLimits getWithdrawLimitsSync(@Nonnull String accountId) {
    var request = WithdrawLimitsRequest.newBuilder().setAccountId(accountId).build();
    return WithdrawLimits.fromResponse(unaryCall(() -> operationsBlockingStub.getWithdrawLimits(request)));
  }


  /**
   * Получение (синхронное) данных их справки о доходах за пределами РФ на указанной странице.
   *
   * @param taskId Идентификатор задачи.
   * @param page   Номер страницы (начиная с 0).
   * @return Справка о доходах за пределами РФ.
   */
  public GetDividendsForeignIssuerReportResponse getDividendsForeignIssuerSync(@Nonnull String taskId, int page) {
    checkPage(page);

    var request = GetDividendsForeignIssuerRequest.newBuilder()
      .setGetDivForeignIssuerReport(GetDividendsForeignIssuerReportRequest.newBuilder().setTaskId(taskId).setPage(page).build())
      .build();
    return unaryCall(() -> operationsBlockingStub.getDividendsForeignIssuer(request).getDivForeignIssuerReport());
  }

  /**
   * Получение (асинхронное) данных их справки о доходах за пределами РФ на указанной странице.
   *
   * @param taskId Идентификатор задачи.
   * @param page   Номер страницы (начиная с 0).
   * @return Справка о доходах за пределами РФ.
   */
  @Nonnull
  public CompletableFuture<GetDividendsForeignIssuerReportResponse> getDividendsForeignIssuer(@Nonnull String taskId,
                                                                                              int page) {
    checkPage(page);

    var request = GetDividendsForeignIssuerRequest.newBuilder()
      .setGetDivForeignIssuerReport(
        GetDividendsForeignIssuerReportRequest.newBuilder().setTaskId(taskId).setPage(page).build())
      .build();
    return Helpers.<GetDividendsForeignIssuerResponse>unaryAsyncCall(
        observer -> operationsStub.getDividendsForeignIssuer(request, observer))
      .thenApply(GetDividendsForeignIssuerResponse::getDivForeignIssuerReport);
  }

  /**
   * Заказ (синхронный) справки о доходах за пределами РФ.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Справка о доходах, либо task_id задачи на формирование отчета.
   */
  @Nonnull
  public GetDividendsForeignIssuerResponse getDividendsForeignIssuerSync(@Nonnull String accountId,
                                                                         @Nonnull Instant from,
                                                                         @Nonnull Instant to) {
    checkFromTo(from, to);

    var request = GetDividendsForeignIssuerRequest.newBuilder()
      .setGenerateDivForeignIssuerReport(
        GenerateDividendsForeignIssuerReportRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .build();
    return unaryCall(() -> operationsBlockingStub.getDividendsForeignIssuer(request));
  }

  /**
   * Заказ (асинхронный) справки о доходах за пределами РФ.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Справка о доходах, либо task_id задачи на формирование отчета.
   */

  @Nonnull
  public CompletableFuture<GetDividendsForeignIssuerResponse> getDividendsForeignIssuer(@Nonnull String accountId,
                                                                                        @Nonnull Instant from,
                                                                                        @Nonnull Instant to) {
    checkFromTo(from, to);

    return Helpers.unaryAsyncCall(
      observer ->
        operationsStub.getDividendsForeignIssuer(GetDividendsForeignIssuerRequest.newBuilder()
          .setGenerateDivForeignIssuerReport(
            GenerateDividendsForeignIssuerReportRequest.newBuilder()
              .setAccountId(accountId)
              .setFrom(DateUtils.instantToTimestamp(from))
              .setTo(DateUtils.instantToTimestamp(to))
              .build())
          .build(), observer));
  }

  /**
   * Получение (асинхронное) списка операций всех типов по счёту в заданном периоде времени.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Список операций.
   */
  @Nonnull
  public CompletableFuture<List<Operation>> getAllOperations(@Nonnull String accountId,
                                                             @Nonnull Instant from,
                                                             @Nonnull Instant to) {
    checkFromTo(from, to);
    var request = OperationsRequest.newBuilder()
      .setAccountId(accountId)
      .setFrom(DateUtils.instantToTimestamp(from))
      .setTo(DateUtils.instantToTimestamp(to))
      .build();
    return Helpers.<OperationsResponse>unaryAsyncCall(
        observer -> operationsStub.getOperations(request, observer))
      .thenApply(OperationsResponse::getOperationsList);
  }

  /**
   * Получение (асинхронное) списка исполненных операций по счёту в заданном периоде времени.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Список операций.
   */
  @Nonnull
  public CompletableFuture<List<Operation>> getExecutedOperations(@Nonnull String accountId,
                                                                  @Nonnull Instant from,
                                                                  @Nonnull Instant to) {
    checkFromTo(from, to);

    return Helpers.<OperationsResponse>unaryAsyncCall(
        observer -> operationsStub.getOperations(
          OperationsRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .setState(OperationState.OPERATION_STATE_EXECUTED)
            .build(),
          observer))
      .thenApply(OperationsResponse::getOperationsList);
  }

  /**
   * Получение (асинхронное) списка отменённых операций по счёту в заданном периоде времени.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Список операций.
   */
  @Nonnull
  public CompletableFuture<List<Operation>> getCancelledOperations(@Nonnull String accountId,
                                                                   @Nonnull Instant from,
                                                                   @Nonnull Instant to) {
    checkFromTo(from, to);

    return Helpers.<OperationsResponse>unaryAsyncCall(
        observer -> operationsStub.getOperations(
          OperationsRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .setState(OperationState.OPERATION_STATE_CANCELED)
            .build(),
          observer))
      .thenApply(OperationsResponse::getOperationsList);
  }

  /**
   * Получение (асинхронное) списка операций всех типов по счёту
   * в заданном периоде времени в рамках указанного инструмента.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @param figi      FIGI-идентификатор инструмента.
   * @return Список операций.
   */
  @Nonnull
  public CompletableFuture<List<Operation>> getAllOperations(@Nonnull String accountId,
                                                             @Nonnull Instant from,
                                                             @Nonnull Instant to,
                                                             @Nonnull String figi) {
    checkFromTo(from, to);

    return Helpers.<OperationsResponse>unaryAsyncCall(
        observer -> operationsStub.getOperations(
          OperationsRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .setFigi(figi)
            .build(),
          observer))
      .thenApply(OperationsResponse::getOperationsList);
  }

  /**
   * Получение (асинхронное) списка исполненных операций по счёту
   * в заданном периоде времени в рамках указанного инструмента.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @param figi      FIGI-идентификатор инструмента.
   * @return Список операций.
   */
  @Nonnull
  public CompletableFuture<List<Operation>> getExecutedOperations(@Nonnull String accountId,
                                                                  @Nonnull Instant from,
                                                                  @Nonnull Instant to,
                                                                  @Nonnull String figi) {
    checkFromTo(from, to);

    return Helpers.<OperationsResponse>unaryAsyncCall(
        observer -> operationsStub.getOperations(
          OperationsRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .setState(OperationState.OPERATION_STATE_EXECUTED)
            .setFigi(figi)
            .build(),
          observer))
      .thenApply(OperationsResponse::getOperationsList);
  }

  /**
   * Получение (асинхронное) списка отменённых операций по счёту
   * в заданном периоде времени в рамках указанного инструмента.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @param figi      FIGI-идентификатор инструмента.
   * @return Список операций.
   */
  @Nonnull
  public CompletableFuture<List<Operation>> getCancelledOperations(@Nonnull String accountId,
                                                                   @Nonnull Instant from,
                                                                   @Nonnull Instant to,
                                                                   @Nonnull String figi) {
    checkFromTo(from, to);

    return Helpers.<OperationsResponse>unaryAsyncCall(
        observer -> operationsStub.getOperations(
          OperationsRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .setState(OperationState.OPERATION_STATE_CANCELED)
            .setFigi(figi)
            .build(),
          observer))
      .thenApply(OperationsResponse::getOperationsList);
  }

  /**
   * Получение (асинхронное) портфеля по счёту.
   *
   * @param accountId Идентификатор счёта.
   * @return Состояние портфеля.
   */
  @Nonnull
  public CompletableFuture<Portfolio> getPortfolio(@Nonnull String accountId) {
    var request = PortfolioRequest.newBuilder().setAccountId(accountId).build();
    return Helpers.<PortfolioResponse>unaryAsyncCall(
        observer -> operationsStub.getPortfolio(request, observer))
      .thenApply(Portfolio::fromResponse);
  }

  /**
   * Получение (асинхронное) списка позиций по счёту.
   *
   * @param accountId Идентификатор счёта.
   * @return Список позиций.
   */
  @Nonnull
  public CompletableFuture<Positions> getPositions(@Nonnull String accountId) {
    var request = PositionsRequest.newBuilder().setAccountId(accountId).build();
    return Helpers.<PositionsResponse>unaryAsyncCall(
        observer -> operationsStub.getPositions(request, observer))
      .thenApply(Positions::fromResponse);
  }

  /**
   * Получение (синхронное) доступного остатка для вывода средств.
   *
   * @param accountId Идентификатор счёта.
   * @return Доступного остаток для вывода средств.
   */
  @Nonnull
  public CompletableFuture<WithdrawLimits> getWithdrawLimits(@Nonnull String accountId) {
    var request = WithdrawLimitsRequest.newBuilder().setAccountId(accountId).build();
    return Helpers.<WithdrawLimitsResponse>unaryAsyncCall(
        observer -> operationsStub.getWithdrawLimits(request, observer))
      .thenApply(WithdrawLimits::fromResponse);
  }

  /**
   * Заказ (синхронный) брокерского отчёта.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Брокерский отчет, либо task_id задачи на формирование отчета.
   */
  @Nonnull
  public BrokerReportResponse getBrokerReportSync(@Nonnull String accountId,
                                                  @Nonnull Instant from,
                                                  @Nonnull Instant to) {
    checkFromTo(from, to);

    var request = BrokerReportRequest.newBuilder()
      .setGenerateBrokerReportRequest(
        GenerateBrokerReportRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .build();
    return unaryCall(() -> operationsBlockingStub.getBrokerReport(request));
  }

  /**
   * Заказ (асинхронный) брокерского отчёта.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Брокерский отчет, либо task_id задачи на формирование отчета.
   */
  @Nonnull
  public CompletableFuture<BrokerReportResponse> getBrokerReport(@Nonnull String accountId,
                                                                 @Nonnull Instant from,
                                                                 @Nonnull Instant to) {
    checkFromTo(from, to);

    var request = BrokerReportRequest.newBuilder()
      .setGenerateBrokerReportRequest(
        GenerateBrokerReportRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .build();
    return Helpers.unaryAsyncCall(observer -> operationsStub.getBrokerReport(request, observer));
  }

  /**
   * Получение (асинхронное) данных их брокерского отчёта на указанной странице.
   *
   * @param taskId Идентификатор задачи.
   * @param page   Номер страницы (начиная с 0).
   * @return Брокерский отчёт.
   */
  @Nonnull
  public CompletableFuture<GetBrokerReportResponse> getBrokerReport(@Nonnull String taskId, int page) {
    checkPage(page);

    var request = BrokerReportRequest.newBuilder()
      .setGetBrokerReportRequest(GetBrokerReportRequest.newBuilder().setTaskId(taskId).setPage(page).build())
      .build();
    return Helpers.<BrokerReportResponse>unaryAsyncCall(
        observer -> operationsStub.getBrokerReport(request, observer))
      .thenApply(BrokerReportResponse::getGetBrokerReportResponse);
  }


  /**
   * Получение (синхронное) данных их брокерского отчёта на указанной странице.
   *
   * @param taskId Идентификатор задачи.
   * @param page   Номер страницы (начиная с 0).
   * @return Брокерский отчёт.
   */
  @Nonnull
  public GetBrokerReportResponse getBrokerReportSync(@Nonnull String taskId, int page) {
    checkPage(page);

    var request = BrokerReportRequest.newBuilder()
      .setGetBrokerReportRequest(GetBrokerReportRequest.newBuilder().setTaskId(taskId).setPage(page).build())
      .build();
    return unaryCall(() -> operationsBlockingStub.getBrokerReport(request).getGetBrokerReportResponse());
  }
}
