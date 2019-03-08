package com.senstrgrs.griffinjohnson.sensortriggers

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.DisplayMetrics
import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import com.google.firebase.FirebaseApp
import co.revely.gradient.RevelyGradient
import com.google.errorprone.annotations.CompatibleWith
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login_full.*
import kotlinx.android.synthetic.main.activity_login_full.view.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.triggerdialog.*
import org.jetbrains.anko.*

class MainActivity : AppCompatActivity()
{
    data class User(var email : String, var password : String)

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_full)

        window.navigationBarColor = ContextCompat.getColor(baseContext, R.color.blueish)
        window.navigationBarDividerColor = ContextCompat.getColor(baseContext, R.color.login_color)
        window.statusBarColor = Color.parseColor("#4158D0")

        FirebaseApp.getInstance()
        FirebaseApp.initializeApp(applicationContext)

        RevelyGradient
                .linear()
                .angle(45f)
                .colors(intArrayOf(Color.parseColor("#4158D0"), Color.parseColor("#C850C0"), Color.parseColor("#FFCC80")))
                .onBackgroundOf(view)

        val valueanimator = ValueAnimator.ofFloat(0f, 1f)
        valueanimator.addUpdateListener {
            val value = it.animatedValue as Float

            register_fab.alpha = value
            loginButton.alpha = value

            email_box.alpha = value
            pass_box.alpha = value
        }

        valueanimator.interpolator = AccelerateInterpolator()
        valueanimator.duration = 2000L

        ObjectAnimator.ofFloat(base, "translationY", -100f)
                .setDuration(1200)
                .start()

        valueanimator.start()

        loginButton.setOnClickListener {

            var lpacket = User(email = "", password = "")
            var err = 0;

            if(email_box.text.toString().compareTo("") != 0)
            {
                lpacket.email = email_box.text.toString()
            }
            else
            {
                err++;
            }

            if(pass_box.text.toString().compareTo("") != 0)
            {
                lpacket.password = pass_box.text.toString()
            }
            else
            {
                err++
            }

            if(err == 0)
            {
                Toast.makeText(this, "Logging in...", Toast.LENGTH_LONG)

                val auth = login_dispatch(lpacket)
                auth.login()
            }
        }

        register_fab.setOnClickListener {
            var lpacket = User(email = "", password = "")
            var err = 0;

            if(email_box.text.toString().compareTo("") != 0)
            {
                lpacket.email = email_box.text.toString()
            }
            else
            {
                err++
            }

            if(pass_box.text.toString().compareTo("") != 0)
            {
                lpacket.password = pass_box.text.toString()
            }
            else
            {
                err++
            }

            if(err == 0)
            {
                val auth = login_dispatch(lpacket)

                alert("Are you sure you'd like to register with the following email?\n\n" + lpacket.email) {
                    yesButton{
                        auth.register()
                    }
                    noButton {

                    }
                }.also {
                    this.setTheme(R.style.MyDialogTheme)
                }.show()
                        .apply {
                            getButton(AlertDialog.BUTTON_POSITIVE)?.let { it.setTextColor(ContextCompat.getColor(context, R.color.blueish)) }
                            getButton(AlertDialog.BUTTON_NEGATIVE)?.let { it.setTextColor(ContextCompat.getColor(context, R.color.blueish)) }
                        }
            }
        }
    }

    fun login_dispatch(user : User) : LoginHandler
    {
        return LoginHandler(user, this)
    }
}
