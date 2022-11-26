package ru.tinkoff.piapi.core.models;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.tinkoff.piapi.contract.v1.VirtualPortfolioPosition;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
@Builder
public class VirtualPosition {
  private final String figi;
  private final String positionUid;
  private final String instrumentUid;
  private final String instrumentType;
  private final BigDecimal quantity;
  private final Money averagePositionPrice;
  private final BigDecimal expectedYield;
  private final BigDecimal expectedYieldFifo;
  private final Money currentPrice;
  private final Money averagePositionPriceFifo;
  private final BigDecimal quantityLots;
  private Instant expireDate;

  @Nonnull
  public static VirtualPosition fromResponse(@Nonnull VirtualPortfolioPosition virtualPosition) {
    return VirtualPosition.builder()
      .figi(virtualPosition.getFigi())
      .instrumentUid(virtualPosition.getInstrumentUid())
      .positionUid(virtualPosition.getPositionUid())
      .figi(virtualPosition.getFigi())
      .instrumentType(virtualPosition.getInstrumentType())
      .quantity(MapperUtils.quotationToBigDecimal(virtualPosition.getQuantity()))
      .averagePositionPrice(Money.fromResponse(virtualPosition.getAveragePositionPrice()))
      .expectedYield(MapperUtils.quotationToBigDecimal(virtualPosition.getExpectedYield()))
      .expectedYieldFifo(MapperUtils.quotationToBigDecimal(virtualPosition.getExpectedYieldFifo()))
      .expireDate(DateUtils.timestampToInstant(virtualPosition.getExpireDate()))
      .currentPrice(Money.fromResponse(virtualPosition.getCurrentPrice()))
      .averagePositionPriceFifo(Money.fromResponse(virtualPosition.getAveragePositionPriceFifo()))
      .build();
  }

  public static List<VirtualPosition> fromResponse(@Nonnull List<VirtualPortfolioPosition> virtualPositions) {
    return virtualPositions.stream().map(VirtualPosition::fromResponse).collect(Collectors.toList());
  }
}
