package ru.tinkoff.piapi.core;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.models.Positions;
import ru.tinkoff.piapi.core.models.WithdrawLimits;
import ru.tinkoff.piapi.core.utils.DateUtils;

import java.time.Instant;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class OperationsServiceTest extends GrpcClientTester<OperationsService> {

  @Rule
  public ExpectedException futureThrown = ExpectedException.none();

  @Override
  protected OperationsService createClient(Channel channel) {
    return new OperationsService(
      OperationsServiceGrpc.newBlockingStub(channel),
      OperationsServiceGrpc.newStub(channel));
  }

  @Test
  void getPositions_Test() {
    var accountId = "accountId";
    var expected = PositionsResponse.newBuilder()
      .setLimitsLoadingInProgress(true)
      .addBlocked(MoneyValue.newBuilder().setUnits(10).setCurrency("RUB").build())
      .addMoney(MoneyValue.newBuilder().setUnits(100).setCurrency("RUB").build())
      .build();
    var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
      new OperationsServiceGrpc.OperationsServiceImplBase() {
        @Override
        public void getPositions(PositionsRequest request,
                                        StreamObserver<PositionsResponse> responseObserver) {
          responseObserver.onNext(expected);
          responseObserver.onCompleted();
        }
      }));
    var service = mkClientBasedOnServer(grpcService);

    var actualSync = service.getPositionsSync(accountId);
    var actualAsync = service.getPositions(accountId).join();

    assertEquals(Positions.fromResponse(expected), actualSync);
    assertEquals(Positions.fromResponse(expected), actualAsync);

    var inArg = PositionsRequest.newBuilder()
      .setAccountId(accountId)
      .build();
    verify(grpcService, times(2)).getPositions(eq(inArg), any());
  }

  @Nested
  class GetOperationsTest {
    @Test
    void getAllOperations_Test() {
      var accountId = "accountId";
      var someMoment = Instant.now();
      var expected = OperationsResponse.newBuilder()
        .addOperations(Operation.newBuilder().setId("operationId").build())
        .build();
      var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
        new OperationsServiceGrpc.OperationsServiceImplBase() {
          @Override
          public void getOperations(OperationsRequest request,
                                    StreamObserver<OperationsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync =
        service.getAllOperationsSync(accountId, someMoment, someMoment);
      var actualAsync =
        service.getAllOperations(accountId, someMoment, someMoment)
          .join();

      assertEquals(expected.getOperationsList(), actualSync);
      assertEquals(expected.getOperationsList(), actualAsync);

      var inArg = OperationsRequest.newBuilder()
        .setAccountId(accountId)
        .setFrom(DateUtils.instantToTimestamp(someMoment))
        .setTo(DateUtils.instantToTimestamp(someMoment))
        .build();
      verify(grpcService, times(2)).getOperations(eq(inArg), any());
    }

    @Test
    void getExecutedOperations_Test() {
      var accountId = "accountId";
      var someMoment = Instant.now();
      var expected = OperationsResponse.newBuilder()
        .addOperations(Operation.newBuilder().setId("operationId").build())
        .build();
      var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
        new OperationsServiceGrpc.OperationsServiceImplBase() {
          @Override
          public void getOperations(OperationsRequest request,
                                    StreamObserver<OperationsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync =
        service.getExecutedOperationsSync(accountId, someMoment, someMoment);
      var actualAsync =
        service.getExecutedOperations(accountId, someMoment, someMoment)
          .join();

      assertEquals(expected.getOperationsList(), actualSync);
      assertEquals(expected.getOperationsList(), actualAsync);

      var inArg = OperationsRequest.newBuilder()
        .setAccountId(accountId)
        .setFrom(DateUtils.instantToTimestamp(someMoment))
        .setTo(DateUtils.instantToTimestamp(someMoment))
        .setState(OperationState.OPERATION_STATE_EXECUTED)
        .build();
      verify(grpcService, times(2)).getOperations(eq(inArg), any());
    }

    @Test
    void getCancelledOperations_Test() {
      var accountId = "accountId";
      var someMoment = Instant.now();
      var expected = OperationsResponse.newBuilder()
        .addOperations(Operation.newBuilder().setId("operationId").build())
        .build();
      var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
        new OperationsServiceGrpc.OperationsServiceImplBase() {
          @Override
          public void getOperations(OperationsRequest request,
                                    StreamObserver<OperationsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync =
        service.getCancelledOperationsSync(accountId, someMoment, someMoment);
      var actualAsync =
        service.getCancelledOperations(accountId, someMoment, someMoment)
          .join();

      assertEquals(expected.getOperationsList(), actualSync);
      assertEquals(expected.getOperationsList(), actualAsync);

      var inArg = OperationsRequest.newBuilder()
        .setAccountId(accountId)
        .setFrom(DateUtils.instantToTimestamp(someMoment))
        .setTo(DateUtils.instantToTimestamp(someMoment))
        .setState(OperationState.OPERATION_STATE_CANCELED)
        .build();
      verify(grpcService, times(2)).getOperations(eq(inArg), any());
    }

    @Test
    void getAllOperationsForFigi_Test() {
      var figi = "figi";
      var accountId = "accountId";
      var someMoment = Instant.now();
      var expected = OperationsResponse.newBuilder()
        .addOperations(Operation.newBuilder().setId("operationId").build())
        .build();
      var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
        new OperationsServiceGrpc.OperationsServiceImplBase() {
          @Override
          public void getOperations(OperationsRequest request,
                                    StreamObserver<OperationsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync =
        service.getAllOperationsSync(accountId, someMoment, someMoment, figi);
      var actualAsync =
        service.getAllOperations(accountId, someMoment, someMoment, figi)
          .join();

      assertEquals(expected.getOperationsList(), actualSync);
      assertEquals(expected.getOperationsList(), actualAsync);

      var inArg = OperationsRequest.newBuilder()
        .setAccountId(accountId)
        .setFrom(DateUtils.instantToTimestamp(someMoment))
        .setTo(DateUtils.instantToTimestamp(someMoment))
        .setFigi(figi)
        .build();
      verify(grpcService, times(2)).getOperations(eq(inArg), any());
    }

    @Test
    void getExecutedOperationsForFigi_Test() {
      var figi = "figi";
      var accountId = "accountId";
      var someMoment = Instant.now();
      var expected = OperationsResponse.newBuilder()
        .addOperations(Operation.newBuilder().setId("operationId").build())
        .build();
      var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
        new OperationsServiceGrpc.OperationsServiceImplBase() {
          @Override
          public void getOperations(OperationsRequest request,
                                    StreamObserver<OperationsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync =
        service.getExecutedOperationsSync(accountId, someMoment, someMoment, figi);
      var actualAsync =
        service.getExecutedOperations(accountId, someMoment, someMoment, figi)
          .join();

      assertEquals(expected.getOperationsList(), actualSync);
      assertEquals(expected.getOperationsList(), actualAsync);

      var inArg = OperationsRequest.newBuilder()
        .setAccountId(accountId)
        .setFrom(DateUtils.instantToTimestamp(someMoment))
        .setTo(DateUtils.instantToTimestamp(someMoment))
        .setState(OperationState.OPERATION_STATE_EXECUTED)
        .setFigi(figi)
        .build();
      verify(grpcService, times(2)).getOperations(eq(inArg), any());
    }

    @Test
    void getCancelledOperationsForFigi_Test() {
      var figi = "figi";
      var accountId = "accountId";
      var someMoment = Instant.now();
      var expected = OperationsResponse.newBuilder()
        .addOperations(Operation.newBuilder().setId("operationId").build())
        .build();
      var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
        new OperationsServiceGrpc.OperationsServiceImplBase() {
          @Override
          public void getOperations(OperationsRequest request,
                                    StreamObserver<OperationsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync =
        service.getCancelledOperationsSync(accountId, someMoment, someMoment, figi);
      var actualAsync =
        service.getCancelledOperations(accountId, someMoment, someMoment, figi)
          .join();

      assertEquals(expected.getOperationsList(), actualSync);
      assertEquals(expected.getOperationsList(), actualAsync);

      var inArg = OperationsRequest.newBuilder()
        .setAccountId(accountId)
        .setFrom(DateUtils.instantToTimestamp(someMoment))
        .setTo(DateUtils.instantToTimestamp(someMoment))
        .setState(OperationState.OPERATION_STATE_CANCELED)
        .setFigi(figi)
        .build();
      verify(grpcService, times(2)).getOperations(eq(inArg), any());
    }
  }

  @Test
  void getPortfolio_Test() {
    var accountId = "accountId";
    var expected = PortfolioResponse.newBuilder()
      .setTotalAmountBonds(MoneyValue.newBuilder().setUnits(1).setCurrency("RUB").build())
      .setTotalAmountCurrencies(MoneyValue.newBuilder().setUnits(2).setCurrency("RUB").build())
      .setTotalAmountEtf(MoneyValue.newBuilder().setUnits(3).setCurrency("RUB").build())
      .setTotalAmountFutures(MoneyValue.newBuilder().setUnits(4).setCurrency("RUB").build())
      .setTotalAmountShares(MoneyValue.newBuilder().setUnits(5).setCurrency("RUB").build())
      .setExpectedYield(Quotation.newBuilder().setUnits(6).build())
      .build();
    var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
      new OperationsServiceGrpc.OperationsServiceImplBase() {
        @Override
        public void getPortfolio(PortfolioRequest request,
                                        StreamObserver<PortfolioResponse> responseObserver) {
          responseObserver.onNext(expected);
          responseObserver.onCompleted();
        }
      }));
    var service = mkClientBasedOnServer(grpcService);

    var actualSync = service.getPortfolioSync(accountId);
    var actualAsync = service.getPortfolio(accountId).join();

    assertEquals(Portfolio.fromResponse(expected), actualSync);
    assertEquals(Portfolio.fromResponse(expected), actualAsync);

    var inArg = PortfolioRequest.newBuilder()
      .setAccountId(accountId)
      .build();
    verify(grpcService, times(2)).getPortfolio(eq(inArg), any());
  }

  @Test
  void getWithdrawLimits_Test() {
    var accountId = "accountId";
    var expected = WithdrawLimitsResponse.newBuilder()
      .addBlocked(MoneyValue.newBuilder().setUnits(1).setCurrency("RUB").build())
      .addMoney(MoneyValue.newBuilder().setUnits(2).setCurrency("RUB").build())
      .build();
    var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
      new OperationsServiceGrpc.OperationsServiceImplBase() {
        @Override
        public void getWithdrawLimits(WithdrawLimitsRequest request,
                                      StreamObserver<WithdrawLimitsResponse> responseObserver) {
          responseObserver.onNext(expected);
          responseObserver.onCompleted();
        }
      }));
    var service = mkClientBasedOnServer(grpcService);

    var actualSync = service.getWithdrawLimitsSync(accountId);
    var actualAsync = service.getWithdrawLimits(accountId).join();

    assertEquals(WithdrawLimits.fromResponse(expected), actualSync);
    assertEquals(WithdrawLimits.fromResponse(expected), actualAsync);

    var inArg = WithdrawLimitsRequest.newBuilder()
      .setAccountId(accountId)
      .build();
    verify(grpcService, times(2)).getWithdrawLimits(eq(inArg), any());
  }

  @Nested
  class RequestBrokerReportTest {

    @Test
    void request_Test() {
      var accountId = "accountId";
      var expected = BrokerReportResponse.newBuilder()
        .setGenerateBrokerReportResponse(
          GenerateBrokerReportResponse.newBuilder().setTaskId("taskId").build())
        .build();
      var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
        new OperationsServiceGrpc.OperationsServiceImplBase() {
          @Override
          public void getBrokerReport(BrokerReportRequest request,
                                      StreamObserver<BrokerReportResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var someMoment = Instant.now();
      var someMomentPlusMinute = someMoment.plusSeconds(60);
      var actualSync = service.requestBrokerReportSync(accountId, someMoment, someMomentPlusMinute);
      var actualAsync = service.requestBrokerReport(accountId, someMoment, someMomentPlusMinute).join();

      assertEquals(expected.getGenerateBrokerReportResponse().getTaskId(), actualSync);
      assertEquals(expected.getGenerateBrokerReportResponse().getTaskId(), actualAsync);

      var inArg = BrokerReportRequest.newBuilder()
        .setGenerateBrokerReportRequest(
          GenerateBrokerReportRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(someMoment))
            .setTo(DateUtils.instantToTimestamp(someMomentPlusMinute))
            .build())
        .build();
      verify(grpcService, times(2)).getBrokerReport(eq(inArg), any());
    }

    @Test
    void request_shouldThrowIfToIsNotAfterFrom_Test() {
      var accountId = "accountId";
      var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
        new OperationsServiceGrpc.OperationsServiceImplBase() {}));
      var service = mkClientBasedOnServer(grpcService);

      var someMoment = Instant.now();
      var someMomentPlusMinute = someMoment.plusSeconds(60);

      assertThrows(IllegalArgumentException.class, () -> service.requestBrokerReportSync(
        accountId,
        someMomentPlusMinute,
        someMoment));
      futureThrown.expect(CompletionException.class);
      futureThrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
      service.requestBrokerReport(accountId, someMomentPlusMinute, someMoment);

      verify(grpcService, never()).getBrokerReport(any(), any());
    }

  }

  @Nested
  class GetBrokerReportTest {

    @Test
    void get_Test() {
      var taskId = "taskId";
      var expected = BrokerReportResponse.newBuilder()
        .setGetBrokerReportResponse(
          GetBrokerReportResponse.newBuilder().build())
        .build();
      var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
        new OperationsServiceGrpc.OperationsServiceImplBase() {
          @Override
          public void getBrokerReport(BrokerReportRequest request,
                                      StreamObserver<BrokerReportResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync = service.getBrokerReportSync(taskId, 0);
      var actualAsync = service.getBrokerReport(taskId, 0).join();

      assertEquals(expected.getGetBrokerReportResponse(), actualSync);
      assertEquals(expected.getGetBrokerReportResponse(), actualAsync);

      var inArg = BrokerReportRequest.newBuilder()
        .setGetBrokerReportRequest(
          GetBrokerReportRequest.newBuilder()
            .setTaskId(taskId)
            .setPage(0)
            .build())
        .build();
      verify(grpcService, times(2)).getBrokerReport(eq(inArg), any());
    }

    @Test
    void request_shouldThrowIfPageIsNegative_Test() {
      var taskId = "taskId";
      var grpcService = mock(OperationsServiceGrpc.OperationsServiceImplBase.class, delegatesTo(
        new OperationsServiceGrpc.OperationsServiceImplBase() {}));
      var service = mkClientBasedOnServer(grpcService);

      var someMoment = Instant.now();
      var someMomentPlusMinute = someMoment.plusSeconds(60);

      assertThrows(IllegalArgumentException.class, () -> service.getBrokerReportSync(taskId, -1));
      futureThrown.expect(CompletionException.class);
      futureThrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
      service.getBrokerReport(taskId, -1);

      verify(grpcService, never()).getBrokerReport(any(), any());
    }

  }

}
