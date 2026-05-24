package com.teleport.app.update

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UpdateManagerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun testJsonParsing_correctlyParsesProductionAndBeta() {
        val jsonString = """
        {
          "production": {
            "latestVersionCode": 2,
            "latestVersionName": "1.1",
            "updateUrl": "https://github.com/production",
            "releaseNotes": "Production release notes",
            "isForceUpdate": false
          },
          "beta": {
            "latestVersionCode": 3,
            "latestVersionName": "1.2-beta",
            "updateUrl": "https://github.com/beta",
            "releaseNotes": "Beta release notes",
            "isForceUpdate": true
          }
        }
        """.trimIndent()

        val config = json.decodeFromString<UpdateConfig>(jsonString)
        
        assertEquals(2, config.production.latestVersionCode)
        assertEquals("1.1", config.production.latestVersionName)
        assertEquals("https://github.com/production", config.production.updateUrl)
        assertEquals("Production release notes", config.production.releaseNotes)
        assertFalse(config.production.isForceUpdate)

        assertEquals(3, config.beta.latestVersionCode)
        assertEquals("1.2-beta", config.beta.latestVersionName)
        assertEquals("https://github.com/beta", config.beta.updateUrl)
        assertEquals("Beta release notes", config.beta.releaseNotes)
        assertEquals(true, config.beta.isForceUpdate)
    }

    @Test
    fun testGetUpdateChannel_forBetaPackage_returnsBeta() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val context = object : ContextWrapper(baseContext) {
            override fun getPackageName(): String = "com.carfry369.teleport.beta"
        }

        val manager = UpdateManager(context)
        assertEquals("beta", manager.getUpdateChannel())
    }

    @Test
    fun testGetUpdateChannel_forProdPackage_returnsProduction() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val context = object : ContextWrapper(baseContext) {
            override fun getPackageName(): String = "com.carfry369.teleport"
        }
        
        // Make sure it doesn't have FLAG_DEBUGGABLE to avoid defaulting to beta
        val appInfo = context.applicationInfo
        appInfo.flags = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE.inv()

        val manager = UpdateManager(context)
        assertEquals("production", manager.getUpdateChannel())
    }
}
