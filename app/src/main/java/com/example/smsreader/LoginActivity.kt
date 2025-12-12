package com.example.smsreader

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smsreader.api.ApiService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var edtUsername: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        edtUsername = findViewById(R.id.edtUsername)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (validateInput(username, password)) {
                loginUser(username, password)
            }
        }
    }

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            edtUsername.error = "Phone number is required"
            edtUsername.requestFocus()
            return false
        }

        if (username.length < 10) {
            edtUsername.error = "Enter a valid phone number"
            edtUsername.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            edtPassword.error = "Password is required"
            edtPassword.requestFocus()
            return false
        }

        if (password.length < 4) {
            edtPassword.error = "Password must be at least 4 characters"
            edtPassword.requestFocus()
            return false
        }

        return true
    }

    private fun loginUser(username: String, password: String) {
        showLoading(true)

        ApiService.login(username, password) { response, error ->
            runOnUiThread {
                showLoading(false)

                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                if (response != null && response.status == "success" && response.user != null) {
                    val sharedPrefs = getSharedPreferences("user_session", MODE_PRIVATE)
                    sharedPrefs.edit().apply {
                        putString("user_id", response.user.id)
                        putString("user_name", response.user.name)
                        putString("user_role", response.user.role)
                        putString("user_phone", response.user.phone)
                        putString("user_email", response.user.email)
                        apply()
                    }

                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("user_name", response.user.name)
                    intent.putExtra("user_role", response.user.role)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        response?.message ?: "Login failed. Please check your credentials.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            btnLogin.alpha = 0.5f
        } else {
            progressBar.visibility = View.GONE
            btnLogin.isEnabled = true
            btnLogin.alpha = 1f
        }
    }
}
