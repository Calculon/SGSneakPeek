package com.aus.sgsp;

import java.util.List;
import java.util.Map;

import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;


public class Api {

    private static final String API_URL = "http://api.seatgeek.com/2";

    // Retropsect kindly unpacks everything into these class definitions
    public static class Page {
        Meta meta;
        List<Event> events;

        public static class Meta {
            int page;
            int per_page;
            int total;

            public int nextPage() {
                if( total - (page * per_page) > 0) {
                    return page + 1;
                } else {
                    return -1;
                }
            }
        }

        public static class Event {
            String title;
            String short_title;
            String datetime_local;
            long id;
            List<Performer> performers;
            Map<String, Double> stats;
            double score;

            public static class Performer {
                Map<String, String> images;
            }
        }
    }

    interface SGService {

        @GET("/events")
        public Observable<Page> getFirstPage(@Query("lat") double lat, @Query("lon") double lon,
                                             @Query("range") String range);

        @GET("/events")
        public Observable<Page> getPage(@Query("lat") double lat, @Query("lon") double lon,
                                              @Query("range") String range, @Query("page") int page);

    }

    public static RestAdapter getAdapter() {
        return new RestAdapter.Builder().setEndpoint(API_URL).build();
    }

}
