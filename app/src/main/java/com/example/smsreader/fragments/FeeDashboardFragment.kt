package com.example.smsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smsreader.R
import com.example.smsreader.adapters.FeeAdapter
import com.example.smsreader.adapters.FeeItem
import com.example.smsreader.api.ApiService
import com.example.smsreader.models.Fee
import com.example.smsreader.models.Player
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

class FeeDashboardFragment : Fragment() {

    private lateinit var tvExpected: TextView
    private lateinit var tvCollected: TextView
    private lateinit var tvPending: TextView
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerYear: Spinner
    private lateinit var btnAllStudents: MaterialButton
    private lateinit var btnPaid: MaterialButton
    private lateinit var btnPending: MaterialButton
    private lateinit var rvFees: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var feeAdapter: FeeAdapter

    private val allFees = mutableListOf<Fee>()
    private val allPlayers = mutableListOf<Player>()
    private val displayFees = mutableListOf<FeeItem>()
    private var currentFilter = "all"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fee_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvExpected = view.findViewById(R.id.tvExpected)
        tvCollected = view.findViewById(R.id.tvCollected)
        tvPending = view.findViewById(R.id.tvPending)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        spinnerYear = view.findViewById(R.id.spinnerYear)
        btnAllStudents = view.findViewById(R.id.btnAllStudents)
        btnPaid = view.findViewById(R.id.btnPaid)
        btnPending = view.findViewById(R.id.btnPending)
        rvFees = view.findViewById(R.id.rvFees)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)

        setupSpinners()
        setupRecyclerView()
        setupFilterButtons()
        loadData()
    }

    private fun setupSpinners() {
        val months = listOf("January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December")
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter
        spinnerMonth.setSelection(11)

        val years = (2020..2030).map { it.toString() }
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYear.adapter = yearAdapter
        spinnerYear.setSelection(5)

        spinnerMonth.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterFees()
                updateSummary()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        spinnerYear.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterFees()
                updateSummary()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        feeAdapter = FeeAdapter(displayFees)
        rvFees.layoutManager = LinearLayoutManager(requireContext())
        rvFees.adapter = feeAdapter
    }

    private fun setupFilterButtons() {
        btnAllStudents.setOnClickListener {
            currentFilter = "all"
            updateFilterButtons()
            filterFees()
        }

        btnPaid.setOnClickListener {
            currentFilter = "paid"
            updateFilterButtons()
            filterFees()
        }

        btnPending.setOnClickListener {
            currentFilter = "pending"
            updateFilterButtons()
            filterFees()
        }
    }

    private fun updateFilterButtons() {
        val primaryColor = resources.getColor(R.color.primary_green, null)
        val whiteColor = resources.getColor(R.color.white, null)
        val textColor = resources.getColor(R.color.text_primary, null)

        when (currentFilter) {
            "all" -> {
                btnAllStudents.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                btnAllStudents.setTextColor(whiteColor)
                btnPaid.backgroundTintList = android.content.res.ColorStateList.valueOf(whiteColor)
                btnPaid.setTextColor(textColor)
                btnPending.backgroundTintList = android.content.res.ColorStateList.valueOf(whiteColor)
                btnPending.setTextColor(textColor)
            }
            "paid" -> {
                btnAllStudents.backgroundTintList = android.content.res.ColorStateList.valueOf(whiteColor)
                btnAllStudents.setTextColor(textColor)
                btnPaid.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                btnPaid.setTextColor(whiteColor)
                btnPending.backgroundTintList = android.content.res.ColorStateList.valueOf(whiteColor)
                btnPending.setTextColor(textColor)
            }
            "pending" -> {
                btnAllStudents.backgroundTintList = android.content.res.ColorStateList.valueOf(whiteColor)
                btnAllStudents.setTextColor(textColor)
                btnPaid.backgroundTintList = android.content.res.ColorStateList.valueOf(whiteColor)
                btnPaid.setTextColor(textColor)
                btnPending.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                btnPending.setTextColor(whiteColor)
            }
        }
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        rvFees.visibility = View.GONE

        ApiService.getPlayers { playersResponse, playersError ->
            if (playersError != null) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Failed to load players: ${playersError.message}"
                }
                return@getPlayers
            }

            allPlayers.clear()
            allPlayers.addAll(playersResponse?.players?.filter {
                it.role.equals("student", ignoreCase = true)
            } ?: emptyList())

            ApiService.getFees { fees, feesError ->
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE

                    if (feesError != null) {
                        tvError.visibility = View.VISIBLE
                        tvError.text = "Failed to load fees: ${feesError.message}"
                        return@runOnUiThread
                    }

                    allFees.clear()
                    allFees.addAll(fees ?: emptyList())
                    calculateAndDisplayFees()
                    rvFees.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun calculateAndDisplayFees() {
        displayFees.clear()

        allPlayers.forEach { player ->
            val expected = player.monthlyFee.toDoubleOrNull() ?: 0.0
            val paid = allFees.filter { it.userid == player.id && it.paidType == "CREDIT" }
                .sumOf { it.amount }
            val balance = expected - paid

            displayFees.add(FeeItem(player, expected, paid, balance))
        }

        filterFees()
        updateSummary()
    }

    private fun filterFees() {
        val filtered = when (currentFilter) {
            "paid" -> displayFees.filter { it.balance <= 0 }
            "pending" -> displayFees.filter { it.balance > 0 }
            else -> displayFees
        }

        val adapter = FeeAdapter(filtered)
        rvFees.adapter = adapter
    }

    private fun updateSummary() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val totalExpected = displayFees.sumOf { it.expected }
        val totalCollected = displayFees.sumOf { it.paid }
        val totalPending = displayFees.sumOf { it.balance }

        tvExpected.text = formatter.format(totalExpected)
        tvCollected.text = formatter.format(totalCollected)
        tvPending.text = formatter.format(totalPending)
    }
}
