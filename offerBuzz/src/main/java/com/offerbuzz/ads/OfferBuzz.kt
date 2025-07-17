package com.offerbuzz.ads

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import com.offerbuzz.ads.apis.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.offerbuzz.ads.apis.Apis
import com.offerbuzz.ads.apis.Notification

class OfferBuzz(private val context: Context, private val appId:String, private val userId:String, private val isWebView:Boolean?=true) {

    private var sdk = false
    private lateinit var notification: Notification

    interface InitializeCallback {
        fun onSuccess(message: String?)
        fun onFailure(error: String?)
    }

    interface StartOfferCallback {
        /** Called once the Offer Activity was successfully launched */
        fun onSuccess()

        /**
         * Called if we couldn’t launch, e.g. because the SDK wasn’t initialized yet.
         * @param reason a human-readable error message
         */
        fun onError(reason: String)
    }

    private suspend fun fetchGoogleAdId(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
            if (!info.isLimitAdTrackingEnabled) info.id else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun initializeSdk(initializeCallback : InitializeCallback){
        notification = Notification(context)
        CoroutineScope(Dispatchers.Main).launch {
            val gaid = fetchGoogleAdId(context) ?: ""
            try {
                val resp = RetrofitClient.service.sdkIniti(
                    appId      = appId,
                    googleAdId = gaid,
                    deviceId   = getDeviceId(context),
                    userId     = userId
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    sdk = body?.status == true
                    if (body?.status == true) {

                        val token   = body.token
                        val title   = body.title
                        val message = body.message

                        context.getSharedPreferences("prefs", MODE_PRIVATE)
                            .edit()
                            .putString("sdk_token", token)
                            .apply()

                        Log.d("SDK_INIT","token $token")

                        initializeCallback.onSuccess("$title $message")
                        notification.check(token)
                    } else {
                        Log.e("SDK_INIT", "Failed: ${body?.title}  ${body?.message}")
                        initializeCallback.onFailure(body?.title+" "+body?.message)
                    }
                } else {
                    Log.e("SDK_INIT", "HTTP ${resp.code()}: ${resp.errorBody()?.string()}")
                    initializeCallback.onFailure("HTTP ${resp.code()}: ${resp.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("SDK_INIT", "Network error", e)
            }
        }

    }

    fun isAvailable(): Boolean {
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("sdk_token", null)
        return !token.isNullOrEmpty()
    }

    fun startOffer(callback: StartOfferCallback) {
        if (sdk){
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("sdk_token", null)
            if (token.isNullOrEmpty()) {
                callback.onError("SDK not initialized")
                return
            }
            val offerUrl = Apis.OFFER_URL+"?key=$token"

            if (isWebView == false){
                try {
                    context.openInCustomTab(offerUrl)
                    callback.onSuccess()
                } catch (e: Exception) {
                    callback.onError("Failed to open offer: ${e.message}")
                }
            }else{
                val intent = Intent(context, CheckingActivity::class.java).apply {
                    putExtra("url", offerUrl)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
            }

        }else{
            callback.onError("SDK not initialized")
        }

    }



    @SuppressLint("HardwareIds")
   private fun getDeviceId(context: Context): String =
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

    private fun Context.openInCustomTab(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(this, Uri.parse(url))
    }

}