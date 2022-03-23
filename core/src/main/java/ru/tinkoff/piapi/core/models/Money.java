package ru.tinkoff.piapi.core.models;

import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

public class Money {
  private final Currency currency;
  private final BigDecimal value;

  private Money(@Nonnull Currency currency, @Nonnull BigDecimal value) {
    this.currency = currency;
    this.value = value;
  }

  public static Money fromResponse(@Nonnull MoneyValue moneyValue) {
    return new Money(
      Currency.getInstance(moneyValue.getCurrency().toUpperCase(Locale.ROOT)),
      MapperUtils.moneyValueToBigDecimal(moneyValue)
    );
  }

  @Nonnull
  public Currency getCurrency() {
    return currency;
  }

  @Nonnull
  public BigDecimal getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Money money = (Money) o;
    return currency.equals(money.currency) && value.equals(money.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(currency, value);
  }
}
