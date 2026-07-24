package irden.space.proxy.plugin.site.web.rest.v1.online;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OnlinePlayerInfoDto(
        String uuid,

        @JsonProperty("raw_name")
        String rawName,

        String nickname,

        @JsonProperty("online_time")
        Long onlineTime
) {

}
