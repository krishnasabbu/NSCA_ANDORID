package com.example.smsreader.api

import com.example.smsreader.models.ApiResponse
import com.example.smsreader.models.LoginResponse
import com.example.smsreader.models.Player
import com.example.smsreader.models.PlayersResponse
import com.google.gson.Gson
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object ApiService {
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbws-3vOds45ba7yDXhz10qYd3ENvrHliFlS-io6Qd5h3C6Bis9b7IaY1EZPSoQDeVrA7Q/exec"
    private val gson = Gson()

    fun login(phone: String, password: String, callback: (LoginResponse?, Exception?) -> Unit) {
        Thread {
            try {
                val url = URL(BASE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val jsonBody = JSONObject().apply {
                    put("action", "login")
                    put("phone", phone)
                    put("password", password)
                }.toString()

                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.writeBytes(jsonBody)
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                val loginResponse = gson.fromJson(response, LoginResponse::class.java)
                callback(loginResponse, null)

            } catch (e: Exception) {
                callback(null, e)
            }
        }.start()
    }

    fun getPlayers(callback: (PlayersResponse?, Exception?) -> Unit) {
        Thread {
            try {
                val url = URL("$BASE_URL?action=listUsers")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                val players = gson.fromJson(response, Array<Player>::class.java).toList()
                val playersResponse = PlayersResponse(
                    status = "success",
                    message = "Players loaded successfully",
                    players = players
                )
                callback(playersResponse, null)

            } catch (e: Exception) {
                callback(null, e)
            }
        }.start()
    }

    fun addPlayer(player: Player, callback: (ApiResponse?, Exception?) -> Unit) {
        Thread {
            try {
                val url = URL(BASE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val jsonBody = JSONObject().apply {
                    put("action", "createUser")
                    put("name", player.name)
                    put("phone", player.phone)
                    put("email", player.email)
                    put("age", player.age)
                    put("batch", player.batch)
                    put("specialization", player.specialization)
                    put("battingStyle", player.battingStyle)
                    put("bowlingStyle", player.bowlingStyle)
                    put("monthlyFee", player.monthlyFee)
                    put("role", "student")
                    put("password", "cricket123")
                }.toString()

                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.writeBytes(jsonBody)
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                val apiResponse = gson.fromJson(response, ApiResponse::class.java)
                callback(apiResponse, null)

            } catch (e: Exception) {
                callback(null, e)
            }
        }.start()
    }

    fun submitAttendance(
        playerIds: List<String>,
        date: String,
        markedBy: String,
        callback: (ApiResponse?, Exception?) -> Unit
    ) {
        Thread {
            try {
                val url = URL(BASE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val attendanceRecords = playerIds.map { playerId ->
                    JSONObject().apply {
                        put("userId", playerId)
                        put("date", date)
                        put("status", "present")
                        put("markedBy", markedBy)
                    }
                }

                val jsonBody = JSONObject().apply {
                    put("action", "createAttendanceRecord")
                    put("records", org.json.JSONArray(attendanceRecords))
                }.toString()

                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.writeBytes(jsonBody)
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                val apiResponse = gson.fromJson(response, ApiResponse::class.java)
                callback(apiResponse, null)

            } catch (e: Exception) {
                callback(null, e)
            }
        }.start()
    }
}
