package ru.tinkoff.piapi.core.stream;

import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.InfoInstrument;
import ru.tinkoff.piapi.contract.v1.LastPriceInstrument;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.OrderBookInstrument;
import ru.tinkoff.piapi.contract.v1.SubscribeCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeInfoRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeLastPriceRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeOrderBookRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeTradesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.tinkoff.piapi.contract.v1.TradeInstrument;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

public class MarketDataSubscriptionService {
  private final StreamObserver<MarketDataRequest> observer;

  public MarketDataSubscriptionService(
    @Nonnull MarketDataStreamServiceGrpc.MarketDataStreamServiceStub stub, StreamProcessor<MarketDataResponse> streamProcessor, Consumer<Throwable> onErrorCallback) {
    this.observer = stub.marketDataStream(new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback));
  }

  public void subscribeTrades(@Nonnull List<String> figis) {
    tradesStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE);
  }

  public void unsubscribeTrades(@Nonnull List<String> figis) {
    tradesStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE);
  }

  public void subscribeOrderbook(@Nonnull List<String> figis,
                                 int depth) {
    orderBookStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, depth);
  }

  public void subscribeOrderbook(@Nonnull List<String> figis) {
    orderBookStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, 1);
  }

  public void unsubscribeOrderbook(@Nonnull List<String> figis,
                                   int depth) {
    orderBookStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, depth);
  }

  public void unsubscribeOrderbook(@Nonnull List<String> figis) {
    orderBookStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, 1);
  }

  public void subscribeInfo(@Nonnull List<String> figis) {
    infoStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE);
  }


  public void unsubscribeInfo(@Nonnull List<String> figis) {
    infoStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE);
  }


  public void subscribeCandles(@Nonnull List<String> figis) {
    candlesStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE,
      SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE);
  }

  public void subscribeCandles(@Nonnull List<String> figis, SubscriptionInterval interval) {
    candlesStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, interval);
  }

  public void unsubscribeCandles(@Nonnull List<String> figis) {
    candlesStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE,
      SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE);
  }

  public void unsubscribeCandles(@Nonnull List<String> figis, SubscriptionInterval interval) {
    candlesStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, interval);
  }


  public void subscribeLastPrices(@Nonnull List<String> figis) {
    lastPricesStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE);
  }

  public void unsubscribeLastPrices(@Nonnull List<String> figis) {
    lastPricesStream(figis, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE);
  }


  private void candlesStream(@Nonnull List<String> figis,
                             @Nonnull SubscriptionAction action,
                             @Nonnull SubscriptionInterval interval) {
    var builder = SubscribeCandlesRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (String figi : figis) {
      builder.addInstruments(CandleInstrument
        .newBuilder()
        .setInterval(interval)
        .setFigi(figi)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeCandlesRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void lastPricesStream(@Nonnull List<String> figis,
                                @Nonnull SubscriptionAction action) {
    var builder = SubscribeLastPriceRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (String figi : figis) {
      builder.addInstruments(LastPriceInstrument
        .newBuilder()
        .setFigi(figi)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeLastPriceRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void tradesStream(@Nonnull List<String> figis,
                            @Nonnull SubscriptionAction action) {
    var builder = SubscribeTradesRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (String figi : figis) {
      builder.addInstruments(TradeInstrument
        .newBuilder()
        .setFigi(figi)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeTradesRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void orderBookStream(@Nonnull List<String> figis,
                               @Nonnull SubscriptionAction action,
                               int depth) {
    var builder = SubscribeOrderBookRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (String figi : figis) {
      builder.addInstruments(OrderBookInstrument
        .newBuilder()
        .setDepth(depth)
        .setFigi(figi)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeOrderBookRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void infoStream(@Nonnull List<String> figis,
                          @Nonnull SubscriptionAction action) {
    var builder = SubscribeInfoRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (String figi : figis) {
      builder.addInstruments(InfoInstrument.newBuilder().setFigi(figi).build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeInfoRequest(builder)
      .build();
    observer.onNext(request);
  }
}
