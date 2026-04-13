package irden.space.proxy.protocol.payload.packet.fly_ship.system_location;

public sealed interface SystemLocation permits CelestialSystemLocation, CelestialSystemOrbit, SystemUuid, SystemVec2F, UniverseSystemLocation {
}
