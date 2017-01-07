package mattw.youtube.commensuitefx;

public class ElapsedTime {
	
	public long start = 0;
	
	public void set() {
		start = System.currentTimeMillis();
	}
	
	public long getTime() {
		return System.currentTimeMillis() - start;
	}
	
	public long getSeconds() {
		return getTime() / 1000;
	}
	
	public String getTimeString() {
		long time = getTime();
		long ms = 0;
		long s = 0;
		long m = 0;
		long h = 0;
		
		ms = time % 1000; time /= 1000;
		s = time % 60; time /= 60;
		m = time % 60; time /= 60;
		h = time % 24; time /= 24;
		
		String string = (h > 0 ? h+"h ":"")+(m > 0 ? m+"m ":"")+(s > 0 ? s+"s ":"")+(ms > 0 ? ms+"ms":"");
		return string;
	}
}
