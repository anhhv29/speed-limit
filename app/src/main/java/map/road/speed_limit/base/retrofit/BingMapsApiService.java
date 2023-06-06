package map.road.speed_limit.base.retrofit;

import map.road.speed_limit.base.response.SnapToRoadResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BingMapsApiService {
    @GET("REST/v1/Routes/SnapToRoad")
    Call<SnapToRoadResponse> snapToRoad(
            @Query("points") String points,
            @Query("IncludeSpeedLimit") boolean includeSpeedLimit,
            @Query("speedUnit") String speedUnit,
            @Query("key") String apiKey
    );
}
