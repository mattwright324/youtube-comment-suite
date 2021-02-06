package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.db.Group;

import java.util.List;

public class GroupDeleteEvent {

    private List<Group> groups;

    public GroupDeleteEvent(List<Group> groups) {
        this.groups = groups;
    }

    public List<Group> getGroups() {
        return groups;
    }

}
