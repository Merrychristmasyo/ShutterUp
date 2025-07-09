package com.example.shutterup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 시스템 스플래시 즉시 종료
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false } // 즉시 종료
        
        super.onCreate(savedInstanceState)
        
        // 액션바 숨기기 (추가 보장)
        actionBar?.hide()
        
        setContentView(R.layout.activity_splash)

        // 1.5초 후 MainActivity로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)
    }
}
