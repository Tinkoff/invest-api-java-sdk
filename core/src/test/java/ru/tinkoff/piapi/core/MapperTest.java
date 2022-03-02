package ru.tinkoff.piapi.core;

import org.junit.jupiter.api.Test;
import ru.tinkoff.piapi.contract.v1.GetFuturesMarginResponse;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapperTest {

  @Test
  public void moneyValueToBigDecimalTest() {
    var value = MoneyValue.newBuilder().setUnits(10).setNano(100000000).build();
    var actualValue = MapperUtils.moneyValueToBigDecimal(value);
    var expectedValue = BigDecimal.valueOf(10.1);
    assertEquals(0, actualValue.compareTo(expectedValue));
  }

  @Test
  public void quotationToBigDecimalTest() {
    var value = Quotation.newBuilder().setUnits(10).setNano(100000000).build();
    var actualValue = MapperUtils.quotationToBigDecimal(value);
    var expectedValue = BigDecimal.valueOf(10.1);
    assertEquals(0, actualValue.compareTo(expectedValue));
  }

  @Test
  public void bigDecimalToMoneyValueTest() {
    var value = BigDecimal.valueOf(10.1);
    var actualValue = MapperUtils.bigDecimalToMoneyValue(value);
    var expectedValue = MoneyValue.newBuilder().setUnits(10).setNano(100000000).build();
    assertEquals(expectedValue, actualValue);
  }

  @Test
  public void bigDecimalToMoney2ValueTest() {
    var value = BigDecimal.valueOf(10.1);
    var actualValue = MapperUtils.bigDecimalToMoneyValue(value, "RUB");
    var expectedValue = MoneyValue.newBuilder().setUnits(10).setNano(100000000).setCurrency("rub").build();
    assertEquals(expectedValue, actualValue);
  }

  @Test
  public void bigDecimalToQuotationTest() {
    var value = BigDecimal.valueOf(10.1);
    var actualValue = MapperUtils.bigDecimalToQuotation(value);
    var expectedValue = Quotation.newBuilder().setUnits(10).setNano(100000000).build();
    assertEquals(expectedValue, actualValue);
  }

  @Test
  public void futuresPriceBigDecimalTest() {
    var value = BigDecimal.valueOf(30);
    var response = GetFuturesMarginResponse
      .newBuilder()
      .setMinPriceIncrement(Quotation.newBuilder().setUnits(10).setNano(0).build())
      .setMinPriceIncrementAmount(Quotation.newBuilder().setUnits(20).setNano(0).build())
      .build();
    var actualValue = MapperUtils.futuresPrice(value, response);
    var expectedValue = BigDecimal.valueOf(60); //30 / 10 * 20
    assertEquals(0, actualValue.compareTo(expectedValue));
  }

  @Test
  public void futuresPriceQuotationTest() {
    var value = Quotation.newBuilder().setUnits(30).setNano(0).build();
    var response = GetFuturesMarginResponse
      .newBuilder()
      .setMinPriceIncrement(Quotation.newBuilder().setUnits(10).setNano(0).build())
      .setMinPriceIncrementAmount(Quotation.newBuilder().setUnits(20).setNano(0).build())
      .build();
    var actualValue = MapperUtils.futuresPrice(value, response);
    var expectedValue = BigDecimal.valueOf(60); //30 / 10 * 20
    assertEquals(0, actualValue.compareTo(expectedValue));
  }
}
