package mattw.youtube.commentsuite.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Measures elapsed time from a given millis timestamp.
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public class ElapsedTime {

	private long start = 0;

	public ElapsedTime() {
		setNow();
	}

	public ElapsedTime(long startTime) {
		this.start = startTime;
	}

	public void setNow() {
		setAs(System.currentTimeMillis());
	}

	public void setAs(long startTime) {
		this.start = startTime;
	}
	
	public long getElapsedMillis() {
		return System.currentTimeMillis() - start;
	}
	
	public long getElapsedSeconds() {
		return getElapsedMillis() / 1000;
	}
	
	public String getElapsedString() {
		long time = getElapsedMillis();
		long ms   = time % 1000; time /= 1000;
		long s    = time % 60; time /= 60;
		long m    = time % 60; time /= 60;
		long h    = time % 24;

		return formatTimeString(h, m, s, ms);
	}

	public String formatTimeString(long hours, long minutes, long seconds, long millis) {
		List<String> parts = new ArrayList<>();
		if(hours > 0)   { parts.add(hours + "h"); }
		if(minutes > 0) { parts.add(minutes + "m"); }
		if(seconds > 0) { parts.add(seconds + "s"); }
		parts.add(millis + "ms");

		return String.join(" ", parts);
	}
}
