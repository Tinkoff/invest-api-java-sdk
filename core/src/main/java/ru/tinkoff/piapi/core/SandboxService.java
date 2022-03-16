package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.contract.v1.CancelOrderRequest;
import ru.tinkoff.piapi.contract.v1.CancelOrderResponse;
import ru.tinkoff.piapi.contract.v1.CloseSandboxAccountRequest;
import ru.tinkoff.piapi.contract.v1.CloseSandboxAccountResponse;
import ru.tinkoff.piapi.contract.v1.GetAccountsRequest;
import ru.tinkoff.piapi.contract.v1.GetAccountsResponse;
import ru.tinkoff.piapi.contract.v1.GetOrderStateRequest;
import ru.tinkoff.piapi.contract.v1.GetOrdersRequest;
import ru.tinkoff.piapi.contract.v1.GetOrdersResponse;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.OpenSandboxAccountRequest;
import ru.tinkoff.piapi.contract.v1.OpenSandboxAccountResponse;
import ru.tinkoff.piapi.contract.v1.Operation;
import ru.tinkoff.piapi.contract.v1.OperationState;
import ru.tinkoff.piapi.contract.v1.OperationsRequest;
import ru.tinkoff.piapi.contract.v1.OperationsResponse;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.contract.v1.OrderType;
import ru.tinkoff.piapi.contract.v1.PortfolioRequest;
import ru.tinkoff.piapi.contract.v1.PortfolioResponse;
import ru.tinkoff.piapi.contract.v1.PositionsRequest;
import ru.tinkoff.piapi.contract.v1.PositionsResponse;
import ru.tinkoff.piapi.contract.v1.PostOrderRequest;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.SandboxPayInRequest;
import ru.tinkoff.piapi.contract.v1.SandboxPayInResponse;
import ru.tinkoff.piapi.contract.v1.SandboxServiceGrpc.SandboxServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.SandboxServiceGrpc.SandboxServiceStub;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.tinkoff.piapi.core.utils.Helpers.unaryCall;

public class SandboxService {
  private final SandboxServiceBlockingStub sandboxBlockingStub;
  private final SandboxServiceStub sandboxStub;

  SandboxService(@Nonnull SandboxServiceBlockingStub sandboxBlockingStub,
                 @Nonnull SandboxServiceStub sandboxStub) {
    this.sandboxBlockingStub = sandboxBlockingStub;
    this.sandboxStub = sandboxStub;
  }

  @Nonnull
  public String openAccountSync() {
    return unaryCall(() -> sandboxBlockingStub.openSandboxAccount(
        OpenSandboxAccountRequest.newBuilder()
          .build())
      .getAccountId());
  }

  @Nonnull
  public List<Account> getAccountsSync() {
    return unaryCall(() -> sandboxBlockingStub.getSandboxAccounts(
        GetAccountsRequest.newBuilder()
          .build())
      .getAccountsList());
  }

  public void closeAccountSync(@Nonnull String accountId) {
    unaryCall(() -> sandboxBlockingStub.closeSandboxAccount(
      CloseSandboxAccountRequest.newBuilder()
        .setAccountId(accountId)
        .build()));
  }

  @Nonnull
  public PostOrderResponse postOrderSync(@Nonnull String figi,
                                         long quantity,
                                         @Nonnull Quotation price,
                                         @Nonnull OrderDirection direction,
                                         @Nonnull String accountId,
                                         @Nonnull OrderType type,
                                         @Nonnull String orderId) {
    return unaryCall(() -> sandboxBlockingStub.postSandboxOrder(
      PostOrderRequest.newBuilder()
        .setFigi(figi)
        .setQuantity(quantity)
        .setPrice(price)
        .setDirection(direction)
        .setAccountId(accountId)
        .setOrderType(type)
        .setOrderId(Helpers.preprocessInputOrderId(orderId))
        .build()));
  }

  @Nonnull
  public List<OrderState> getOrdersSync(@Nonnull String accountId) {
    return unaryCall(() -> sandboxBlockingStub.getSandboxOrders(
        GetOrdersRequest.newBuilder()
          .setAccountId(accountId)
          .build())
      .getOrdersList());
  }

  @Nonnull
  public Instant cancelOrderSync(@Nonnull String accountId,
                                 @Nonnull String orderId) {
    var responseTime = unaryCall(() -> sandboxBlockingStub.cancelSandboxOrder(
        CancelOrderRequest.newBuilder()
          .setAccountId(accountId)
          .setOrderId(orderId)
          .build())
      .getTime());

    return DateUtils.timestampToInstant(responseTime);
  }

  @Nonnull
  public OrderState getOrderStateSync(@Nonnull String accountId,
                                      @Nonnull String orderId) {
    return unaryCall(() -> sandboxBlockingStub.getSandboxOrderState(
      GetOrderStateRequest.newBuilder()
        .setAccountId(accountId)
        .setOrderId(orderId)
        .build()));
  }

  @Nonnull
  public PositionsResponse getPositionsSync(@Nonnull String accountId) {
    return unaryCall(() -> sandboxBlockingStub.getSandboxPositions(
      PositionsRequest.newBuilder().setAccountId(accountId).build()));
  }

