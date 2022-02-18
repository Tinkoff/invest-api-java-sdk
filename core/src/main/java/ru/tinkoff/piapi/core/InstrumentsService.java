package ru.tinkoff.piapi.core;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Сервис предоставления справочной информации о ценных бумагах.
 * <p>
 * Подробности в <a href="https://tinkoff.github.io/investAPI/head-instruments/">документации</a>.
 */
public class InstrumentsService {

  private static final String TO_IS_NOT_AFTER_FROM_MESSAGE = "Окончание периода не может быть раньше начала.";

  private final InstrumentsServiceGrpc.InstrumentsServiceBlockingStub instrumentsBlockingStub;
  private final InstrumentsServiceGrpc.InstrumentsServiceStub instrumentsStub;

  InstrumentsService(
    @Nonnull InstrumentsServiceGrpc.InstrumentsServiceBlockingStub instrumentsBlockingStub,
    @Nonnull InstrumentsServiceGrpc.InstrumentsServiceStub instrumentsStub) {
    this.instrumentsBlockingStub = instrumentsBlockingStub;
    this.instrumentsStub = instrumentsStub;
  }

  private static boolean areFromAndToValid(Instant from, Instant to) {
    return from.isBefore(to);
  }

  private static <R> Optional<R> wrapPossibleNotFoundWithOptional(R response, Throwable ex) {
    if (response != null) {
      return Optional.of(response);
    } else {
      if (ex instanceof StatusRuntimeException && ((StatusRuntimeException) ex).getStatus() == Status.NOT_FOUND) {
        return Optional.empty();
      } else {
        if (ex instanceof RuntimeException) {
          throw (RuntimeException) ex;
        } else {
          throw new RuntimeException(ex);
        }
      }
    }
  }

  /**
   * Получение (синхронное) расписания торгов <em>всех</em> торговых площадок.
   * <p>
   * Начало периода не должно быть меньше полуночи текущих суток по Московскому времени.
   *
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Окончание периода по часовому поясу UTC.
   * @return Расписания торгов.
   */
  @Nonnull
  public List<TradingSchedule> getTradingSchedulesSync(
    @Nonnull Instant from,
    @Nonnull Instant to) {
    if (areFromAndToValid(from, to)) {
      return instrumentsBlockingStub.tradingSchedules(
          TradingSchedulesRequest.newBuilder()
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build())
        .getExchangesList();
    } else {
      throw new IllegalArgumentException(TO_IS_NOT_AFTER_FROM_MESSAGE);
    }
  }

  /**
   * Получение (синхронное) расписания торгов торговой площадки.
   * <p>
   * Начало периода не должно быть меньше полуночи текущих суток по Московскому времени.
   *
   * @param exchange Наименование биржи или расчетного календаря.
   * @param from     Начало периода по часовому поясу UTC.
   * @param to       Окончание периода по часовому поясу UTC.
   * @return Расписание торгов площадки (если таковая существует).
   */
  @Nonnull
  public Optional<TradingSchedule> getTradingScheduleSync(
    @Nonnull String exchange,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    if (areFromAndToValid(from, to)) {
      try {
        return Optional.of(
          instrumentsBlockingStub.tradingSchedules(
              TradingSchedulesRequest.newBuilder()
                .setExchange(exchange)
                .setFrom(DateUtils.instantToTimestamp(from))
                .setTo(DateUtils.instantToTimestamp(to))
                .build())
            .getExchangesList()
            .get(0));
      } catch (StatusRuntimeException ex) {
        if (ex.getStatus() == Status.NOT_FOUND) {
          return Optional.empty();
        } else {
          throw ex;
        }
      }
    } else {
      throw new IllegalArgumentException(TO_IS_NOT_AFTER_FROM_MESSAGE);
    }
  }

