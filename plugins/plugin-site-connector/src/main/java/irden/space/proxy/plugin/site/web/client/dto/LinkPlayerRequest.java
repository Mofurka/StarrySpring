package irden.space.proxy.plugin.site.web.client.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LinkPlayerRequest(
        @NotBlank String uuid,
        @NotBlank String name,
        @NotBlank String secret
){
}