  @Nonnull
  public List<Operation> getOperationsSync(@Nonnull String accountId,
                                           @Nonnull Instant from,
                                           @Nonnull Instant to,
                                           @Nonnull OperationState operationState,
                                           @Nullable String figi) {
    return unaryCall(() -> sandboxBlockingStub.getSandboxOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setState(operationState)
          .setFigi(figi == null ? "" : figi)
          .build())
      .getOperationsList());
  }

  @Nonnull
  public PortfolioResponse getPortfolioSync(@Nonnull String accountId) {
    return unaryCall(() -> sandboxBlockingStub.getSandboxPortfolio(
      PortfolioRequest.newBuilder().setAccountId(accountId).build()));
  }

  @Nonnull
  public MoneyValue payInSync(@Nonnull String accountId, @Nonnull MoneyValue moneyValue) {
    return unaryCall(() -> sandboxBlockingStub.sandboxPayIn(
        SandboxPayInRequest.newBuilder()
          .setAccountId(accountId)
          .setAmount(moneyValue)
          .build())
      .getBalance());
  }

  @Nonnull
  public CompletableFuture<String> openAccount() {
    return Helpers.<OpenSandboxAccountResponse>unaryAsyncCall(
        observer -> sandboxStub.openSandboxAccount(
          OpenSandboxAccountRequest.newBuilder()
            .build(),
          observer))
      .thenApply(OpenSandboxAccountResponse::getAccountId);
  }

  @Nonnull
  public CompletableFuture<List<Account>> getAccounts() {
    return Helpers.<GetAccountsResponse>unaryAsyncCall(
        observer -> sandboxStub.getSandboxAccounts(
          GetAccountsRequest.newBuilder().build(),
          observer))
      .thenApply(GetAccountsResponse::getAccountsList);
  }

  @Nonnull
  public CompletableFuture<Void> closeAccount(@Nonnull String accountId) {
    return Helpers.<CloseSandboxAccountResponse>unaryAsyncCall(
        observer -> sandboxStub.closeSandboxAccount(
          CloseSandboxAccountRequest.newBuilder()
            .setAccountId(accountId)
            .build(),
          observer))
      .thenApply(r -> null);
  }

  @Nonnull
  public CompletableFuture<PostOrderResponse> postOrder(@Nonnull String figi,
                                                        long quantity,
                                                        @Nonnull Quotation price,
                                                        @Nonnull OrderDirection direction,
                                                        @Nonnull String accountId,
                                                        @Nonnull OrderType type,
                                                        @Nonnull String orderId) {
    return Helpers.unaryAsyncCall(
      observer -> sandboxStub.postSandboxOrder(
        PostOrderRequest.newBuilder()
          .setFigi(figi)
          .setQuantity(quantity)
          .setPrice(price)
          .setDirection(direction)
          .setAccountId(accountId)
          .setOrderType(type)
          .setOrderId(Helpers.preprocessInputOrderId(orderId))
          .build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<List<OrderState>> getOrders(@Nonnull String accountId) {
    return Helpers.<GetOrdersResponse>unaryAsyncCall(
        observer -> sandboxStub.getSandboxOrders(
          GetOrdersRequest.newBuilder()
            .setAccountId(accountId)
            .build(),
          observer))
      .thenApply(GetOrdersResponse::getOrdersList);
  }

  @Nonnull
  public CompletableFuture<Instant> cancelOrder(@Nonnull String accountId,
                                                @Nonnull String orderId) {
    return Helpers.<CancelOrderResponse>unaryAsyncCall(
        observer -> sandboxStub.cancelSandboxOrder(
          CancelOrderRequest.newBuilder()
            .setAccountId(accountId)
            .setOrderId(orderId)
            .build(),
          observer))
      .thenApply(response -> DateUtils.timestampToInstant(response.getTime()));
  }

  @Nonnull
  public CompletableFuture<OrderState> getOrderState(@Nonnull String accountId,
                                                     @Nonnull String orderId) {
    return Helpers.unaryAsyncCall(
      observer -> sandboxStub.getSandboxOrderState(
        GetOrderStateRequest.newBuilder()
          .setAccountId(accountId)
          .setOrderId(orderId)
          .build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<PositionsResponse> getPositions(@Nonnull String accountId) {
    return Helpers.unaryAsyncCall(
      observer -> sandboxStub.getSandboxPositions(
        PositionsRequest.newBuilder().setAccountId(accountId).build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<List<Operation>> getOperations(@Nonnull String accountId,
                                                          @Nonnull Instant from,
                                                          @Nonnull Instant to,
                                                          @Nonnull OperationState operationState,
                                                          @Nullable String figi) {
    return Helpers.<OperationsResponse>unaryAsyncCall(
        observer -> sandboxStub.getSandboxOperations(
          OperationsRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .setState(operationState)
            .setFigi(figi == null ? "" : figi)
            .build(),
          observer))
      .thenApply(OperationsResponse::getOperationsList);
  }

  @Nonnull
  public CompletableFuture<PortfolioResponse> getPortfolio(@Nonnull String accountId) {
    return Helpers.unaryAsyncCall(
      observer -> sandboxStub.getSandboxPortfolio(
        PortfolioRequest.newBuilder().setAccountId(accountId).build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<MoneyValue> payIn(@Nonnull String accountId,
                                             @Nonnull MoneyValue moneyValue) {
    return Helpers.<SandboxPayInResponse>unaryAsyncCall(
        observer -> sandboxStub.sandboxPayIn(
          SandboxPayInRequest.newBuilder()
            .setAccountId(accountId)
            .setAmount(moneyValue)
            .build(),
          observer))
      .thenApply(SandboxPayInResponse::getBalance);
  }
}
