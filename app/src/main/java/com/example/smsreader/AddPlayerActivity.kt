package com.example.smsreader

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smsreader.api.ApiService
import com.example.smsreader.models.Player
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AddPlayerActivity : AppCompatActivity() {

    private lateinit var edtName: TextInputEditText
    private lateinit var edtPhone: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtAge: TextInputEditText
    private lateinit var edtBatch: TextInputEditText
    private lateinit var edtSpecialization: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_player)

        edtName = findViewById(R.id.edtName)
        edtPhone = findViewById(R.id.edtPhone)
        edtEmail = findViewById(R.id.edtEmail)
        edtAge = findViewById(R.id.edtAge)
        edtBatch = findViewById(R.id.edtBatch)
        edtSpecialization = findViewById(R.id.edtSpecialization)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            if (validateInput()) {
                savePlayer()
            }
        }
    }

    private fun validateInput(): Boolean {
        val name = edtName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()

        if (name.isEmpty()) {
            edtName.error = "Name is required"
            edtName.requestFocus()
            return false
        }

        if (phone.isEmpty()) {
            edtPhone.error = "Phone number is required"
            edtPhone.requestFocus()
            return false
        }

        return true
    }

    private fun savePlayer() {
        val player = Player(
            name = edtName.text.toString().trim(),
            phone = edtPhone.text.toString().trim(),
            email = edtEmail.text.toString().trim(),
            age = edtAge.text.toString().trim(),
            batch = edtBatch.text.toString().trim(),
            specialization = edtSpecialization.text.toString().trim()
        )

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false
        btnCancel.isEnabled = false

        ApiService.addPlayer(player) { response, error ->
            runOnUiThread {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                btnCancel.isEnabled = true

                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                if (response != null && response.status == "success") {
                    Toast.makeText(this, "Player added successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        response?.message ?: "Failed to add player",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
