package mattw.youtube.commensuitefx;

class VideoGroup {
	
	public final int gitem_id;
	public final String video_id;
	
	public VideoGroup(int gitem_id, String video_id) {
		this.gitem_id = gitem_id;
		this.video_id = video_id;
	}
	
	
	public boolean equals(Object o) {
		if(o instanceof VideoGroup) {
			VideoGroup vg = (VideoGroup) o;
			return vg.gitem_id == gitem_id && vg.video_id.equals(video_id);
		}
		return false;
	}
	
	
}
