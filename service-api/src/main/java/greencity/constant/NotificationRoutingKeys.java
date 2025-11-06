package greencity.constant;

public final class NotificationRoutingKeys {
    // News-related routing keys
    public static final String NEWS_LIKE_CREATED = "news.like.created";
    public static final String NEWS_LIKE_DELETED = "news.like.deleted";
    public static final String NEWS_COMMENT_ADDED = "news.comment.added";
    public static final String NEWS_COMMENT_DELETED = "news.comment.deleted";
    public static final String NEWS_SHARED = "news.shared";

    // Comment-related routing keys
    public static final String COMMENT_ADDED = "comment.added";
    public static final String COMMENT_DELETED = "comment.deleted";
    public static final String COMMENT_LIKE_CREATED = "comment.like.created";
    public static final String COMMENT_LIKE_DELETED = "comment.like.deleted";

    // Post-related routing keys
    public static final String POST_SHARED = "post.shared";
    public static final String POST_LIKE_CREATED = "post.like.created";
    public static final String POST_LIKE_DELETED = "post.like.deleted";

    // User-related routing keys
    public static final String USER_FOLLOWED = "user.followed";
    public static final String USER_UNFOLLOWED = "user.unfollowed";

    // Friend request routing keys
    public static final String FRIEND_REQUEST_SENT = "friend_request.sent";
    public static final String FRIEND_REQUEST_ACCEPTED = "friend_request.accepted";

    // Event-related routing keys
    public static final String EVENT_CREATED = "event.created";
    public static final String EVENT_UPDATED = "event.updated";
    public static final String EVENT_ATTENDANCE_ADDED = "event.attendance.added";
    public static final String EVENT_ATTENDANCE_REMOVED = "event.attendance.removed";

    // Habit-related routing keys
    public static final String HABIT_ASSIGNED = "habit.assigned";
    public static final String HABIT_ACQUIRED = "habit.acquired";

    public static final String NOTIFICATIONS_EXCHANGE = "notifications.events";

    public static final String NOTIFICATIONS_WRITE_QUEUE = "notifications.write";

    private NotificationRoutingKeys() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String buildRoutingKey(String objectType, String actionType) {
        return objectType + "." + actionType;
    }
}

