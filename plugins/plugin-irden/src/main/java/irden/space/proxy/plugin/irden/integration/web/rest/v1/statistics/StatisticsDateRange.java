package irden.space.proxy.plugin.irden.integration.web.rest.v1.statistics;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;

public record StatisticsDateRange(YearMonth from, YearMonth to) {

    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";

    private static final StatisticsDateRange ALL = new StatisticsDateRange(null, null);

    public static StatisticsDateRange of(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "%s не может быть позже %s".formatted(START_DATE, END_DATE)
            );
        }

        if (startDate == null && endDate == null) {
            return ALL;
        }

        return new StatisticsDateRange(
                startDate == null ? null : YearMonth.from(startDate),
                endDate == null ? null : YearMonth.from(endDate)
        );
    }

    public boolean contains(int year, Month month) {
        YearMonth value = YearMonth.of(year, month);
        return (from == null || !value.isBefore(from))
                && (to == null || !value.isAfter(to));
    }


    public int minYear() {
        return from == null ? Year.MIN_VALUE : from.getYear();
    }


    public int maxYear() {
        return to == null ? Year.MAX_VALUE : to.getYear();
    }
}
