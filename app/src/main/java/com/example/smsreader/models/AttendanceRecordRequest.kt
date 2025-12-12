package com.example.smsreader.models

data class AttendanceRecordRequest(
    val userId: String,
    val sessionId: String,
    val date: String,
    val status: String
)
