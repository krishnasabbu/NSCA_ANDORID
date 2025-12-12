package com.example.smsreader.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.smsreader.MainActivity
import com.example.smsreader.R
import com.example.smsreader.adapters.SmsTransactionAdapter
import com.example.smsreader.api.ApiService
import com.example.smsreader.models.SmsTransaction
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.Locale

class SmsTransactionFragment : Fragment() {

    private lateinit var tvTotalMessages: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvUniqueSenders: TextView
    private lateinit var edtSearch: TextInputEditText
    private lateinit var spinnerTransactionType: Spinner
    private lateinit var spinnerSender: Spinner
    private lateinit var btnSync: MaterialButton
    private lateinit var rvSmsTransactions: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var transactionAdapter: SmsTransactionAdapter

    private val allTransactions = mutableListOf<SmsTransaction>()
    private val filteredTransactions = mutableListOf<SmsTransaction>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sms_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalMessages = view.findViewById(R.id.tvTotalMessages)
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)
        tvUniqueSenders = view.findViewById(R.id.tvUniqueSenders)
        edtSearch = view.findViewById(R.id.edtSearch)
        spinnerTransactionType = view.findViewById(R.id.spinnerTransactionType)
        spinnerSender = view.findViewById(R.id.spinnerSender)
        btnSync = view.findViewById(R.id.btnSync)
        rvSmsTransactions = view.findViewById(R.id.rvSmsTransactions)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)

        setupRecyclerView()
        setupFilters()
        loadTransactions()

        btnSync.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MessagesFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = SmsTransactionAdapter(filteredTransactions)
        rvSmsTransactions.layoutManager = LinearLayoutManager(requireContext())
        rvSmsTransactions.adapter = transactionAdapter
    }

    private fun setupFilters() {
        val types = listOf("All Types", "CREDIT", "DEBIT")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTransactionType.adapter = typeAdapter

        spinnerTransactionType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterTransactions()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterTransactions()
            }
        })
    }

    private fun loadTransactions() {
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        rvSmsTransactions.visibility = View.GONE

        ApiService.getSmsTransactions { transactions, error ->
            activity?.runOnUiThread {
                progressBar.visibility = View.GONE

                if (error != null) {
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Failed to load transactions: ${error.message}"
                    return@runOnUiThread
                }

                allTransactions.clear()
                allTransactions.addAll(transactions ?: emptyList())

                setupSenderFilter()
                filterTransactions()
                updateSummary()
                rvSmsTransactions.visibility = View.VISIBLE
            }
        }
    }

    private fun setupSenderFilter() {
        val senders = mutableSetOf("All Senders")
        senders.addAll(allTransactions.map { it.senderAddress }.distinct())

        val senderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, senders.toList())
        senderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSender.adapter = senderAdapter

        spinnerSender.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterTransactions()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun filterTransactions() {
        val searchText = edtSearch.text.toString().lowercase()
        val selectedType = spinnerTransactionType.selectedItem?.toString() ?: "All Types"
        val selectedSender = spinnerSender.selectedItem?.toString() ?: "All Senders"

        filteredTransactions.clear()
        filteredTransactions.addAll(allTransactions.filter { transaction ->
            val matchesSearch = searchText.isEmpty() ||
                    transaction.fullMessage.lowercase().contains(searchText) ||
                    transaction.partyName.lowercase().contains(searchText) ||
                    transaction.upiId.lowercase().contains(searchText)

            val matchesType = selectedType == "All Types" ||
                    transaction.transactionType.equals(selectedType, ignoreCase = true)

            val matchesSender = selectedSender == "All Senders" ||
                    transaction.senderAddress == selectedSender

            matchesSearch && matchesType && matchesSender
        })

        val adapter = SmsTransactionAdapter(filteredTransactions)
        rvSmsTransactions.adapter = adapter

        updateFilteredSummary()
    }

    private fun updateFilteredSummary() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val totalAmount = filteredTransactions.sumOf { it.amount }
        val uniqueSenders = filteredTransactions.map { it.senderAddress }.distinct().size

        tvTotalMessages.text = filteredTransactions.size.toString()
        tvTotalAmount.text = formatter.format(totalAmount)
        tvUniqueSenders.text = uniqueSenders.toString()
    }

    private fun updateSummary() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val totalAmount = allTransactions.sumOf { it.amount }
        val uniqueSenders = allTransactions.map { it.senderAddress }.distinct().size

        tvTotalMessages.text = allTransactions.size.toString()
        tvTotalAmount.text = formatter.format(totalAmount)
        tvUniqueSenders.text = uniqueSenders.toString()
    }
}
