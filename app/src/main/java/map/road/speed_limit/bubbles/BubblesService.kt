package map.road.speed_limit.bubbles

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import map.road.speed_limit.R
import map.road.speed_limit.base.response.SnapToRoadResponse
import map.road.speed_limit.base.retrofit.BingMapsApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class BubblesService : Service() {
    private val binder: BubblesServiceBinder = BubblesServiceBinder()
    private val bubbles: MutableList<BubbleLayout> = ArrayList()
    private var bubblesTrash: BubbleTrashLayout? = null
    private var windowManager: WindowManager? = null
    private var layoutCoordinator: BubblesLayoutCoordinator? = null
    private var mLocationManager: LocationManager? = null
    private val LOCATION_REFRESH_TIME = 500L // 1 seconds to update
    private val LOCATION_REFRESH_DISTANCE = 10f // 50 meters to update
    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        for (bubble in bubbles) {
            recycleBubble(bubble)
        }
        bubbles.clear()
        mLocationManager = null
        return super.onUnbind(intent)
    }

    private fun recycleBubble(bubble: BubbleLayout) {
        Handler(Looper.getMainLooper()).post {
            getWindowManager()!!.removeView(bubble)
            for (cachedBubble in bubbles) {
                if (cachedBubble === bubble) {
                    bubble.notifyBubbleRemoved()
                    bubbles.remove(cachedBubble)
                    break
                }
            }
        }
    }

    private fun getWindowManager(): WindowManager? {
        if (windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }
        return windowManager
    }

    @SuppressLint("SetTextI18n")
    fun addBubble(bubble: BubbleLayout, x: Int, y: Int) {
        val layoutParams = buildLayoutParamsForBubble(x, y)
        bubble.windowManager = getWindowManager()
        bubble.viewParams = layoutParams
        bubble.layoutCoordinator = layoutCoordinator
        bubbles.add(bubble)
        addViewToWindow(bubble)
        val limitLabelText = bubble.findViewById<TextView>(R.id.limit_label_text)
        val limitText = bubble.findViewById<TextView>(R.id.limit_text)
        val speed = bubble.findViewById<TextView>(R.id.speed)
        val limit = bubble.findViewById<CardView>(R.id.limit)
        val mSpeedUnits = bubble.findViewById<TextView>(R.id.mSpeedUnits)
        val speedUnits = bubble.findViewById<TextView>(R.id.speedUnits)
        getApiRoadInfo(limitLabelText, limitText, speed, limit, mSpeedUnits, speedUnits)
    }

    fun addTrash(trashLayoutResourceId: Int) {
        if (trashLayoutResourceId != 0) {
            bubblesTrash = BubbleTrashLayout(this)
            bubblesTrash?.windowManager = windowManager
            bubblesTrash?.viewParams = buildLayoutParamsForTrash()
            bubblesTrash?.visibility = View.GONE
            LayoutInflater.from(this).inflate(trashLayoutResourceId, bubblesTrash, true)
            addViewToWindow(bubblesTrash!!)
            initializeLayoutCoordinator()
        }
    }

    private fun initializeLayoutCoordinator() {
        layoutCoordinator =
            BubblesLayoutCoordinator.Builder(this).setWindowManager(getWindowManager())
                .setTrashView(bubblesTrash).build()
    }

    private fun addViewToWindow(view: BubbleBaseLayout) {
        Handler(Looper.getMainLooper()).post { getWindowManager()!!.addView(view, view.viewParams) }
    }

    private fun buildLayoutParamsForBubble(x: Int, y: Int): WindowManager.LayoutParams {
        val layoutParams: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            332,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParams,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = x
        params.y = y
        return params
    }

    private fun buildLayoutParamsForTrash(): WindowManager.LayoutParams {
        val layoutParams: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val x = 0
        val y = 0
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutParams,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        )
        params.x = x
        params.y = y
        return params
    }

    fun removeBubble(bubble: BubbleLayout) {
        recycleBubble(bubble)
    }

    inner class BubblesServiceBinder : Binder() {
        val service: BubblesService
            get() = this@BubblesService
    }

    private fun getApiRoadInfo(
        limitLabelText: TextView,
        limitText: TextView,
        speed: TextView,
        limit: CardView,
        mSpeedUnits: TextView,
        speedUnits: TextView
    ) {
        try {

            if (mLocationManager == null) {
                mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            }

            val isGPSEnabled = mLocationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                mLocationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            val isFusedEnabled =
                mLocationManager!!.isProviderEnabled(LocationManager.FUSED_PROVIDER)
            val isPassiveEnabled =
                mLocationManager!!.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
            var provider = ""
            if (isFusedEnabled) provider =
                LocationManager.FUSED_PROVIDER else if (isGPSEnabled) provider =
                LocationManager.GPS_PROVIDER else if (isNetworkEnabled) provider =
                LocationManager.NETWORK_PROVIDER else if (isPassiveEnabled) provider =
                LocationManager.PASSIVE_PROVIDER
            if (provider.isNotEmpty()) {
                if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    limitLabelText.text = "GPS?"
                    limitText.text = "--"
                    speed.text = "--"
                    Toast.makeText(
                        this@BubblesService, "Kiểm tra GPS", Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                var maxSpeed: Int
                mLocationManager?.requestLocationUpdates(
                    provider, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE
                ) { location ->
                    val lat = location.latitude.toString()
                    val lon = location.longitude.toString()

                    val points = "${location.latitude},${location.longitude}"
                    val includeSpeedLimit = true
                    val speedUnit = "MPH"
                    val apiKey = "Aj5iq3vsLgF8YigYxuY0nqdU807N700gG7ehvcWeeJbJz5-wTHtgrX7D3Bp3elrl"

                    val retrofit = Retrofit.Builder().baseUrl("https://dev.virtualearth.net/")
                        .addConverterFactory(GsonConverterFactory.create()).build()

                    val apiService = retrofit.create(BingMapsApiService::class.java)
                    apiService.snapToRoad(points, includeSpeedLimit, speedUnit, apiKey)
                        .enqueue(object : Callback<SnapToRoadResponse> {
                            override fun onResponse(
                                call: Call<SnapToRoadResponse>,
                                response: Response<SnapToRoadResponse>
                            ) {
                                val roadName = response.body()!!
                                    .getResourceSets()[0].getResources()[0].getSnappedPoints()[0].getName()

                                if (roadName?.trim()?.isEmpty() == true) {
                                    limitLabelText.text = ""
                                } else limitLabelText.text = roadName

                                val currentSpeed = (location.speed * 3.6).toInt().toString()
                                limitText.text = currentSpeed

                                val speedLimit = response.body()!!
                                    .getResourceSets()[0].getResources()[0].getSnappedPoints()[0].getSpeedLimit()
                                val speedLimitKmh = speedLimit * 1.60934f
                                maxSpeed = speedLimitKmh.roundToInt()
                                val sMaxSpeed = maxSpeed.toString()
                                speed.text = sMaxSpeed

                                val intent = Intent()
                                intent.action = "data"
                                intent.putExtra("lat", lat)
                                intent.putExtra("lon", lon)
                                intent.putExtra("currentSpeed", currentSpeed)
                                intent.putExtra("roadName", roadName)
                                intent.putExtra("maxSpeed", sMaxSpeed)
                                sendBroadcast(intent)

                                if (maxSpeed < (location.speed * 3.6).toInt() && maxSpeed != 0) {
                                    limit.setCardBackgroundColor(Color.parseColor("#FF0000"))
                                    limitLabelText.setTextColor(Color.parseColor("#FFFFFF"))
                                    limitText.setTextColor(Color.parseColor("#FFFFFF"))
                                    mSpeedUnits.setTextColor(Color.parseColor("#FFFFFF"))
                                    speed.setTextColor(Color.parseColor("#000000"))
                                    speedUnits.setTextColor(Color.parseColor("#000000"))
                                } else {
                                    limit.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                                    limitLabelText.setTextColor(Color.parseColor("#000000"))
                                    limitText.setTextColor(Color.parseColor("#000000"))
                                    mSpeedUnits.setTextColor(Color.parseColor("#000000"))
                                    speed.setTextColor(Color.parseColor("#000000"))
                                    speedUnits.setTextColor(Color.parseColor("#000000"))
                                }
                            }

                            override fun onFailure(call: Call<SnapToRoadResponse>, t: Throwable) {
                                limitLabelText.text = "GPS?"
                                limitText.text = "--"
                                speed.text = "--"
                                Toast.makeText(
                                    this@BubblesService, "Kiểm tra GPS", Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
            } else {
                limitLabelText.text = "GPS?"
                limitText.text = "--"
                speed.text = "--"
                Toast.makeText(
                    this@BubblesService, "Kiểm tra GPS", Toast.LENGTH_SHORT
                ).show()
                mLocationManager = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}