package irden.space.proxy.plugin.ban_manager.utils;

import irden.space.proxy.plugin.utils.messages.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

@RequiredArgsConstructor
@Component
public class BanFormatUtils {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final MessageUtils messageUtils;

    public String formatBanMessage(String reason, boolean permanent, LocalDateTime expiresAt) {
        var message = new StringBuilder();

        message.append(messageUtils.get("ban.message.header"));
        message.append(System.lineSeparator());

        message.append(messageUtils.get("ban.message.reason", reason));

        if (!permanent && expiresAt != null) {
            message.append(System.lineSeparator());
            message.append(System.lineSeparator());

            message.append(messageUtils.get("ban.message.expire.header"));
            message.append(System.lineSeparator());

            message.append(formatUntil(expiresAt));
            message.append(System.lineSeparator());
        } else {
            message.append(System.lineSeparator());
            message.append(messageUtils.get("ban.message.permanent"));
        }

        return message.toString();
    }

    public String formatUntil(LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();

        if (expiresAt.isBefore(now.plusMinutes(1))) {
            return messageUtils.get("ban.until.less-than-minute");
        }

        if (expiresAt.isBefore(now.plusHours(2))) {
            Duration duration = Duration.between(now, expiresAt);
            long minutes = duration.toMinutes();

            String pluralSuffix = minutes > 1 ? "s" : "";

            return messageUtils.get("ban.until.minutes", minutes, pluralSuffix);
        }

        String time = expiresAt.toLocalTime().format(TIME_FORMATTER);

        LocalDate today = now.toLocalDate();
        LocalDate expiresDate = expiresAt.toLocalDate();

        if (expiresDate.equals(today)) {
            return messageUtils.get("ban.until.today", time);
        }

        if (expiresDate.equals(today.plusDays(1))) {
            return messageUtils.get("ban.until.tomorrow", time);
        }

        if (expiresDate.isBefore(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)))) {
            String dayName = messageUtils
                    .get("ban.until.this-week.names")
                    .split(",")[expiresAt.getDayOfWeek().getValue() - 1];

            return messageUtils.get("ban.until.this-week", dayName, time);
        }

        if (expiresAt.isBefore(now.plusMonths(1))) {
            return messageUtils.get("ban.until.this-month", expiresAt.getDayOfMonth(), time);
        }

        return messageUtils.get("ban.until.datetime", expiresAt.toString());
    }

    public LocalDateTime parseExpiresAt(String durationStr) {
        LocalDateTime expiresAt = LocalDateTime.now();

        for (String part : durationStr.trim().split("\\s+")) {
            if (part.length() < 2) {
                throw new IllegalArgumentException("Invalid duration part: " + part);
            }

            long value;
            try {
                value = Long.parseLong(part.substring(0, part.length() - 1));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid duration value: " + part, ex);
            }

            char unit = Character.toLowerCase(part.charAt(part.length() - 1));
            switch (unit) {
                case 's' -> expiresAt = expiresAt.plusSeconds(value);
                case 'm' -> expiresAt = expiresAt.plusMinutes(value);
                case 'h' -> expiresAt = expiresAt.plusHours(value);
                case 'd' -> expiresAt = expiresAt.plusDays(value);
                case 'w' -> expiresAt = expiresAt.plusWeeks(value);
                default -> throw new IllegalArgumentException("Unsupported duration unit: " + unit);
            }
        }

        return expiresAt;
    }

}