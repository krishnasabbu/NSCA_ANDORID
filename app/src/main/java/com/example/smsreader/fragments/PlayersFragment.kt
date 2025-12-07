package com.example.smsreader.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.smsreader.AddPlayerActivity
import com.example.smsreader.R
import com.example.smsreader.adapters.PlayersAdapter
import com.example.smsreader.api.ApiService
import com.example.smsreader.models.Player
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class PlayersFragment : Fragment() {

    companion object {
        private const val ADD_PLAYER_REQUEST_CODE = 100
    }

    private lateinit var playersRecyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyTextView: TextView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var fabAddPlayer: FloatingActionButton

    private lateinit var playersAdapter: PlayersAdapter
    private var playersList = mutableListOf<Player>()
    private var filteredList = mutableListOf<Player>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_players, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playersRecyclerView = view.findViewById(R.id.playersRecyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        emptyTextView = view.findViewById(R.id.emptyTextView)
        searchEditText = view.findViewById(R.id.searchEditText)
        fabAddPlayer = view.findViewById(R.id.fabAddPlayer)

        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()
        setupFab()

        loadPlayers()
    }

    private fun setupRecyclerView() {
        playersAdapter = PlayersAdapter(filteredList)
        playersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = playersAdapter
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterPlayers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadPlayers()
        }
    }

    private fun setupFab() {
        fabAddPlayer.setOnClickListener {
            val intent = Intent(requireContext(), AddPlayerActivity::class.java)
            startActivityForResult(intent, ADD_PLAYER_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_PLAYER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadPlayers()
        }
    }

    private fun loadPlayers() {
        showLoading(true)

        ApiService.getPlayers { response, error ->
            activity?.runOnUiThread {
                showLoading(false)
                swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                if (response != null && response.status == "success") {
                    playersList.clear()
                    playersList.addAll(response.players)
                    filterPlayers(searchEditText.text.toString())
                } else {
                    Toast.makeText(
                        context,
                        response?.message ?: "Failed to load players",
                        Toast.LENGTH_LONG
                    ).show()
                }

                updateEmptyState()
            }
        }
    }

    private fun filterPlayers(query: String) {
        filteredList.clear()

        if (query.isEmpty()) {
            filteredList.addAll(playersList)
        } else {
            val lowerQuery = query.lowercase()
            filteredList.addAll(
                playersList.filter {
                    it.name.lowercase().contains(lowerQuery) ||
                            it.phone.contains(lowerQuery) ||
                            it.batch.lowercase().contains(lowerQuery)
                }
            )
        }

        playersAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            playersRecyclerView.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            playersRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateEmptyState() {
        if (filteredList.isEmpty()) {
            emptyTextView.visibility = View.VISIBLE
            playersRecyclerView.visibility = View.GONE
        } else {
            emptyTextView.visibility = View.GONE
            playersRecyclerView.visibility = View.VISIBLE
        }
    }
}
