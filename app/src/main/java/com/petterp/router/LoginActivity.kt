package com.petterp.router

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.petterp.router.annotations.Router

/**
 *
 * @author petterp
 */
@Router("router://login", "登录页面")
class LoginActivity : AppCompatActivity(R.layout.activity_login) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundleExtra = intent.getStringExtra("name")
        findViewById<TextView>(R.id.login).text=bundleExtra
    }
}
