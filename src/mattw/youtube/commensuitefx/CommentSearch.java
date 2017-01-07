package mattw.youtube.commensuitefx;
import java.util.List;

public class CommentSearch {
	
	public int total_results;
	public List<Comment> results;
	
	public CommentSearch(int total_results, List<Comment> results) {
		this.total_results = total_results;
		this.results = results;
	}
	
}
