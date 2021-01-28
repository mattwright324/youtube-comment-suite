package io.mattw.youtube.commentsuite.util;

public class StringTuple {

    private final String first;
    private final String second;

    public StringTuple(final String first, final String second) {
        this.first = first;
        this.second = second;
    }

    public String getFirst() {
        return first;
    }

    public String getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "StringTuple{" +
                "first='" + first + '\'' +
                ", second='" + second + '\'' +
                '}';
    }
}
