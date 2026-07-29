package irden.space.proxy.plugin.irden.persistence.model.statistic;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Month;

@Entity
@IdClass(PlayerStatisticId.class)
@Table(
        name = "player_statistic",
        schema = "irden",
        indexes = {
                @Index(
                        name = "idx_player_statistic_uuid",
                        columnList = "player_uuid"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerStatisticRecordEntity {

    @Id
    @Column(name = "player_uuid", nullable = false, length = 32)
    private String playerUuid;

    @Id
    @Column(name = "year", nullable = false)
    private int year;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "month", nullable = false, length = 16)
    private Month month;

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @Column(name = "words_count", nullable = false)
    private int wordCount = 0;

    @Column(name = "characters_count", nullable = false)
    private int charactersCount = 0;

    @Column(name = "in_game_time_seconds", nullable = false)
    private long inGameTimeSeconds = 0;

    public static PlayerStatisticRecordEntity create(
            String playerUuid,
            int year,
            Month month
    ) {
        PlayerStatisticRecordEntity entity = new PlayerStatisticRecordEntity();
        entity.playerUuid = playerUuid;
        entity.year = year;
        entity.month = month;
        return entity;
    }

    public void updateStatistic(
            int messages,
            int words,
            int characters,
            long seconds
    ) {
        this.messageCount += messages;
        this.wordCount += words;
        this.charactersCount += characters;
        this.inGameTimeSeconds += seconds;
    }
}
