package mattw.youtube.commensuitefx;
import java.util.List;

public class CommentSearch {
	
	public int total_results;
	public List<Comment> results;
	
	/* TODO
	 * 
	 * Traverse all results with by pages. 
	 * ResultSet first() returns back to the start
	 * 
	 * page_count = (int) (total_results / 1000.0) + 1
	 * 2500 = 2.5 = page 1 (1000), 2 (1000), 3 (500)
	 * 
	 * public int getPageCount()
	 * public 
	 * 
	 */
	
	public CommentSearch(int total_results, List<Comment> results) {
		this.total_results = total_results;
		this.results = results;
	}
	
}
