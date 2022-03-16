package ru.tinkoff.piapi.core;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import org.reactivestreams.FlowAdapters;
import ru.tinkoff.piapi.contract.v1.CancelOrderRequest;
import ru.tinkoff.piapi.contract.v1.CancelOrderResponse;
import ru.tinkoff.piapi.contract.v1.GetOrderStateRequest;
import ru.tinkoff.piapi.contract.v1.GetOrdersRequest;
import ru.tinkoff.piapi.contract.v1.GetOrdersResponse;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.contract.v1.OrderType;
import ru.tinkoff.piapi.contract.v1.OrdersServiceGrpc.OrdersServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.OrdersServiceGrpc.OrdersServiceStub;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc.OrdersStreamServiceStub;
import ru.tinkoff.piapi.contract.v1.PostOrderRequest;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.TradesStreamRequest;
import ru.tinkoff.piapi.contract.v1.TradesStreamResponse;
import ru.tinkoff.piapi.core.stream.OrdersStreamService;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Consumer;

import static ru.tinkoff.piapi.core.utils.Helpers.unaryCall;
import static ru.tinkoff.piapi.core.utils.ValidationUtils.checkReadonly;

public class OrdersService {
  private final OrdersStreamServiceStub ordersStreamStub;
  private final OrdersServiceBlockingStub ordersBlockingStub;
  private final OrdersServiceStub ordersStub;
  private final boolean readonlyMode;

  OrdersService(@Nonnull OrdersStreamServiceStub ordersStreamStub,
                @Nonnull OrdersServiceBlockingStub ordersBlockingStub,
                @Nonnull OrdersServiceStub ordersStub,
                boolean readonlyMode) {
    this.ordersStreamStub = ordersStreamStub;
    this.ordersBlockingStub = ordersBlockingStub;
    this.ordersStub = ordersStub;
    this.readonlyMode = readonlyMode;
  }


  /**
   * Deprecated. Используйте {@link OrdersStreamService}
   */
  @Nonnull
  public Publisher<TradesStreamResponse> ordersStream() {
    var mutinyPublisher = Multi.createFrom().<TradesStreamResponse>emitter(
      emitter -> ordersStreamStub.tradesStream(
        TradesStreamRequest.newBuilder().build(),
        Helpers.wrapEmitterWithStreamObserver(emitter)),
      BackPressureStrategy.BUFFER);

    return FlowAdapters.toFlowPublisher(mutinyPublisher);
  }

  /**
   * Deprecated. Используйте {@link OrdersStreamService}
   */
  public void subscribeTradesStream(Consumer<TradesStreamResponse> consumer) {
    Multi.createFrom()
      .safePublisher(
        FlowAdapters.toPublisher(ordersStream()))
      .subscribe()
      .asIterable()
      .forEach(consumer);
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
}
