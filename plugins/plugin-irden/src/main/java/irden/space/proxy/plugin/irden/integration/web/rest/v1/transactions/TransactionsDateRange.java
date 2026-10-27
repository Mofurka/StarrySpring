package irden.space.proxy.plugin.irden.integration.web.rest.v1.transactions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;


public record TransactionsDateRange(Instant from, Instant toExclusive) {

    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";

    private static final Instant MIN = Instant.EPOCH;

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 1, 1);

    public static TransactionsDateRange of(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "%s не может быть позже %s".formatted(START_DATE, END_DATE)
            );
        }

        return new TransactionsDateRange(
                startDate == null ? MIN : startOfDay(startDate),
                startOfDay(endDate == null ? MAX_DATE : endDate.plusDays(1))
        );
    }

    private static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
