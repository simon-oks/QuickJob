package ro.oks.quickjob.common.money;

import java.util.Currency;

/**
 * Montant en unité mineure (entier) + devise ISO 4217.
 * XOF n'a pas de subdivision : 1 500 XOF = amountMinor 1500.
 * GHS se stocke en pesewas : 5.00 GHS = amountMinor 500.
 */
public record Money(long amountMinor, Currency currency) {

    public Money {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif");
        }
        if (currency == null) {
            throw new IllegalArgumentException("La devise est obligatoire");
        }
    }

    public static Money of(long amountMinor, String currencyCode) {
        return new Money(amountMinor, Currency.getInstance(currencyCode));
    }

    public String currencyCode() {
        return currency.getCurrencyCode();
    }
}
