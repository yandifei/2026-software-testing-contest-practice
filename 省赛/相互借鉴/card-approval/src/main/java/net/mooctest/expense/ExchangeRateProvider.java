package net.mooctest.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

/** External exchange-rate boundary suitable for Mockito stubbing. */
@FunctionalInterface
public interface ExchangeRateProvider {

    BigDecimal rateToBase(Currency currency, LocalDate date);
}
