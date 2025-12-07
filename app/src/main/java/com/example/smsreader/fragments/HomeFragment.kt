package com.example.smsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smsreader.R
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

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

        cardAttendance.setOnClickListener {
            navigateToAttendance()
        }

        cardPayments.setOnClickListener {
            navigateToPayments()
        }

        cardSession.setOnClickListener {
            navigateToSession()
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
}
