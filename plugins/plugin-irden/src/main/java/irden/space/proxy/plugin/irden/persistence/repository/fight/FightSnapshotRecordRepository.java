package irden.space.proxy.plugin.irden.persistence.repository.fight;

import irden.space.proxy.plugin.irden.persistence.model.fight.IrdenFightSnapshotRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FightSnapshotRecordRepository extends JpaRepository<IrdenFightSnapshotRecordEntity, String> {

}
