package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.db.Group;

public class GroupDeleteEvent {

    private Group group;

    public GroupDeleteEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

}
