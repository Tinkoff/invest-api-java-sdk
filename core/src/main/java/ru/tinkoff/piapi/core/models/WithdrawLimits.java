package ru.tinkoff.piapi.core.models;

import ru.tinkoff.piapi.contract.v1.WithdrawLimitsResponse;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Доступный для вывода остаток.
 */
public class WithdrawLimits {
  private final List<Money> money;
  private final List<Money> blocked;
  private final List<Money> blockedGuarantee;

  private WithdrawLimits(@Nonnull List<Money> money,
                         @Nonnull List<Money> blocked,
                         @Nonnull List<Money> blockedGuarantee) {
    this.money = money;
    this.blocked = blocked;
    this.blockedGuarantee = blockedGuarantee;
  }

  public static WithdrawLimits fromResponse(@Nonnull WithdrawLimitsResponse withdrawLimitsResponse) {
    return new WithdrawLimits(
      withdrawLimitsResponse.getMoneyList().stream().map(Money::fromResponse).collect(Collectors.toList()),
      withdrawLimitsResponse.getBlockedList().stream().map(Money::fromResponse).collect(Collectors.toList()),
      withdrawLimitsResponse.getBlockedGuaranteeList().stream().map(Money::fromResponse).collect(Collectors.toList())
    );
  }

  /**
   * Получение валютных позиций портфеля.
   *
   * @return Валютные позиции портфеля.
   */
  @Nonnull
  public List<Money> getMoney() {
    return money;
  }

  /**
   * Получение заблокированных валютных позиций портфеля.
   *
   * @return Заблокированные валютные позиций портфеля.
   */
  @Nonnull
  public List<Money> getBlocked() {
    return blocked;
  }

  /**
   * Получение средств заблокированных под гарантийное обеспечение фьючерсов.
   *
   * @return Средства заблокированные под гарантийное обеспечение фьючерсов.
   */
  @Nonnull
  public List<Money> getBlockedGuarantee() {
    return blockedGuarantee;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WithdrawLimits that = (WithdrawLimits) o;
    return money.equals(that.money) && blocked.equals(that.blocked) && blockedGuarantee.equals(that.blockedGuarantee);
  }

  @Override
  public int hashCode() {
    return Objects.hash(money, blocked, blockedGuarantee);
  }
}
