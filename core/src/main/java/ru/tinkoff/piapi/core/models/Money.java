package ru.tinkoff.piapi.core.models;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

@Getter
@EqualsAndHashCode
@Builder
public class Money {
  private final String currency;
  private final BigDecimal value;

  private Money(@Nonnull String currency, @Nonnull BigDecimal value) {
    this.currency = currency;
    this.value = value;
  }

  public static Money fromResponse(@Nonnull MoneyValue moneyValue) {
    return Money.builder()
      .currency(moneyValue.getCurrency())
      .value(MapperUtils.moneyValueToBigDecimal(moneyValue))
      .build();
  }
}
