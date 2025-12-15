// MApplication.kt
package com.sloth.registerapp // Certifique-se de que o package name está correto

import android.app.Application
import android.content.Context
import com.cySdkyc.clx.Helper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Helper.install(this) // 'this' refere-se à instância de MyApplication
    }
}