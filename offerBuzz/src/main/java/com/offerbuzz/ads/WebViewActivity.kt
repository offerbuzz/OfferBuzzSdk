package com.offerbuzz.ads

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.offerbuzz.ads.databinding.ActivityWebViewBinding
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.offerbuzz.ads.apis.Apis
import com.offerbuzz.ads.apis.Notification
import com.offerbuzz.ads.models.LoadingDialog
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest


class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding
    private var token = ""
    private lateinit var notification: Notification
    private lateinit var loadingDialog: LoadingDialog
    private var back = 0

    private var lastBackPressTime = 0L
    private val backPressThreshold = 2000L

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.webView.webViewClient = WebViewClient()

        val webSettings: WebSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)

        notification = Notification(this)
        loadingDialog = LoadingDialog(this)

        Log.d("generateDeviceFingerprint",generateDeviceFingerprint(this))


        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val cookies = cookieManager.getCookie(url)
                println("Cookies for $url: $cookies")
            }
        }
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true

        val prefs = this.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        token = prefs.getString("sdk_token", null).toString()

        notification.check(token)


        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                return handleUrl(view, url)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadingDialog.show()

            }


            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                loadingDialog.dismiss()

            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return handleUrl(view, url)
            }

            private fun handleUrl(view: WebView?, url: String?): Boolean {
                if (url == null) return true

                val uri = Uri.parse(url)

                val baseUrl = "${uri.scheme}://${uri.host}${uri.path}"

                Log.d("baseUrl", "opening URL: $baseUrl and $url")

                return if (baseUrl == Apis.TRACKING) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        view?.context?.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("WebViewClient", "Error opening URL: $url", e)
                    }
                    true
                } else {
                    view?.loadUrl(url)
                    false
                }
            }

        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {

                    binding.webView.goBack()
                } else {

                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime <= backPressThreshold) {

                        isEnabled = false
                        finish()
                    } else {

                        Toast.makeText(
                            this@WebViewActivity,
                            "Press back again to exit",
                            Toast.LENGTH_SHORT
                        ).show()
                        lastBackPressTime = now
                    }
                }
            }
        })


        binding.webView.loadUrl(intent.getStringExtra("url") ?: "https://offerbuzz.in")

    }


    override fun onStart() {
        super.onStart()
        notification.check(token)
    }

    @SuppressLint("HardwareIds")
    fun generateDeviceFingerprint(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val deviceInfo = listOf(
            Build.BOARD,
            Build.BRAND,
            Build.DEVICE,
            Build.DISPLAY,
            Build.HOST,
            Build.ID,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.PRODUCT,
            Build.TAGS,
            Build.TYPE,
            Build.USER,
            androidId
        ).joinToString(separator = ":")

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(deviceInfo.toByteArray())

        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}