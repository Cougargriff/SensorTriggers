package com.senstrgrs.griffinjohnson.sensortriggers

import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ProgressBar


class LoginHandler(lPacket : LoginActivity.User, context: Context, progress : ProgressBar)
{
    var mAuth  = FirebaseAuth.getInstance()
    var email = lPacket.email
    var psw = lPacket.password
    var context = context
    var pBar = progress

    fun transition()
    {
        val intent = Intent(context, WatchComms::class.java)

        pBar.visibility = View.INVISIBLE
        ContextCompat.startActivity(context, intent, null)
    }

    fun login()
    {
        pBar.visibility = View.VISIBLE
        mAuth.signInWithEmailAndPassword(email, psw).addOnCompleteListener {
            if(it.isSuccessful)
            {
                // go to new screen
                transition()
            }
        }
    }

    fun register()
    {
        pBar.visibility = View.VISIBLE
        mAuth.createUserWithEmailAndPassword(email, psw).addOnCompleteListener {
            if(it.isSuccessful)
            {
                // go to new screen
                transition()
            }
        }
    }
}