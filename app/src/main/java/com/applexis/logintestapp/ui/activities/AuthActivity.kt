package com.applexis.logintestapp.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import com.applexis.logintestapp.isPasswordStrong
import com.applexis.logintestapp.isValidEmail
import android.view.View.OnTouchListener
import android.widget.Toast
import com.applexis.logintestapp.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_auth.*

class AuthActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        supportActionBar?.title = getString(R.string.auth_activity_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        authBtn.setOnClickListener {
            val email = authEmailInput.text.toString().trim()
            val password = authPasswordInput.text.toString()
            if (isUserInputValid(email, password)) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
                authLoginProgressBar.visibility = View.VISIBLE

                authLoginProgressBar.visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }

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
