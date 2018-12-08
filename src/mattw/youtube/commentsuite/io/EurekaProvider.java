package mattw.youtube.commentsuite.io;

public class EurekaProvider implements LocationProvider {

    private String format = "JSON";
    private String apiKey = "SAKF9WKS23364926J8NZ";

    @Override
    public String getRequestUrl(String ipv4) {
        return String.format("http://api.eurekapi.com/iplocation/v1.8/locateip?key=%s&ip=%s&format=%s",
                apiKey,
                ipv4,
                format);
    }

    /**
     * @param apiKey Valid API key for the service.
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * @param format JSON or XML
     */
    public void setFormat(String format) {
        this.format = format;
    }

    public class Location {
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
