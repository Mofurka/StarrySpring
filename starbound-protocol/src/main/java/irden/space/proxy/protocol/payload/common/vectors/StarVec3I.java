package irden.space.proxy.protocol.payload.common.vectors;

import java.util.Objects;

public record StarVec3I(
        int x,
        int y,
        int z
) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StarVec3I(int x1, int y1, int z1))) return false;
        return x == x1 && y == y1 && z == z1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
