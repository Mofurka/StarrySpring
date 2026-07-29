package irden.space.proxy.plugin.player_manager.model.player_position;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class PlayerPosition {
    private PlayerLocation previousLocation;
    private PlayerLocation currentLocation;
    private Float x;
    private Float y;


    @Override
    public String toString() {
        return "PlayerPosition{" +
                "previousLocation=" + previousLocation +
                ", currentLocation=" + currentLocation +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}