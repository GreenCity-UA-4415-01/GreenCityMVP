package greencity.repository;

public interface FriendCandidateProjection {
    Long getId();
    String getName();
    String getUsername();
    String getAvatar();       // може бути null, якщо у схемі інша колонка — ми мапимо як alias
    String getCity();         // так само
    Integer getPersonalRate();// так само
    Integer getMutualFriends();
    Boolean getRequestSent();
}
