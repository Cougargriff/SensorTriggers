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
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import com.google.firebase.FirebaseApp


class LoginHandler(lPacket : MainActivity.User)
{
    lateinit var mAuth : FirebaseAuth

    var email = lPacket.email
    var psw = lPacket.password

    fun initialize()
    {

        mAuth = FirebaseAuth.getInstance()
    }

    fun temp_login(context: Context)
    {
        val intent = Intent(context, WatchComms::class.java)
        startActivity(context, intent,null)
    }

    fun login()
    {
        mAuth.signInWithEmailAndPassword(email, psw).addOnSuccessListener {
            Log.i("mAUTH", "Login Success")
        }
    }

    fun register()
    {
        mAuth.createUserWithEmailAndPassword(email, psw).addOnSuccessListener {
            Log.i("mAuth", "Register User Success - email : " + email + ", psw : " + psw)
        }
    }
}