package com.example.smsreader.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smsreader.R
import com.example.smsreader.models.Player

class PlayersAdapter(private val players: List<Player>) :
    RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtAvatar: TextView = view.findViewById(R.id.txtAvatar)
        val txtPlayerName: TextView = view.findViewById(R.id.txtPlayerName)
        val txtPlayerPhone: TextView = view.findViewById(R.id.txtPlayerPhone)
        val txtPlayerBatch: TextView = view.findViewById(R.id.txtPlayerBatch)
        val txtPlayerRole: TextView = view.findViewById(R.id.txtPlayerRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = players[position]

        val initials = player.name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

        holder.txtAvatar.text = initials.ifEmpty { "?" }
        holder.txtPlayerName.text = player.name
        holder.txtPlayerPhone.text = "Phone: ${player.phone}"
        holder.txtPlayerBatch.text = player.batch.ifEmpty { "No Batch" }
        holder.txtPlayerRole.text = player.role.ifEmpty { "Student" }
    }

    override fun getItemCount() = players.size
}
