package irden.space.proxy.protocol.payload.common.net_tile;

import irden.space.proxy.protocol.payload.common.collision.CollisionKind;
import irden.space.proxy.protocol.payload.common.liquid.LiquidNetUpdate;
import lombok.Builder;

@Builder(toBuilder = true)
public record NetTile(
        int background,
        int backgroundHueShift,
        int backgroundColorVariant,
        int backgroundMod,
        int backgroundModHueShift,
        int foreground,
        int foregroundHueShift,
        int foregroundColorVariant,
        int foregroundMod,
        int foregroundModHueShift,
        CollisionKind collision,
        int blockBiomeIndex,
        int environmentBiomeIndex,
        LiquidNetUpdate liquid,
        int dungeonId
) {

    /**
     * Пустой и непроходимый.
     */
    public static final int EMPTY_MATERIAL_ID = 65535;
    /**
     * Тайл без мода.
     */
    public static final int NO_MOD_ID = 65535;
    public static final int DEFAULT_MATERIAL_COLOR_VARIANT = 0;
    public static final int NO_DUNGEON_ID = 65535;

    public boolean hasBackground() {
        return background != EMPTY_MATERIAL_ID;
    }

    public boolean hasForeground() {
        return foreground != EMPTY_MATERIAL_ID;
    }
}
