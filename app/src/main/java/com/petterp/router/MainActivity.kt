package com.petterp.router

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.petterp.router.annotations.Router
import com.petterp.router.runtime.RouterSingleControl

@Router("router://home", "应用主页")
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.main).setOnClickListener {
            RouterSingleControl.go(this, "router://login?name=shiyihui")
        }
    }
}
