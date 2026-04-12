package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.ForwardPacketDecision;
import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.ReplacePacketDecision;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class PacketInterceptionChainTest {

	@Test
	void applyInvokesHandlerRegisteredForConcretePacketType() {
		DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
		PacketInterceptionChain chain = new PacketInterceptionChain(registry);

		PacketDecision expectedDecision = new ReplacePacketDecision(envelope(PacketType.CHAT_RECEIVE));
		registry.register(PacketType.CHAT_SENT, context -> {
			assertSame(PacketType.CHAT_SENT, context.envelope().packetType());
			return expectedDecision;
		});

		PacketDecision matchedDecision = chain.apply(context(PacketType.CHAT_SENT));
		PacketDecision unmatchedDecision = chain.apply(context(PacketType.CHAT_RECEIVE));

		assertSame(expectedDecision, matchedDecision);
		assertSame(ForwardPacketDecision.INSTANCE, unmatchedDecision);
	}

	@Test
	void applyInvokesHandlerRegisteredForSeveralPacketTypes() {
		DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
		PacketInterceptionChain chain = new PacketInterceptionChain(registry);

		registry.register(List.of(PacketType.CHAT_SENT, PacketType.CHAT_RECEIVE), context -> {
			assertSame(PacketType.CHAT_RECEIVE, context.envelope().packetType());
			return new ReplacePacketDecision(envelope(PacketType.PROTOCOL_RESPONSE));
		});

		PacketDecision chatReceivedDecision = chain.apply(context(PacketType.CHAT_RECEIVE));
		PacketDecision protocolRequestDecision = chain.apply(context(PacketType.PROTOCOL_REQUEST));

		ReplacePacketDecision replaceDecision = assertInstanceOf(ReplacePacketDecision.class, chatReceivedDecision);
		assertSame(PacketType.PROTOCOL_RESPONSE, replaceDecision.envelope().packetType());
		assertSame(ForwardPacketDecision.INSTANCE, protocolRequestDecision);
	}

	private static PacketInterceptionContext context(PacketType packetType) {
		return new PacketInterceptionContext(
				new DefaultPluginSessionContext("session-1", "127.0.0.1", false, false),
				envelope(packetType),
				"payload-" + packetType,
				PacketDirection.TO_SERVER
		);
	}

	private static PacketEnvelope envelope(PacketType packetType) {
		return new PacketEnvelope(
				packetType.id(),
				packetType,
				0,
				false,
				new byte[0],
				new byte[0],
				PacketDirection.TO_SERVER
		);
	}
}

