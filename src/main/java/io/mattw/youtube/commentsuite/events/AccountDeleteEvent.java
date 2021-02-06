package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.oauth2.YouTubeAccount;

public class AccountDeleteEvent {

    private final YouTubeAccount account;

    public AccountDeleteEvent(final YouTubeAccount account) {
        this.account = account;
    }

    public YouTubeAccount getAccount() {
        return account;
    }

}
