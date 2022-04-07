package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.GetCandlesResponse;
import ru.tinkoff.piapi.contract.v1.GetLastPricesRequest;
import ru.tinkoff.piapi.contract.v1.GetLastPricesResponse;
import ru.tinkoff.piapi.contract.v1.GetLastTradesRequest;
import ru.tinkoff.piapi.contract.v1.GetLastTradesResponse;
import ru.tinkoff.piapi.contract.v1.GetOrderBookRequest;
import ru.tinkoff.piapi.contract.v1.GetOrderBookResponse;
import ru.tinkoff.piapi.contract.v1.GetTradingStatusRequest;
import ru.tinkoff.piapi.contract.v1.GetTradingStatusResponse;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.Trade;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc.MarketDataServiceBlockingStub;
import static ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc.MarketDataServiceStub;
import static ru.tinkoff.piapi.core.utils.Helpers.unaryCall;
import static ru.tinkoff.piapi.core.utils.ValidationUtils.checkFromTo;

public class MarketDataService {
  private final MarketDataServiceBlockingStub marketDataBlockingStub;
  private final MarketDataServiceStub marketDataStub;

  MarketDataService(@Nonnull MarketDataServiceBlockingStub marketDataBlockingStub,
                    @Nonnull MarketDataServiceStub marketDataStub) {
    this.marketDataBlockingStub = marketDataBlockingStub;
    this.marketDataStub = marketDataStub;
  }


  @Nonnull
  public List<Trade> getLastTradesSync(@Nonnull String figi,
                                       @Nonnull Instant from,
                                       @Nonnull Instant to) {
    checkFromTo(from, to);

    return unaryCall(() -> marketDataBlockingStub.getLastTrades(
        GetLastTradesRequest.newBuilder()
          .setFigi(figi)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .getTradesList());
  }

  @Nonnull
  public List<Trade> getLastTradesSync(@Nonnull String figi) {
    var to = Instant.now();
    var from = Instant.now().minus(60, ChronoUnit.MINUTES);
    return getLastTradesSync(figi, from, to);
  }

  @Nonnull
  public List<HistoricCandle> getCandlesSync(@Nonnull String figi,
                                             @Nonnull Instant from,
                                             @Nonnull Instant to,
                                             @Nonnull CandleInterval interval) {
    checkFromTo(from, to);

    return unaryCall(() -> marketDataBlockingStub.getCandles(
        GetCandlesRequest.newBuilder()
          .setFigi(figi)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setInterval(interval)
          .build())
      .getCandlesList());
  }

  @Nonnull
  public List<LastPrice> getLastPricesSync(@Nonnull Iterable<String> figies) {
    return unaryCall(() -> marketDataBlockingStub.getLastPrices(
        GetLastPricesRequest.newBuilder()
          .addAllFigi(figies)
          .build())
      .getLastPricesList());
  }

  @Nonnull
  public GetOrderBookResponse getOrderBookSync(@Nonnull String figi, int depth) {
    return unaryCall(() -> marketDataBlockingStub.getOrderBook(
      GetOrderBookRequest.newBuilder()
        .setFigi(figi)
        .setDepth(depth)
        .build()));
  }

  @Nonnull
  public GetTradingStatusResponse getTradingStatusSync(@Nonnull String figi) {
    return unaryCall(() -> marketDataBlockingStub.getTradingStatus(
      GetTradingStatusRequest.newBuilder()
        .setFigi(figi)
        .build()));
  }

  @Nonnull
  public CompletableFuture<List<HistoricCandle>> getCandles(@Nonnull String figi,
                                                            @Nonnull Instant from,
                                                            @Nonnull Instant to,
                                                            @Nonnull CandleInterval interval) {
    checkFromTo(from, to);

    return Helpers.<GetCandlesResponse>unaryAsyncCall(
        observer -> marketDataStub.getCandles(
          GetCandlesRequest.newBuilder()
            .setFigi(figi)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .setInterval(interval)
            .build(),
          observer))
      .thenApply(GetCandlesResponse::getCandlesList);
  }

  @Nonnull
  public CompletableFuture<List<Trade>> getLastTrades(@Nonnull String figi,
                                                      @Nonnull Instant from,
                                                      @Nonnull Instant to) {
    checkFromTo(from, to);

    return Helpers.<GetLastTradesResponse>unaryAsyncCall(
        observer -> marketDataStub.getLastTrades(
          GetLastTradesRequest.newBuilder()
            .setFigi(figi)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build(),
          observer))
      .thenApply(GetLastTradesResponse::getTradesList);
  }

  @Nonnull
  public CompletableFuture<List<Trade>> getLastTrades(@Nonnull String figi) {
    var to = Instant.now();
    var from = Instant.now().minus(60, ChronoUnit.MINUTES);
    return getLastTrades(figi, from, to);
  }

  @Nonnull
  public CompletableFuture<List<LastPrice>> getLastPrices(@Nonnull Iterable<String> figies) {
    return Helpers.<GetLastPricesResponse>unaryAsyncCall(
        observer -> marketDataStub.getLastPrices(
          GetLastPricesRequest.newBuilder()
            .addAllFigi(figies)
            .build(),
          observer))
      .thenApply(GetLastPricesResponse::getLastPricesList);
  }

  @Nonnull
  public CompletableFuture<GetOrderBookResponse> getOrderBook(@Nonnull String figi, int depth) {
    return Helpers.unaryAsyncCall(
      observer -> marketDataStub.getOrderBook(
        GetOrderBookRequest.newBuilder()
          .setFigi(figi)
          .setDepth(depth)
          .build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<GetTradingStatusResponse> getTradingStatus(@Nonnull String figi) {
    return Helpers.unaryAsyncCall(
      observer -> marketDataStub.getTradingStatus(
        GetTradingStatusRequest.newBuilder()
          .setFigi(figi)
          .build(),
        observer));
  }
}
