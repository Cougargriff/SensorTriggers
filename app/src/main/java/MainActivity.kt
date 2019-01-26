package com.senstrgrs.griffinjohnson.sensortriggers

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.google.firebase.FirebaseApp
import co.revely.gradient.RevelyGradient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login_full.*
import kotlinx.android.synthetic.main.activity_login_full.view.*

class MainActivity : AppCompatActivity()
{
    data class User(var email : String, var password : String)




    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_full)

        FirebaseApp.initializeApp(this.applicationContext)

        RevelyGradient
                .linear()
                .angle(45f)
                .colors(intArrayOf(Color.parseColor("#4158D0"), Color.parseColor("#C850C0"), Color.parseColor("#FFCC80")))
                .onBackgroundOf(view)


        // Login Button Callback Closure

        loginButton.setOnClickListener {
            var lpacket = User(email = "", password = "")

            if(email_box.text.toString().compareTo("") != 0)
            {
                lpacket.email = email_box.text.toString()
            }

            if(pass_box.text.toString().compareTo("") != 0)
            {
                lpacket.password = pass_box.text.toString()
            }
            Toast.makeText(this, "Logging in...", Toast.LENGTH_LONG)
            login_dispatch(lpacket)
        }






    }

    fun login_dispatch(user : User)
    {
        var handler = LoginHandler(user)
//        handler.initialize()
//        handler.login()

        handler.temp_login(this)



    }




}
