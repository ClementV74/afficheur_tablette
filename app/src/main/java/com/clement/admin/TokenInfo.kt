package com.clement.admin

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TokenInfo {

    private var apiToken: String? = null

    // Méthode pour récupérer le token depuis l'API
    private suspend fun fetchApiToken(): String? = withContext(Dispatchers.IO) {
        val tokenUrl = "https://feegaffe.fr/smart_screen/get_token_secure"

        try {
            val url = URL(tokenUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            // Ajouter les en-têtes d'authentification Basic
            val userCredentials = "clement:a7f8b3c2d5e6f9g4h7j8k9l0m1n2o3p"
            val basicAuth = "Basic " + Base64.encodeToString(userCredentials.toByteArray(), Base64.NO_WRAP)
            connection.setRequestProperty("Authorization", basicAuth)

            // Lire le contenu de la réponse
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseContent = connection.inputStream.bufferedReader().use { it.readText() }

                // Désérialiser la réponse JSON pour extraire le token
                val json = JSONObject(responseContent)
                val token = json.optString("token", null)

                if (token != null) {
                    token
                } else {
                    Log.e("TokenInfo", "Le token n'a pas été trouvé dans la réponse.")
                    null
                }
            } else {
                Log.e("TokenInfo", "Erreur HTTP : $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e("TokenInfo", "Erreur lors de la récupération du token", e)
            null
        }
    }

    // Méthode pour obtenir le token, récupère le token si nécessaire
    suspend fun getToken(): String {
        if (apiToken == null) {
            apiToken = fetchApiToken() ?: throw Exception("Erreur lors de la récupération du token.")
        }
        return apiToken!!
    }
}