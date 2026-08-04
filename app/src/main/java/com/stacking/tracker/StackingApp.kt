package com.stacking.tracker

import android.app.Application

class StackingApp : Application() {

    lateinit var container: ContainerApp
        private set

    override fun onCreate() {
        super.onCreate()
        container = ContainerApp(this)
    }
}
