package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.contract.v1.CancelStopOrderRequest;
import ru.tinkoff.piapi.contract.v1.CancelStopOrderResponse;
import ru.tinkoff.piapi.contract.v1.GetStopOrdersRequest;
import ru.tinkoff.piapi.contract.v1.GetStopOrdersResponse;
import ru.tinkoff.piapi.contract.v1.PostStopOrderRequest;
import ru.tinkoff.piapi.contract.v1.PostStopOrderResponse;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.StopOrder;
import ru.tinkoff.piapi.contract.v1.StopOrderDirection;
import ru.tinkoff.piapi.contract.v1.StopOrderExpirationType;
import ru.tinkoff.piapi.contract.v1.StopOrderType;
import ru.tinkoff.piapi.contract.v1.StopOrdersServiceGrpc.StopOrdersServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.StopOrdersServiceGrpc.StopOrdersServiceStub;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.tinkoff.piapi.core.utils.Helpers.unaryCall;
import static ru.tinkoff.piapi.core.utils.ValidationUtils.checkReadonly;
import static ru.tinkoff.piapi.core.utils.ValidationUtils.checkSandbox;

public class StopOrdersService {
  private final StopOrdersServiceBlockingStub stopOrdersBlockingStub;
  private final StopOrdersServiceStub stopOrdersStub;
  private final boolean readonlyMode;
  private final boolean sandboxMode;

  StopOrdersService(@Nonnull StopOrdersServiceBlockingStub stopOrdersBlockingStub,
                    @Nonnull StopOrdersServiceStub stopOrdersStub,
                    boolean readonlyMode,
                    boolean sandboxMode) {
    this.sandboxMode = sandboxMode;
    this.stopOrdersBlockingStub = stopOrdersBlockingStub;
    this.stopOrdersStub = stopOrdersStub;
    this.readonlyMode = readonlyMode;
  }

