package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.contract.v1.UsersServiceGrpc.UsersServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.UsersServiceGrpc.UsersServiceStub;
import ru.tinkoff.piapi.core.utils.Helpers;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.tinkoff.piapi.core.utils.Helpers.unaryCall;

public class UsersService {
  private final UsersServiceBlockingStub userBlockingStub;
  private final UsersServiceStub userStub;

  UsersService(
    @Nonnull UsersServiceBlockingStub userBlockingStub,
    @Nonnull UsersServiceStub userStub) {
    this.userBlockingStub = userBlockingStub;
    this.userStub = userStub;
  }

  @Nonnull
  public List<Account> getAccountsSync() {
    return unaryCall(() -> userBlockingStub.getAccounts(
        GetAccountsRequest.newBuilder()
          .build())
      .getAccountsList());
  }


  @Nonnull
  public CompletableFuture<List<Account>> getAccounts() {
    return Helpers.<GetAccountsResponse>unaryAsyncCall(
        observer -> userStub.getAccounts(
          GetAccountsRequest.newBuilder().build(),
          observer))
      .thenApply(GetAccountsResponse::getAccountsList);
  }

  @Nonnull
  public GetMarginAttributesResponse getMarginAttributesSync(@Nonnull String accountId) {
    return unaryCall(() -> userBlockingStub.getMarginAttributes(
      GetMarginAttributesRequest.newBuilder().setAccountId(accountId).build()));
  }


  @Nonnull
  public CompletableFuture<GetMarginAttributesResponse> getMarginAttributes(
    @Nonnull String accountId) {
    return Helpers.unaryAsyncCall(
      observer -> userStub.getMarginAttributes(
        GetMarginAttributesRequest.newBuilder().setAccountId(accountId).build(),
        observer));
  }

  @Nonnull
  public GetUserTariffResponse getUserTariffSync() {
    return unaryCall(() -> userBlockingStub.getUserTariff(GetUserTariffRequest.newBuilder().build()));
  }

  @Nonnull
  public CompletableFuture<GetUserTariffResponse> getUserTariff() {
    return Helpers.unaryAsyncCall(
      observer -> userStub.getUserTariff(
        GetUserTariffRequest.newBuilder().build(),
        observer));
  }

  @Nonnull
  public GetInfoResponse getInfoSync() {
    return unaryCall(() -> userBlockingStub.getInfo(GetInfoRequest.newBuilder().build()));
  }

  @Nonnull
  public CompletableFuture<GetInfoResponse> getInfo() {
    return Helpers.unaryAsyncCall(
      observer -> userStub.getInfo(
        GetInfoRequest.newBuilder().build(),
        observer));
  }
}
