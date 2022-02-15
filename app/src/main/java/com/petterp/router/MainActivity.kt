package com.petterp.router

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petterp.router.annotations.Router

@Router("router://home", "应用主页")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
