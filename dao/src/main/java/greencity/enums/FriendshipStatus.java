package greencity.enums;

/**
 * Status of a friendship request. IMPORTANT: must match DB check constraint
 * values defined in Liquibase: ('PENDING','ACCEPTED','REJECTED','BLOCKED')
 */
public enum FriendshipStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    BLOCKED
}
