package greencity.enums;

public enum NotificationActionType {
    LIKE_CREATED("like.created"),
    LIKE_DELETED("like.deleted"),
    COMMENT_ADDED("comment.added"),
    COMMENT_DELETED("comment.deleted"),
    POST_SHARED("post.shared"),
    USER_FOLLOWED("user.followed"),
    USER_UNFOLLOWED("user.unfollowed"),
    FRIEND_REQUEST_SENT("friend.request.sent"),
    FRIEND_REQUEST_ACCEPTED("friend.request.accepted");

    private final String value;

    NotificationActionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationActionType fromValue(String value) {
        for (NotificationActionType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}

