package mattw.youtube.commensuitefx;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class OA2Handler {
	
	final static Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL).create();
	final static String client_id = "972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com";
	final static String client_secret = "QuTdoA-KArupKMWwDrrxOcoS";
	
	public static String getOAuth2Url() throws UnsupportedEncodingException {
		return "https://accounts.google.com/o/oauth2/auth?"
				+ "client_id="+client_id
				+ "&redirect_uri="+URLEncoder.encode("urn:ietf:wg:oauth:2.0:oob", "UTF-8")
				+ "&response_type=code"
				+ "&scope="+URLEncoder.encode("https://www.googleapis.com/auth/youtube.force-ssl", "UTF-8");
	}
	
	public static OA2Tokens getAccessTokens(String code) throws IOException {
		Document doc = Jsoup.connect("https://accounts.google.com/o/oauth2/token")
				.ignoreContentType(true)
				.data("code", code)
				.data("client_id", "972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com")
				.data("client_secret", "QuTdoA-KArupKMWwDrrxOcoS")
				.data("redirect_uri", "urn:ietf:wg:oauth:2.0:oob")
				.data("grant_type", "authorization_code")
				.post();
		return gson.fromJson(doc.text(), OA2Tokens.class);
	}
	
	public static OA2Tokens refreshAccessTokens(OA2Tokens old_tokens) throws IOException {
		Document doc = Jsoup.connect("https://accounts.google.com/o/oauth2/token")
				.ignoreContentType(true)
				.data("client_id", "972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com")
				.data("client_secret", "QuTdoA-KArupKMWwDrrxOcoS")
				.data("refresh_token", old_tokens.refresh_token)
				.data("grant_type", "refresh_token")
				.post();
		OA2Tokens new_tokens = gson.fromJson(doc.text(), OA2Tokens.class);
		return new_tokens;
	}
	
}
