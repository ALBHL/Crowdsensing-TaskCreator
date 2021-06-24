package com.example.taskcreator

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taskcreator.PermissionUtils.requestPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_creat_task.*
import kotlinx.android.synthetic.main.activity_register.*
import org.florescu.android.rangeseekbar.RangeSeekBar
import java.util.*
import kotlin.math.roundToInt


class CreatTaskActivity : AppCompatActivity(), OnMapReadyCallback, OnMyLocationButtonClickListener,
    OnMyLocationClickListener, GoogleMap.OnCameraIdleListener, SensorEventListener {
    val PROVIDER_NAME = "com.example.collector/AcronymProvider"
    val URL = "content://$PROVIDER_NAME/Inbox"
    val CONTENT_URI = Uri.parse(URL)

    val TABLEIN_NAME = "Inbox"
    val COL_NAME = "name"
    val COL_AGE = "age"
    val COL_ID = "id"
    val COL_URL = "imageurl"
    val COL_PROFILE = "profileimg"
    val COL_STAGE = "current_stage"
    val COL_IMAGE_BIT = "picturetaken"

    var modelSelected = 0;

    // GMAP
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var taskLatitude: Double = 0.0
    private var taskLongitude: Double = 0.0
    private var markerReadyFlag: Boolean = false

    // sensor
    private lateinit var sensorManager: SensorManager
    private var mdeclination: Float = 0f
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(16)
    private val orientationAngles = FloatArray(3)

    // map overlay
    private lateinit var taskValidRangeOverlay: GroundOverlay
    var mRadius = 300f
    var mDegreeStart = -90f
    var mDegreeEnd = -60f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creat_task)

        // initialize gmap service
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // gmap slider
//        val slider = supportFragmentManager.findFragmentById(R.id.gmap_radius_slider) as Slider
//        slider.addOnChangeListener { slider, value, fromUser ->
//            // Responds to when slider's value is changed
//        }
        radiusSeekBar.setProgress(300)
        radiusSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    Log.i("radiusSeekBar progress:", progress.toString())
                    mRadius = progress.toFloat()
                    radiusSeekBarTextView.setText("Radius: $mRadius")
                    if (taskValidRangeOverlay.isVisible) {
                        if (ContextCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { location: Location? ->
                                    val latLng =
                                        location?.latitude?.let { LatLng(it, location?.longitude) }
                                    if (latLng != null) {
                                        taskValidRangeOverlay.remove()
                                        addOverlay(latLng, map)
                                    }
                                }
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                Toast.makeText(
                    this@CreatTaskActivity,
                    "radius set: " + seek.progress,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        val rangeSeekBar = RangeSeekBar<Int>(this)
        rangeSeekBar.setRangeValues(0, 360)
        rangeSeekBar.setSelectedMinValue(mDegreeStart.roundToInt() + 90)
        rangeSeekBar.setSelectedMaxValue(mDegreeEnd.roundToInt() + 90)
        rangeSeekBar.setOnRangeSeekBarChangeListener(object :
            RangeSeekBar.OnRangeSeekBarChangeListener<Int> {
            override fun onRangeSeekBarValuesChanged(
                bar: RangeSeekBar<*>?,
                minValue: Int?,
                maxValue: Int?
            ) {
                Log.i("degreeSeekBar progress:", "$minValue, $maxValue")
                if (minValue != null && maxValue != null) {
                    mDegreeStart = minValue.toFloat() - 90
                    mDegreeEnd = maxValue.toFloat() - 90
                    degreeSeekBarTextView.setText("Angle: $minValue - $maxValue")
                }
                if (taskValidRangeOverlay.isVisible) {
                    updateOverlay(taskValidRangeOverlay)
                }
            }
        })

        val layout = findViewById<View>(R.id.seekbar_placeholder) as FrameLayout
        layout.addView(rangeSeekBar)

//        degreeSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
//            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
//                if (fromUser) {
//                    Log.i("degreeSeekBar progress:", progress.toString())
//                    mDegree = progress.toFloat()
//                    if (taskValidRangeOverlay.isVisible) {
//                        updateOverlay(taskValidRangeOverlay)
//                    }
//                }
//            }
//
//            override fun onStartTrackingTouch(seek: SeekBar) {
//                // write custom code for progress is started
//            }
//
//            override fun onStopTrackingTouch(seek: SeekBar) {
//                Toast.makeText(
//                    this@CreatTaskActivity,
//                    "degree set: " + seek.progress,
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        })

        button_back_main.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val profiles = listOf<String>(
            "https://i.pinimg.com/originals/52/44/95/524495ccf8c05ab40f5905d852a358c2.png",
            "https://i.pinimg.com/236x/6c/39/9b/6c399b619a43f86c2d5341aacd75702e.jpg",
            "https://i.pinimg.com/564x/69/72/bd/6972bde9f6d0d45d41530c85f8379a88.jpg",
            "https://i.pinimg.com/236x/d8/d7/b6/d8d7b6e15d06590ef551a4c561e22a55.jpg",
            "https://www.cctvforum.com/uploads/monthly_2018_12/S_member_24879.png",
            "https://i.pinimg.com/236x/85/79/ec/8579ec86d8863850953fd49fb41e2a71.jpg",
            "https://i.pinimg.com/236x/65/76/d3/6576d3d4b94f08be3c082d666a48e798.jpg",
            "https://i.pinimg.com/236x/58/f7/98/58f79869d4466999f24597632d8d06e4.jpg",
            "https://i.pinimg.com/564x/d5/be/82/d5be825883f116248f5cd3a2f58874b1.jpg"
        )
        val ran = (0 until 8).random()

        var chooseMdl: Spinner = findViewById(R.id.spinner_choosemodel)
        val models = arrayOf(
            "Object Detection",
            "Face Detection",
            "Pose Detection",
            "Image Labeling",
            "Text Recognition",
            "customSeg",
            "From URL"
        )
        chooseMdl.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, models)

        chooseMdl.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
                editTextURL.visibility = View.GONE
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 6) { // enter custom URL
                    editTextURL.visibility = View.VISIBLE
                } else {
                    editTextURL.visibility = View.GONE
                }
            }
        }

        btnInsert.setOnClickListener {
            if (editTextName.text.toString().isNotEmpty() &&
                editTextAge.text.toString().isNotEmpty() &&
                mediaType.checkedRadioButtonId != -1
            ) {
                val cv = ContentValues()
                cv.put(COL_NAME, editTextName.text.toString())
                cv.put(COL_AGE, editTextAge.text.toString().toInt())
                cv.put(COL_URL, "")
                cv.put(COL_PROFILE, profiles[ran])
                cv.put(COL_STAGE, "to collect")
                contentResolver.insert(CONTENT_URI, cv)
                // new field
                val checkedMediaType = getMediaType()
                modelSelected = chooseMdl.selectedItemPosition
                if (modelSelected == 6) {
                    val customURL = editTextURL.text.toString()
                }

                // upload the entered things to firebase
                val uid = FirebaseAuth.getInstance().uid ?: ""
                val task_id = UUID.randomUUID().toString()
                val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks/$task_id")

                val task = Task(
                    task_id,
                    editTextName.text.toString(),
                    editTextDes.text.toString(),
                    models[0],
                    uid,
                    taskLatitude.toString(),
                    taskLongitude.toString(),
                    mRadius.toString(),
                    "${mDegreeStart + 90}, ${mDegreeEnd + 90}"
                )

                ref.setValue(task)
                    .addOnSuccessListener {
                        Log.d(
                            "CreateTaskActivity",
                            "Finally we saved the task to Firebase Database id: $task_id"
                        )
                    }
                    .addOnFailureListener {
                        Log.d(
                            "CreateTaskActivity",
                            "Failed to set task value to database: ${it.message}"
                        )
                    }

            } else {
                Toast.makeText(
                    this,
                    "Please enter Name and/or Number and media type",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Navigate to the success page
            val intent = Intent(this, SuccessActivity::class.java)
            startActivity(intent)
        }
    }

    fun getMediaType(): String {
        val id = mediaType.checkedRadioButtonId
        val checkedBtn: RadioButton = findViewById(id)
        return checkedBtn.text.toString()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onMapReady(googleMap: GoogleMap?) {
        val mainScrollView = findViewById<View>(R.id.mainScrollView) as ScrollView
        var transparentTouchPanel: ImageView = findViewById(R.id.transparent_image);
        transparentTouchPanel.setOnTouchListener { _, event ->
            val action = event.action
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    mainScrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    mainScrollView.requestDisallowInterceptTouchEvent(false)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    mainScrollView.requestDisallowInterceptTouchEvent(true)
                    false
                }
                else -> true
            }
        }
        map = googleMap ?: return
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        googleMap.setOnCameraIdleListener(this)
        enableMyLocation()
        locate()
    }

    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermission(
                this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )
        }
    }

    private fun locate() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        val mlocManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val mlocListener: LocationListener = MyLocationListener(mdeclination)
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0f, mlocListener)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                val lat = location?.latitude
                val long = location?.longitude
                map.setMyLocationEnabled(true)
                moveMap(lat, long)
                map.getUiSettings().setMyLocationButtonEnabled(true)
                map.getUiSettings().setCompassEnabled(true)
            }
    }

    private fun moveMap(latitude: Double?, longitude: Double?) {
        if (latitude != null && longitude != null) {
            val latLng = LatLng(latitude, longitude)
            taskLatitude = latitude
            taskLongitude = longitude
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title("Marker in India")
            )

            addOverlay(latLng, map)

            val cameraPosition =
                CameraPosition.Builder().target(latLng).bearing(0.0f).tilt(0.0f).zoom(15F).build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            map.getUiSettings().setZoomControlsEnabled(true)
            markerReadyFlag = true
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap =
                Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    private fun addOverlay(latLng: LatLng, map: GoogleMap) {
        // circular overlay

//        val newarkMap = bitmapDescriptorFromVector(this, R.drawable.circle_overlay)?.let {
//            GroundOverlayOptions()
//                .image(it)
//                .position(latLng, 650f, 650f)
//        }
//        map.addGroundOverlay(newarkMap)

        val bd: BitmapDescriptor = paintOverlay()
        var overlayOptions = GroundOverlayOptions()
            .image(bd)
            .position(latLng, mRadius * 2.1f, mRadius * 2.1f)
        radiusSeekBarTextView.setText("Radius: $mRadius")
        degreeSeekBarTextView.setText("Angle: ${mDegreeStart + 90} - ${mDegreeEnd + 90}")
        taskValidRangeOverlay = map.addGroundOverlay(overlayOptions)
    }

    private fun updateOverlay(overlay: GroundOverlay) {
        val bd: BitmapDescriptor = paintOverlay()
        overlay.setImage(bd)
    }


    private fun paintOverlay(): BitmapDescriptor {
        val paint = Paint()
        val rect = RectF()
        val mWidth = mRadius * 2.1f
        val mHeight = mRadius * 2.1f
        rect[mWidth / 2 - mRadius, mHeight / 2 - mRadius, mWidth / 2 + mRadius] =
            mHeight / 2 + mRadius
        val bitmap = Bitmap.createBitmap(mWidth.toInt(), mHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        paint.setColor(resources.getColor(R.color.colorOverlayBlue))
        paint.setStrokeWidth(20f)
        paint.setAntiAlias(true)
        paint.setStrokeCap(Paint.Cap.BUTT)
        paint.setStyle(Paint.Style.FILL_AND_STROKE)

        canvas.drawArc(rect, mDegreeStart, mDegreeEnd - mDegreeStart, true, paint)
        val bd: BitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        return bd
    }

    override fun onMyLocationButtonClick(): Boolean {
        locate()
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location:\n$location", Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val SENSOR_INTERVAL_MICROSECONDS = 1000000
    }

    override fun onCameraIdle() {

    }

    class MyLocationListener(var mdeclination: Float) : LocationListener {
        override fun onLocationChanged(location: Location) {
            var geoField = GeomagneticField(
                java.lang.Double.valueOf(location.latitude).toFloat(),
                java.lang.Double.valueOf(location.longitude).toFloat(),
                java.lang.Double.valueOf(location.altitude).toFloat(),
                System.currentTimeMillis()
            )
            this.mdeclination = geoField.getDeclination()
        }
    }

    // sensor
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also { rotation ->
            sensorManager.registerListener(
                this,
                rotation,
                SENSOR_INTERVAL_MICROSECONDS,
                SENSOR_INTERVAL_MICROSECONDS
            )
        }

    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
//        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
//            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
//        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
//            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
//        } else if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
////            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
//            if (this::map.isInitialized && markerReadyFlag) {
//                SensorManager.getRotationMatrixFromVector(
//                    rotationMatrix, event.values
//                )
//                val orientation = FloatArray(3)
//                SensorManager.getOrientation(rotationMatrix, orientation)
//                val bearing: Double = Math.toDegrees(orientation[0].toDouble()) + mdeclination
//                Log.i("bearing:", bearing.toString())
//                updateCamera(bearing)
//            }
//        }
    }

    private fun updateCamera(bearing: Double) {
        val latLng = LatLng(taskLatitude, taskLongitude)
        val pos =
            CameraPosition.builder().target(latLng).bearing(bearing.toFloat()).zoom(15F).build()
        map.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
    }
}