package com.airquality.weatherstation

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.airquality.weatherstation.databinding.ActivityHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private var progressDialog: ProgressDialog? = null

    private val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://smartairqualityproject.000webhostapp.com/login/detail_api.php/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "History Data"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        showProgressDialog()
        fetchData()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun fetchData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val weatherData = weatherApi.getAllWeatherData()

                Firebase.crashlytics.setCustomKeys {
                    Firebase.crashlytics.log("History data fetched.")
                    key("historyDataSize",  weatherData.size)
                    key("firstHistoryData", Gson().toJson(weatherData.first()))
                    key("lastHistoryData", Gson().toJson(weatherData.last()))
                }

                withContext(Dispatchers.Main) {
                    setupTable(weatherData)
                    dismissProgressDialog()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Firebase.crashlytics.recordException(e)
            }
        }
    }

    private fun setupTable(weatherDataList:List<WeatherData>) {
        val headerRow = createTableRow()
        addHeaderCell("ID", headerRow)
        addHeaderCell("Temperature", headerRow)
        addHeaderCell("Humidity", headerRow)
        addHeaderCell("Air Quality", headerRow)
        addHeaderCell("Time Stamp", headerRow)
        binding.tableLayout.addView(headerRow)

        for (weather in weatherDataList) {
            val dataRow = createTableRow()
            addDataCell(weather.id, dataRow)
            addDataCell(weather.temperature, dataRow)
            addDataCell(weather.humidity, dataRow)
            addDataCell(weather.airQuality, dataRow)
            addDataCell(weather.timestamp, dataRow)
            binding.tableLayout.addView(dataRow)
        }
    }

    private fun createTableRow(): TableRow {
        val tableRow = TableRow(this)
        val params = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        tableRow.layoutParams = params
        tableRow.gravity = Gravity.CENTER
        return tableRow
    }

    private fun addHeaderCell(text: String, row: TableRow) {
        val textView = createTextView(text, true)
        row.addView(textView)
    }

    private fun addDataCell(text: String, row: TableRow) {
        val textView = createTextView(text, false)
        row.addView(textView)
    }

    private fun createTextView(text: String, isHeader: Boolean): TextView {
        val textView = TextView(this)
        val params = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(8, 8, 8, 8)
        textView.setPadding(8, 8, 8, 8)
        textView.layoutParams = params
        textView.text = text
        textView.gravity = Gravity.CENTER
        textView.setBackgroundResource(R.drawable.table_border)
        textView.setTextColor(if (isHeader)  ContextCompat.getColor(this,R.color.blue)  else
            ContextCompat.getColor(this,R.color.black))

        return textView
    }

    private fun showProgressDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage("Fetching data...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
    }
}