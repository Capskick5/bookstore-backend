package edu.fpt.sba301.bookstore;

import edu.fpt.sba301.bookstore.payment.PayOsPaymentService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PayOsPaymentServiceTest {

    @Test
    void parseTransactionDateTimeUsesVietnamOffset() {
        OffsetDateTime parsed = PayOsPaymentService.parseTransactionDateTime("2023-02-04 18:25:00");
        assertNotNull(parsed);
        assertEquals(ZoneOffset.ofHours(7), parsed.getOffset());
        assertEquals(18, parsed.getHour());
    }
}
