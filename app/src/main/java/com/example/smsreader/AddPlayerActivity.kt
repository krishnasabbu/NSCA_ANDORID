package com.example.smsreader

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smsreader.api.ApiService
import com.example.smsreader.models.Batch
import com.example.smsreader.models.Player
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AddPlayerActivity : AppCompatActivity() {

    private lateinit var edtName: TextInputEditText
    private lateinit var edtPhone: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtAge: TextInputEditText
    private lateinit var edtAltPhone: TextInputEditText
    private lateinit var edtSpecialization: TextInputEditText
    private lateinit var edtBattingStyle: TextInputEditText
    private lateinit var edtBowlingStyle: TextInputEditText
    private lateinit var edtMonthlyFee: TextInputEditText
    private lateinit var edtUpi: TextInputEditText
    private lateinit var spinnerBatch: Spinner
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val batchList = mutableListOf<Batch>()
    private var selectedBatch: Batch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_player)

        edtName = findViewById(R.id.edtName)
        edtPhone = findViewById(R.id.edtPhone)
        edtEmail = findViewById(R.id.edtEmail)
        edtAge = findViewById(R.id.edtAge)
        edtAltPhone = findViewById(R.id.edtAltPhone)
        edtSpecialization = findViewById(R.id.edtSpecialization)
        edtBattingStyle = findViewById(R.id.edtBattingStyle)
        edtBowlingStyle = findViewById(R.id.edtBowlingStyle)
        edtMonthlyFee = findViewById(R.id.edtMonthlyFee)
        edtUpi = findViewById(R.id.edtUpi)
        spinnerBatch = findViewById(R.id.spinnerBatch)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)

        loadBatches()

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            if (validateInput()) {
                savePlayer()
            }
        }
    }

    private fun loadBatches() {
        progressBar.visibility = View.VISIBLE

        ApiService.getBatches { response, error ->
            runOnUiThread {
                progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(this, "Error loading batches: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                if (response != null && response.status == "success") {
                    batchList.clear()
                    batchList.addAll(response.batches)

                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        batchList
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerBatch.adapter = adapter
                }
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
        val selectedBatch = if (spinnerBatch.selectedItem != null) {
            spinnerBatch.selectedItem as Batch
        } else {
            null
        }

        val player = Player(
            name = edtName.text.toString().trim(),
            phone = edtPhone.text.toString().trim(),
            email = edtEmail.text.toString().trim(),
            age = edtAge.text.toString().trim(),
            altPhone = edtAltPhone.text.toString().trim(),
            batch = selectedBatch?.name ?: "",
            batchId = selectedBatch?.id ?: "",
            specialization = edtSpecialization.text.toString().trim(),
            battingStyle = edtBattingStyle.text.toString().trim(),
            bowlingStyle = edtBowlingStyle.text.toString().trim(),
            monthlyFee = edtMonthlyFee.text.toString().trim(),
            upi = edtUpi.text.toString().trim()
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
