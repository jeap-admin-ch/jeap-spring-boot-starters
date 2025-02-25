package ch.admin.bit.jeap.security.user;

public interface JeapCurrentUserCustomizer<T extends JeapCurrentUser> {

    T customize(JeapCurrentUser jeapCurrentUser);

}
