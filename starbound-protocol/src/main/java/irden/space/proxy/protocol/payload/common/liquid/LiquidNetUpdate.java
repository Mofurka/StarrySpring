package irden.space.proxy.protocol.payload.common.liquid;

public record LiquidNetUpdate(
        int liquid,
        int level
) {

    public static final int EMPTY_LIQUID_ID = 0;

    public static LiquidNetUpdate empty() {
        return new LiquidNetUpdate(EMPTY_LIQUID_ID, 0);
    }

    public boolean isEmpty() {
        return liquid == EMPTY_LIQUID_ID;
    }
}