  /**
   * Получение (синхронное) облигации по тикеру и бирже.
   *
   * @param ticker    Тикер облигации.
   * @param classCode Биржевой класс-код.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public Optional<Bond> getBondByTickerSync(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return getInstrumentByTickerSync(
      ticker,
      classCode,
      request -> instrumentsBlockingStub.bondBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) облигации по FIGI.
   *
   * @param figi FIGI облигации.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public Optional<Bond> getBondByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(
      figi,
      request -> instrumentsBlockingStub.bondBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) списка облигаций доступных для торговли через Tinkoff Invest API.
   *
   * @return Список облигаций.
   */
  @Nonnull
  public List<Bond> getTradableBondsSync() {
    return instrumentsBlockingStub.bonds(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
          .build())
      .getInstrumentsList();
  }

  /**
   * Получение (синхронное) списка всех облигаций доступных в Тинькофф Инвестиции.
   *
   * @return Список облигаций.
   */
  @Nonnull
  public List<Bond> getAllBondsSync() {
    return instrumentsBlockingStub.bonds(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
          .build())
      .getInstrumentsList();
  }

  /**
   * Получение (синхронное) валюты по тикеру и бирже.
   *
   * @param ticker    Тикер валюты.
   * @param classCode Биржевой класс-код.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public Optional<Currency> getCurrencyByTickerSync(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return getInstrumentByTickerSync(
      ticker,
      classCode,
      request -> instrumentsBlockingStub.currencyBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) валюты по FIGI.
   *
   * @param figi FIGI валюты.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public Optional<Currency> getCurrencyByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(
      figi,
      request -> instrumentsBlockingStub.currencyBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) списка валют доступных для торговли через Tinkoff Invest API.
   *
   * @return Список валют.
   */
  @Nonnull
  public List<Currency> getTradableCurrenciesSync() {
    return instrumentsBlockingStub.currencies(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
          .build())
      .getInstrumentsList();
  }

  /**
   * Получение (синхронное) списка всех вслют доступных в Тинькофф Инвестиции.
   *
   * @return Список валют.
   */
  @Nonnull
  public List<Currency> getAllCurrenciesSync() {
    return instrumentsBlockingStub.currencies(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
          .build())
      .getInstrumentsList();
  }

  /**
   * Получение (синхронное) фонда по тикеру и бирже.
   *
   * @param ticker    Тикер фонда.
   * @param classCode Биржевой класс-код.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public Optional<Etf> getEtfByTickerSync(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return getInstrumentByTickerSync(
      ticker,
      classCode,
      request -> instrumentsBlockingStub.etfBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) фонда по FIGI.
   *
   * @param figi FIGI фонда.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public Optional<Etf> getEtfByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(
      figi,
      request -> instrumentsBlockingStub.etfBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) списка фондов доступных для торговли через Tinkoff Invest API.
   *
   * @return Список фондов.
   */
  @Nonnull
  public List<Etf> getTradableEtfsSync() {
    return instrumentsBlockingStub.etfs(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
          .build())
      .getInstrumentsList();
  }

  /**
   * Получение (синхронное) списка всех фондов доступных в Тинькофф Инвестиции.
   *
   * @return Список фондов.
   */
  @Nonnull
  public List<Etf> getAllEtfsSync() {
    return instrumentsBlockingStub.etfs(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
          .build())
      .getInstrumentsList();
  }

  /**
   * Получение (синхронное) фьючерса по тикеру и бирже.
   *
   * @param ticker    Тикер фьючерса.
   * @param classCode Биржевой класс-код.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public Optional<Future> getFutureByTickerSync(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return getInstrumentByTickerSync(
      ticker,
      classCode,
      request -> instrumentsBlockingStub.futureBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) фьючерса по FIGI.
   *
   * @param figi FIGI фьючерса.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public Optional<Future> getFutureByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(
      figi,
      request -> instrumentsBlockingStub.futureBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) списка фьючерсов доступных для торговли через Tinkoff Invest API.
   *
   * @return Список фьючерсов.
   */
  @Nonnull
  public List<Future> getTradableFuturesSync() {
    return instrumentsBlockingStub.futures(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
          .build())
      .getInstrumentsList();
  }

