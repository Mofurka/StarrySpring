package irden.space.proxy.plugin.discord.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DiscordRoleManagerSelfCheck {

    private DiscordRoleManagerSelfCheck() {
    }

    @SuppressWarnings("unused")
    static void main(String[] args) throws IOException {
        resolvesSeveralServerRolesFromSingleDiscordRole();
        createsEmptyConfigWhenItDoesNotExist();
    }

    static void resolvesSeveralServerRolesFromSingleDiscordRole() throws IOException {
        Path tempDir = Files.createTempDirectory("discord-starryRole-manager-test");
        Path configPath = tempDir.resolve("discord-ranks.jsonc");
        Files.writeString(configPath, """
                [
                  {
                    // one discord starryRole can grant several server starryRoles
                    "name": "Host",
                    "roleId": 1506509194797252691
                  },
                  {
                    "name": "HostSupport",
                    "roleId": 1506509194797252691
                  },
                  {
                    "name": "GM",
                    "roleId": 42
                  }
                ]
                """);

        DiscordRoleManager roleManager = new DiscordRoleManager(configPath);

        List<String> expected = List.of("Host", "HostSupport", "GM");
        List<String> actual = roleManager.resolveServerRoleNames(List.of(1506509194797252691L, 42L, 42L));

        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    static void createsEmptyConfigWhenItDoesNotExist() throws IOException {
        Path tempDir = Files.createTempDirectory("discord-starryRole-manager-empty-test");
        Path configPath = tempDir.resolve("nested").resolve("discord-ranks.jsonc");

        DiscordRoleManager roleManager = new DiscordRoleManager(configPath);

        if (!Files.exists(configPath)) {
            throw new AssertionError("Expected config file to be created at " + configPath);
        }
        if (!roleManager.resolveServerRoleNames(List.of(1L)).isEmpty()) {
            throw new AssertionError("Expected no server starryRoles to resolve from an empty config");
        }
    }
}


