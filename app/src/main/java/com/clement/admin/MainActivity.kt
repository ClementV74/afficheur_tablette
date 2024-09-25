package com.clement.admin

import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.clement.admin.ui.ViewScreen
import com.clement.admin.ui.theme.AdminTheme
import kotlinx.coroutines.delay
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        actionBar?.hide()

        setContent {
            AdminTheme {
                var selectedScreenId by remember { mutableStateOf<String?>(null) }

                ViewScreen(onScreenSelected = { screenId ->
                    selectedScreenId = screenId
                })

                selectedScreenId?.let {
                    SmartDisplayScreen(it)
                }
            }
        }
    }
}
@Composable
fun SmartDisplayScreen(idAfficheur: String) {
    val tokenInfo = remember { TokenInfo() }
    val token = produceState<String?>(initialValue = null) {
        value = try {
            tokenInfo.getToken()
        } catch (e: Exception) {
            Log.e("SmartDisplayScreen", "Erreur lors de l'obtention du token", e)
            null
        }
    }

    val infoUrl = remember(token.value) { "https://feegaffe.fr/smart_screen/api.php?id_afficheur=$idAfficheur&token=${token.value}" }
    val weatherUrl = remember(token.value) { "https://feegaffe.fr/smart_screen/meteo/weather_api.php" }
    val alertUrl = remember(token.value) { "https://feegaffe.fr/smart_screen/alerte.php?token=${token.value}&action=get_status" }

    var weatherData by remember { mutableStateOf(WeatherData()) }
    var infoList by remember { mutableStateOf(listOf<InfoData>()) }
    var currentIndex by remember { mutableStateOf(0) }
    var currentTime by remember { mutableStateOf("") }
    var alertStatus by remember { mutableStateOf(AlertStatus()) }

    // Fonction pour vérifier les alertes
    fun checkAlertStatus(alert: AlertStatus): Boolean {
        return alert.incendie == "1" || alert.intrusion == "1" || alert.gaz == "1"
    }

    LaunchedEffect(token.value) {
        while (true) {
            token.value?.let {
                fetchWeatherData(weatherUrl) { data -> weatherData = data }
                fetchInfoData(infoUrl) { data -> infoList = data }
                fetchAlertData(alertUrl) { alert -> alertStatus = alert } // Récupération des alertes
            }
            delay(10000) // Mise à jour toutes les 10 secondes
        }
    }

    LaunchedEffect(currentIndex) {
        delay(10000)
        currentIndex = (currentIndex + 1) % infoList.size
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            delay(1000)
        }
    }

    // Si une alerte est activée, afficher un écran rouge avec l'alerte correspondante
    if (checkAlertStatus(alertStatus)) {
        val alertMessage = when {
            alertStatus.incendie == "1" -> "Alerte Incendie"
            alertStatus.intrusion == "1" -> "Alerte Intrusion"
            alertStatus.gaz == "1" -> "Alerte Gaz"
            else -> ""
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red), // Écran rouge
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = alertMessage,
                color = Color.White,
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        // Dégradé de couleurs animé
        val color1 = remember { Color(0xFF6A7B8A) } // Gris-bleu
        val color2 = remember { Color(0xFF9DA5B1) } // Gris clair
        val color3 = remember { Color(0xFFBDC8CC) } // Gris pâle

        // Animation pour changer de couleur
        val animatedColor by animateColorAsState(
            targetValue = if (currentIndex % 2 == 0) color1 else color2,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedColor) // Utiliser uniquement une couleur ici
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${weatherData.condition} : ${weatherData.temperature}°C",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Image(
                        painter = rememberImagePainter(weatherData.icon),
                        contentDescription = "Icône météo",
                        modifier = Modifier.size(60.dp).align(Alignment.CenterHorizontally)
                    )
                }

                if (infoList.isNotEmpty()) {
                    val currentItem = infoList[currentIndex]
                    Text(
                        text = currentItem.nom_salle,
                        color = Color.White,
                        fontSize = 40.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp)) {
                if (infoList.isNotEmpty()) {
                    val currentItem = infoList[currentIndex]
                    if (currentItem.type_info == "message") {
                        Text(
                            text = currentItem.contenu,
                            color = Color.White,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (currentItem.type_info == "image") {
                        if (currentItem.contenu.endsWith(".mp4")) {
                            val context = LocalContext.current
                            AndroidView(
                                factory = {
                                    VideoView(context).apply {
                                        setVideoPath(currentItem.contenu)
                                        start()
                                        setOnPreparedListener { it.isLooping = true }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = rememberImagePainter(currentItem.contenu),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp)
            )
        }
    }
}

// Modèle de données pour les alertes
data class AlertStatus(
    val incendie: String = "0",
    val intrusion: String = "0",
    val gaz: String = "0"
)

// Fonction pour récupérer les données d'alerte
suspend fun fetchAlertData(url: String, onSuccess: (AlertStatus) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    val status = json.getJSONObject("status")
                    val alert = AlertStatus(
                        incendie = status.getString("incendie"),
                        intrusion = status.getString("intrusion"),
                        gaz = status.getString("gaz")
                    )
                    onSuccess(alert)
                }
            } else {
                Log.e("fetchAlertData", "Erreur HTTP: ${connection.responseCode}")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("fetchAlertData", "Erreur lors de la récupération des alertes", e)
        }
    }
}