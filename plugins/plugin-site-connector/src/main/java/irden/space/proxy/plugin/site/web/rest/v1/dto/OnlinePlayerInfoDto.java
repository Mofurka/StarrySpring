package irden.space.proxy.plugin.site.web.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.ResponseBody;

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
