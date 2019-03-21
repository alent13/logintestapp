package com.applexis.logintestapp.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import com.applexis.logintestapp.isPasswordStrong
import com.applexis.logintestapp.isValidEmail
import android.view.View.OnTouchListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.applexis.logintestapp.R
import com.applexis.logintestapp.network.WeatherAPI
import com.applexis.logintestapp.network.data.weather.WeatherData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_auth.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 2001
    private val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION

    private val weatherAPIUrl = "https://api.openweathermap.org/data/2.5/"

    private lateinit var weatherAPI: WeatherAPI
    private var weatherDataCall: Call<WeatherData>? = null
    private lateinit var locationManager: LocationManager

    val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            loadWeatherInfo(location.latitude, location.longitude)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        supportActionBar?.title = getString(R.string.auth_activity_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        authBtn.setOnClickListener {
            authenticate()
        }

        // обработка клика на иконку (?) - восстановление пароля
        authPasswordInput.setOnTouchListener(OnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= authPasswordInput.right - authPasswordInput.compoundDrawables[DRAWABLE_RIGHT].bounds.width()) {
                    // отображаем диалого восстановления пароля или переходим на активити восстановления пароля
                    val dialog = BottomSheetDialog(this@AuthActivity)
                    dialog.setContentView(layoutInflater.inflate(R.layout.dialog_forget_password, null))
                    dialog.show()
                    return@OnTouchListener true
                }
            }
            false
        })

        authPasswordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                authenticate()
                return@setOnEditorActionListener true
            }
            false
        }

        // при увеличении количества запросов и активити необходимо перенести логику в отдельный класс и реализовать
        // доступ к этому классу, например с использованием DI
        val retrofit = Retrofit.Builder()
            .baseUrl(weatherAPIUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherAPI = retrofit.create(WeatherAPI::class.java)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun authenticate() {
        val email = authEmailInput.text.toString().trim()
        val password = authPasswordInput.text.toString()
        if (isUserInputValid(email, password)) {
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, locationPermission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, locationPermission)) {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.auth_location_rationale)
                        .setOnCancelListener { it.dismiss() }
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                            requestPermissions(arrayOf(locationPermission), LOCATION_PERMISSION_REQUEST_CODE)
                        }
                        .show()
                } else {
                    requestPermissions(arrayOf(locationPermission), LOCATION_PERMISSION_REQUEST_CODE)
                }
            } else {
                requestUserLocation()
            }
        } else {
            requestUserLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestUserLocation() {
        showLoadingState()
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null)
    }

    private fun loadWeatherInfo(lat: Double, lon: Double) {
        showLoadingState()
        // сохраняем вызов в переменную, чтобы иметь возможность отменить запрос при закрытии активити
        // при необходимости защитить ключ API от кражи необходимо перенести его в функцию NDK для усложения доступа
        // при декомпиляции, либо перенести логику на сервер
        weatherDataCall = weatherAPI.weatherByCoordinates(lat, lon, getString(R.string.openweathermap_api_key))
        weatherDataCall?.enqueue(object : Callback<WeatherData> {
            override fun onResponse(call: Call<WeatherData>, response: Response<WeatherData>) {
                val weatherData = response.body()
                if (response.isSuccessful && weatherData != null) {
                    val weatherText = with(weatherData) {
                        getString(
                            R.string.weather_info, name, main.temp,
                            weather[0].description, main.humidity, main.pressure
                        )
                    }
                    showSnackbar(weatherText)
                }
                hideLoadingState()
                weatherDataCall = null
            }

            override fun onFailure(call: Call<WeatherData>, t: Throwable) {
                showSnackbar(t.localizedMessage)
                hideLoadingState()
                weatherDataCall = null
            }
        })
    }

    private fun showSnackbar(text: String) {
        val snackbarView = Snackbar.make(authRootView, text, Snackbar.LENGTH_LONG)
            .setAction(android.R.string.ok) {}
        val textView = snackbarView.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.maxLines = 3
        snackbarView.show()
    }

    /**
     * Отобразить состояние загрузки и заблокировать доступ к интерфейсу.
     */
    private fun showLoadingState() {
        authLoginProgressBar.visibility = View.VISIBLE
        authEmailInput.clearFocus()
        authPasswordInput.clearFocus()
        hideKeyboardFrom()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideKeyboardFrom() {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(authRootView.windowToken, 0)
    }

    /**
     * Скрыть состояние загрузки и разблокировать доступ к интерфейсу.
     */
    private fun hideLoadingState() {
        authLoginProgressBar.visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    override fun onDestroy() {
        weatherDataCall?.cancel()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestUserLocation()
                } else {
                    // если пользователь не дал разрешение на местоположение отобразить погоду для Москвы
                    loadWeatherInfo(55.753086, 37.6239223)
                }
            }
            else -> {
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.auth_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
        when (item?.itemId) {
            R.id.auth_menu_create_account -> {
                // переходим на активити создания аккаунта
                Toast.makeText(this@AuthActivity, "Создать аккаунт?", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun isUserInputValid(email: String, password: String): Boolean {
        var result = true
        if (!email.isValidEmail()) {
            authEmailInputLayout.error = getString(R.string.auth_error_invalid_email)
            result = false
        } else {
            authEmailInputLayout.error = ""
        }
        if (!password.isPasswordStrong()) {
            authPasswordInputLayout.error = getString(R.string.auth_error_password_not_strong_enough)
            result = false
        } else {
            authPasswordInputLayout.error = ""
        }
        return result
    }
}
