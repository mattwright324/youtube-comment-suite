package mattw.youtube.commensuitefx;

public class Group {
	public int group_id;
	public String group_name;
	
	public boolean refreshing = false;
	
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
