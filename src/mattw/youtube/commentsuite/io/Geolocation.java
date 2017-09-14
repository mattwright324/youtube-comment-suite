package mattw.youtube.commentsuite.io;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Geolocation {

    public static Gson gson = new Gson();
    public static String useragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";

    public static String externalAddress() throws IOException {
        return Jsoup.connect("http://checkip.amazonaws.com").get().text();
    }

    public static Location getMyLocation() throws JsonSyntaxException, IOException {
        return getLocation(externalAddress());
    }

    public static Location getLocation(String ipv4) throws JsonSyntaxException, IOException {
        Document result = Jsoup.connect("http://api.eurekapi.com/iplocation/v1.8/locateip?key=SAKF9WKS23364926J8NZ&ip="+ipv4+"&format=JSON")
                .userAgent(useragent)
                .ignoreContentType(true)
                .get();
        return gson.fromJson(result.text(), Location.class);
    }

    public static class Location {
        public Status query_status;
        public String ip_address;
        public Data geolocation_data;

        public String getContinentL() {
            try {
                return this.geolocation_data.continent_name;
            } catch (NullPointerException e) {
                return "null";
            }
        }

        public String getContinentS() {
            try {
                return this.geolocation_data.continent_code;
            } catch (NullPointerException e) {
                return "null";
            }
        }

        public String getCountryL() {
            try {
                return this.geolocation_data.country_name;
            } catch (NullPointerException e) {
                return "null";
            }
        }

        public String getCountryS() {
            try {
                return this.geolocation_data.country_code_iso3166alpha3;
            } catch (NullPointerException e) {
                return "null";
            }
        }

        public String getCity_State() {
            try {
                return this.geolocation_data.city + ", " + this.geolocation_data.region_name;
            } catch (NullPointerException e) {
                return "null";
            }
        }

        public String getServiceProvider() {
            try {
                return this.geolocation_data.isp;
            } catch (NullPointerException e) {
                return "null";
            }
        }

        public class Data {
            public String continent_code;
            public String continent_name;
            public String country_code_iso3166alpha2;
            public String country_code_iso3166alpha3;
            public String country_code_iso3166numeric;
            public String country_name;
            public String region_code;
            public String region_name;
            public String city;
            public String postal_code;
            public String metro_code;
            public String area_code;
            public double latitude;
            public double longitude;
            public String isp;
            public String organization;
        }

        public class Status {
            public String query_status_code;
            public String query_status_description;

            public Status() {}
        }
    }
}
