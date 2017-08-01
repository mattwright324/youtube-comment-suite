package mattw.youtube.commentsuitefx;

public class Group {
	public final int group_id;
	public final String group_name;
	
	private boolean refreshing = false;
	
	public Group(int group_id, String group_name) {
		this.group_id = group_id;
		this.group_name = group_name;
	}
	
	public String toString() {
		return group_name;
	}
	
	public void setRefreshing(boolean is) {
		refreshing = is;
	}
	
	public boolean isRefreshing() {
		return refreshing;
	}
	
}
