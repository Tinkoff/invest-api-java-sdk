package ru.tinkoff.piapi.core.models;

import ru.tinkoff.piapi.contract.v1.PortfolioResponse;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Portfolio {
  private final Money totalAmountShares;
  private final Money totalAmountBonds;
  private final Money totalAmountEtfs;
  private final Money totalAmountCurrencies;
  private final Money totalAmountFutures;
  private final BigDecimal expectedYield;
  private final List<Position> positions;

  private Portfolio(@Nonnull Money totalAmountShares,
                    @Nonnull Money totalAmountBonds,
                    @Nonnull Money totalAmountEtfs,
                    @Nonnull Money totalAmountCurrencies,
                    @Nonnull Money totalAmountFutures,
                    @Nonnull BigDecimal expectedYield,
                    @Nonnull List<Position> positions) {
    this.totalAmountShares = totalAmountShares;
    this.totalAmountBonds = totalAmountBonds;
    this.totalAmountEtfs = totalAmountEtfs;
    this.totalAmountCurrencies = totalAmountCurrencies;
    this.totalAmountFutures = totalAmountFutures;
    this.expectedYield = expectedYield;
    this.positions = positions;
  }

  public static Portfolio fromResponse(@Nonnull PortfolioResponse portfolioResponse) {
    return new Portfolio(
      Money.fromResponse(portfolioResponse.getTotalAmountShares()),
      Money.fromResponse(portfolioResponse.getTotalAmountBonds()),
      Money.fromResponse(portfolioResponse.getTotalAmountEtf()),
      Money.fromResponse(portfolioResponse.getTotalAmountCurrencies()),
      Money.fromResponse(portfolioResponse.getTotalAmountFutures()),
      MapperUtils.quotationToBigDecimal(portfolioResponse.getExpectedYield()),
      portfolioResponse.getPositionsList().stream().map(Position::fromResponse).collect(Collectors.toList())
    );
  }

  public Money getTotalAmountShares() {
    return totalAmountShares;
  }

  public Money getTotalAmountBonds() {
    return totalAmountBonds;
  }

  public Money getTotalAmountEtfs() {
    return totalAmountEtfs;
  }

  public Money getTotalAmountCurrencies() {
    return totalAmountCurrencies;
  }

  public Money getTotalAmountFutures() {
    return totalAmountFutures;
  }

  public BigDecimal getExpectedYield() {
    return expectedYield;
  }

  public List<Position> getPositions() {
    return positions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Portfolio portfolio = (Portfolio) o;
    return totalAmountShares.equals(portfolio.totalAmountShares) &&
      totalAmountBonds.equals(portfolio.totalAmountBonds) && totalAmountEtfs.equals(portfolio.totalAmountEtfs) &&
      totalAmountCurrencies.equals(portfolio.totalAmountCurrencies) &&
      totalAmountFutures.equals(portfolio.totalAmountFutures) && expectedYield.equals(portfolio.expectedYield) &&
      positions.equals(portfolio.positions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalAmountShares, totalAmountBonds, totalAmountEtfs, totalAmountCurrencies, totalAmountFutures,
      expectedYield, positions);
  }
}
