package com.donutclient.util;

import com.donutclient.util.PayCommandParser.Result;
import com.donutclient.util.PayCommandParser.Status;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PayCommandParserTest {
    @Test
    void validPay() {
        Result result = PayCommandParser.parse("pay Pseudo 1000000");
        assertEquals(Status.OK, result.status());
        assertEquals("Pseudo", result.player());
        assertEquals("1,000,000", result.formattedAmount());
    }

    @Test
    void amountFormats() {
        assertEquals(0, new BigDecimal("1000000").compareTo(PayCommandParser.parseAmount("1,000,000")));
        assertEquals(0, new BigDecimal("2500").compareTo(PayCommandParser.parseAmount("2.5k")));
        assertEquals(0, new BigDecimal("1000000").compareTo(PayCommandParser.parseAmount("1M")));
        assertEquals("1,234.5", PayCommandParser.parse("pay a 1234.5").formattedAmount());
    }

    @Test
    void invalidAmounts() {
        assertEquals(Status.INVALID_AMOUNT, PayCommandParser.parse("pay Pseudo abc").status());
        assertEquals(Status.INVALID_AMOUNT, PayCommandParser.parse("pay Pseudo -5").status());
        assertEquals(Status.INVALID_AMOUNT, PayCommandParser.parse("pay Pseudo 0").status());
        assertNull(PayCommandParser.parseAmount("1kk"));
    }

    @Test
    void usageAndPlayer() {
        assertEquals(Status.USAGE, PayCommandParser.parse("pay").status());
        assertEquals(Status.USAGE, PayCommandParser.parse("pay Pseudo").status());
        assertEquals(Status.USAGE, PayCommandParser.parse("pay Pseudo 10 extra").status());
        assertEquals(Status.INVALID_PLAYER, PayCommandParser.parse("pay Pseu-do! 10").status());
        assertEquals(Status.INVALID_PLAYER, PayCommandParser.parse("pay ThisNameIsWayTooLong 10").status());
    }

    @Test
    void otherCommandsUntouched() {
        assertEquals(Status.NOT_PAY, PayCommandParser.parse("home").status());
        assertEquals(Status.NOT_PAY, PayCommandParser.parse("payall 10").status());
        assertEquals(Status.NOT_PAY, PayCommandParser.parse("msg Pseudo pay 10").status());
        assertEquals(Status.OK, PayCommandParser.parse("PAY Pseudo 10").status());
    }
}
