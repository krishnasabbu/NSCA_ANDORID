package com.example.smsreader.models

data class Fee(
    val id: String = "",
    val userid: String = "",
    val name: String = "",
    val phone: String = "",
    val amount: Double = 0.0,
    val paidType: String = "",
    val date: String = "",
    val remarks: String = ""
)

data class FeesResponse(
    val status: String,
    val message: String,
    val fees: List<Fee> = emptyList()
)
