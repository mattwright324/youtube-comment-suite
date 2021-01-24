package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.db.Group;

public class GroupItemAddEvent {

    private Group group;

    public GroupItemAddEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

}
