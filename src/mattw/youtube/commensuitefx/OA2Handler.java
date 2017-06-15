package mattw.youtube.commensuitefx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mattw.youtube.datav3.list.CommentsList;

public class OA2Handler {
	
	private final static Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL).create();
	private final static String client_id = "972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com";
	private final static String client_secret = "QuTdoA-KArupKMWwDrrxOcoS";
	private final static String redirect_uri = "urn:ietf:wg:oauth:2.0:oob";
	
	public static void postNewReply(String parentId, String textOriginal, Account account) {
		try {
			Object response;
			boolean tryagain = false;
			do {
				response = postReply(parentId, textOriginal, account.getTokens());
				if(response instanceof GlobalDomainError) {
					GlobalDomainError gde = (GlobalDomainError) response;
					for(GlobalDomainError.GlobalError.Error error : gde.error.errors) {
						System.out.println("GlobalDomainError "+gde.error.code+": "+error.message);
					}
					if(gde.error.code == 401) {
						System.out.println("Refreshing tokens and trying again.");
						account.refreshTokens();
						tryagain = true;
					}
				} else if(response instanceof CommentsList.Item) {
					
				}
			} while(response instanceof GlobalDomainError && tryagain);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private static Object postReply(String parentId, String textOriginal, OA2Tokens tokens) throws IOException {
		System.out.println("Replying to ["+parentId+"]:    "+textOriginal);
		String payload = new Gson().toJson(new MakeReply(parentId, textOriginal), MakeReply.class);
		HttpURLConnection url = (HttpURLConnection) new URL("https://www.googleapis.com/youtube/v3/comments?part=snippet&access_token="+tokens.access_token).openConnection();
		System.out.println("    "+url.getURL().toString());
		url.setDoOutput(true);
		url.setDoInput(true);
		url.setRequestProperty("Content-Type", "application/json");
		OutputStream os = url.getOutputStream();
		os.write(payload.getBytes("UTF-8"));
		StringBuilder response = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(url.getInputStream()));
			String line;
			while((line = br.readLine()) != null) {
				response.append(line);}
			return gson.fromJson(response.toString(), CommentsList.Item.class);
		} catch (IOException e) {
			BufferedReader br = new BufferedReader(new InputStreamReader(url.getErrorStream()));
			String line;
			while((line = br.readLine()) != null) {
				response.append(line);}
			return gson.fromJson(response.toString(), GlobalDomainError.class);
		}
	}
	
	static class MakeReply {
		public MakeReply(String parentId, String textOriginal) {
			snippet = new Snippet();
			snippet.parentId = parentId;
			snippet.textOriginal = textOriginal;
		}
		public final Snippet snippet;
		public class Snippet {
			public String parentId;
			public String textOriginal;
		}
	}
	
	static class GlobalDomainError {
		public GlobalError error;
		public class GlobalError {
			public Error[] errors;
			public class Error {
				public String domain;
				public String reason;
				public String message;
				public String locationType;
				public String location;
			}
			public int code;
			public String message;
		}
	}
	
	public static String getOAuth2Url() throws UnsupportedEncodingException {
		return "https://accounts.google.com/o/oauth2/auth?"
				+ "client_id="+client_id
				+ "&redirect_uri="+URLEncoder.encode(redirect_uri, "UTF-8")
				+ "&response_type=code"
				+ "&scope="+URLEncoder.encode("https://www.googleapis.com/auth/youtube.force-ssl", "UTF-8");
	}
	
	public static OA2Tokens getAccessTokens(String code) throws IOException {
		Document doc = Jsoup.connect("https://accounts.google.com/o/oauth2/token")
				.ignoreContentType(true)
				.data("code", code)
				.data("client_id", client_id)
				.data("client_secret", client_secret)
				.data("redirect_uri", redirect_uri)
				.data("grant_type", "authorization_code")
				.post();
		return gson.fromJson(doc.text(), OA2Tokens.class);
	}
	
	public static OA2Tokens refreshAccessTokens(OA2Tokens old_tokens) throws IOException {
		Document doc = Jsoup.connect("https://accounts.google.com/o/oauth2/token")
				.ignoreContentType(true)
				.data("client_id", client_id)
				.data("client_secret", client_secret)
				.data("refresh_token", old_tokens.refresh_token)
				.data("grant_type", "refresh_token")
				.post();
		OA2Tokens new_tokens = gson.fromJson(doc.text(), OA2Tokens.class);
		new_tokens.setRefreshToken(old_tokens.refresh_token);
		System.out.println(old_tokens.access_token+" -> "+new_tokens.access_token);
		return new_tokens;
	}
	
	
	/* 
	 * Replies are the most important, may implement commenting to videos later.
	 * 
	public static void postNewComment(String channelId, String videoId, String textOriginal) {
		try {
			Object response;
			boolean tryagain = false;
			do {
				response = postComment(channelId, videoId, textOriginal);
				if(response instanceof GlobalDomainError) {
					GlobalDomainError gde = (GlobalDomainError) response;
					for(GlobalDomainError.GlobalError.Error error : gde.error.errors) {
						System.out.println("GlobalDomainError "+gde.error.code+": "+error.message);
					}
					if(gde.error.code == 401) {
						System.out.println("Refreshing tokens and trying again.");
						CommentSuiteFX.instance.refreshTokens();
						tryagain = true;
					}
				} else if(response instanceof CommentThreadsList.Item) {
					
				}
			} while(response instanceof GlobalDomainError && tryagain);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private static Object postComment(String channelId, String videoId, String textOriginal) throws IOException {
		System.out.println("Commenting on ["+videoId+", "+channelId+"]:    "+textOriginal);
		String payload = new Gson().toJson(new MakeComment(channelId, videoId, textOriginal), MakeComment.class);
		HttpURLConnection url = (HttpURLConnection) new URL("https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&access_token="+CommentSuiteFX.instance.config.getAccessTokens().access_token).openConnection();
		url.setDoOutput(true);
		url.setDoInput(true);
		url.setRequestProperty("Content-Type", "application/json");
		OutputStream os = url.getOutputStream();
		os.write(payload.getBytes("UTF-8"));
		String response = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(url.getInputStream()));
			String line;
			while((line = br.readLine()) != null) {response+=line;}
			return gson.fromJson(response, CommentThreadsList.Item.class);
		} catch (IOException e) {
			BufferedReader br = new BufferedReader(new InputStreamReader(url.getErrorStream()));
			String line;
			while((line = br.readLine()) != null) {response+=line;}
			return gson.fromJson(response, GlobalDomainError.class);
		}
	}
	
	static class MakeComment {
		public MakeComment(String channel_id, String textOriginal) {
			snippet.channelId = channel_id;
			snippet.topLevelComment.snippet.textOriginal = textOriginal;
		}
		public MakeComment(String channel_id, String videoId, String textOriginal) {
			this(channel_id, textOriginal);
			snippet.videoId = videoId;
		}
		public Snippet snippet = new Snippet();
		public class Snippet {
			public String channelId;
			public String videoId;
			public TopLevelComment topLevelComment = new TopLevelComment();
			public class TopLevelComment {
				public TLCSnippet snippet = new TLCSnippet();
				public class TLCSnippet {
					public String textOriginal;
				}
			}
		}
	}*/
	
}
