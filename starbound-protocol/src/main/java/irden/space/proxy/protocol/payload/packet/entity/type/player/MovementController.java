package irden.space.proxy.protocol.payload.packet.entity.type.player;

import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.common.star_poly.StarPolyF;
import lombok.Builder;

import java.util.Optional;

@Builder
public record MovementController(
        StarPolyF collisionPoly,
        Float mass,
        Float xPosition,
        Float yPosition,
        Float xVelocity,
        Float yVelocity,
        Float rotation,
        Boolean colliding,
        Boolean collisionStuck,
        Boolean nullColliding,
        Optional<Float> stickingDirection,
        Boolean onGround,
        Boolean zeroG,
        Optional<StarPair<Integer, Integer>> surfaceMovingCollision,
        Float xRelativeSurfaceMovingCollisionPosition,
        Float yRelativeSurfaceMovingCollisionPosition
) {
}
