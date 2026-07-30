package irden.space.proxy.plugin.player_manager.model.player_position;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayerPosition that)) return false;
        return Objects.equals(getPreviousLocation(), that.getPreviousLocation()) && Objects.equals(getCurrentLocation(), that.getCurrentLocation()) && Objects.equals(getX(), that.getX()) && Objects.equals(getY(), that.getY());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPreviousLocation(), getCurrentLocation(), getX(), getY());
    }
}