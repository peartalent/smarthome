package com.sunasterisk.smarthomejava

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.sunasterisk.smarthomejava.config.Config
import kotlinx.android.synthetic.main.activity_begging.*


class Begging : AppCompatActivity() {
    var token: String = ""
    override fun onStart() {
        super.onStart()
        loadToken()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_begging)

        val animation: Animation
        animation = AnimationUtils.loadAnimation(applicationContext,
                R.anim.top_animation)
        imgLogo.startAnimation(animation)
        textLogo.startAnimation(AnimationUtils.loadAnimation(applicationContext,
                R.anim.bottom_animation))

        var countDownTimer = object : CountDownTimer(2000, 10) {
            override fun onTick(p0: Long) {
            }

            override fun onFinish() {
                if (token.equals("true")) {
                    var intent = Intent(this@Begging, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    var intent = Intent(this@Begging, Login::class.java)
                    startActivity(intent)
                    finish()
                }
            }

        }
        countDownTimer.start()
    }

    private fun loadToken() {
        val sharedPreferences = getSharedPreferences(Config.FILE_USER, Context.MODE_PRIVATE)
        if (sharedPreferences != null) {
            token = sharedPreferences.getString(Config.FILE_USER_TOKEN_SESSION, "").toString();
            Log.d("token",token)
        }
    }
}
