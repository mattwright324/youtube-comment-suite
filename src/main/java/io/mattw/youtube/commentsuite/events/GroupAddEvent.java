package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.db.Group;

public class GroupAddEvent {

    private Group group;

    public GroupAddEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

}
