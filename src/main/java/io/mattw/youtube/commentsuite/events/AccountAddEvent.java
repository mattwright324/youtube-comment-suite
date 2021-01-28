package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.oauth2.YouTubeAccount;

public class AccountAddEvent {

    private final YouTubeAccount account;

    public AccountAddEvent(final YouTubeAccount account) {
        this.account = account;
    }

    public YouTubeAccount getAccount() {
        return account;
    }

}
