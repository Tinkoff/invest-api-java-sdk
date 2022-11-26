package ru.tinkoff.piapi.core.models;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.tinkoff.piapi.contract.v1.PortfolioResponse;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.List;

@Getter
@EqualsAndHashCode
@Builder
public class Portfolio {
  private final Money totalAmountShares;
  private final Money totalAmountBonds;
  private final Money totalAmountEtfs;
  private final Money totalAmountCurrencies;
  private final Money totalAmountFutures;
  private final Money totalAmountPortfolio;
  private final BigDecimal expectedYield;
  private final List<Position> positions;
  private final Money totalAmountSp;
  private final Money totalAmountOptions;
  private final List<VirtualPosition> virtualPositions;

  public static Portfolio fromResponse(@Nonnull PortfolioResponse portfolioResponse) {
    return Portfolio.builder()
      .totalAmountShares(Money.fromResponse(portfolioResponse.getTotalAmountShares()))
      .totalAmountBonds(Money.fromResponse(portfolioResponse.getTotalAmountBonds()))
      .totalAmountEtfs(Money.fromResponse(portfolioResponse.getTotalAmountEtf()))
      .totalAmountCurrencies(Money.fromResponse(portfolioResponse.getTotalAmountCurrencies()))
      .totalAmountFutures(Money.fromResponse(portfolioResponse.getTotalAmountFutures()))
      .totalAmountOptions(Money.fromResponse(portfolioResponse.getTotalAmountOptions()))
      .totalAmountSp(Money.fromResponse(portfolioResponse.getTotalAmountSp()))
      .totalAmountPortfolio(Money.fromResponse(portfolioResponse.getTotalAmountPortfolio()))
      .expectedYield(MapperUtils.quotationToBigDecimal(portfolioResponse.getExpectedYield()))
      .virtualPositions(VirtualPosition.fromResponse(portfolioResponse.getVirtualPositionsList()))
      .positions(Position.fromResponse(portfolioResponse.getPositionsList()))
      .build();
  }
}
