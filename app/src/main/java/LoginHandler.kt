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
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import com.google.firebase.FirebaseApp
import org.jetbrains.anko.makeCall


class LoginHandler(lPacket : MainActivity.User, context: Context)
{
    var mAuth  = FirebaseAuth.getInstance()

    var email = lPacket.email
    var psw = lPacket.password
    var context = context


    fun transition()
    {
        val intent = Intent(context, WatchComms::class.java)
        ContextCompat.startActivity(context, intent, null)
    }

    fun login()
    {
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