package ru.tinkoff.piapi.core.stream;

import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.InfoInstrument;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.OrderBookInstrument;
import ru.tinkoff.piapi.contract.v1.SubscribeCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeInfoRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeOrderBookRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeTradesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.tinkoff.piapi.contract.v1.TradeInstrument;
import ru.tinkoff.piapi.core.stream.StreamObserverWithProcessor;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MarketDataStreamService {
  private final MarketDataStreamServiceGrpc.MarketDataStreamServiceStub stub;
  private final Map<StreamProcessor<MarketDataResponse>, StreamObserverWithProcessor<MarketDataResponse>> processorObserverMap;

  public MarketDataStreamService(
    @Nonnull MarketDataStreamServiceGrpc.MarketDataStreamServiceStub stub) {
    this.stub = stub;
    processorObserverMap = new HashMap<>();
  }

  public void subscribeTradesStream(@Nonnull List<String> figis,
                                    @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                    @Nullable Consumer<Throwable> onErrorCallback) {
    tradesStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, onErrorCallback);
  }

  public void subscribeTradesStream(@Nonnull List<String> figis,
                                    @Nonnull StreamProcessor<MarketDataResponse> streamProcessor) {
    tradesStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, null);
  }

  public void unsubscribeTradesStream(@Nonnull List<String> figis,
                                      @Nonnull StreamProcessor<MarketDataResponse> streamProcessor) {
    tradesStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, null);
  }

  public void subscribeOrderbookStream(@Nonnull List<String> figis,
                                       @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                       @Nullable Consumer<Throwable> onErrorCallback) {
    orderBookStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, onErrorCallback, 1);
  }

  public void subscribeOrderbookStream(@Nonnull List<String> figis,
                                       @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                       int depth) {
    orderBookStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, null, depth);
  }

  public void subscribeOrderbookStream(@Nonnull List<String> figis,
                                       @Nonnull StreamProcessor<MarketDataResponse> streamProcessor) {
    orderBookStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, null, 1);
  }

  public void subscribeOrderbookStream(@Nonnull List<String> figis,
                                       @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                       @Nullable Consumer<Throwable> onErrorCallback,
                                       int depth) {
    orderBookStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, onErrorCallback, depth);
  }

  public void unsubscribeOrderbookStream(@Nonnull List<String> figis,
                                         @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                         int depth) {
    orderBookStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, null, depth);
  }

  public void subscribeInfoStream(@Nonnull List<String> figis,
                                  @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                  @Nullable Consumer<Throwable> onErrorCallback) {
    infoStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, onErrorCallback);
  }

  public void subscribeInfoStream(@Nonnull List<String> figis,
                                  @Nonnull StreamProcessor<MarketDataResponse> streamProcessor) {
    infoStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, null);
  }

  public void unsubscribeInfoStream(@Nonnull List<String> figis,
                                    @Nonnull StreamProcessor<MarketDataResponse> streamProcessor) {
    infoStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, null);
  }

  public void subscribeCandlesStream(@Nonnull List<String> figis,
                                     @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                     @Nullable Consumer<Throwable> onErrorCallback,
                                     @Nonnull SubscriptionInterval interval) {
    candlesStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, onErrorCallback, interval);
  }

  public void subscribeCandlesStream(@Nonnull List<String> figis,
                                     @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                     @Nullable Consumer<Throwable> onErrorCallback) {
    candlesStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, onErrorCallback,
      SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE);
  }

  public void subscribeCandlesStream(@Nonnull List<String> figis,
                                     @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                     @Nonnull SubscriptionInterval interval) {
    candlesStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, null, interval);
  }

  public void subscribeCandlesStream(@Nonnull List<String> figis,
                                     @Nonnull StreamProcessor<MarketDataResponse> streamProcessor) {
    candlesStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE, null,
      SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE);
  }

  public void unsubscribeCandlesStream(@Nonnull List<String> figis,
                                       @Nonnull StreamProcessor<MarketDataResponse> streamProcessor) {
    candlesStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, null,
      SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE);
  }

  public void unsubscribeCandlesStream(@Nonnull List<String> figis,
                                       @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                       @Nonnull SubscriptionInterval interval) {
    candlesStream(figis, streamProcessor, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE, null, interval);
  }


  private void candlesStream(@Nonnull List<String> figis,
                             @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                             @Nonnull SubscriptionAction action,
                             @Nullable Consumer<Throwable> onErrorCallback,
                             @Nonnull SubscriptionInterval interval) {
    var candlesBuilder = SubscribeCandlesRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (String figi : figis) {
      candlesBuilder.addInstruments(CandleInstrument
        .newBuilder()
        .setInterval(interval)
        .setFigi(figi)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeCandlesRequest(candlesBuilder)
      .build();
    stub.marketDataStream(getObserver(streamProcessor, onErrorCallback)).onNext(request);
  }

  private void tradesStream(@Nonnull List<String> figis,
                            @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                            @Nonnull SubscriptionAction action,
                            @Nullable Consumer<Throwable> onErrorCallback) {
    var orderBookBuilder = SubscribeTradesRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (String figi : figis) {
      orderBookBuilder.addInstruments(TradeInstrument
        .newBuilder()
        .setFigi(figi)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeTradesRequest(orderBookBuilder)
      .build();
    stub.marketDataStream(getObserver(streamProcessor, onErrorCallback)).onNext(request);
  }

  private void orderBookStream(@Nonnull List<String> figis,
                               @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                               @Nonnull SubscriptionAction action,
                               @Nullable Consumer<Throwable> onErrorCallback,
                               int depth) {
    var orderBookBuilder = SubscribeOrderBookRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (String figi : figis) {
      orderBookBuilder.addInstruments(OrderBookInstrument
        .newBuilder()
        .setDepth(depth)
        .setFigi(figi)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeOrderBookRequest(orderBookBuilder)
      .build();
    stub.marketDataStream(getObserver(streamProcessor, onErrorCallback)).onNext(request);
  }

  private void infoStream(@Nonnull List<String> figis,
                          @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                          @Nonnull SubscriptionAction action,
                          @Nullable Consumer<Throwable> onErrorCallback) {
    var candlesBuilder = SubscribeInfoRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (String figi : figis) {
      candlesBuilder.addInstruments(InfoInstrument.newBuilder().setFigi(figi).build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeInfoRequest(candlesBuilder)
      .build();
    stub.marketDataStream(getObserver(streamProcessor, onErrorCallback)).onNext(request);
  }

  private StreamObserverWithProcessor<MarketDataResponse> getObserver(@Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                                                      @Nullable Consumer<Throwable> onErrorCallback) {
    if (processorObserverMap.containsKey(streamProcessor)) {
      return processorObserverMap.get(streamProcessor);
    }
    var observer = new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback);
    processorObserverMap.put(streamProcessor, observer);
    return observer;
  }
}
