package irden.space.proxy.plugin.star_custom_chat.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IrdenCustomChatProximityData extends IrdenCustomChatSHData {

    private Integer proximityRadius;
}