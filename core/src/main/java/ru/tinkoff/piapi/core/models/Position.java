package ru.tinkoff.piapi.core.models;

import ru.tinkoff.piapi.contract.v1.PortfolioPosition;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Objects;

public class Position {
  private final String figi;
  private final String instrumentType;
  private final BigDecimal quantity;
  private final Money averagePositionPrice;
  private final BigDecimal expectedYield;
  private final Money currentNkd;
  private final BigDecimal averagePositionPricePt;
  private final Money currentPrice;
  private final Money averagePositionPriceFifo;
  private final BigDecimal quantityLots;

  private Position(@Nonnull String figi,
                   @Nonnull String instrumentType,
                   @Nonnull BigDecimal quantity,
                   @Nonnull Money averagePositionPrice,
                   @Nonnull BigDecimal expectedYield,
                   @Nonnull Money currentNkd,
                   @Nonnull BigDecimal averagePositionPricePt,
                   @Nonnull Money currentPrice,
                   @Nonnull Money averagePositionPriceFifo,
                   @Nonnull BigDecimal quantityLots) {
    this.figi = figi;
    this.instrumentType = instrumentType;
    this.quantity = quantity;
    this.averagePositionPrice = averagePositionPrice;
    this.expectedYield = expectedYield;
    this.currentNkd = currentNkd;
    this.averagePositionPricePt = averagePositionPricePt;
    this.currentPrice = currentPrice;
    this.averagePositionPriceFifo = averagePositionPriceFifo;
    this.quantityLots = quantityLots;
  }

  @Nonnull
  public static Position fromResponse(@Nonnull PortfolioPosition portfolioPosition) {
    return new Position(
      portfolioPosition.getFigi(),
      portfolioPosition.getInstrumentType(),
      MapperUtils.quotationToBigDecimal(portfolioPosition.getQuantity()),
      Money.fromResponse(portfolioPosition.getAveragePositionPrice()),
      MapperUtils.quotationToBigDecimal(portfolioPosition.getExpectedYield()),
      Money.fromResponse(portfolioPosition.getCurrentNkd()),
      MapperUtils.quotationToBigDecimal(portfolioPosition.getAveragePositionPricePt()),
      Money.fromResponse(portfolioPosition.getCurrentPrice()),
      Money.fromResponse(portfolioPosition.getAveragePositionPriceFifo()),
      MapperUtils.quotationToBigDecimal(portfolioPosition.getQuantityLots())
    );
  }

  @Nonnull
  public String getFigi() {
    return figi;
  }

  @Nonnull
  public String getInstrumentType() {
    return instrumentType;
  }

  @Nonnull
  public BigDecimal getQuantity() {
    return quantity;
  }

  @Nonnull
  public Money getAveragePositionPrice() {
    return averagePositionPrice;
  }

  @Nonnull
  public BigDecimal getExpectedYield() {
    return expectedYield;
  }

  @Nonnull
  public Money getCurrentNkd() {
    return currentNkd;
  }

  @Nonnull
  public BigDecimal getAveragePositionPricePt() {
    return averagePositionPricePt;
  }

  @Nonnull
  public Money getCurrentPrice() {
    return currentPrice;
  }

  @Nonnull
  public Money getAveragePositionPriceFifo() {
    return averagePositionPriceFifo;
  }

  @Nonnull
  public BigDecimal getQuantityLots() {
    return quantityLots;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Position position = (Position) o;
    return figi.equals(position.figi) && instrumentType.equals(position.instrumentType) &&
      quantity.equals(position.quantity) && averagePositionPrice.equals(position.averagePositionPrice) &&
      expectedYield.equals(position.expectedYield) && currentNkd.equals(position.currentNkd) &&
      averagePositionPricePt.equals(position.averagePositionPricePt) &&
      currentPrice.equals(position.currentPrice) &&
      averagePositionPriceFifo.equals(position.averagePositionPriceFifo) &&
      quantityLots.equals(position.quantityLots);
  }

  @Override
  public int hashCode() {
    return Objects.hash(figi, instrumentType, quantity, averagePositionPrice, expectedYield, currentNkd,
      averagePositionPricePt, currentPrice, averagePositionPriceFifo, quantityLots);
  }
}
