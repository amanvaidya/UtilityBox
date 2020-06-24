package com.example.utilitybox.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.example.utilitybox.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer


class FragmentHome : Fragment() {
    var mapView: MapView? = null
    var map: GoogleMap? = null
    var v: View?= null
   override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       v = inflater.inflate(R.layout.fragment_fragment_home, container, false)
       try{
            MapsInitializer.initialize(activity)
       }catch(e: GooglePlayServicesNotAvailableException){
           Log.e("Address Map", "Could not initialize google play", e)
       }
       when (GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity()) )
       {
           ConnectionResult.SUCCESS-> Toast.makeText(getActivity(), "SUCCESS", Toast.LENGTH_SHORT).show()
           mapView = v.findViewById<MapView>(R.id.map)

       }


        return v
    }
}
