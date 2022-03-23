package ru.tinkoff.piapi.core;

import com.google.protobuf.Timestamp;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.rules.ExpectedException;
import ru.tinkoff.piapi.contract.v1.AccruedInterest;
import ru.tinkoff.piapi.contract.v1.Bond;
import ru.tinkoff.piapi.contract.v1.BondResponse;
import ru.tinkoff.piapi.contract.v1.BondsResponse;
import ru.tinkoff.piapi.contract.v1.CurrenciesResponse;
import ru.tinkoff.piapi.contract.v1.Currency;
import ru.tinkoff.piapi.contract.v1.CurrencyResponse;
import ru.tinkoff.piapi.contract.v1.Dividend;
import ru.tinkoff.piapi.contract.v1.Etf;
import ru.tinkoff.piapi.contract.v1.EtfResponse;
import ru.tinkoff.piapi.contract.v1.EtfsResponse;
import ru.tinkoff.piapi.contract.v1.Future;
import ru.tinkoff.piapi.contract.v1.FutureResponse;
import ru.tinkoff.piapi.contract.v1.FuturesResponse;
import ru.tinkoff.piapi.contract.v1.GetAccruedInterestsRequest;
import ru.tinkoff.piapi.contract.v1.GetAccruedInterestsResponse;
import ru.tinkoff.piapi.contract.v1.GetDividendsRequest;
import ru.tinkoff.piapi.contract.v1.GetDividendsResponse;
import ru.tinkoff.piapi.contract.v1.GetFuturesMarginRequest;
import ru.tinkoff.piapi.contract.v1.GetFuturesMarginResponse;
import ru.tinkoff.piapi.contract.v1.Instrument;
import ru.tinkoff.piapi.contract.v1.InstrumentIdType;
import ru.tinkoff.piapi.contract.v1.InstrumentRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentResponse;
import ru.tinkoff.piapi.contract.v1.InstrumentStatus;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.contract.v1.ShareResponse;
import ru.tinkoff.piapi.contract.v1.SharesResponse;
import ru.tinkoff.piapi.contract.v1.TradingSchedule;
import ru.tinkoff.piapi.contract.v1.TradingSchedulesRequest;
import ru.tinkoff.piapi.contract.v1.TradingSchedulesResponse;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import java.time.Instant;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToInstant;

public class InstrumentsServiceTest extends GrpcClientTester<InstrumentsService> {

  @Rule
  public ExpectedException futureThrown = ExpectedException.none();

  @Override
  protected InstrumentsService createClient(Channel channel) {
    return new InstrumentsService(
      InstrumentsServiceGrpc.newBlockingStub(channel),
      InstrumentsServiceGrpc.newStub(channel));
  }

  private void assertThrowsApiRuntimeException(String code, Executable executable) {
    var apiRuntimeException = assertThrows(ApiRuntimeException.class, executable);
    assertEquals(code, apiRuntimeException.getCode());
  }

  private void assertThrowsAsyncApiRuntimeException(String code, Executable executable) {
    var throwable = assertThrows(CompletionException.class, executable).getCause();
    assertTrue(throwable instanceof ApiRuntimeException);
    assertEquals(code, ((ApiRuntimeException) throwable).getCode());
  }

  @Nested
  class GetTradingSchedulesTest {

