package com.offerbuzz

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.offerbuzz.ads.OfferBuzz
import com.offerbuzz.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {


    private lateinit var offerBuzz: OfferBuzz
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        offerBuzz = OfferBuzz(this,"39906ac4",getDeviceId(this))

        offerBuzz.initializeSdk(object : OfferBuzz.InitializeCallback {
            override fun onSuccess(message: String?) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(error: String?) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
            }

        })

        binding.button.setOnClickListener {
            if (offerBuzz.isAvailable()){
                offerBuzz.startOffer(object : OfferBuzz.StartOfferCallback {
                    override fun onSuccess() {

                    }
                    override fun onError(reason: String) {
                        Toast.makeText(this@MainActivity, reason, Toast.LENGTH_SHORT).show()
                    }
                })
            }else{
                Toast.makeText(this@MainActivity, "isAvailable", Toast.LENGTH_SHORT).show()
            }
        }




    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(context: Context): String =
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
}