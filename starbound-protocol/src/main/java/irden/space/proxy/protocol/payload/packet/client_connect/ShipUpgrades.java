package irden.space.proxy.protocol.payload.packet.client_connect;

import irden.space.proxy.protocol.payload.common.string_set.StringSet;

public record ShipUpgrades(
        int shipLevel,
        int maxFuel,
        int crewSize,
        float fuelEfficiency,
        float shipSpeed,
        StringSet capabilities
) {
}
