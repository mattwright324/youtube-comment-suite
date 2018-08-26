package mattw.youtube.commentsuite.io;

/**
 * Measures elapsed time from a given millis timestamp.
 *
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
		long ms = time % 1000; time /= 1000;
		long s = time % 60; time /= 60;
		long m = time % 60; time /= 60;
		long h = time % 24;

		return formatTimeString(h, m, s, ms);
	}

	public String formatTimeString(long hours, long minutes, long seconds, long millis) {
		String[] parts = new String[4];
		if(hours > 0) { parts[0] = hours + "h"; }
		if(minutes > 0) { parts[1] = minutes + "m"; }
		if(seconds > 0) { parts[2] = seconds + "s"; }
		parts[3] = millis + "ms";

		StringBuilder timeStr = new StringBuilder();
		for(String part : parts) {
			if(!timeStr.toString().isEmpty()) {
				timeStr.append(" ");
			}
			if(part != null) {
				timeStr.append(part);
			}
		}
		return timeStr.toString();
	}
}
