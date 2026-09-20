package com.example.zenalarm

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.zenalarm.data.AlarmRepository
import com.example.zenalarm.data.AppSettings
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var bridge: WebAppBridge
    private lateinit var repository: AlarmRepository
    private lateinit var permissionHelper: PermissionHelper

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = AlarmRepository(this)
        permissionHelper = PermissionHelper(this)
        val appSettings = AppSettings(this)
        runBlocking { repository.ensureDefaultGroup() }

        webView = findViewById(R.id.webView)
        bridge = WebAppBridge(
            activity = this,
            repository = repository,
            permissionHelper = permissionHelper,
            appSettings = appSettings,
            webView = webView,
            runOnUiThread = { runnable -> runOnUiThread(runnable) },
        )

        WebViewHelper.configure(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                bridge.notifyStateChanged()
                bridge.notifyPermissionsChanged()
                bridge.notifySettingsChanged()
                repository.refreshNextAlarmNotification()
            }
        }

        webView.addJavascriptInterface(bridge, "AndroidBridge")
        WebViewHelper.loadAssetHtml(webView, "index.html")
    }

    override fun onResume() {
        super.onResume()
        if (::bridge.isInitialized) {
            bridge.notifyPermissionsChanged()
            repository.refreshNextAlarmNotification()
        }
    }

    fun requestNotificationPermissionFromJs() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            bridge.notifyPermissionsChanged()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            bridge.notifyPermissionsChanged()
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATIONS,
        )
    }

    fun openExactAlarmSettingsFromJs() {
        startActivity(permissionHelper.createExactAlarmSettingsIntent())
    }

    fun openDndSettingsFromJs() {
        startActivity(permissionHelper.createDndSettingsIntent())
    }

    fun openNotificationSettingsFromJs() {
        startActivity(permissionHelper.createAppNotificationSettingsIntent())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            bridge.notifyPermissionsChanged()
            repository.refreshNextAlarmNotification()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 100
    }
}
