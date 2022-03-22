package ru.tinkoff.piapi.core.models;

import ru.tinkoff.piapi.contract.v1.PositionsResponse;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Positions {
  private final List<Money> money;
  private final List<Money> blocked;
  private final List<SecurityPosition> securities;
  private final boolean limitsLoadingInProgress;
  private final List<FuturePosition> futures;

  @Nonnull
  public static Positions fromResponse(@Nonnull PositionsResponse positionsResponse) {
    return new Positions(
      positionsResponse.getMoneyList().stream().map(Money::fromResponse).collect(Collectors.toList()),
      positionsResponse.getBlockedList().stream().map(Money::fromResponse).collect(Collectors.toList()),
      positionsResponse.getSecuritiesList().stream().map(SecurityPosition::fromResponse).collect(Collectors.toList()),
      positionsResponse.getLimitsLoadingInProgress(),
      positionsResponse.getFuturesList().stream().map(FuturePosition::fromResponse).collect(Collectors.toList())
    );
  }

  private Positions(@Nonnull List<Money> money,
            @Nonnull List<Money> blocked,
            @Nonnull List<SecurityPosition> securities,
            boolean limitsLoadingInProgress,
            @Nonnull List<FuturePosition> futures) {
    this.money = money;
    this.blocked = blocked;
    this.securities = securities;
    this.limitsLoadingInProgress = limitsLoadingInProgress;
    this.futures = futures;
  }

  @Nonnull
  public List<Money> getMoney() {
    return money;
  }

  @Nonnull
  public List<Money> getBlocked() {
    return blocked;
  }

  @Nonnull
  public List<SecurityPosition> getSecurities() {
    return securities;
  }

  public boolean isLimitsLoadingInProgress() {
    return limitsLoadingInProgress;
  }

  @Nonnull
  public List<FuturePosition> getFutures() {
    return futures;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Positions positions = (Positions) o;
    return limitsLoadingInProgress == positions.limitsLoadingInProgress && money.equals(positions.money) &&
      blocked.equals(positions.blocked) && securities.equals(positions.securities) &&
      futures.equals(positions.futures);
  }

  @Override
  public int hashCode() {
    return Objects.hash(money, blocked, securities, limitsLoadingInProgress, futures);
  }
}
