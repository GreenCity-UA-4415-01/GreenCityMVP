package greencity.enums;

public enum NotificationObjectType {
    NEWS("news"),
    COMMENT("comment"),
    POST("post"),
    USER("user"),
    EVENT("event"),
    HABIT("habit"),
    FRIEND_REQUEST("friend_request");

    private final String value;

    NotificationObjectType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationObjectType fromValue(String value) {
        for (NotificationObjectType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}

