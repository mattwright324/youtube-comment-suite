package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.db.Group;

public class GroupItemDeleteEvent {

    private final Group group;

    public GroupItemDeleteEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

}
