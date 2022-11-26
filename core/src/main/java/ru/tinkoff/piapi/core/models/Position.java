package ru.tinkoff.piapi.core.models;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.tinkoff.piapi.contract.v1.PortfolioPosition;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
@Builder
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

  public static List<Position> fromResponse(@Nonnull List<PortfolioPosition> portfolioPositions) {
    return portfolioPositions.stream().map(Position::fromResponse).collect(Collectors.toList());
  }
}
