package ro.oks.quickjob.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import ro.oks.quickjob.common.money.Money;

class MoneyTest {

    @Test
    void creates_xof_money() {
        Money money = Money.of(1500, "XOF");
        assertThat(money.amountMinor()).isEqualTo(1500);
        assertThat(money.currencyCode()).isEqualTo("XOF");
    }

    @Test
    void rejects_zero_amount() {
        assertThatThrownBy(() -> Money.of(0, "XOF"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_negative_amount() {
        assertThatThrownBy(() -> Money.of(-100, "XOF"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
