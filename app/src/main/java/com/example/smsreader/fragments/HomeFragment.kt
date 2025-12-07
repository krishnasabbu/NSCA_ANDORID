package com.example.smsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smsreader.R
import com.example.smsreader.adapters.PlayersAdapter
import com.example.smsreader.api.ApiService
import com.example.smsreader.models.Player
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    private lateinit var rvRecentPlayers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvViewAllPlayers: TextView
    private lateinit var tvTotalPlayers: TextView
    private lateinit var playersAdapter: PlayersAdapter
    private val playersList = mutableListOf<Player>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardAttendance = view.findViewById<MaterialCardView>(R.id.cardAttendance)
        val cardPayments = view.findViewById<MaterialCardView>(R.id.cardPayments)
        val cardSession = view.findViewById<MaterialCardView>(R.id.cardSession)

        rvRecentPlayers = view.findViewById(R.id.rvRecentPlayers)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)
        tvViewAllPlayers = view.findViewById(R.id.tvViewAllPlayers)
        tvTotalPlayers = view.findViewById(R.id.tvTotalPlayers)

        setupRecyclerView()
        loadPlayers()

        cardAttendance.setOnClickListener {
            navigateToAttendance()
        }

        cardPayments.setOnClickListener {
            navigateToPayments()
        }

        cardSession.setOnClickListener {
            navigateToSession()
        }

        tvViewAllPlayers.setOnClickListener {
            navigateToPlayers()
        }
    }

    private fun setupRecyclerView() {
        playersAdapter = PlayersAdapter(playersList)
        rvRecentPlayers.layoutManager = LinearLayoutManager(requireContext())
        rvRecentPlayers.adapter = playersAdapter
    }

    private fun loadPlayers() {
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        rvRecentPlayers.visibility = View.GONE

        ApiService.getPlayers { response, error ->
            activity?.runOnUiThread {
                progressBar.visibility = View.GONE

                if (error != null) {
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Failed to load players: ${error.message}"
                    return@runOnUiThread
                }

                if (response?.status == "success" && response.players.isNotEmpty()) {
                    playersList.clear()
                    playersList.addAll(response.players.take(5))
                    playersAdapter.notifyDataSetChanged()
                    rvRecentPlayers.visibility = View.VISIBLE
                    tvTotalPlayers.text = response.players.size.toString()
                } else {
                    tvError.visibility = View.VISIBLE
                    tvError.text = response?.message ?: "Failed to load players"
                }
            }
        }
    }

    private fun navigateToAttendance() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AttendanceFragment())
            .commit()
    }

    private fun navigateToPayments() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, PaymentsFragment())
            .commit()
    }

    private fun navigateToSession() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, TrainingFragment())
            .commit()
    }

    private fun navigateToPlayers() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, PlayersFragment())
            .commit()
    }
}