  /**
   * Получение (синхронное) списка всех фьючерсов доступных в Тинькофф Инвестиции.
   *
   * @return Список фондов.
   */
  @Nonnull
  public List<Future> getAllFuturesSync() {
    return instrumentsBlockingStub.futures(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
          .build())
      .getInstrumentsList();
  }

  /**
   * Получение (синхронное) акции по тикеру и бирже.
   *
   * @param ticker    Тикер акции.
   * @param classCode Биржевой класс-код.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public Optional<Share> getShareByTickerSync(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return getInstrumentByTickerSync(
      ticker,
      classCode,
      request -> instrumentsBlockingStub.shareBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) акции по FIGI.
   *
   * @param figi FIGI акции.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public Optional<Share> getShareByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(
      figi,
      request -> instrumentsBlockingStub.shareBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) списка акций доступных для торговли через Tinkoff Invest API.
   *
   * @return Список акций.
   */
  @Nonnull
  public List<Share> getTradableSharesSync() {
    return instrumentsBlockingStub.shares(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
          .build())
      .getInstrumentsList();
  }

  /**
   * Получение (синхронное) списка всех акций доступных в Тинькофф Инвестиции.
   *
   * @return Список акций.
   */
  @Nonnull
  public List<Share> getAllSharesSync() {
    return instrumentsBlockingStub.shares(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
          .build())
      .getInstrumentsList();
  }

  /**
   * Получение (синхронное) накопленного купонного дохода по облигации.
   *
   * @param figi FIGI облигации.
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Конец периода по часовому поясу UTC.
   * @return НКД по облигации (если есть).
   */
  @Nonnull
  public Optional<List<AccruedInterest>> getAccruedInterestsSync(
    @Nonnull String figi,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    if (areFromAndToValid(from, to)) {
      try {
        return Optional.of(
          instrumentsBlockingStub.getAccruedInterests(
              GetAccruedInterestsRequest.newBuilder()
                .setFigi(figi)
                .setFrom(DateUtils.instantToTimestamp(from))
                .setTo(DateUtils.instantToTimestamp(to))
                .build())
            .getAccruedInterestsList());
      } catch (StatusRuntimeException ex) {
        if (ex.getStatus() == Status.NOT_FOUND) {
          return Optional.empty();
        } else {
          throw ex;
        }
      }
    } else {
      throw new IllegalArgumentException(TO_IS_NOT_AFTER_FROM_MESSAGE);
    }
  }

