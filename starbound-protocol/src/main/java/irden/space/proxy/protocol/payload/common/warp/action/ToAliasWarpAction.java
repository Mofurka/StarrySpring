package irden.space.proxy.protocol.payload.common.warp.action;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record ToAliasWarpAction(
        WarpAliasType aliasId
) implements WarpAction {

    @Getter
    @AllArgsConstructor
    public enum WarpAliasType {
        RETURN(0),
        ORBITED(1),
        SHIP(3);
        private final int id;

        public static WarpAliasType fromId(int id) {
            return WarpAliasType.values()[id];
        }

    }
}
