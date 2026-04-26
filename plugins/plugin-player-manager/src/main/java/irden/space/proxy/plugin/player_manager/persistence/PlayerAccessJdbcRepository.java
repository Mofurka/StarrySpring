package irden.space.proxy.plugin.player_manager.persistence;

import irden.space.proxy.plugin.player_manager.persistence.model.PlayerPermissionOverrideRecord;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRoleRecord;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class PlayerAccessJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PlayerRoleRowMapper playerRoleRowMapper = new PlayerRoleRowMapper();
    private final PlayerPermissionOverrideRowMapper playerPermissionOverrideRowMapper = new PlayerPermissionOverrideRowMapper();

    public PlayerAccessJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PlayerRoleRecord> findRolesByPlayerUuid(String playerUuid) {
        //noinspection SqlResolve
        return jdbcTemplate.query("""
                SELECT *
                FROM player_manager_player_roles
                WHERE player_uuid = ?
                ORDER BY assigned_at, role_name
                """, playerRoleRowMapper, playerUuid);
    }

    public void assignRole(String playerUuid, String roleName, String assignedBy) {
        //noinspection SqlResolve
        jdbcTemplate.update("""
                INSERT INTO player_manager_player_roles (
                    player_uuid,
                    role_name,
                    assigned_by,
                    assigned_at
                ) VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (player_uuid, role_name) DO UPDATE SET
                    assigned_by = EXCLUDED.assigned_by,
                    assigned_at = EXCLUDED.assigned_at
                """, playerUuid, roleName, assignedBy);
    }

    public void removeRole(String playerUuid, String roleName) {
        //noinspection SqlResolve
        jdbcTemplate.update("""
                DELETE FROM player_manager_player_roles
                WHERE player_uuid = ?
                  AND role_name = ?
                """, playerUuid, roleName);
    }

    public List<PlayerPermissionOverrideRecord> findPermissionOverridesByPlayerUuid(String playerUuid) {
        //noinspection SqlResolve
        return jdbcTemplate.query("""
                SELECT *
                FROM player_manager_player_permission_overrides
                WHERE player_uuid = ?
                ORDER BY changed_at, permission_name
                """, playerPermissionOverrideRowMapper, playerUuid);
    }

    public void savePermissionOverride(String playerUuid, String permissionName, boolean granted, String changedBy) {
        //noinspection SqlResolve
        jdbcTemplate.update("""
                INSERT INTO player_manager_player_permission_overrides (
                    player_uuid,
                    permission_name,
                    granted,
                    changed_by,
                    changed_at
                ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (player_uuid, permission_name) DO UPDATE SET
                    granted = EXCLUDED.granted,
                    changed_by = EXCLUDED.changed_by,
                    changed_at = EXCLUDED.changed_at
                """, playerUuid, permissionName, granted, changedBy);
    }

    public void deletePermissionOverride(String playerUuid, String permissionName) {
        //noinspection SqlResolve
        jdbcTemplate.update("""
                DELETE FROM player_manager_player_permission_overrides
                WHERE player_uuid = ?
                  AND permission_name = ?
                """, playerUuid, permissionName);
    }
}

