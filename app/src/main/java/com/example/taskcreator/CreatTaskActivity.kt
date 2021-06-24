package com.example.taskcreator

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_creat_task.*
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*
import kotlin.math.PI


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creat_task)

        // initialize gmap service
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

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
                    taskLongitude.toString()
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

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap ?: return
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        googleMap.setOnCameraIdleListener(this)
        enableMyLocation()
        locate()
    }

    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        // [START maps_check_location_permission]
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
        // [END maps_check_location_permission]
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
                // Got last known location. In some rare situations this can be null.
                val lat = location?.latitude
                val long = location?.longitude
                map.setMyLocationEnabled(true)
                map.getUiSettings().setMyLocationButtonEnabled(true)
                map.getUiSettings().setCompassEnabled(true)
                moveMap(lat, long)
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

            val cameraPosition =
                CameraPosition.Builder().target(latLng).bearing(0.0f).tilt(0.0f).zoom(15F).build()

            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

//            map.moveCamera(CameraUpdateFactory.newLatLng(latLng))
//            map.animateCamera(CameraUpdateFactory.zoomTo(15F))
            map.getUiSettings().setZoomControlsEnabled(true)
            markerReadyFlag = true
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
//        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show()
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).

        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        var azimuth = orientationAngles[0] * 180 / 3.14f
        var pitch = orientationAngles[1] * 180 / 3.14f
        var roll = orientationAngles[2] * 180 / 3.14f
        var facing = (orientationAngles[0] + PI * 2) % (PI * 2)

        Log.d(
            "Sensor",
            "Orientation: ${azimuth}, " + "${pitch}, " + "${roll}, heading: ${facing}"
        )
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location:\n$location", Toast.LENGTH_LONG).show()
    }

    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val SENSOR_INTERVAL_MICROSECONDS = 1000000
    }

    override fun onCameraIdle() {
//        updateOrientationAngles()
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

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
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

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this)
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
//            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            if (this::map.isInitialized && markerReadyFlag) {
                SensorManager.getRotationMatrixFromVector(
                    rotationMatrix, event.values
                )
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val bearing: Double = Math.toDegrees(orientation[0].toDouble()) + mdeclination
                Log.i("bearing:", bearing.toString())
                updateCamera(bearing)
            }
        }
    }

    private fun updateCamera(bearing: Double) {
        val latLng = LatLng(taskLatitude, taskLongitude)
        val pos = CameraPosition.builder().target(latLng).bearing(bearing.toFloat()).zoom(15F).build()
        map.moveCamera(CameraUpdateFactory.newCameraPosition(pos))

    }

    data class Tilt(val yaw: Float, val pitch: Float, val roll: Float)

    fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)
    }
}