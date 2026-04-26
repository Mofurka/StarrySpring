package irden.space.proxy.plugin.player_manager.permissions.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StarryRoles {
    private String defaultAccount = "Neofit";
    private String ownerUuid = "58c329f6797ed81484c877727d2439c5";
    private List<StarryRole> accounts = List.of(new StarryRole());


    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StarryRole {
        private String name = "Neofit";
        private String colorPrefix = "^blue;";
        private int priority = 0;
        private List<String> inherits = List.of();
        private List<String> permissions = List.of("command.help");
    }
}
