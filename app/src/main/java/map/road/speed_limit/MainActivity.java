package map.road.speed_limit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import map.road.speed_limit.base.response.SnapToRoadResponse;
import map.road.speed_limit.base.retrofit.BingMapsApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 0;

    TextView tvLat, tvLon, tvLocation, tvSpeedLimit, tvCurrentSpeed;

    ImageView btnSetting, btnBubble;
    String Lat, Lon;

    @Override
    protected void onResume() {

        checkLocation();
        super.onResume();
    }

    @SuppressLint({"MissingInflatedId", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLat = findViewById(R.id.tvLat);
        tvLon = findViewById(R.id.tvLon);
        tvLocation = findViewById(R.id.tvLocation);
        tvSpeedLimit = findViewById(R.id.tvSpeedLimit);
        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);

        btnSetting = findViewById(R.id.btnSetting);
        btnBubble = findViewById(R.id.btnBubble);

        btnSetting.setOnClickListener(v -> {
            startActivity(
                    new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null)
                    )
            );
        });
    }

    @SuppressLint("SetTextI18n")
    private void checkLocation() {
        // Kiểm tra quyền truy cập vị trí
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Nếu chưa được cấp quyền, yêu cầu cấp quyền
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);

        } else {
            // Nếu đã được cấp quyền, lấy vị trí hiện tại
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (location != null) {

                LocationListener mLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        float speed = location.getSpeed() * 3.6f;
                        int mSpeed = Math.round(speed);

                        // Sử dụng latitude và longitude ở đây

                        Lat = Double.toString(latitude);
                        Lon = Double.toString(longitude);

                        resultLocation();

                        Log.d("location", "lat: " + Lat);
                        Log.d("location", "lon: " + Lon);

                        tvLat.setText("Latitude (Vĩ Độ): \n" + Lat);
                        tvLon.setText("Longitude (Kinh Độ): \n" + Lon);
                        tvCurrentSpeed.setText(mSpeed + " Km/h");
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                };
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 500, 10, mLocationListener);
            }
        }
    }

    private void resultLocation() {
        String points = Lat + "," + Lon;
        boolean includeSpeedLimit = true;
        String speedUnit = "MPH";
        String apiKey = "Aj5iq3vsLgF8YigYxuY0nqdU807N700gG7ehvcWeeJbJz5-wTHtgrX7D3Bp3elrl";

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://dev.virtualearth.net/").addConverterFactory(GsonConverterFactory.create()).build();

        BingMapsApiService apiService = retrofit.create(BingMapsApiService.class);

        Call<SnapToRoadResponse> call = apiService.snapToRoad(points, includeSpeedLimit, speedUnit, apiKey);
        call.enqueue(new Callback<SnapToRoadResponse>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<SnapToRoadResponse> call, @NonNull Response<SnapToRoadResponse> response) {
                // Xử lý kết quả trả về
                Log.e("check", "success");
//                Toast.makeText(MainActivity.this, "Gọi API thành công", Toast.LENGTH_SHORT).show();

                assert response.body() != null;
                String name = response.body().getResourceSets().get(0).getResources().get(0).getSnappedPoints().get(0).getName();
                int speedLimit = response.body().getResourceSets().get(0).getResources().get(0).getSnappedPoints().get(0).getSpeedLimit();

                tvLocation.setText(name);

                float speedLimitKmh = speedLimit * 1.60934f;
                int mSpeedLimit = Math.round(speedLimitKmh);
                tvSpeedLimit.setText(mSpeedLimit + " Km/h");
                Log.e("result", name + " " + speedLimit);
            }

            @Override
            public void onFailure(@NonNull Call<SnapToRoadResponse> call, Throwable t) {
                // Xử lý lỗi
                Log.e("check", "error");
                Toast.makeText(MainActivity.this, "Có lỗi xảy ra vui lòng thử lại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocation();
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(MainActivity.this, "Vui lòng cấp quyền vị trí đế sử dụng ứng dụng", Toast.LENGTH_SHORT).show();
                startActivity(
                        new Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null)
                        )
                );
            }
        }
    }
}