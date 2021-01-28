package io.mattw.youtube.commentsuite.util;

public class IpApiProvider implements LocationProvider {

    @Override
    public String getRequestUrl(String ipv4) {
        return String.format("http://ip-api.com/json/%s",
                ipv4);
    }

    public static class Location {
        public String status;
        public String country;
        public String countryCode;
        public String region;
        public String regionName;
        public String city;
        public String zip;
        public String lat;
        public String lon;
        public String timezone;
        public String isp;
        public String org;
        public String as;
        public String query;
    }

}
