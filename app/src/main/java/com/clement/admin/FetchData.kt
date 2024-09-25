package com.clement.admin

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

// Instance de TokenInfo pour gérer le token
private val tokenInfo = TokenInfo()

fun fetchWeatherData(url: String, onResult: (WeatherData) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    Thread {
        try {
            val response = URL(url).readText()
            val json = JSONObject(response)
            val weather = WeatherData(
                condition = json.getString("condition"),
                temperature = json.getInt("temperature"),
                icon = json.getString("icon")
            )
            handler.post { onResult(weather) }
        } catch (e: Exception) {
            Log.e("SmartDisplay", "Échec de la récupération des données météorologiques", e)
        }
    }.start()
}

fun fetchInfoData(url: String, onResult: (List<InfoData>) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    Thread {
        try {
            val response = URL(url).readText()
            val jsonArray = JSONArray(response)
            val infoList = mutableListOf<InfoData>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                infoList.add(
                    InfoData(
                        id_info = item.getString("id_info"),
                        id_afficheur = item.getString("id_afficheur"),
                        type_info = item.getString("type_info"),
                        contenu = item.getString("contenu"),
                        nom_salle = item.getString("nom_salle"),
                        type_ecran = item.getString("type_ecran"),
                        resolution = item.getString("resolution"),
                        batiment = item.getString("batiment")
                    )
                )
            }
            handler.post { onResult(infoList) }
        } catch (e: Exception) {
            Log.e("SmartDisplay", "Échec de la récupération des données d'information", e)
        }
    }.start()
}

fun fetchScreensData(url: String, onResult: (List<Screen>) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    Thread {
        try {
            val response = URL(url).readText()
            val jsonArray = JSONArray(response)
            val screenList = mutableListOf<Screen>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                screenList.add(
                    Screen(
                        id_afficheur = item.getString("id_afficheur"),
                        nom_salle = item.getString("nom_salle")
                    )
                )
            }
            handler.post { onResult(screenList) }
        } catch (e: Exception) {
            Log.e("SmartDisplay", "Échec de la récupération des données des écrans", e)
        }
    }.start()
}
