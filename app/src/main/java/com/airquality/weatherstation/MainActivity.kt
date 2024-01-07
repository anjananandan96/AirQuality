package com.airquality.weatherstation

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.airquality.weatherstation.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private  val AQI_THRESHOLD = 300

    private lateinit var binding: ActivityMainBinding

    private val executorService: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    private var isFetchingData = false
    private var isAlertVisible = false
    private var isAlertShown = false
    private var updateTimeSec: Long = 2
    private var requestCount: Long = 0
    private var skippedCount: Long = 0
    private var responseCount: Long = 0
    private var errorCount: Long = 0

    private val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://smartairqualityproject.000webhostapp.com/login/airquality_api.php/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewMoreButton()

        executorService.scheduleAtFixedRate({
            fetchData()
            Firebase.crashlytics.setCustomKeys {
                key("requestCount", ++requestCount)
            }

        }, 0, updateTimeSec, TimeUnit.SECONDS)
    }

    private fun setupViewMoreButton() {
        binding.btViewMore.setOnClickListener {
            Firebase.analytics.logEvent("viewMoreButtonClicked") {}

//            val intent = Intent(Intent.ACTION_VIEW,
//                Uri.parse("https://smartairqualityproject.000webhostapp.com/login/detail_api.php"))
//            startActivity(intent)
            val newActivityIntent = Intent(this, HistoryActivity::class.java)
            startActivity(newActivityIntent)
        }
    }

    private fun fetchData() {
        if (isFetchingData) {
            Firebase.crashlytics.log("Previous execution still in progress. Skipping.")
            Firebase.crashlytics.setCustomKeys {
                key("skippedCount", ++skippedCount)
            }
            return
        }

        isFetchingData = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val weatherData = weatherApi.getWeatherData()

                Firebase.crashlytics.setCustomKeys {
                    key("lastApiData", Gson().toJson(weatherData))
                }

                Firebase.crashlytics.setCustomKeys {
                    key("responseCount", ++responseCount)
                }

                withContext(Dispatchers.Main) {
                    binding.tvTemperatureValue.text =
                        getString(R.string.text_view_temperature_value, weatherData.temperature)
                    binding.tvHumidityValue.text =
                        getString(R.string.text_view_humidity_value, weatherData.humidity)
                    binding.tvAirQualityValue.text =
                        getString(R.string.text_view_air_quality_value, weatherData.airQuality)
                    binding.tvTimestamp.text =
                        getString(R.string.text_view_updated, weatherData.timestamp)


                    if(weatherData.airQuality.toInt()>AQI_THRESHOLD){
                        Firebase.crashlytics.log("AQI threshold crossed: $weatherData.airQuality")
                        if (!isAlertVisible && !isAlertShown){
                            Firebase.crashlytics.log("AQI alert triggered!")
                            showAlertDialog("Alert",
                                "Air Quality warning:  Value greater than $AQI_THRESHOLD!",
                                this@MainActivity)
                        }
                    }
                    else{
                        isAlertShown = false
                    }


                }
            } catch (e: Exception) {
                e.printStackTrace()
                Firebase.crashlytics.recordException(e)
                Firebase.crashlytics.setCustomKeys {
                    key("errorCount", ++errorCount)
                }
            } finally {
                isFetchingData = false
            }
        }
    }

    private fun showAlertDialog(title: String, message: String, context: Context) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(title)

        alertDialogBuilder
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, id ->
                isAlertVisible = false
                dialog.dismiss()
            })

        val alertDialog = alertDialogBuilder.create()
        isAlertVisible = true
        isAlertShown = true
        alertDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown()
        Firebase.crashlytics.log("onDestroy Called!")
    }
}