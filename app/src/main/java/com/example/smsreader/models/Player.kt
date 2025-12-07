package com.example.smsreader.models

data class Player(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "",
    val age: String = "",
    val batch: String = "",
    val batchId: String = "",
    val specialization: String = "",
    val joinDate: String = "",
    val status: String = "",
    val avatar: String = "",
    val skillLevel: String = "",
    val battingStyle: String = "",
    val bowlingStyle: String = "",
    val fitnessLevel: String = "",
    val experience: String = "",
    val rating: String = "",
    val studentsCount: String = "",
    val permissions: String = "",
    val assignedCoachId: String = "",
    val isFirstLogin: String = "",
    val coachingType: String = "",
    val monthlyFee: String = "",
    val altPhone: String = "",
    val upi: String = ""
)

data class ApiResponse(
    val status: String,
    val message: String? = null,
    val data: Any? = null
)

data class LoginResponse(
    val status: String,
    val message: String,
    val user: Player? = null
)

data class PlayersResponse(
    val status: String,
    val message: String,
    val players: List<Player> = emptyList()
)
