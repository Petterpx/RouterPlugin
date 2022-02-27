package com.petterp.router

import android.app.Application
import com.petterp.router.runtime.RouterSingleControl

/**
 *
 * @author petterp
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        RouterSingleControl.init()
    }
}
