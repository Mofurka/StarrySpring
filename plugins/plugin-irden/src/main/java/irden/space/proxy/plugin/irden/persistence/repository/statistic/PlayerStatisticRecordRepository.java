package irden.space.proxy.plugin.irden.persistence.repository.statistic;

import irden.space.proxy.plugin.irden.persistence.model.statistic.PlayerStatisticId;
import irden.space.proxy.plugin.irden.persistence.model.statistic.PlayerStatisticRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerStatisticRecordRepository
        extends JpaRepository<PlayerStatisticRecordEntity, PlayerStatisticId> {
}
