package com.example.utilitybox.fragments

import android.Manifest
import android.content.Context

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.example.utilitybox.R
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class FragmentHome : Fragment(), OnMapReadyCallback {

    private var v:View?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       v = inflater.inflate(R.layout.fragment_fragment_home, container, false)

        if(getString(R.string.map_api_key).isEmpty()){
            Toast.makeText(activity, "Add your own API key in MapWithMarker/app/secure.properties as MAPS_API_KEY=YOUR_API_KEY", Toast.LENGTH_LONG).show()
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)



        return v
    }


    override fun onMapReady(googleMap: GoogleMap) {
        googleMap?.apply {
            val sydney = LatLng(-33.852, 151.211)
            addMarker(
                MarkerOptions()
                    .position(sydney)
                    .title("Marker in Sydney")
            )
            // [START_EXCLUDE silent]
            moveCamera(CameraUpdateFactory.newLatLng(sydney))
            // [END_EXCLUDE]
        }
    }
    // [END maps_marker_on_map_ready_add_marker]
}


