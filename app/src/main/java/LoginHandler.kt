package com.senstrgrs.griffinjohnson.sensortriggers

import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import android.support.annotation.NonNull
import com.google.android.gms.tasks.OnCompleteListener
import android.R.attr.password
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import org.jetbrains.anko.*
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.google.firebase.FirebaseApp
import org.jetbrains.anko.makeCall


class LoginHandler(lPacket : MainActivity.User, context: Context, progress : ProgressBar)
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
        mAuth.createUserWithEmailAndPassword(email, psw).addOnCompleteListener {
            if(it.isSuccessful)
            {
                // go to new screen
                transition()
            }
        }
    }
}