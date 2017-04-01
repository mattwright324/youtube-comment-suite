package mattw.youtube.commensuitefx;

class ElapsedTime {
	
	private long start = 0;
	
	public void set() {
		start = System.currentTimeMillis();
	}
	
	private long getTime() {
		return System.currentTimeMillis() - start;
	}
	
	public long getSeconds() {
		return getTime() / 1000;
	}
	
	public String getTimeString() {
		long time = getTime();
		long ms;
		long s;
		long m;
		long h;
		
		ms = time % 1000; time /= 1000;
		s = time % 60; time /= 60;
		m = time % 60; time /= 60;
		h = time % 24;

		return (h > 0 ? h+"h ":"")+(m > 0 ? m+"m ":"")+(s > 0 ? s+"s ":"")+(ms > 0 ? ms+"ms":"");
	}
}
