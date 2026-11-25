package com.aungheinmin.kashy

import android.app.Application
import com.aungheinmin.kashy.data.KashyDatabase

class KashyApp : Application() {
    val database: KashyDatabase by lazy { KashyDatabase.create(this) }
}
