package irden.space.proxy.plugin.irden.persistence.model.fight;


import irden.space.proxy.plugin.irden.d20.initiative.model.IrdenFightSnapshot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "fight_snapshot",
        schema = "irden",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fight_name",
                        columnNames = {
                                "fight_name",
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IrdenFightSnapshotRecordEntity {

    @Id
    @Column(name = "fight_name", nullable = false, updatable = false)
    private String fightName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb")
    private IrdenFightSnapshot snapshot;

    @Version
    @Column(name = "entity_version", nullable = false)
    private long entityVersion;

    public IrdenFightSnapshotRecordEntity(IrdenFightSnapshot snapshot) {
        this.fightName = snapshot.fightName();
        this.snapshot = snapshot;
    }

    public void updateSnapshot(IrdenFightSnapshot snapshot) {
        if (!fightName.equals(snapshot.fightName())) {
            throw new IllegalArgumentException(
                    "Fight name cannot be changed: %s -> %s"
                            .formatted(fightName, snapshot.fightName())
            );
        }

        this.snapshot = snapshot;
    }
}
