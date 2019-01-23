package mattw.youtube.commentsuite.io;

/**
 * @param <K> first element type in tuple
 * @param <V> second element type in tuple
 */
public class Tuple<K,V> {
    private final K first;
    private final V second;

    public Tuple(K first, V second) {
        this.first = first;
        this.second = second;
    }

    public K getFirst() {
        return first;
    }

    public V getSecond() {
        return second;
    }
}
