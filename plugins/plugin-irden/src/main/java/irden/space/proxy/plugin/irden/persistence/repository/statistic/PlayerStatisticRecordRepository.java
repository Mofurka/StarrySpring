package irden.space.proxy.plugin.irden.persistence.repository.statistic;

import irden.space.proxy.plugin.irden.persistence.model.statistic.PlayerStatisticId;
import irden.space.proxy.plugin.irden.persistence.model.statistic.PlayerStatisticRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PlayerStatisticRecordRepository
        extends JpaRepository<PlayerStatisticRecordEntity, PlayerStatisticId> {

    List<PlayerStatisticRecordEntity> findAllByPlayerUuidIn(Collection<String> playerUuids);
}
