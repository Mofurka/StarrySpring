package irden.space.proxy.plugin.irden.integration.web.client.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LinkPlayerRequest(
        @NotBlank String uuid,
        @NotBlank String name,
        @NotBlank String secret
) {
}
