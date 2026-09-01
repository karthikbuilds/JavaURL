package com.karthik.JavaURL.util;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62CodecTest {

    @Test
    void encodeHandlesZero() {
        assertThat(Base62Codec.encode(0)).isEqualTo("0");
    }

    @Test
    void encodeRejectsNegativeValues() {
        assertThatThrownBy(() -> Base62Codec.encode(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encodeDecodeRoundTripsSampledValues() {
        long[] samples = {1, 9, 10, 61, 62, 63, 3843, 3844, 123_456_789L, Long.MAX_VALUE / 2};
        for (long value : samples) {
            assertThat(Base62Codec.decode(Base62Codec.encode(value)))
                    .as("round trip of %d", value)
                    .isEqualTo(value);
        }
    }

    @Test
    void decodeRejectsInvalidCharacters() {
        assertThatThrownBy(() -> Base62Codec.decode("abc!def"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("!");
    }

    @Test
    void randomCodeHasRequestedLengthAndAlphabetOnly() {
        String code = Base62Codec.randomCode(7, new Random(42));
        assertThat(code).hasSize(7).matches("[" + Base62Codec.ALPHABET + "]+");

        String other = Base62Codec.randomCode(12, new Random(7));
        assertThat(other).hasSize(12).matches("[" + Base62Codec.ALPHABET + "]+");
    }

    @Test
    void combinationsMatchesAlphabetSizeToPowerOfLength() {
        assertThat(Base62Codec.combinations(2).longValue()).isEqualTo(62 * 62);
        assertThat(Base62Codec.combinations(7)).isEqualByComparingTo(java.math.BigInteger.valueOf(3_521_614_606_208L));
    }
}