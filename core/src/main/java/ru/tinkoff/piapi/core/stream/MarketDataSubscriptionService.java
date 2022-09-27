package ru.tinkoff.piapi.core.stream;

import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.contract.v1.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class MarketDataSubscriptionService {
  private final StreamObserver<MarketDataRequest> observer;

  public MarketDataSubscriptionService(
    @Nonnull MarketDataStreamServiceGrpc.MarketDataStreamServiceStub stub,
    @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
    @Nullable Consumer<Throwable> onErrorCallback) {
    this.observer = stub.marketDataStream(new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback));
  }

  public void subscribeTrades(@Nonnull List<String> instrumentIds) {
    tradesStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE);
  }

  public void unsubscribeTrades(@Nonnull List<String> instrumentIds) {
    tradesStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE);
  }

  public void subscribeOrderbook(@Nonnull List<String> instrumentIds,
                                 int depth) {
    orderBookStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, depth);
  }

  public void subscribeOrderbook(@Nonnull List<String> instrumentIds) {
    orderBookStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, 1);
  }

  public void unsubscribeOrderbook(@Nonnull List<String> instrumentIds,
                                   int depth) {
    orderBookStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, depth);
  }

  public void unsubscribeOrderbook(@Nonnull List<String> instrumentIds) {
    orderBookStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, 1);
  }

  public void subscribeInfo(@Nonnull List<String> instrumentIds) {
    infoStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE);
  }

  public void unsubscribeInfo(@Nonnull List<String> instrumentIds) {
    infoStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE);
  }


  public void subscribeCandles(@Nonnull List<String> instrumentIds) {
    candlesStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE,
      SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE);
  }

  public void subscribeCandles(@Nonnull List<String> instrumentIds, SubscriptionInterval interval) {
    candlesStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, interval);
  }

  public void unsubscribeCandles(@Nonnull List<String> instrumentIds) {
    candlesStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE,
      SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE);
  }

  public void unsubscribeCandles(@Nonnull List<String> instrumentIds, SubscriptionInterval interval) {
    candlesStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, interval);
  }


  public void subscribeLastPrices(@Nonnull List<String> instrumentIds) {
    lastPricesStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE);
  }

  public void unsubscribeLastPrices(@Nonnull List<String> instrumentIds) {
    lastPricesStream(instrumentIds, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE);
  }


  private void candlesStream(@Nonnull List<String> instrumentIds,
                             @Nonnull SubscriptionAction action,
                             @Nonnull SubscriptionInterval interval) {
    var builder = SubscribeCandlesRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (var instrumentId : instrumentIds) {
      builder.addInstruments(CandleInstrument
        .newBuilder()
        .setInterval(interval)
        .setInstrumentId(instrumentId)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeCandlesRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void lastPricesStream(@Nonnull List<String> instrumentIds,
                                @Nonnull SubscriptionAction action) {
    var builder = SubscribeLastPriceRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (var instrumentId : instrumentIds) {
      builder.addInstruments(LastPriceInstrument
        .newBuilder()
        .setInstrumentId(instrumentId)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeLastPriceRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void tradesStream(@Nonnull List<String> instrumentIds,
                            @Nonnull SubscriptionAction action) {
    var builder = SubscribeTradesRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (String instrumentId : instrumentIds) {
      builder.addInstruments(TradeInstrument
        .newBuilder()
        .setInstrumentId(instrumentId)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeTradesRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void orderBookStream(@Nonnull List<String> instrumentIds,
                               @Nonnull SubscriptionAction action,
                               int depth) {
    var builder = SubscribeOrderBookRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (var instrumentId : instrumentIds) {
      builder.addInstruments(OrderBookInstrument
        .newBuilder()
        .setDepth(depth)
        .setInstrumentId(instrumentId)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeOrderBookRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void infoStream(@Nonnull List<String> instrumentIds,
                          @Nonnull SubscriptionAction action) {
    var builder = SubscribeInfoRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (var instrumentId : instrumentIds) {
      builder.addInstruments(InfoInstrument.newBuilder().setInstrumentId(instrumentId).build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeInfoRequest(builder)
      .build();
    observer.onNext(request);
  }
}
