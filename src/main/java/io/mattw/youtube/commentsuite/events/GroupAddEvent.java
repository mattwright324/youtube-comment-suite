package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.db.Group;

import java.util.List;

public class GroupAddEvent {

    private final List<Group> groups;

    public GroupAddEvent(List<Group> groups) {
        this.groups = groups;
    }

    public List<Group> getGroups() {
        return groups;
    }

}
