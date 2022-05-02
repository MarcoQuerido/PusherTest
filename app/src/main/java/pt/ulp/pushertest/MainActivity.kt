package pt.ulp.pushertest

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.SubscriptionEventListener
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.pusher.client.crypto.nacl.Salsa.core

class MainActivity : AppCompatActivity() {

    var adapter: Adapter = Adapter(this@MainActivity)
    lateinit var pusher: Pusher
    val MY_PERMISSIONS_REQUEST_LOCATION = 100
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        setupPusher()

        fab.setOnClickListener { view ->
            if (checkLocationPermission())
                sendLocation()
        }
        with(recyclerView){
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location!=null){
                    Log.e("TAG","location is not null")
                    val jsonObject = JSONObject()
                    jsonObject.put("latitude",location.latitude)
                    jsonObject.put("longitude",location.longitude)
                    jsonObject.put("username", intent.extras!!.getString("username"))

                    val body = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString())
                    Log.e("TAG",jsonObject.toString())
                    Client().getClient().sendLocation(body).enqueue(object: Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {}

                        override fun onFailure(call: Call<String>?, t: Throwable) {
                            t.message?.let { Log.e("TAG", it) }
                        }

                    })

                } else {
                    Log.e("TAG","location is null")
                }
            }

    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                AlertDialog.Builder(this)
                    .setTitle("Location permission")
                    .setMessage("You need the location permission for some things to work")
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->

                        ActivityCompat.requestPermissions(this@MainActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            MY_PERMISSIONS_REQUEST_LOCATION)
                    })
                    .create()
                    .show()

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
            }
            return false
        } else {
            return true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        sendLocation()
                    }
                } else {
                    // permission denied!
                }
                return
            }
        }
    }

    private fun setupPusher() {
        Log.e("TAG","Pusher setup")
        val options = PusherOptions()
        options.setCluster("eu")
        pusher = Pusher("5fe4f01e68d96d8f5bf6", options)

        val channel = pusher.subscribe("feed")

        /*channel.bind("location") {_,_,data ->
            val jsonObject = JSONObject(data)
            Log.d("TAG",jsonObject.toString())
            val lat:Double = jsonObject.getString("latitude").toDouble()
            val lon:Double = jsonObject.getString("longitude").toDouble()
            val name:String = jsonObject.getString("username").toString()
            runOnUiThread {
                val model = Model(lat,lon,name)
                adapter.addItem(model)
            }
        }*/

        channel.bind("location", object : SubscriptionEventListener {
            fun onEvent(channelName: String?, eventName: String?, data: String) {
                val jsonObject = JSONObject(data)
                val lat:Double = jsonObject.getString("latitude").toDouble()
                val lon:Double = jsonObject.getString("longitude").toDouble()
                val name:String = jsonObject.getString("username").toString()
                runOnUiThread {
                    val model = Model(lat,lon,name)
                    adapter.addItem(model)
                }
            }

            override fun onEvent(event: PusherEvent?) {
                TODO("Not yet implemented")
            }
        })
    }

    override fun onStart() {
        super.onStart()
        pusher.connect()
    }

    override fun onStop() {
        super.onStop()
        pusher.disconnect()
    }
}