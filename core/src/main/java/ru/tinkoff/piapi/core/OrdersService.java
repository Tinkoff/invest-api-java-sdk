package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.contract.v1.OrdersServiceGrpc.OrdersServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.OrdersServiceGrpc.OrdersServiceStub;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.tinkoff.piapi.core.utils.Helpers.unaryCall;
import static ru.tinkoff.piapi.core.utils.ValidationUtils.checkReadonly;

public class OrdersService {
  private final OrdersServiceBlockingStub ordersBlockingStub;
  private final OrdersServiceStub ordersStub;
  private final boolean readonlyMode;

  OrdersService(@Nonnull OrdersServiceBlockingStub ordersBlockingStub,
                @Nonnull OrdersServiceStub ordersStub,
                boolean readonlyMode) {
    this.ordersBlockingStub = ordersBlockingStub;
    this.ordersStub = ordersStub;
    this.readonlyMode = readonlyMode;
  }


  @Nonnull
  public PostOrderResponse postOrderSync(@Nonnull String figi,
                                         long quantity,
                                         @Nonnull Quotation price,
                                         @Nonnull OrderDirection direction,
                                         @Nonnull String accountId,
                                         @Nonnull OrderType type,
                                         @Nonnull String orderId) {
    checkReadonly(readonlyMode);

    return unaryCall(() -> ordersBlockingStub.postOrder(
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
  public Instant cancelOrderSync(@Nonnull String accountId,
                                 @Nonnull String orderId) {
    checkReadonly(readonlyMode);

    var responseTime = unaryCall(() -> ordersBlockingStub.cancelOrder(
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
    return unaryCall(() -> ordersBlockingStub.getOrderState(
      GetOrderStateRequest.newBuilder()
        .setAccountId(accountId)
        .setOrderId(orderId)
        .build()));
  }

  @Nonnull
  public List<OrderState> getOrdersSync(@Nonnull String accountId) {
    return unaryCall(() -> ordersBlockingStub.getOrders(
        GetOrdersRequest.newBuilder()
          .setAccountId(accountId)
          .build())
      .getOrdersList());
  }

  @Nonnull
  public CompletableFuture<PostOrderResponse> postOrder(@Nonnull String figi,
                                                        long quantity,
                                                        @Nonnull Quotation price,
                                                        @Nonnull OrderDirection direction,
                                                        @Nonnull String accountId,
                                                        @Nonnull OrderType type,
                                                        @Nonnull String orderId) {
    checkReadonly(readonlyMode);

    return Helpers.unaryAsyncCall(
      observer -> ordersStub.postOrder(
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
  public CompletableFuture<Instant> cancelOrder(@Nonnull String accountId,
                                                @Nonnull String orderId) {
    checkReadonly(readonlyMode);

    return Helpers.<CancelOrderResponse>unaryAsyncCall(
        observer -> ordersStub.cancelOrder(
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
      observer -> ordersStub.getOrderState(
        GetOrderStateRequest.newBuilder()
          .setAccountId(accountId)
          .setOrderId(orderId)
          .build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<List<OrderState>> getOrders(@Nonnull String accountId) {
    return Helpers.<GetOrdersResponse>unaryAsyncCall(
        observer -> ordersStub.getOrders(
          GetOrdersRequest.newBuilder()
            .setAccountId(accountId)
            .build(),
          observer))
      .thenApply(GetOrdersResponse::getOrdersList);
  }

  /** Последовательное выполнение 2 операций - отмены и выставления нового ордера
   *
   * @param accountId Номер счета
   * @param quantity Количество лотов
   * @param price Цена за 1 инструмент
   * @param idempotencyKey Новый идентификатор запроса выставления поручения для целей идемпотентности. Максимальная длина 36 символов. Перезатирает старый ключ
   * @param orderId Идентификатор заявки на бирже
   * @param priceType Тип цены. Пока не используется (можно передавать null)
   * @return Информация о выставлении поручения
   */
  @Nonnull
  public CompletableFuture<PostOrderResponse> replaceOrder(@Nonnull String accountId,
                                                           long quantity,
                                                           @Nonnull Quotation price,
                                                           @Nullable String idempotencyKey,
                                                           @Nonnull String orderId,
                                                           @Nullable PriceType priceType) {
    var request = ReplaceOrderRequest.newBuilder()
      .setAccountId(accountId)
      .setPrice(price)
      .setQuantity(quantity)
      .setIdempotencyKey(idempotencyKey == null ? "" : idempotencyKey)
      .setOrderId(orderId)
      .setPriceType(priceType == null ? PriceType.PRICE_TYPE_UNSPECIFIED : priceType)
      .build();
    return Helpers.unaryAsyncCall(
      observer -> ordersStub.replaceOrder(request, observer));
  }

  /** Последовательное выполнение 2 операций - отмены и выставления нового ордера
   *
   * @param accountId Номер счета
   * @param quantity Количество лотов
   * @param price Цена за 1 инструмент
   * @param idempotencyKey Новый идентификатор запроса выставления поручения для целей идемпотентности. Максимальная длина 36 символов. Перезатирает старый ключ
   * @param orderId Идентификатор заявки на бирже
   * @param priceType Тип цены. Пока не используется (можно передавать null)
   * @return Информация о выставлении поручения
   */
  @Nonnull
  public PostOrderResponse replaceOrderSync(@Nonnull String accountId,
                                            long quantity,
                                            @Nonnull Quotation price,
                                            @Nullable String idempotencyKey,
                                            @Nonnull String orderId,
                                            @Nullable PriceType priceType) {
    var request = ReplaceOrderRequest.newBuilder()
      .setAccountId(accountId)
      .setPrice(price)
      .setQuantity(quantity)
      .setIdempotencyKey(idempotencyKey == null ? "" : idempotencyKey)
      .setOrderId(orderId)
      .setPriceType(priceType == null ? PriceType.PRICE_TYPE_UNSPECIFIED : priceType)
      .build();
    return unaryCall(() -> ordersBlockingStub.replaceOrder(request));
  }
}