  /**
   * Получение (синхронное) размера гарантийного обеспечения по фьючерсам.
   *
   * @param figi FIGI фьючерса.
   * @return Размер гарантийного обеспечения по фьючерсу (если есть).
   */
  @Nonnull
  public Optional<GetFuturesMarginResponse> getFuturesMarginSync(@Nonnull String figi) {
    try {
      return Optional.of(
        instrumentsBlockingStub.getFuturesMargin(
          GetFuturesMarginRequest.newBuilder()
            .setFigi(figi)
            .build()));
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus() == Status.NOT_FOUND) {
        return Optional.empty();
      } else {
        throw ex;
      }
    }
  }

  /**
   * Получение (синхронное) основной информации об инструменте.
   *
   * @param ticker    Тикер инфструмента.
   * @param classCode Биржевой класс-код.
   * @return Основная информация об инструменте (если есть).
   */
  @Nonnull
  public Optional<Instrument> getInstrumentByTickerSync(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return getInstrumentByTickerSync(
      ticker,
      classCode,
      request -> instrumentsBlockingStub.getInstrumentBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) основной информации об инструменте.
   *
   * @param figi FIGI инфструмента.
   * @return Основная информация об инструменте (если есть).
   */
  @Nonnull
  public Optional<Instrument> getInstrumentByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(
      figi,
      request -> instrumentsBlockingStub.getInstrumentBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) событий выплаты дивидендов по инструменту.
   *
   * @param figi FIGI инфструмента.
   * @param from Начало периода по часовому поясу UTC.
   * @param to Конец периода по часовому поясу UTC.
   * @return События выплаты дивидендов по инструменту (если есть).
   */
  @Nonnull
  public Optional<List<Dividend>> getDividendsSync(
    @Nonnull String figi,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    if (areFromAndToValid(from, to)) {
      try {
        return Optional.of(
          instrumentsBlockingStub.getDividends(
              GetDividendsRequest.newBuilder()
                .setFigi(figi)
                .setFrom(DateUtils.instantToTimestamp(from))
                .setTo(DateUtils.instantToTimestamp(to))
                .build())
            .getDividendsList());
      } catch (StatusRuntimeException ex) {
        if (ex.getStatus() == Status.NOT_FOUND) {
          return Optional.empty();
        } else {
          throw ex;
        }
      }
    } else {
      throw new IllegalArgumentException(TO_IS_NOT_AFTER_FROM_MESSAGE);
    }
  }

  /**
   * Получение (асинхронное) расписания торгов <em>всех</em> торговых площадок.
   * <p>
   * Начало периода не должно быть меньше полуночи текущих суток по Московскому времени.
   *
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Окончание периода по часовому поясу UTC.
   * @return Расписания торгов.
   */
  @Nonnull
  public CompletableFuture<List<TradingSchedule>> getTradingSchedules(
    @Nonnull Instant from,
    @Nonnull Instant to) {
    if (areFromAndToValid(from, to)) {
      return Helpers.<TradingSchedulesResponse>wrapWithFuture(
          observer -> instrumentsStub.tradingSchedules(
            TradingSchedulesRequest.newBuilder()
              .setFrom(DateUtils.instantToTimestamp(from))
              .setTo(DateUtils.instantToTimestamp(to))
              .build(),
            observer))
        .thenApply(TradingSchedulesResponse::getExchangesList);
    } else {
      return CompletableFuture.failedFuture(new IllegalArgumentException(TO_IS_NOT_AFTER_FROM_MESSAGE));
    }
  }

  /**
   * Получение (асинхронное) расписания торгов торговой площадки.
   * <p>
   * Начало периода не должно быть меньше полуночи текущих суток по Московскому времени.
   *
   * @param exchange Наименование биржи или расчетного календаря.
   * @param from     Начало периода по часовому поясу UTC.
   * @param to       Окончание периода по часовому поясу UTC.
   * @return Расписание торгов площадки (если таковая существует).
   */
  @Nonnull
  public CompletableFuture<Optional<TradingSchedule>> getTradingSchedule(
    @Nonnull String exchange,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    if (areFromAndToValid(from, to)) {
      return Helpers.<TradingSchedulesResponse>wrapWithFuture(
          observer -> instrumentsStub.tradingSchedules(
            TradingSchedulesRequest.newBuilder()
              .setExchange(exchange)
              .setFrom(DateUtils.instantToTimestamp(from))
              .setTo(DateUtils.instantToTimestamp(to))
              .build(),
            observer))
        .handle(InstrumentsService::wrapPossibleNotFoundWithOptional)
        .thenApply(x -> x.map(v -> v.getExchangesList().get(0)));
    } else {
      return CompletableFuture.failedFuture(new IllegalArgumentException(TO_IS_NOT_AFTER_FROM_MESSAGE));
    }
  }

  /**
   * Получение (асинхронное) облигации по тикеру и бирже.
   *
   * @param ticker    Тикер облигации.
   * @param classCode Биржевой класс-код.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Bond>> getBondByTicker(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return this.getInstrumentByTicker(
      ticker,
      classCode,
      instrumentsStub::bondBy,
      BondResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) облигации по FIGI.
   *
   * @param figi FIGI облигации.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Bond>> getBondByFigi(@Nonnull String figi) {
    return this.getInstrumentByFigi(
      figi,
      instrumentsStub::bondBy,
      BondResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) списка облигаций доступных для торговли через Tinkoff Invest API.
   *
   * @return Список облигаций.
   */
  @Nonnull
  public CompletableFuture<List<Bond>> getTradableBonds() {
    return Helpers.<BondsResponse>wrapWithFuture(
        observer -> instrumentsStub.bonds(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
            .build(),
          observer))
      .thenApply(BondsResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка всех облигаций доступных в Тинькофф Инвестиции.
   *
   * @return Список облигаций.
   */
  @Nonnull
  public CompletableFuture<List<Bond>> getAllBonds() {
    return Helpers.<BondsResponse>wrapWithFuture(
        observer -> instrumentsStub.bonds(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
            .build(),
          observer))
      .thenApply(BondsResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) валюты по тикеру и бирже.
   *
   * @param ticker    Тикер валюты.
   * @param classCode Биржевой класс-код.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Currency>> getCurrencyByTicker(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return this.getInstrumentByTicker(
      ticker,
      classCode,
      instrumentsStub::currencyBy,
      CurrencyResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) валюты по FIGI.
   *
   * @param figi FIGI валюты.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Currency>> getCurrencyByFigi(@Nonnull String figi) {
    return this.getInstrumentByFigi(
      figi,
      instrumentsStub::currencyBy,
      CurrencyResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) списка валют доступных для торговли через Tinkoff Invest API.
   *
   * @return Список валют.
   */
  @Nonnull
  public CompletableFuture<List<Currency>> getTradableCurrencies() {
    return Helpers.<CurrenciesResponse>wrapWithFuture(
        observer -> instrumentsStub.currencies(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
            .build(),
          observer))
      .thenApply(CurrenciesResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка всех вслют доступных в Тинькофф Инвестиции.
   *
   * @return Список валют.
   */
  @Nonnull
  public CompletableFuture<List<Currency>> getAllCurrencies() {
    return Helpers.<CurrenciesResponse>wrapWithFuture(
        observer -> instrumentsStub.currencies(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
            .build(),
          observer))
      .thenApply(CurrenciesResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) фонда по тикеру и бирже.
   *
   * @param ticker    Тикер фонда.
   * @param classCode Биржевой класс-код.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Etf>> getEtfByTicker(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return this.getInstrumentByTicker(
      ticker,
      classCode,
      instrumentsStub::etfBy,
      EtfResponse::getInstrument);
  }

  /**
   * Получение (синхронное) фонда по FIGI.
   *
   * @param figi FIGI фонда.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Etf>> getEtfByFigi(@Nonnull String figi) {
    return this.getInstrumentByFigi(
      figi,
      instrumentsStub::etfBy,
      EtfResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) списка фондов доступных для торговли через Tinkoff Invest API.
   *
   * @return Список фондов.
   */
  @Nonnull
  public CompletableFuture<List<Etf>> getTradableEtfs() {
    return Helpers.<EtfsResponse>wrapWithFuture(
        observer -> instrumentsStub.etfs(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
            .build(),
          observer))
      .thenApply(EtfsResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка всех фондов доступных в Тинькофф Инвестиции.
   *
   * @return Список фондов.
   */
  @Nonnull
  public CompletableFuture<List<Etf>> getAllEtfs() {
    return Helpers.<EtfsResponse>wrapWithFuture(
        observer -> instrumentsStub.etfs(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
            .build(),
          observer))
      .thenApply(EtfsResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) фьючерса по тикеру и бирже.
   *
   * @param ticker    Тикер фьючерса.
   * @param classCode Биржевой класс-код.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Future>> getFutureByTicker(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return this.getInstrumentByTicker(
      ticker,
      classCode,
      instrumentsStub::futureBy,
      FutureResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) фьючерса по FIGI.
   *
   * @param figi FIGI фьючерса.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Future>> getFutureByFigi(@Nonnull String figi) {
    return this.getInstrumentByFigi(
      figi,
      instrumentsStub::futureBy,
      FutureResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) списка фьючерсов доступных для торговли через Tinkoff Invest API.
   *
   * @return Список фьючерсов.
   */
  @Nonnull
  public CompletableFuture<List<Future>> getTradableFutures() {
    return Helpers.<FuturesResponse>wrapWithFuture(
        observer -> instrumentsStub.futures(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
            .build(),
          observer))
      .thenApply(FuturesResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка всех фьючерсов доступных в Тинькофф Инвестиции.
   *
   * @return Список фондов.
   */
  @Nonnull
  public CompletableFuture<List<Future>> getAllFutures() {
    return Helpers.<FuturesResponse>wrapWithFuture(
        observer -> instrumentsStub.futures(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
            .build(),
          observer))
      .thenApply(FuturesResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) акции по тикеру и бирже.
   *
   * @param ticker    Тикер акции.
   * @param classCode Биржевой класс-код.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Share>> getShareByTicker(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return this.getInstrumentByTicker(
      ticker,
      classCode,
      instrumentsStub::shareBy,
      ShareResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) акции по FIGI.
   *
   * @param figi FIGI акции.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Share>> getShareByFigi(@Nonnull String figi) {
    return this.getInstrumentByFigi(
      figi,
      instrumentsStub::shareBy,
      ShareResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) списка акций доступных для торговли через Tinkoff Invest API.
   *
   * @return Список акций.
   */
  @Nonnull
  public CompletableFuture<List<Share>> getTradableShares() {
    return Helpers.<SharesResponse>wrapWithFuture(
        observer -> instrumentsStub.shares(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
            .build(),
          observer))
      .thenApply(SharesResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка всех акций доступных в Тинькофф Инвестиции.
   *
   * @return Список акций.
   */
  @Nonnull
  public CompletableFuture<List<Share>> getAllShares() {
    return Helpers.<SharesResponse>wrapWithFuture(
        observer -> instrumentsStub.shares(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
            .build(),
          observer))
      .thenApply(SharesResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) накопленного купонного дохода по облигации.
   *
   * @param figi FIGI облигации.
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Конец периода по часовому поясу UTC.
   * @return НКД по облигации (если есть).
   */
  @Nonnull
  public CompletableFuture<Optional<List<AccruedInterest>>> getAccruedInterests(
    @Nonnull String figi,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    if (areFromAndToValid(from, to)) {
      return Helpers.<GetAccruedInterestsResponse>wrapWithFuture(
          observer -> instrumentsStub.getAccruedInterests(
            GetAccruedInterestsRequest.newBuilder()
              .setFigi(figi)
              .setFrom(DateUtils.instantToTimestamp(from))
              .setTo(DateUtils.instantToTimestamp(to))
              .build(),
            observer))
        .handle(InstrumentsService::wrapPossibleNotFoundWithOptional)
        .thenApply(x -> x.map(GetAccruedInterestsResponse::getAccruedInterestsList));
    } else {
      return CompletableFuture.failedFuture(new IllegalArgumentException(TO_IS_NOT_AFTER_FROM_MESSAGE));
    }
  }

  /**
   * Получение (асинхронное) размера гарантийного обеспечения по фьючерсам.
   *
   * @param figi FIGI фьючерса.
   * @return Размер гарантийного обеспечения по фьючерсу (если есть).
   */
  @Nonnull
  public CompletableFuture<Optional<GetFuturesMarginResponse>> getFuturesMargin(@Nonnull String figi) {
    return Helpers.<GetFuturesMarginResponse>wrapWithFuture(
        observer -> instrumentsStub.getFuturesMargin(
          GetFuturesMarginRequest.newBuilder()
            .setFigi(figi)
            .build(),
          observer))
      .handle(InstrumentsService::wrapPossibleNotFoundWithOptional);
  }

  /**
   * Получение (асинхронное) основной информации об инструменте.
   *
   * @param ticker    Тикер инфструмента.
   * @param classCode Биржевой класс-код.
   * @return Основная информация об инструменте (если есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Instrument>> getInstrumentByTicker(
    @Nonnull String ticker,
    @Nonnull String classCode) {
    return getInstrumentByTicker(
      ticker,
      classCode,
      instrumentsStub::getInstrumentBy,
      InstrumentResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) основной информации об инструменте.
   *
   * @param figi FIGI инфструмента.
   * @return Основная информация об инструменте (если есть).
   */
  @Nonnull
  public CompletableFuture<Optional<Instrument>> getInstrumentByFigi(@Nonnull String figi) {
    return getInstrumentByFigi(
      figi,
      instrumentsStub::getInstrumentBy,
      InstrumentResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) событий выплаты дивидендов по инструменту.
   *
   * @param figi FIGI инфструмента.
   * @param from Начало периода по часовому поясу UTC.
   * @param to Конец периода по часовому поясу UTC.
   * @return События выплаты дивидендов по инструменту (если есть).
   */
  @Nonnull
  public CompletableFuture<Optional<List<Dividend>>> getDividends(
    @Nonnull String figi,
    @Nonnull Instant from,
    @Nonnull Instant to) {
    if (areFromAndToValid(from, to)) {
      return Helpers.<GetDividendsResponse>wrapWithFuture(
          observer -> instrumentsStub.getDividends(
            GetDividendsRequest.newBuilder()
              .setFigi(figi)
              .setFrom(DateUtils.instantToTimestamp(from))
              .setTo(DateUtils.instantToTimestamp(to))
              .build(),
            observer))
        .handle(InstrumentsService::wrapPossibleNotFoundWithOptional)
        .thenApply(x -> x.map(GetDividendsResponse::getDividendsList));
    } else {
      return CompletableFuture.failedFuture(new IllegalArgumentException(TO_IS_NOT_AFTER_FROM_MESSAGE));
    }
  }

  private <T> Optional<T> getInstrumentByTickerSync(
    @Nonnull String ticker,
    @Nonnull String classCode,
    Function<InstrumentRequest, T> getter) {
    try {
      return Optional.of(
        getter.apply(
          InstrumentRequest.newBuilder()
            .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
            .setId(ticker)
            .setClassCode(classCode)
            .build()));
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus() == Status.NOT_FOUND) {
        return Optional.empty();
      } else {
        throw ex;
      }
    }
  }

  private <T> Optional<T> getInstrumentByFigiSync(
    @Nonnull String figi,
    Function<InstrumentRequest, T> getter) {
    try {
      return Optional.of(
        getter.apply(
          InstrumentRequest.newBuilder()
            .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
            .setId(figi)
            .build()));
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus() == Status.NOT_FOUND) {
        return Optional.empty();
      } else {
        throw ex;
      }
    }
  }

  private <T, R> CompletableFuture<Optional<T>> getInstrumentByTicker(
    @Nonnull String ticker,
    @Nonnull String classCode,
    BiConsumer<InstrumentRequest, StreamObserver<R>> call,
    Function<R, T> extractor) {
    return Helpers.<R>wrapWithFuture(
        observer -> call.accept(
          InstrumentRequest.newBuilder()
            .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
            .setId(ticker)
            .setClassCode(classCode)
            .build(),
          observer))
      .handle(InstrumentsService::wrapPossibleNotFoundWithOptional)
      .thenApply(x -> x.map(extractor));
  }

  private <T, R> CompletableFuture<Optional<T>> getInstrumentByFigi(
    @Nonnull String figi,
    BiConsumer<InstrumentRequest, StreamObserver<R>> call,
    Function<R, T> extractor) {
    return Helpers.<R>wrapWithFuture(
        observer -> call.accept(
          InstrumentRequest.newBuilder()
            .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
            .setId(figi)
            .build(),
          observer))
      .handle(InstrumentsService::wrapPossibleNotFoundWithOptional)
      .thenApply(x -> x.map(extractor));
  }

}
