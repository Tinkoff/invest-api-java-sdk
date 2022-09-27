package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

  /**
   * Получение (синхронное) списка обезличенных сделок по инструменту.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @param from         Начало периода (по UTC).
   * @param to           Окончание периода (по UTC).
   * @return Список обезличенных сделок по инструменту.
   */
  @Nonnull
  public List<Trade> getLastTradesSync(@Nonnull String instrumentId,
                                       @Nonnull Instant from,
                                       @Nonnull Instant to) {
    checkFromTo(from, to);

    return unaryCall(() -> marketDataBlockingStub.getLastTrades(
        GetLastTradesRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .getTradesList());
  }

  /**
   * Получение (синхронное) списка обезличенных сделок по инструменту за последний час.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @return Список обезличенных сделок по инструменту.
   */
  @Nonnull
  public List<Trade> getLastTradesSync(@Nonnull String instrumentId) {
    var to = Instant.now();
    var from = to.minus(60, ChronoUnit.MINUTES);
    return getLastTradesSync(instrumentId, from, to);
  }

  /**
   * Получение (синхронное) списка свечей по инструменту.
   *
   * @param instrumentId идентификатор инструмента. Может принимать значение FIGI или uid
   * @param from         Начало периода (по UTC).
   * @param to           Окончание периода (по UTC).
   * @param interval     Интервал свечей
   * @return Список свечей
   */
  @Nonnull
  public List<HistoricCandle> getCandlesSync(@Nonnull String instrumentId,
                                             @Nonnull Instant from,
                                             @Nonnull Instant to,
                                             @Nonnull CandleInterval interval) {
    checkFromTo(from, to);

    return unaryCall(() -> marketDataBlockingStub.getCandles(
        GetCandlesRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setInterval(interval)
          .build())
      .getCandlesList());
  }

  /**
   * Получение (синхронное) списка последних цен по инструментам
   *
   * @param instrumentIds FIGI-идентификатор / uid инструмента.
   * @return Список последний цен
   */
  @Nonnull
  public List<LastPrice> getLastPricesSync(@Nonnull Iterable<String> instrumentIds) {
    return unaryCall(() -> marketDataBlockingStub.getLastPrices(
        GetLastPricesRequest.newBuilder()
          .addAllInstrumentId(instrumentIds)
          .build())
      .getLastPricesList());
  }

  /**
   * Получение (синхронное) информации о стакане
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @param depth        глубина стакана. Может принимать значения 1, 10, 20, 30, 40, 50
   * @return стакан для инструмента
   */
  @Nonnull
  public GetOrderBookResponse getOrderBookSync(@Nonnull String instrumentId, int depth) {
    return unaryCall(() -> marketDataBlockingStub.getOrderBook(
      GetOrderBookRequest.newBuilder()
        .setInstrumentId(instrumentId)
        .setDepth(depth)
        .build()));
  }

  /**
   * Получение (синхронное) текущего торгового статуса инструмента
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @return текущий торговый статус инструмента
   */
  @Nonnull
  public GetTradingStatusResponse getTradingStatusSync(@Nonnull String instrumentId) {
    return unaryCall(() -> marketDataBlockingStub.getTradingStatus(
      GetTradingStatusRequest.newBuilder()
        .setInstrumentId(instrumentId)
        .build()));
  }

  /**
   * Получение (асинхронное) списка свечей по инструменту.
   *
   * @param instrumentId идентификатор инструмента. Может принимать значение FIGI или uid
   * @param from         Начало периода (по UTC).
   * @param to           Окончание периода (по UTC).
   * @param interval     Интервал свечей
   * @return Список свечей
   */
  @Nonnull
  public CompletableFuture<List<HistoricCandle>> getCandles(@Nonnull String instrumentId,
                                                            @Nonnull Instant from,
                                                            @Nonnull Instant to,
                                                            @Nonnull CandleInterval interval) {
    checkFromTo(from, to);

    return Helpers.<GetCandlesResponse>unaryAsyncCall(
        observer -> marketDataStub.getCandles(
          GetCandlesRequest.newBuilder()
            .setInstrumentId(instrumentId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .setInterval(interval)
            .build(),
          observer))
      .thenApply(GetCandlesResponse::getCandlesList);
  }

  /**
   * Получение (асинхронное) списка обезличенных сделок по инструменту.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @param from         Начало периода (по UTC).
   * @param to           Окончание периода (по UTC).
   * @return Список обезличенных сделок по инструменту.
   */
  @Nonnull
  public CompletableFuture<List<Trade>> getLastTrades(@Nonnull String instrumentId,
                                                      @Nonnull Instant from,
                                                      @Nonnull Instant to) {
    checkFromTo(from, to);

    return Helpers.<GetLastTradesResponse>unaryAsyncCall(
        observer -> marketDataStub.getLastTrades(
          GetLastTradesRequest.newBuilder()
            .setInstrumentId(instrumentId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build(),
          observer))
      .thenApply(GetLastTradesResponse::getTradesList);
  }

  /**
   * Получение (асинхронное) списка обезличенных сделок по инструменту за последний час.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента..
   * @return Список обезличенных сделок по инструменту.
   */
  @Nonnull
  public CompletableFuture<List<Trade>> getLastTrades(@Nonnull String instrumentId) {
    var to = Instant.now();
    var from = to.minus(60, ChronoUnit.MINUTES);
    return getLastTrades(instrumentId, from, to);
  }

  /**
   * Получение (асинхронное) списка цен закрытия торговой сессии по инструменту.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @return Цена закрытия торговой сессии по инструменту.
   */
  @Nonnull
  public CompletableFuture<List<InstrumentClosePriceResponse>> getClosePrices(@Nonnull String instrumentId) {
    var instruments = InstrumentClosePriceRequest.newBuilder().setInstrumentId(instrumentId).build();

    return Helpers.<GetClosePricesResponse>unaryAsyncCall(
        observer -> marketDataStub.getClosePrices(
          GetClosePricesRequest.newBuilder()
            .addAllInstruments(List.of(instruments))
            .build(),
          observer))
      .thenApply(GetClosePricesResponse::getClosePricesList);
  }

  /**
   * Получение (асинхронное) списка цен закрытия торговой сессии по инструментам.
   *
   * @param instrumentIds FIGI-идентификатор / uid инструментов.
   * @return Цена закрытия торговой сессии по инструментам.
   */
  @Nonnull
  public CompletableFuture<List<InstrumentClosePriceResponse>> getClosePrices(@Nonnull Iterable<String> instrumentIds) {
    var instruments = new ArrayList<InstrumentClosePriceRequest>();
    for (String instrumentId : instrumentIds) {
      instruments.add(InstrumentClosePriceRequest.newBuilder().setInstrumentId(instrumentId).build());
    }

    return Helpers.<GetClosePricesResponse>unaryAsyncCall(
        observer -> marketDataStub.getClosePrices(
          GetClosePricesRequest.newBuilder()
            .addAllInstruments(instruments)
            .build(),
          observer))
      .thenApply(GetClosePricesResponse::getClosePricesList);
  }

  /**
   * Получение (асинхронное) списка цен закрытия торговой сессии по инструменту.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @return Цена закрытия торговой сессии по инструменту.
   */
  @Nonnull
  public List<InstrumentClosePriceResponse> getClosePricesSync(@Nonnull String instrumentId) {
    var instruments = InstrumentClosePriceRequest.newBuilder().setInstrumentId(instrumentId).build();

    return unaryCall(() -> marketDataBlockingStub.getClosePrices(
      GetClosePricesRequest.newBuilder()
        .addAllInstruments(List.of(instruments))
        .build()).getClosePricesList());
  }

  /**
   * Получение (асинхронное) списка цен закрытия торговой сессии по инструментам.
   *
   * @param instrumentIds FIGI-идентификатор / uid инструментов.
   * @return Цена закрытия торговой сессии по инструментам.
   */
  @Nonnull
  public List<InstrumentClosePriceResponse> getClosePricesSync(@Nonnull Iterable<String> instrumentIds) {
    var instruments = new ArrayList<InstrumentClosePriceRequest>();
    for (String instrumentId : instrumentIds) {
      instruments.add(InstrumentClosePriceRequest.newBuilder().setInstrumentId(instrumentId).build());
    }

    return unaryCall(() -> marketDataBlockingStub.getClosePrices(
      GetClosePricesRequest.newBuilder()
        .addAllInstruments(instruments)
        .build()).getClosePricesList());
  }

  /**
   * Получение (асинхронное) списка последних цен по инструментам
   *
   * @param instrumentIds FIGI-идентификатор / uid инструмента.
   * @return Список последний цен
   */
  @Nonnull
  public CompletableFuture<List<LastPrice>> getLastPrices(@Nonnull Iterable<String> instrumentIds) {
    return Helpers.<GetLastPricesResponse>unaryAsyncCall(
        observer -> marketDataStub.getLastPrices(
          GetLastPricesRequest.newBuilder()
            .addAllInstrumentId(instrumentIds)
            .build(),
          observer))
      .thenApply(GetLastPricesResponse::getLastPricesList);
  }

  /**
   * Получение (асинхронное) информации о стакане
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @param depth        глубина стакана
   * @return
   */
  @Nonnull
  public CompletableFuture<GetOrderBookResponse> getOrderBook(@Nonnull String instrumentId, int depth) {
    return Helpers.unaryAsyncCall(
      observer -> marketDataStub.getOrderBook(
        GetOrderBookRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setDepth(depth)
          .build(),
        observer));
  }

  /**
   * Получение (асинхронное) информации о торговом статусе инструмента
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @return Информация о торговом статусе
   */
  @Nonnull
  public CompletableFuture<GetTradingStatusResponse> getTradingStatus(@Nonnull String instrumentId) {
    return Helpers.unaryAsyncCall(
      observer -> marketDataStub.getTradingStatus(
        GetTradingStatusRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .build(),
        observer));
  }
}
