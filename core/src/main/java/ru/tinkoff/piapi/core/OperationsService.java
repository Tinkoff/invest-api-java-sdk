package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.models.Positions;
import ru.tinkoff.piapi.core.models.WithdrawLimits;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис получения информации о портфеле по конкретному счёту.
 */
public class OperationsService {
  private final OperationsServiceGrpc.OperationsServiceBlockingStub operationsBlockingStub;
  private final OperationsServiceGrpc.OperationsServiceStub operationsStub;

  OperationsService(
    @Nonnull OperationsServiceGrpc.OperationsServiceBlockingStub operationsBlockingStub,
    @Nonnull OperationsServiceGrpc.OperationsServiceStub operationsStub) {
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
  public List<Operation> getAllOperationsSync(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    return operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .getOperationsList();
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
  public List<Operation> getExecutedOperationsSync(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    return operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setState(OperationState.OPERATION_STATE_EXECUTED)
          .build())
      .getOperationsList();
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
  public List<Operation> getCancelledOperationsSync(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    return operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setState(OperationState.OPERATION_STATE_CANCELED)
          .build())
      .getOperationsList();
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
  public List<Operation> getAllOperationsSync(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to,
    @Nonnull String figi) {
    return operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setFigi(figi)
          .build())
      .getOperationsList();
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
  public List<Operation> getExecutedOperationsSync(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to,
    @Nonnull String figi) {
    return operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setState(OperationState.OPERATION_STATE_EXECUTED)
          .setFigi(figi)
          .build())
      .getOperationsList();
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
  public List<Operation> getCancelledOperationsSync(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to,
    @Nonnull String figi) {
    return operationsBlockingStub.getOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setState(OperationState.OPERATION_STATE_CANCELED)
          .setFigi(figi)
          .build())
      .getOperationsList();
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
    return Portfolio.fromResponse(operationsBlockingStub.getPortfolio(request));
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
    return Positions.fromResponse(operationsBlockingStub.getPositions(request));
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
    return WithdrawLimits.fromResponse(operationsBlockingStub.getWithdrawLimits(request));
  }

  /**
   * Заказ (синхронный) брокерского отчёта.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Идентификатор задачи составления отчёта.
   */
  @Nonnull
  public String requestBrokerReportSync(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    if (Helpers.areFromAndToValid(from, to)) {
      var request = BrokerReportRequest.newBuilder()
        .setGenerateBrokerReportRequest(
          GenerateBrokerReportRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build())
        .build();
      return operationsBlockingStub.getBrokerReport(request).getGenerateBrokerReportResponse().getTaskId();
    } else {
      throw new IllegalArgumentException(Helpers.TO_IS_NOT_AFTER_FROM_MESSAGE);
    }
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
    if (page < 0) {
      throw new IllegalArgumentException("Номерами страниц могут быть только положительные числа.");
    }

    var request = BrokerReportRequest.newBuilder()
      .setGetBrokerReportRequest(GetBrokerReportRequest.newBuilder().setTaskId(taskId).setPage(page).build())
      .build();
    return operationsBlockingStub.getBrokerReport(request).getGetBrokerReportResponse();
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
  public CompletableFuture<List<Operation>> getAllOperations(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    return Helpers.<OperationsResponse>wrapWithFuture(
        observer -> operationsStub.getOperations(
          OperationsRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build(),
          observer))
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
  public CompletableFuture<List<Operation>> getExecutedOperations(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    return Helpers.<OperationsResponse>wrapWithFuture(
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
  public CompletableFuture<List<Operation>> getCancelledOperations(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    return Helpers.<OperationsResponse>wrapWithFuture(
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
   * @return Список операций.
   */
  @Nonnull
  public CompletableFuture<List<Operation>> getAllOperations(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to,
    @Nonnull String figi) {
    return Helpers.<OperationsResponse>wrapWithFuture(
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
   * @return Список операций.
   */
  @Nonnull
  public CompletableFuture<List<Operation>> getExecutedOperations(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to,
    @Nonnull String figi) {
    return Helpers.<OperationsResponse>wrapWithFuture(
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
   * @return Список операций.
   */
  @Nonnull
  public CompletableFuture<List<Operation>> getCancelledOperations(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to,
    @Nonnull String figi) {
    return Helpers.<OperationsResponse>wrapWithFuture(
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
    return Helpers.<PortfolioResponse>wrapWithFuture(
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
    return Helpers.<PositionsResponse>wrapWithFuture(
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
  public CompletableFuture<WithdrawLimits> getWithdrawLimits(
    @Nonnull String accountId) {
    var request = WithdrawLimitsRequest.newBuilder().setAccountId(accountId).build();
    return Helpers.<WithdrawLimitsResponse>wrapWithFuture(
        observer -> operationsStub.getWithdrawLimits(request, observer))
      .thenApply(WithdrawLimits::fromResponse);
  }

  /**
   * Заказ (асинхронный) брокерского отчёта.
   *
   * @param accountId Идентификатор счёта.
   * @param from      Начало периода (по UTC).
   * @param to        Окончание периода (по UTC).
   * @return Идентификатор задачи составления отчёта.
   */
  @Nonnull
  public CompletableFuture<String> requestBrokerReport(
    @Nonnull String accountId,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    if (Helpers.areFromAndToValid(from, to)) {
      var request = BrokerReportRequest.newBuilder()
        .setGenerateBrokerReportRequest(
          GenerateBrokerReportRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build())
        .build();
      return Helpers.<BrokerReportResponse>wrapWithFuture(
          observer -> operationsStub.getBrokerReport(request, observer))
        .thenApply(BrokerReportResponse::getGenerateBrokerReportResponse)
        .thenApply(GenerateBrokerReportResponse::getTaskId);
    } else {
      return CompletableFuture.failedFuture(new IllegalArgumentException(Helpers.TO_IS_NOT_AFTER_FROM_MESSAGE));
    }
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
    if (page < 0) {
      return CompletableFuture.failedFuture(
        new IllegalArgumentException("Номерами страниц могут быть только положительные числа."));
    }

    var request = BrokerReportRequest.newBuilder()
      .setGetBrokerReportRequest(GetBrokerReportRequest.newBuilder().setTaskId(taskId).setPage(page).build())
      .build();
    return Helpers.<BrokerReportResponse>wrapWithFuture(
        observer -> operationsStub.getBrokerReport(request, observer))
      .thenApply(BrokerReportResponse::getGetBrokerReportResponse);
  }
}
