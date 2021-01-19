package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.db.Group;

public class GroupItemChangeEvent {

    private Group group;

    public GroupItemChangeEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

}
