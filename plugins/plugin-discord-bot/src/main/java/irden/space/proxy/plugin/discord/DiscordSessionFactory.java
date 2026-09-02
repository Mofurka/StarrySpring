package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.Permissions;
import irden.space.proxy.plugin.discord.model.DiscordRoleManager;
import irden.space.proxy.plugin.player_manager.model.StarryRole;
import irden.space.proxy.plugin.player_manager.model.UserPermissions;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;


@Component
@RequiredArgsConstructor
public class DiscordSessionFactory {

    private static final Logger log = LoggerFactory.getLogger(DiscordSessionFactory.class);

    private final RoleManager roleManager;
    private final ObjectProvider<DiscordRoleManager> discordRoleManagerProvider;

    public DiscordSessionContext create(User user, Member member, Object event) {
        String userId = user.getId();
        String userName = user.getName();

        return new DiscordSessionContext(
                userId,
                userName,
                member != null ? member.getEffectiveName() : userName,
                resolvePermissions(userId, member),
                event
        );
    }

    private PermissionView resolvePermissions(String userId, Member member) {
        LinkedHashSet<String> resolvedRoleNames = new LinkedHashSet<>();

        String defaultRoleName = roleManager.defaultRoleName();
        if (defaultRoleName != null && !defaultRoleName.isBlank()) {
            resolvedRoleNames.add(defaultRoleName);
        }

        if (member != null) {
            resolvedRoleNames.addAll(
                    discordRoleManagerProvider.getObject().resolveServerRoleNames(
                            member.getRoles().stream()
                                    .map(Role::getIdLong)
                                    .toList()
                    )
            );
        }

        if (resolvedRoleNames.isEmpty()) {
            return PermissionView.EMPTY;
        }

        List<StarryRole> grantedStarryRoles = new ArrayList<>();
        List<String> unknownRoleNames = new ArrayList<>();

        for (String roleName : resolvedRoleNames) {
            roleManager.findRole(roleName)
                    .ifPresentOrElse(
                            grantedStarryRoles::add,
                            () -> unknownRoleNames.add(roleName)
                    );
        }

        if (!unknownRoleNames.isEmpty()) {
            log.warn("Discord user {} matched unknown server roles from config: {}", userId, unknownRoleNames);
        }

        if (grantedStarryRoles.isEmpty()) {
            return PermissionView.EMPTY;
        }

        return new UserPermissions(grantedStarryRoles, Permissions.none());
    }
}
