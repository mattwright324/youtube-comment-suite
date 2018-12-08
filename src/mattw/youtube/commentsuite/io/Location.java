package mattw.youtube.commentsuite.io;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Location<T extends LocationProvider, K> {

    public static String UA_W10_CHROME70 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36";

    private Logger logger = LogManager.getLogger(this);

    private Gson gson = new Gson();
    private T locationProvider;
    private Type dataType;

    /**
     * Default constructor uses the EurekaProvider
     */
    public Location(T locationProvider, Type dataType) {
        this.locationProvider = locationProvider;
        this.dataType = dataType;
    }

    public T getLocationProvider() {
        return locationProvider;
    }

    /**
     * Using the Amazon checkip service, returns your external address.
     * @return
     * @throws IOException
     */
    public String externalAddress() throws IOException {
        return Jsoup.connect("http://checkip.amazonaws.com").get().text();
    }

    public K getMyLocation() throws JsonSyntaxException, IOException {
        return getLocation(externalAddress());
    }

    public K getLocation(String ipv4) throws JsonSyntaxException, IOException {
        return getLocation(ipv4, UA_W10_CHROME70);
    }

    public K getLocation(String ipv4, String userAgent) throws JsonSyntaxException, IOException {
        Document result = Jsoup.connect(locationProvider.getRequestUrl(ipv4))
                .userAgent(userAgent)
                .ignoreContentType(true)
                .get();
        return gson.fromJson(result.text(), dataType);
    }

}