  @Nonnull
  public String postStopOrderGoodTillCancelSync(@Nonnull String figi,
                                                long quantity,
                                                @Nonnull Quotation price,
                                                @Nonnull Quotation stopPrice,
                                                @Nonnull StopOrderDirection direction,
                                                @Nonnull String accountId,
                                                @Nonnull StopOrderType type) {
    checkReadonly(readonlyMode);
    checkSandbox(sandboxMode);

    return unaryCall(() -> stopOrdersBlockingStub.postStopOrder(
        PostStopOrderRequest.newBuilder()
          .setFigi(figi)
          .setQuantity(quantity)
          .setPrice(price)
          .setStopPrice(stopPrice)
          .setDirection(direction)
          .setAccountId(accountId)
          .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_CANCEL)
          .setStopOrderType(type)
          .build())
      .getStopOrderId());
  }

  @Nonnull
  public String postStopOrderGoodTillDateSync(@Nonnull String figi,
                                              long quantity,
                                              @Nonnull Quotation price,
                                              @Nonnull Quotation stopPrice,
                                              @Nonnull StopOrderDirection direction,
                                              @Nonnull String accountId,
                                              @Nonnull StopOrderType type,
                                              @Nonnull Instant expireDate) {
    checkReadonly(readonlyMode);
    checkSandbox(sandboxMode);

    return unaryCall(() -> stopOrdersBlockingStub.postStopOrder(
        PostStopOrderRequest.newBuilder()
          .setFigi(figi)
          .setQuantity(quantity)
          .setPrice(price)
          .setStopPrice(stopPrice)
          .setDirection(direction)
          .setAccountId(accountId)
          .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_DATE)
          .setStopOrderType(type)
          .setExpireDate(DateUtils.instantToTimestamp(expireDate))
          .build())
      .getStopOrderId());
  }

  @Nonnull
  public List<StopOrder> getStopOrdersSync(@Nonnull String accountId) {
    checkSandbox(sandboxMode);

    return unaryCall(() -> stopOrdersBlockingStub.getStopOrders(
        GetStopOrdersRequest.newBuilder()
          .setAccountId(accountId)
          .build())
      .getStopOrdersList());
  }

  @Nonnull
  public Instant cancelStopOrderSync(@Nonnull String accountId,
                                     @Nonnull String stopOrderId) {
    checkReadonly(readonlyMode);
    checkSandbox(sandboxMode);

    var responseTime = unaryCall(() -> stopOrdersBlockingStub.cancelStopOrder(
        CancelStopOrderRequest.newBuilder()
          .setAccountId(accountId)
          .setStopOrderId(stopOrderId)
          .build())
      .getTime());

    return DateUtils.timestampToInstant(responseTime);
  }

  @Nonnull
  public CompletableFuture<String> postStopOrderGoodTillCancel(@Nonnull String figi,
                                                               long quantity,
                                                               @Nonnull Quotation price,
                                                               @Nonnull Quotation stopPrice,
                                                               @Nonnull StopOrderDirection direction,
                                                               @Nonnull String accountId,
                                                               @Nonnull StopOrderType type) {
    checkReadonly(readonlyMode);
    checkSandbox(sandboxMode);

    return Helpers.<PostStopOrderResponse>unaryAsyncCall(
        observer -> stopOrdersStub.postStopOrder(
          PostStopOrderRequest.newBuilder()
            .setFigi(figi)
            .setQuantity(quantity)
            .setPrice(price)
            .setStopPrice(stopPrice)
            .setDirection(direction)
            .setAccountId(accountId)
            .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_CANCEL)
            .setStopOrderType(type)
            .build(),
          observer))
      .thenApply(PostStopOrderResponse::getStopOrderId);
  }

  @Nonnull
  public CompletableFuture<String> postStopOrderGoodTillDate(@Nonnull String figi,
                                                             long quantity,
                                                             @Nonnull Quotation price,
                                                             @Nonnull Quotation stopPrice,
                                                             @Nonnull StopOrderDirection direction,
                                                             @Nonnull String accountId,
                                                             @Nonnull StopOrderType type,
                                                             @Nonnull Instant expireDate) {
    checkReadonly(readonlyMode);
    checkSandbox(sandboxMode);

    return Helpers.<PostStopOrderResponse>unaryAsyncCall(
        observer -> stopOrdersStub.postStopOrder(
          PostStopOrderRequest.newBuilder()
            .setFigi(figi)
            .setQuantity(quantity)
            .setPrice(price)
            .setStopPrice(stopPrice)
            .setDirection(direction)
            .setAccountId(accountId)
            .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_DATE)
            .setStopOrderType(type)
            .setExpireDate(DateUtils.instantToTimestamp(expireDate))
            .build(),
          observer))
      .thenApply(PostStopOrderResponse::getStopOrderId);
  }

  @Nonnull
  public CompletableFuture<List<StopOrder>> getStopOrders(@Nonnull String accountId) {
    checkSandbox(sandboxMode);

    return Helpers.<GetStopOrdersResponse>unaryAsyncCall(
        observer -> stopOrdersStub.getStopOrders(
          GetStopOrdersRequest.newBuilder()
            .setAccountId(accountId)
            .build(),
          observer))
      .thenApply(GetStopOrdersResponse::getStopOrdersList);
  }

  @Nonnull
  public CompletableFuture<Instant> cancelStopOrder(@Nonnull String accountId,
                                                    @Nonnull String stopOrderId) {
    checkReadonly(readonlyMode);
    checkSandbox(sandboxMode);

    return Helpers.<CancelStopOrderResponse>unaryAsyncCall(
        observer -> stopOrdersStub.cancelStopOrder(
          CancelStopOrderRequest.newBuilder()
            .setAccountId(accountId)
            .setStopOrderId(stopOrderId)
            .build(),
          observer))
      .thenApply(response -> DateUtils.timestampToInstant(response.getTime()));
  }
}
