package com.example.utilitybox

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class MainActivity : AppCompatActivity() {
    private val timeOut:Long=3000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //
        Handler().postDelayed({
           val i = Intent(this@MainActivity, Login::class.java)
            startActivity(i)
            finish()
        },timeOut)
        //
    }
}
