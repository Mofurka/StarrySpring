package irden.space.proxy.plugin.irden.persistence.model.statistic;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Month;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PlayerStatisticId implements Serializable {

    private String playerUuid;
    private int year;
    private Month month;
}
