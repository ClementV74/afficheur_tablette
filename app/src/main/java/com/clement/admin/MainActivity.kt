package com.clement.admin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext

// Classe principale de l'activité de l'application
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure le mode plein écran pour l'application
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        actionBar?.hide()

        // Définit le contenu principal de l'activité à l'aide de Jetpack Compose
        setContent {
            AdminTheme {
                // État pour stocker l'ID de l'écran sélectionné
                var selectedScreenId by remember { mutableStateOf<String?>(null) }

                // Composable ViewScreen pour la sélection des écrans
                ViewScreen(onScreenSelected = { screenId ->
                    selectedScreenId = screenId
                })

                // Composable pour afficher le contenu de l'écran sélectionné
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

    var weatherData by remember { mutableStateOf(WeatherData()) }
    var infoList by remember { mutableStateOf(listOf<InfoData>()) }
    var currentIndex by remember { mutableStateOf(0) }
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(token.value) {
        while (true) {
            token.value?.let {
                fetchWeatherData(weatherUrl) { data -> weatherData = data }
                fetchInfoData(infoUrl) { data -> infoList = data }
            }
            delay(20000)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Affichage des informations météorologiques
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
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.CenterHorizontally)
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

        // Affichage du contenu (message, image ou vidéo)
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp)) {
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
                    // Vérifie si l'URL est une vidéo (.mp4 par exemple)
                    if (currentItem.contenu.endsWith(".mp4")) {
                        // Lecture de la vidéo en arrière-plan
                        val context = LocalContext.current
                        AndroidView(
                            factory = {
                                VideoView(context).apply {
                                    setVideoPath(currentItem.contenu)
                                    start() // Démarre la vidéo automatiquement
                                    setOnPreparedListener { it.isLooping = true } // Boucle la vidéo
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Affichage d'une image
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

        // Affichage de l'heure actuelle
        Text(
            text = currentTime,
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        )
    }
}


// Fonction pour obtenir l'heure actuelle sous forme de chaîne de caractères
fun getCurrentTime(): String {
    val now = java.util.Calendar.getInstance().time
    val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return formatter.format(now)
}

// Modèles de données
data class WeatherData(
    val condition: String = "", // Condition météorologique (ex: "Ensoleillé")
    val temperature: Int = 0,  // Température en degrés Celsius
    val icon: String = ""      // URL de l'icône météo
)

data class InfoData(
    val id_info: String = "",        // ID de l'information
    val id_afficheur: String = "",   // ID de l'écran affichant l'information
    val type_info: String = "",      // Type de l'information (message ou image)
    val contenu: String = "",        // Contenu de l'information (texte ou URL de l'image)
    val nom_salle: String = "",      // Nom de la salle associée
    val type_ecran: String = "",     // Type d'écran
    val resolution: String = "",     // Résolution de l'écran
    val batiment: String = ""        // Nom du bâtiment
)

data class Screen(
    val id_afficheur: String,  // ID de l'écran
    val nom_salle: String      // Nom de la salle associée
)

// Fonction pour récupérer les données météorologiques depuis l'URL fournie
fun fetchWeatherData(url: String, onResult: (WeatherData) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    Thread {
        try {
            val response = URL(url).readText()  // Lit le texte brut de la réponse
            val json = JSONObject(response)  // Convertit la réponse en objet JSON
            val weather = WeatherData(
                condition = json.getString("condition"),
                temperature = json.getInt("temperature"),
                icon = json.getString("icon")
            )
            handler.post { onResult(weather) }  // Met à jour l'état avec les données récupérées
        } catch (e: Exception) {
            Log.e("SmartDisplay", "Échec de la récupération des données météorologiques", e)
        }
    }.start()
}

// Fonction pour récupérer les informations depuis l'URL fournie
fun fetchInfoData(url: String, onResult: (List<InfoData>) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    Thread {
        try {
            val response = URL(url).readText()  // Lit le texte brut de la réponse
            val jsonArray = JSONArray(response)  // Convertit la réponse en tableau JSON
            val infoList = mutableListOf<InfoData>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)  // Récupère chaque objet JSON du tableau
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
            handler.post { onResult(infoList) }  // Met à jour l'état avec les données récupérées
        } catch (e: Exception) {
            Log.e("SmartDisplay", "Échec de la récupération des données d'information", e)
        }
    }.start()
}

// Fonction pour récupérer les données des écrans depuis l'URL fournie
fun fetchScreensData(url: String, onResult: (List<Screen>) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    Thread {
        try {
            val response = URL(url).readText()  // Lit le texte brut de la réponse
            val jsonArray = JSONArray(response)  // Convertit la réponse en tableau JSON
            val screenList = mutableListOf<Screen>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)  // Récupère chaque objet JSON du tableau
                screenList.add(
                    Screen(
                        id_afficheur = item.getString("id_afficheur"),
                        nom_salle = item.getString("nom_salle")
                    )
                )
            }
            handler.post { onResult(screenList) }  // Met à jour l'état avec les données récupérées
        } catch (e: Exception) {
            Log.e("SmartDisplay", "Échec de la récupération des données des écrans", e)
        }
    }.start()
}
