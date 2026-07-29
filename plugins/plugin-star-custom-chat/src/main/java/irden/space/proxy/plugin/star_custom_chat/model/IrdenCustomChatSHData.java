package irden.space.proxy.plugin.star_custom_chat.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import irden.space.proxy.plugin.star_custom_chat.constants.ChatMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "mode",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(
                value = IrdenCustomChatProximityData.class,
                name = "Proximity"
        ),
        @JsonSubTypes.Type(
                value = IrdenCustomChatFightData.class,
                name = "Fight"
        )
})
public abstract class IrdenCustomChatSHData {
    private ChatMode mode;
    private Integer connection;
    private String nickname;
    private String text;
}