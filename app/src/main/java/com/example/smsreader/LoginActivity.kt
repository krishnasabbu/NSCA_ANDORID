package com.example.smsreader

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var edtUsername: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button

    private val LOGIN_URL = "https://script.google.com/macros/s/AKfycbws-3vOds45ba7yDXhz10qYd3ENvrHliFlS-io6Qd5h3C6Bis9b7IaY1EZPSoQDeVrA7Q/exec"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        edtUsername = findViewById(R.id.edtUsername)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(username, password)
        }
    }

    private fun loginUser(username: String, password: String) {
        Thread {
            try {
                val url = URL(LOGIN_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                // Create request JSON
                val jsonBody = """
                    {
                        "action": "login",
                        "phone": "$username",
                        "password": "$password"
                    }
                """.trimIndent()

                // Write the data
                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.writeBytes(jsonBody)
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }

                runOnUiThread {
                    if (responseCode == 200) {
                        // Parse JSON
                        try {
                            val json = JSONObject(response)
                            val status = json.getString("status")

                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()

                        } catch (e: Exception) {
                            Toast.makeText(this, "Invalid response", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(this, "Server error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (ex: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}

