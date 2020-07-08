package com.example.utilitybox.fragments


import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment

import com.example.utilitybox.R

import android.view.LayoutInflater



open class FragmentNotification : Fragment(){



    lateinit var v:View
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v=inflater.inflate(R.layout.fragment_fragment_notification, container, false)


        return v
    }


}
