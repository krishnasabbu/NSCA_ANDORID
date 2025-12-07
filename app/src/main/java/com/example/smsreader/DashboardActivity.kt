package com.example.smsreader

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var statsGrid: GridLayout
    private lateinit var actionsGrid: GridLayout
    private lateinit var recentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_activity)

        statsGrid = findViewById(R.id.statsGrid)
        actionsGrid = findViewById(R.id.actionsGrid)
        recentContainer = findViewById(R.id.recentContainer)

        loadStatsCards()
        loadActionCards()
        loadRecentRegistrations()
    }

    private fun loadStatsCards() {
        val stats = listOf(
            Triple("Active Students", "120", "#2E7D32"),
            Triple("Active Coaches", "12", "#FFD600"),
            Triple("Monthly Revenue", "₹2,85,000", "#FF6B35"),
            Triple("Growth Rate", "+18%", "#1976D2")
        )

        for ((title, value, color) in stats) {
            statsGrid.addView(createStatCard(title, value, color))
        }
    }

    private fun createStatCard(title: String, value: String, color: String): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(20, 20, 20, 20)
        card.setBackgroundColor(Color.parseColor("#FFFFFF"))
        card.elevation = 6f

        val titleView = TextView(this)
        titleView.text = title
        titleView.textSize = 14f
        titleView.setTextColor(Color.parseColor(color))

        val valueView = TextView(this)
        valueView.text = value
        valueView.textSize = 20f
        valueView.setTextColor(Color.parseColor(color))
        valueView.setTypeface(null, Typeface.BOLD)

        card.addView(titleView)
        card.addView(valueView)

        val params = GridLayout.LayoutParams()
        params.width = 0
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(8, 8, 8, 8)
        card.layoutParams = params

        return card
    }

    private fun loadActionCards() {
        val actions = listOf(
            "Manage Users" to "#2E7D32",
            "Create Batch" to "#1976D2",
            "Generate Invoice" to "#FF6B35",
            "Academy Settings" to "#9C27B0"
        )

        for ((title, color) in actions) {
            actionsGrid.addView(createActionCard(title, color))
        }
    }

    private fun createActionCard(title: String, color: String): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(20, 20, 20, 20)
        card.setBackgroundColor(Color.parseColor(color))
        card.elevation = 6f

        val text = TextView(this)
        text.text = title
        text.textSize = 16f
        text.setTextColor(Color.WHITE)
        text.setTypeface(null, Typeface.BOLD)

        card.addView(text)

        val params = GridLayout.LayoutParams()
        params.width = 0
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(8, 8, 8, 8)
        card.layoutParams = params

        card.setOnClickListener {
            Toast.makeText(this, "$title clicked", Toast.LENGTH_SHORT).show()
        }

        return card
    }

    private fun loadRecentRegistrations() {
        val recent = listOf(
            "Rohit Kumar" to "Joined 2025-02-01",
            "Sanjay Verma" to "Joined 2025-01-26",
            "Mahesh Patel" to "Joined 2025-01-20"
        )

        for ((name, date) in recent) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(8, 12, 8, 12)

            val icon = TextView(this)
            icon.text = name.take(2).uppercase()
            icon.setBackgroundColor(Color.parseColor("#2E7D32"))
            icon.setTextColor(Color.WHITE)
            icon.gravity = Gravity.CENTER
            icon.textSize = 14f
            icon.width = 60
            icon.height = 60

            val textLayout = LinearLayout(this)
            textLayout.orientation = LinearLayout.VERTICAL
            textLayout.setPadding(16, 0, 0, 0)

            val nameView = TextView(this)
            nameView.text = name
            nameView.textSize = 16f
            nameView.setTypeface(null, Typeface.BOLD)

            val dateView = TextView(this)
            dateView.text = date
            dateView.textSize = 12f
            dateView.setTextColor(Color.parseColor("#2E7D32"))

            textLayout.addView(nameView)
            textLayout.addView(dateView)

            row.addView(icon)
            row.addView(textLayout)

            recentContainer.addView(row)
        }
    }
}
