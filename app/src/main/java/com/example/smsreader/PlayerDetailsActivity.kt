package com.example.smsreader

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smsreader.models.Player
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson

class PlayerDetailsActivity : AppCompatActivity() {

    private lateinit var tvPlayerName: TextView
    private lateinit var tvPlayerRole: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAltPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAge: TextView
    private lateinit var tvBatch: TextView
    private lateinit var tvJoinDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvSpecialization: TextView
    private lateinit var tvBattingStyle: TextView
    private lateinit var tvBowlingStyle: TextView
    private lateinit var tvMonthlyFee: TextView
    private lateinit var tvUpi: TextView
    private lateinit var btnClose: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_details)

        tvPlayerName = findViewById(R.id.tvPlayerName)
        tvPlayerRole = findViewById(R.id.tvPlayerRole)
        tvPhone = findViewById(R.id.tvPhone)
        tvAltPhone = findViewById(R.id.tvAltPhone)
        tvEmail = findViewById(R.id.tvEmail)
        tvAge = findViewById(R.id.tvAge)
        tvBatch = findViewById(R.id.tvBatch)
        tvJoinDate = findViewById(R.id.tvJoinDate)
        tvStatus = findViewById(R.id.tvStatus)
        tvSpecialization = findViewById(R.id.tvSpecialization)
        tvBattingStyle = findViewById(R.id.tvBattingStyle)
        tvBowlingStyle = findViewById(R.id.tvBowlingStyle)
        tvMonthlyFee = findViewById(R.id.tvMonthlyFee)
        tvUpi = findViewById(R.id.tvUpi)
        btnClose = findViewById(R.id.btnClose)

        val playerJson = intent.getStringExtra("player")
        if (playerJson != null) {
            val player = Gson().fromJson(playerJson, Player::class.java)
            displayPlayerDetails(player)
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun displayPlayerDetails(player: Player) {
        tvPlayerName.text = player.name.ifEmpty { "-" }
        tvPlayerRole.text = when (player.role.lowercase()) {
            "student" -> "Player"
            "coach" -> "Coach"
            "admin" -> "Organizer"
            else -> player.role.ifEmpty { "Student" }
        }
        tvPhone.text = player.phone.ifEmpty { "-" }
        tvAltPhone.text = player.altPhone.ifEmpty { "-" }
        tvEmail.text = player.email.ifEmpty { "-" }
        tvAge.text = player.age.ifEmpty { "-" }
        tvBatch.text = player.batch.ifEmpty { "-" }
        tvJoinDate.text = player.joinDate.ifEmpty { "-" }
        tvStatus.text = player.status.ifEmpty { "Active" }
        tvSpecialization.text = player.specialization.ifEmpty { "-" }
        tvBattingStyle.text = player.battingStyle.ifEmpty { "-" }
        tvBowlingStyle.text = player.bowlingStyle.ifEmpty { "-" }
        tvMonthlyFee.text = if (player.monthlyFee.isNotEmpty()) "₹${player.monthlyFee}" else "-"
        tvUpi.text = player.upi.ifEmpty { "-" }
    }
}
