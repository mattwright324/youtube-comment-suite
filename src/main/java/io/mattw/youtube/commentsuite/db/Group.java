package io.mattw.youtube.commentsuite.db;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Group {

    private final String groupId;
    private String name;

    /**
     * Used when creating a new group.
     */
    public Group(String name) {
        this.name = name;
        this.groupId = generateId();
    }

    /**
     * Use for database init.
     */
    public Group(String groupId, String name) {
        this.groupId = groupId;
        this.name = name;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public int hashCode() {
        return groupId.hashCode();
    }

    public boolean equals(Object o) {
        return o instanceof Group && o.hashCode() == hashCode();
    }

    private String generateId() {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(StandardCharsets.UTF_8.encode(System.nanoTime() + this.name));
            return String.format("%032x", new BigInteger(1, md5.digest()));
        } catch (Exception e) {
            e.printStackTrace();
            return String.valueOf(System.nanoTime());
        }
    }
}
