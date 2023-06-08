package map.road.speed_limit;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import map.road.speed_limit.bubbles.BubbleLayout;
import map.road.speed_limit.bubbles.BubblesManager;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 0;
    public static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    TextView tvLat, tvLon, tvLocation, tvSpeedLimit, tvCurrentSpeed;
    ImageView btnSetting, btnBubble;
    private final BroadcastReceiver mBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String lat = intent.getStringExtra("lat");
            String lon = intent.getStringExtra("lon");
            String roadName = intent.getStringExtra("roadName");
            String currentSpeed = intent.getStringExtra("currentSpeed");
            String maxSpeed = intent.getStringExtra("maxSpeed");

            tvLat.setText(lat);
            tvLon.setText(lon);
            tvLocation.setText(roadName);
            tvCurrentSpeed.setText(currentSpeed);
            tvSpeedLimit.setText(maxSpeed);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Nếu chưa được cấp quyền, yêu cầu cấp quyền
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            //Yêu cầu cấp quyền hiển thị trên cùng
            if (!isLayoutOverlayPermissionGranted(MainActivity.this)) {
                grantLayoutOverlayPermission(MainActivity.this);
            }
        }
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

        IntentFilter filter = new IntentFilter("data");
        registerReceiver(mBroadcast, filter);

        btnSetting.setOnClickListener(v -> {
            startActivity(
                    new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null)
                    )
            );
        });

        btnBubble.setOnClickListener(v -> {
            if (isLayoutOverlayPermissionGranted(MainActivity.this)) {
                initializeBubblesManager();
//                Log.d("123123123","check 1");
            } else {
                grantLayoutOverlayPermission(MainActivity.this);
//                Log.d("123123123","check 2");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBroadcast != null) {
            unregisterReceiver(mBroadcast);
        }
    }

    private BubblesManager bubblesManager = null;

    private void initializeBubblesManager() {
        if (bubblesManager != null) {
            bubblesManager.recycle();
        }

        bubblesManager = new BubblesManager.Builder(MainActivity.this)
                .setTrashLayout(R.layout.bubble_trash)
                .setInitializationCallback(() -> addNewBubble())
                .build();
        bubblesManager.initialize();
    }

    private void addNewBubble() {
        @SuppressLint("InflateParams") BubbleLayout bubbleView2 = (BubbleLayout) LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.bubble_speed, null);

        bubbleView2.setShouldStickToWall(true);
        bubblesManager.addBubble(bubbleView2, 60, 60);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Cấp quyền hiển thị trên cùng
                if (!isLayoutOverlayPermissionGranted(MainActivity.this)) {
                    grantLayoutOverlayPermission(MainActivity.this);
                }
            } else {
                // permission denied, boo! Disable the functionality that depends on this permission.
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

    private boolean isLayoutOverlayPermissionGranted(Activity activity) {
        Log.v(TAG, "Granting Layout Overlay Permission..");
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(activity)) {
            Log.v(TAG, "Permission is denied");
            return false;
        } else {
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    private void grantLayoutOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(activity)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        }
    }
}