    @Test
    void getAllSchedules_Test() {
      var expected = TradingSchedulesResponse.newBuilder()
        .addExchanges(TradingSchedule.newBuilder().setExchange("MOEX").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void tradingSchedules(TradingSchedulesRequest request,
                                       StreamObserver<TradingSchedulesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = TradingSchedulesRequest.newBuilder()
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();
      var actualSync = service.getTradingSchedulesSync(
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo()));
      var actualAsync = service.getTradingSchedules(
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join();

      assertIterableEquals(expected.getExchangesList(), actualSync);
      assertIterableEquals(expected.getExchangesList(), actualAsync);

      verify(grpcService, times(2)).tradingSchedules(eq(inArg), any());
    }

    @Test
    void getAllSchedules_shouldThrowIfToIsNotAfterFrom_Test() {
      var expected = TradingSchedulesResponse.newBuilder()
        .addExchanges(TradingSchedule.newBuilder().setExchange("MOEX").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void tradingSchedules(TradingSchedulesRequest request,
                                       StreamObserver<TradingSchedulesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var now = Instant.now();
      var nowMinusSecond = now.minusSeconds(1);
      assertThrows(IllegalArgumentException.class, () -> service.getTradingSchedulesSync(now, nowMinusSecond));
      futureThrown.expect(CompletionException.class);
      futureThrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
      assertThrows(IllegalArgumentException.class, () -> service.getTradingSchedules(now, nowMinusSecond));

      verify(grpcService, never()).tradingSchedules(any(), any());
    }

    @Test
    void getOneSchedule_Test() {
      var exchange = "MOEX";
      var expected = TradingSchedulesResponse.newBuilder()
        .addExchanges(TradingSchedule.newBuilder().setExchange(exchange).build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void tradingSchedules(TradingSchedulesRequest request,
                                       StreamObserver<TradingSchedulesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = TradingSchedulesRequest.newBuilder()
        .setExchange(exchange)
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();
      var actualSync = service.getTradingScheduleSync(
        exchange,
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo()));
      var actualAsync = service.getTradingSchedule(
        exchange,
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join();

      assertEquals(expected.getExchangesList().get(0), actualSync);
      assertEquals(expected.getExchangesList().get(0), actualAsync);

      verify(grpcService, times(2)).tradingSchedules(eq(inArg), any());
    }

    @Test
    void getOneSchedule_shouldThrowIfToIsNotAfterFrom_Test() {
      var exchange = "MOEX";
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
        }));
      var service = mkClientBasedOnServer(grpcService);

      var now = Instant.now();
      var nowMinusSecond = now.minusSeconds(1);
      assertThrows(IllegalArgumentException.class, () -> service.getTradingScheduleSync(exchange, now, nowMinusSecond));
      futureThrown.expect(CompletionException.class);
      futureThrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
      assertThrows(IllegalArgumentException.class, () -> service.getTradingSchedule(exchange, now, nowMinusSecond));

      verify(grpcService, never()).tradingSchedules(any(), any());
    }

    @Test
    void getOneSchedule_shouldReturnNoneInCaseOfNotFoundStatus_Test() {
      var exchange = "MOEX";
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void tradingSchedules(TradingSchedulesRequest request,
                                       StreamObserver<TradingSchedulesResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50001")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = TradingSchedulesRequest.newBuilder()
        .setExchange(exchange)
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();
      assertThrowsApiRuntimeException("50001", () -> service.getTradingScheduleSync(
        exchange,
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())));
      assertThrowsAsyncApiRuntimeException("50001", () -> service.getTradingSchedule(
        exchange,
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join());


      verify(grpcService, times(2)).tradingSchedules(eq(inArg), any());
    }

  }

  @Nested
  class GetBondsTest {

    @Test
    void getOneByTicker_Test() {
      var expected = BondResponse.newBuilder()
        .setInstrument(Bond.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bondBy(InstrumentRequest request,
                             StreamObserver<BondResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();
      var actualSync = service.getBondByTickerSync(inArg.getId(), inArg.getClassCode());
      var actualAsync = service.getBondByTicker(inArg.getId(), inArg.getClassCode()).join();

      assertEquals(expected.getInstrument(), actualSync);
      assertEquals(expected.getInstrument(), actualAsync);

      verify(grpcService, times(2)).bondBy(eq(inArg), any());
    }

    @Test
    void getOneByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bondBy(InstrumentRequest request,
                             StreamObserver<BondResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getBondByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getBondByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).bondBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_Test() {
      var expected = BondResponse.newBuilder()
        .setInstrument(Bond.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bondBy(InstrumentRequest request,
                             StreamObserver<BondResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();
      var actualSync = service.getBondByFigiSync(inArg.getId());
      var actualAsync = service.getBondByFigi(inArg.getId()).join();

      assertEquals(expected.getInstrument(), actualSync);
      assertEquals(expected.getInstrument(), actualAsync);

      verify(grpcService, times(2)).bondBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bondBy(InstrumentRequest request,
                             StreamObserver<BondResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getBondByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getBondByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).bondBy(eq(inArg), any());
    }

    @Test
    void getTradable_Test() {
      var expected = BondsResponse.newBuilder()
        .addInstruments(Bond.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bonds(InstrumentsRequest request,
                            StreamObserver<BondsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .build();
      var actualSync = service.getTradableBondsSync();
      var actualAsync = service.getTradableBonds().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).bonds(eq(inArg), any());
    }

    @Test
    void getAll_Test() {
      var expected = BondsResponse.newBuilder()
        .addInstruments(Bond.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bonds(InstrumentsRequest request,
                            StreamObserver<BondsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
        .build();
      var actualSync = service.getAllBondsSync();
      var actualAsync = service.getAllBonds().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).bonds(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_all_Test() {
      var expected = BondsResponse.newBuilder()
        .addInstruments(Bond.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bonds(InstrumentsRequest request,
                            StreamObserver<BondsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_ALL;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getBondsSync(instrumentStatus);
      var actualAsync = service.getBonds(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).bonds(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_base_Test() {
      var expected = BondsResponse.newBuilder()
        .addInstruments(Bond.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bonds(InstrumentsRequest request,
                            StreamObserver<BondsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getBondsSync(instrumentStatus);
      var actualAsync = service.getBonds(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).bonds(eq(inArg), any());
    }

  }

  @Nested
  class GetCurrenciesTest {

    @Test
    void getOneByTicker_Test() {
      var expected = CurrencyResponse.newBuilder()
        .setInstrument(Currency.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencyBy(InstrumentRequest request,
                                 StreamObserver<CurrencyResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();
      var actualSync = service.getCurrencyByTickerSync(inArg.getId(), inArg.getClassCode());
      var actualAsync = service.getCurrencyByTicker(inArg.getId(), inArg.getClassCode()).join();

      assertEquals(expected.getInstrument(), actualSync);
      assertEquals(expected.getInstrument(), actualAsync);

      verify(grpcService, times(2)).currencyBy(eq(inArg), any());
    }

    @Test
    void getOneByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencyBy(InstrumentRequest request,
                                 StreamObserver<CurrencyResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getCurrencyByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getCurrencyByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).currencyBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_Test() {
      var expected = CurrencyResponse.newBuilder()
        .setInstrument(Currency.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencyBy(InstrumentRequest request,
                                 StreamObserver<CurrencyResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();
      var actualSync = service.getCurrencyByFigiSync(inArg.getId());
      var actualAsync = service.getCurrencyByFigi(inArg.getId()).join();

      assertEquals(expected.getInstrument(), actualSync);
      assertEquals(expected.getInstrument(), actualAsync);

      verify(grpcService, times(2)).currencyBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencyBy(InstrumentRequest request,
                                 StreamObserver<CurrencyResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getCurrencyByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getCurrencyByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).currencyBy(eq(inArg), any());
    }

    @Test
    void getTradable_Test() {
      var expected = CurrenciesResponse.newBuilder()
        .addInstruments(Currency.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencies(InstrumentsRequest request,
                                 StreamObserver<CurrenciesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .build();
      var actualSync = service.getTradableCurrenciesSync();
      var actualAsync = service.getTradableCurrencies().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).currencies(eq(inArg), any());
    }

    @Test
    void getAll_Test() {
      var expected = CurrenciesResponse.newBuilder()
        .addInstruments(Currency.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencies(InstrumentsRequest request,
                                 StreamObserver<CurrenciesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
        .build();
      var actualSync = service.getAllCurrenciesSync();
      var actualAsync = service.getAllCurrencies().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).currencies(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_all_Test() {
      var expected = CurrenciesResponse.newBuilder()
        .addInstruments(Currency.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencies(InstrumentsRequest request,
                                 StreamObserver<CurrenciesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_ALL;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getCurrenciesSync(instrumentStatus);
      var actualAsync = service.getCurrencies(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).currencies(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_base_Test() {
      var expected = CurrenciesResponse.newBuilder()
        .addInstruments(Currency.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencies(InstrumentsRequest request,
                                 StreamObserver<CurrenciesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getCurrenciesSync(instrumentStatus);
      var actualAsync = service.getCurrencies(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).currencies(eq(inArg), any());
    }

  }

  @Nested
  class GetEtfsTest {

    @Test
    void getOneByTicker_Test() {
      var expected = EtfResponse.newBuilder()
        .setInstrument(Etf.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfBy(InstrumentRequest request,
                            StreamObserver<EtfResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      Assertions.assertEquals(expected.getInstrument(), service.getEtfByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertEquals(expected.getInstrument(), service.getEtfByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).etfBy(eq(inArg), any());
    }

    @Test
    void getOneByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfBy(InstrumentRequest request,
                            StreamObserver<EtfResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getEtfByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getEtfByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).etfBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_Test() {
      var expected = EtfResponse.newBuilder()
        .setInstrument(Etf.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfBy(InstrumentRequest request,
                            StreamObserver<EtfResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertEquals(expected.getInstrument(), service.getEtfByFigiSync(inArg.getId()));
      assertEquals(expected.getInstrument(), service.getEtfByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).etfBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfBy(InstrumentRequest request,
                            StreamObserver<EtfResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getEtfByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getEtfByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).etfBy(eq(inArg), any());
    }

    @Test
    void getTradable_Test() {
      var expected = EtfsResponse.newBuilder()
        .addInstruments(Etf.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfs(InstrumentsRequest request,
                           StreamObserver<EtfsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .build();
      var actualSync = service.getTradableEtfsSync();
      var actualAsync = service.getTradableEtfs().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).etfs(eq(inArg), any());
    }

    @Test
    void getAll_Test() {
      var expected = EtfsResponse.newBuilder()
        .addInstruments(Etf.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfs(InstrumentsRequest request,
                           StreamObserver<EtfsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
        .build();
      var actualSync = service.getAllEtfsSync();
      var actualAsync = service.getAllEtfs().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).etfs(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_all_test() {
      var expected = EtfsResponse.newBuilder()
        .addInstruments(Etf.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfs(InstrumentsRequest request,
                           StreamObserver<EtfsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_ALL;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getEtfsSync(instrumentStatus);
      var actualAsync = service.getEtfs(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).etfs(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_base_test() {
      var expected = EtfsResponse.newBuilder()
        .addInstruments(Etf.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfs(InstrumentsRequest request,
                           StreamObserver<EtfsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getEtfsSync(instrumentStatus);
      var actualAsync = service.getEtfs(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).etfs(eq(inArg), any());
    }

  }

  @Nested
  class GetFuturesTest {

    @Test
    void getOneByTicker_Test() {
      var expected = FutureResponse.newBuilder()
        .setInstrument(Future.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futureBy(InstrumentRequest request,
                               StreamObserver<FutureResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertEquals(expected.getInstrument(), service.getFutureByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertEquals(expected.getInstrument(), service.getFutureByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).futureBy(eq(inArg), any());
    }

    @Test
    void getOneByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futureBy(InstrumentRequest request,
                               StreamObserver<FutureResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("ticker")
        .setClassCode("MOEX")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getFutureByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getFutureByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).futureBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_Test() {
      var expected = FutureResponse.newBuilder()
        .setInstrument(Future.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futureBy(InstrumentRequest request,
                               StreamObserver<FutureResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertEquals(expected.getInstrument(), service.getFutureByFigiSync(inArg.getId()));
      assertEquals(expected.getInstrument(), service.getFutureByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).futureBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futureBy(InstrumentRequest request,
                               StreamObserver<FutureResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getFutureByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getFutureByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).futureBy(eq(inArg), any());
    }

    @Test
    void getTradable_Test() {
      var expected = FuturesResponse.newBuilder()
        .addInstruments(Future.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futures(InstrumentsRequest request,
                              StreamObserver<FuturesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .build();
      var actualSync = service.getTradableFuturesSync();
      var actualAsync = service.getTradableFutures().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).futures(eq(inArg), any());
    }

    @Test
    void getAll_Test() {
      var expected = FuturesResponse.newBuilder()
        .addInstruments(Future.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futures(InstrumentsRequest request,
                              StreamObserver<FuturesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
        .build();
      var actualSync = service.getAllFuturesSync();
      var actualAsync = service.getAllFutures().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).futures(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_all_Test() {
      var expected = FuturesResponse.newBuilder()
        .addInstruments(Future.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futures(InstrumentsRequest request,
                              StreamObserver<FuturesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_ALL;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getFuturesSync(instrumentStatus);
      var actualAsync = service.getFutures(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).futures(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_base_Test() {
      var expected = FuturesResponse.newBuilder()
        .addInstruments(Future.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futures(InstrumentsRequest request,
                              StreamObserver<FuturesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getFuturesSync(instrumentStatus);
      var actualAsync = service.getFutures(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).futures(eq(inArg), any());
    }

  }

  @Nested
  class GetSharesTest {

    @Test
    void getOneByTicker_Test() {
      var expected = ShareResponse.newBuilder()
        .setInstrument(Share.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void shareBy(InstrumentRequest request,
                              StreamObserver<ShareResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertEquals(expected.getInstrument(), service.getShareByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertEquals(expected.getInstrument(), service.getShareByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getOneByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void shareBy(InstrumentRequest request,
                              StreamObserver<ShareResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("ticker")
        .setClassCode("MOEX")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getShareByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getShareByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_Test() {
      var expected = ShareResponse.newBuilder()
        .setInstrument(Share.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void shareBy(InstrumentRequest request,
                              StreamObserver<ShareResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertEquals(expected.getInstrument(), service.getShareByFigiSync(inArg.getId()));
      assertEquals(expected.getInstrument(), service.getShareByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void shareBy(InstrumentRequest request,
                              StreamObserver<ShareResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getShareByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getShareByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getTradable_Test() {
      var expected = SharesResponse.newBuilder()
        .addInstruments(Share.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void shares(InstrumentsRequest request,
                             StreamObserver<SharesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .build();
      var actualSync = service.getTradableSharesSync();
      var actualAsync = service.getTradableShares().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).shares(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_all_Test() {
      var expected = SharesResponse.newBuilder()
        .addInstruments(Share.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void shares(InstrumentsRequest request,
                             StreamObserver<SharesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_ALL;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getSharesSync(instrumentStatus);
      var actualAsync = service.getShares(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).shares(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_base_Test() {
      var expected = SharesResponse.newBuilder()
        .addInstruments(Share.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void shares(InstrumentsRequest request,
                             StreamObserver<SharesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getSharesSync(instrumentStatus);
      var actualAsync = service.getShares(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).shares(eq(inArg), any());
    }

    @Test
    void getAll_Test() {
      var expected = SharesResponse.newBuilder()
        .addInstruments(Share.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void shares(InstrumentsRequest request,
                             StreamObserver<SharesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
        .build();
      var actualSync = service.getAllSharesSync();
      var actualAsync = service.getAllShares().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).shares(eq(inArg), any());
    }

  }

  @Nested
  class GetAccruedInterestsTest {

    @Test
    void get_Test() {
      var expected = GetAccruedInterestsResponse.newBuilder()
        .addAccruedInterests(
          AccruedInterest.newBuilder().setValuePercent(
            Quotation.newBuilder().setUnits(1).build()).build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getAccruedInterests(GetAccruedInterestsRequest request,
                                          StreamObserver<GetAccruedInterestsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetAccruedInterestsRequest.newBuilder()
        .setFigi("figi")
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();

      assertEquals(expected.getAccruedInterestsList(), service.getAccruedInterestsSync(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())));
      assertEquals(expected.getAccruedInterestsList(), service.getAccruedInterests(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join());

      verify(grpcService, times(2)).getAccruedInterests(eq(inArg), any());
    }

    @Test
    void get_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getAccruedInterests(GetAccruedInterestsRequest request,
                                          StreamObserver<GetAccruedInterestsResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetAccruedInterestsRequest.newBuilder()
        .setFigi("figi")
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getAccruedInterestsSync(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getAccruedInterests(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join());

      verify(grpcService, times(2)).getAccruedInterests(eq(inArg), any());
    }

    @Test
    void get_shouldThrowIfToIsNotAfterFrom_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
        }));
      var service = mkClientBasedOnServer(grpcService);

      var now = Instant.now();
      var nowMinusSecond = now.minusSeconds(1);
      assertThrows(IllegalArgumentException.class, () -> service.getAccruedInterestsSync(
        "figi",
        now,
        nowMinusSecond));
      futureThrown.expect(CompletionException.class);
      futureThrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
      assertThrows(IllegalArgumentException.class, () -> service.getAccruedInterests("figi", now, nowMinusSecond));

      verify(grpcService, never()).getAccruedInterests(any(), any());
    }

  }

  @Nested
  class GetFuturesMarginTest {

    @Test
    void get_Test() {
      var expected = GetFuturesMarginResponse.newBuilder()
        .setInitialMarginOnBuy(MoneyValue.newBuilder().setCurrency("USD").setUnits(1).build())
        .setInitialMarginOnSell(MoneyValue.newBuilder().setCurrency("USD").setUnits(2).build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getFuturesMargin(GetFuturesMarginRequest request,
                                       StreamObserver<GetFuturesMarginResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetFuturesMarginRequest.newBuilder()
        .setFigi("figi")
        .build();

      assertEquals(expected, service.getFuturesMarginSync(inArg.getFigi()));
      assertEquals(expected, service.getFuturesMargin(inArg.getFigi()).join());

      verify(grpcService, times(2)).getFuturesMargin(eq(inArg), any());
    }

    @Test
    void get_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getFuturesMargin(GetFuturesMarginRequest request,
                                       StreamObserver<GetFuturesMarginResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetFuturesMarginRequest.newBuilder()
        .setFigi("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getFuturesMarginSync(inArg.getFigi()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getFuturesMargin(inArg.getFigi()).join());

      verify(grpcService, times(2)).getFuturesMargin(eq(inArg), any());
    }

  }

  @Nested
  class GetInstrumentTest {

    @Test
    void getByTicker_Test() {
      var expected = InstrumentResponse.newBuilder()
        .setInstrument(Instrument.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getInstrumentBy(InstrumentRequest request,
                                      StreamObserver<InstrumentResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertEquals(expected.getInstrument(), service.getInstrumentByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertEquals(expected.getInstrument(), service.getInstrumentByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).getInstrumentBy(eq(inArg), any());
    }

    @Test
    void getByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getInstrumentBy(InstrumentRequest request,
                                      StreamObserver<InstrumentResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getInstrumentByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getInstrumentByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).getInstrumentBy(eq(inArg), any());
    }

    @Test
    void getByFigi_Test() {
      var expected = InstrumentResponse.newBuilder()
        .setInstrument(Instrument.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getInstrumentBy(InstrumentRequest request,
                                      StreamObserver<InstrumentResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertEquals(expected.getInstrument(), service.getInstrumentByFigiSync(inArg.getId()));
      assertEquals(expected.getInstrument(), service.getInstrumentByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).getInstrumentBy(eq(inArg), any());
    }

    @Test
    void getByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getInstrumentBy(InstrumentRequest request,
                                      StreamObserver<InstrumentResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getInstrumentByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getInstrumentByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).getInstrumentBy(eq(inArg), any());
    }

  }

  @Nested
  class GetDividendsTest {

    @Test
    void get_Test() {
      var expected = GetDividendsResponse.newBuilder()
        .addDividends(Dividend.newBuilder().setDividendType("Regular Cash").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getDividends(GetDividendsRequest request,
                                   StreamObserver<GetDividendsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetDividendsRequest.newBuilder()
        .setFigi("figi")
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();

      assertEquals(expected.getDividendsList(),  service.getDividendsSync(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())));
      assertEquals(expected.getDividendsList(), service.getDividends(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join());

      verify(grpcService, times(2)).getDividends(eq(inArg), any());
    }

    @Test
    void get_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getDividends(GetDividendsRequest request,
                                   StreamObserver<GetDividendsResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetDividendsRequest.newBuilder()
        .setFigi("figi")
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getDividendsSync(inArg.getFigi(), timestampToInstant(inArg.getFrom()), timestampToInstant(inArg.getTo())));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getDividends(inArg.getFigi(), timestampToInstant(inArg.getFrom()), timestampToInstant(inArg.getTo())).join());

      verify(grpcService, times(2)).getDividends(eq(inArg), any());
    }

    @Test
    void get_shouldThrowIfToIsNotAfterFrom_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
        }));
      var service = mkClientBasedOnServer(grpcService);

      var now = Instant.now();
      var nowMinusSecond = now.minusSeconds(1);
      assertThrows(IllegalArgumentException.class, () -> service.getDividendsSync(
        "figi",
        now,
        nowMinusSecond));
      futureThrown.expect(CompletionException.class);
      futureThrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
      assertThrows(IllegalArgumentException.class, () -> service.getDividends("figi", now, nowMinusSecond));

      verify(grpcService, never()).getDividends(any(), any());
    }
  }
}
