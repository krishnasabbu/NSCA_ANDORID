package com.example.smsreader.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.smsreader.R
import com.example.smsreader.adapters.AttendanceAdapter
import com.example.smsreader.api.ApiService
import com.example.smsreader.models.AttendanceRecord
import com.example.smsreader.models.AttendanceRecordRequest
import com.example.smsreader.models.Batch
import com.example.smsreader.models.Player
import com.example.smsreader.models.PlayerWithAttendance
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AttendanceFragment : Fragment() {

    private lateinit var txtDate: TextView
    private lateinit var txtSummary: TextView
    private lateinit var batchSpinner: Spinner
    private lateinit var attendanceRecyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnDatePicker: MaterialButton
    private lateinit var btnMarkAttendance: MaterialButton
    private lateinit var btnSelectAllAbsent: MaterialButton

    private lateinit var attendanceAdapter: AttendanceAdapter
    private var allPlayers = mutableListOf<Player>()
    private var attendanceRecords = mutableListOf<AttendanceRecord>()
    private var playersWithAttendance = mutableListOf<PlayerWithAttendance>()
    private var batchesList = mutableListOf<Batch>()
    private var selectedBatch: Batch? = null
    private var currentUserId: String = ""
    private var selectedDate: Long = System.currentTimeMillis()
    private var selectedDateString: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtDate = view.findViewById(R.id.txtDate)
        txtSummary = view.findViewById(R.id.txtSummary)
        batchSpinner = view.findViewById(R.id.batchSpinner)
        attendanceRecyclerView = view.findViewById(R.id.attendanceRecyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        btnDatePicker = view.findViewById(R.id.btnDatePicker)
        btnMarkAttendance = view.findViewById(R.id.btnMarkAttendance)
        btnSelectAllAbsent = view.findViewById(R.id.btnSelectAllAbsent)

        loadCurrentUserId()
        setupDate()
        setupDatePicker()
        setupRecyclerView()
        setupSwipeRefresh()
        setupBatchSpinner()
        setupButtons()

        loadBatches()
        loadData()
    }

    private fun loadCurrentUserId() {
        val sharedPrefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getString("user_id", "") ?: ""
    }

    private fun setupDate() {
        updateDateDisplay()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDateString = todayFormat.format(Date(selectedDate))

        val calendar = Calendar.getInstance()
        val today = todayFormat.format(calendar.time)

        txtDate.text = if (selectedDateString == today) {
            "Today"
        } else {
            dateFormat.format(Date(selectedDate))
        }
    }

    private fun setupDatePicker() {
        btnDatePicker.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(selectedDate)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = selection
                updateDateDisplay()
                loadData()
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupBatchSpinner() {
        batchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBatch = if (position > 0 && position <= batchesList.size) {
                    batchesList[position - 1]
                } else {
                    null
                }
                filterPlayersByBatch()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedBatch = null
                filterPlayersByBatch()
            }
        }
    }

    private fun setupRecyclerView() {
        attendanceAdapter = AttendanceAdapter(playersWithAttendance, isToday()) {
            updateUI()
        }
        attendanceRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = attendanceAdapter
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun setupButtons() {
        btnMarkAttendance.setOnClickListener {
            markAttendance()
        }

        btnSelectAllAbsent.setOnClickListener {
            attendanceAdapter.selectAllAbsent()
        }
    }

    private fun loadBatches() {
        ApiService.getBatches { response, error ->
            activity?.runOnUiThread {
                if (error != null) {
                    Toast.makeText(context, "Error loading batches: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                if (response != null && response.status == "success") {
                    batchesList.clear()
                    batchesList.addAll(response.batches)

                    val batchNames = mutableListOf("All Batches")
                    batchNames.addAll(batchesList.map { it.name })

                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        batchNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    batchSpinner.adapter = adapter
                }
            }
        }
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        loadPlayers()
    }

    private fun loadPlayers() {
        ApiService.getPlayers { response, error ->
            activity?.runOnUiThread {
                if (error != null) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                if (response != null && response.status == "success") {
                    allPlayers.clear()
                    allPlayers.addAll(response.players)
                    loadAttendanceRecords()
                } else {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        context,
                        response?.message ?: "Failed to load players",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadAttendanceRecords() {
        ApiService.getAttendanceRecords(selectedDateString) { records, error ->
            activity?.runOnUiThread {
                swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(context, "Error loading attendance: ${error.message}", Toast.LENGTH_SHORT).show()
                    attendanceRecords.clear()
                } else {
                    attendanceRecords.clear()
                    records?.let { attendanceRecords.addAll(it) }
                }

                matchPlayersWithAttendance()
                filterPlayersByBatch()
            }
        }
    }

    private fun matchPlayersWithAttendance() {
        playersWithAttendance.clear()

        allPlayers.forEach { player ->
            val record = attendanceRecords.find { it.userId == player.id }
            playersWithAttendance.add(PlayerWithAttendance(player, record, false))
        }
    }

    private fun filterPlayersByBatch() {
        val filteredList = if (selectedBatch != null) {
            playersWithAttendance.filter { it.player.batchId == selectedBatch?.id }
        } else {
            playersWithAttendance
        }

        attendanceAdapter.updateData(filteredList)
        updateUI()
    }

    private fun updateUI() {
        val total = attendanceAdapter.getTotalCount()
        val present = attendanceAdapter.getPresentCount()
        val absent = attendanceAdapter.getAbsentCount()

        txtSummary.text = "Total: $total | Present: $present | Absent: $absent"

        val isCurrentDateToday = isToday()
        val hasAbsentPlayers = absent > 0

        if (isCurrentDateToday && hasAbsentPlayers) {
            btnMarkAttendance.visibility = View.VISIBLE
            btnSelectAllAbsent.visibility = View.VISIBLE

            val selectedCount = attendanceAdapter.getSelectedPlayers().size
            btnMarkAttendance.text = if (selectedCount > 0) {
                "Mark Selected as Present ($selectedCount)"
            } else {
                "Mark Selected as Present"
            }
        } else {
            btnMarkAttendance.visibility = View.GONE
            btnSelectAllAbsent.visibility = View.GONE
        }
    }

    private fun isToday(): Boolean {
        val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = todayFormat.format(Calendar.getInstance().time)
        return selectedDateString == today
    }

    private fun markAttendance() {
        val selectedPlayers = attendanceAdapter.getSelectedPlayers()

        if (selectedPlayers.isEmpty()) {
            Toast.makeText(context, "Please select at least one player", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedBatch == null) {
            Toast.makeText(context, "Please select a batch", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId.isEmpty()) {
            Toast.makeText(context, "User session not found. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        swipeRefresh.isRefreshing = true
        btnMarkAttendance.isEnabled = false

        val records = selectedPlayers.map { playerWithAttendance ->
            AttendanceRecordRequest(
                userId = playerWithAttendance.player.id,
                sessionId = selectedBatch!!.id,
                date = selectedDateString,
                status = "Present"
            )
        }

        ApiService.submitAttendance(currentUserId, records) { response, error ->
            activity?.runOnUiThread {
                swipeRefresh.isRefreshing = false
                btnMarkAttendance.isEnabled = true

                if (error != null) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                if (response != null && response.status == "success") {
                    Toast.makeText(
                        context,
                        "Attendance marked for ${selectedPlayers.size} player(s)",
                        Toast.LENGTH_SHORT
                    ).show()
                    attendanceAdapter.clearSelection()
                    loadData()
                } else {
                    Toast.makeText(
                        context,
                        response?.message ?: "Failed to submit attendance",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
