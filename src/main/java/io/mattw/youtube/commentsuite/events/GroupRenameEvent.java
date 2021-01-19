package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.db.Group;

public class GroupRenameEvent {

    private Group group;

    public GroupRenameEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

}
