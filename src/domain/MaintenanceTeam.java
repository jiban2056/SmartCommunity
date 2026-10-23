package domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a maintenance team and its members.
 * Membership changes are authorised by the application layer.
 */
public final class MaintenanceTeam implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final String teamId;
    private final String name;
    private final Set<String> memberAccountIds = new HashSet<>();

    public MaintenanceTeam(String teamId, String name) {
        if (teamId == null || teamId.isBlank()) {
            throw new IllegalArgumentException(
                    "Team ID must not be blank.");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Team name must not be blank.");
        }

        this.teamId = teamId.trim();
        this.name = name.trim();
    }

    public String getTeamId() {
        return teamId;
    }

    public String getName() {
        return name;
    }

    public Set<String> getMemberAccountIds() {
        return Set.copyOf(memberAccountIds);
    }

    public void addMember(UserAccount account) {
        if (account == null) {
            throw new IllegalArgumentException(
                    "Member account is required.");
        }

        if (!account.isVerified()
                || !account.hasRole(Role.MAINTENANCE_STAFF)) {
            throw new IllegalArgumentException(
                    "Team members must be verified maintenance staff.");
        }

        memberAccountIds.add(account.getAccountId());
    }

    public boolean hasMember(UserAccount account) {
        return account != null
                && memberAccountIds.contains(account.getAccountId());
    }
}