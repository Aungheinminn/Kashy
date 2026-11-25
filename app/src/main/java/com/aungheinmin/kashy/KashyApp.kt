package com.aungheinmin.kashy

import android.app.Application
import com.aungheinmin.kashy.data.KashyDatabase
import com.aungheinmin.kashy.data.Settings

class KashyApp : Application() {
    val database: KashyDatabase by lazy { KashyDatabase.create(this) }
    val settings: Settings by lazy { Settings(this) }
}
