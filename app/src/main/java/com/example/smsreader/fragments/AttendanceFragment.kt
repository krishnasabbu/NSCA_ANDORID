package com.example.smsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.smsreader.models.Player
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class AttendanceFragment : Fragment() {

    private lateinit var txtDate: TextView
    private lateinit var batchSpinner: Spinner
    private lateinit var attendanceRecyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnSubmitAttendance: MaterialButton

    private lateinit var attendanceAdapter: AttendanceAdapter
    private var playersList = mutableListOf<Player>()

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
        batchSpinner = view.findViewById(R.id.batchSpinner)
        attendanceRecyclerView = view.findViewById(R.id.attendanceRecyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        btnSubmitAttendance = view.findViewById(R.id.btnSubmitAttendance)

        setupDate()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSubmitButton()

        loadPlayers()
    }

    private fun setupDate() {
        val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        txtDate.text = dateFormat.format(Date())
    }

    private fun setupRecyclerView() {
        attendanceAdapter = AttendanceAdapter(playersList)
        attendanceRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = attendanceAdapter
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadPlayers()
        }
    }

    private fun setupSubmitButton() {
        btnSubmitAttendance.setOnClickListener {
            val presentPlayers = attendanceAdapter.getPresentPlayers()

            if (presentPlayers.isEmpty()) {
                Toast.makeText(context, "No players marked as present", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitAttendance(presentPlayers)
        }
    }

    private fun submitAttendance(presentPlayers: List<Player>) {
        swipeRefresh.isRefreshing = true
        btnSubmitAttendance.isEnabled = false

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.format(Date())
        val playerIds = presentPlayers.map { it.id }

        ApiService.submitAttendance(playerIds, date, "admin") { response, error ->
            activity?.runOnUiThread {
                swipeRefresh.isRefreshing = false
                btnSubmitAttendance.isEnabled = true

                if (error != null) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                if (response != null && response.status == "success") {
                    Toast.makeText(
                        context,
                        "Attendance submitted for ${presentPlayers.size} players",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadPlayers()
                } else {
                    Toast.makeText(
                        context,
                        response?.message ?: "Failed to submit attendance",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadPlayers() {
        swipeRefresh.isRefreshing = true

        ApiService.getPlayers { response, error ->
            activity?.runOnUiThread {
                swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                if (response != null && response.status == "success") {
                    playersList.clear()
                    playersList.addAll(response.players)
                    attendanceAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(
                        context,
                        response?.message ?: "Failed to load players",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
