package com.example.smsreader.models

data class SmsTransaction(
    val id: String = "",
    val smsDate: String = "",
    val senderAddress: String = "",
    val transactionType: String = "",
    val amount: Double = 0.0,
    val upiId: String = "",
    val transactionId: String = "",
    val partyName: String = "",
    val fullMessage: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class SmsTransactionsResponse(
    val status: String,
    val message: String,
    val transactions: List<SmsTransaction> = emptyList()
)
