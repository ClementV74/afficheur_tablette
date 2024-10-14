package com.clement.admin


import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.clement.admin.ui.ViewScreen
import com.clement.admin.ui.theme.AdminTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    private lateinit var tokenInfo: TokenInfo // Instance de TokenInfo pour gérer le token

    override fun onCreate(savedInstanceState: Bundle?) { // Appelé lors de la création de l'activité
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = ( // Masquer la barre de statut et la barre de navigation
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        actionBar?.hide() // Masquer la barre d'action

        tokenInfo = TokenInfo() // Initialiser l'instance de TokenInfo

        setContent { // Définir le contenu de l'activité
            AdminTheme {
                var selectedScreenId by remember { mutableStateOf<String?>(null) } // ID de l'écran sélectionné

                ViewScreen(onScreenSelected = { screenId -> // Afficher l'écran de sélection
                    selectedScreenId = screenId // Mettre à jour l'ID de l'écran sélectionné
                })

                selectedScreenId?.let { // Si un écran est sélectionné, afficher l'écran correspondant
                    SmartDisplayScreen(it) // Afficher l'écran intelligent
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                setContent {
                    AdminTheme {
                        ViewScreen(onScreenSelected = { screenId ->
                            // Handle navigation if needed
                        })
                    }
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    @Composable
    fun SmartDisplayScreen(idAfficheur: String) { // Composable pour afficher l'écran intelligent
        val tokenInfo = remember { TokenInfo() } // Instance de TokenInfo pour gérer le token
        var token by remember { mutableStateOf<String?>(null) } // Token d'authentification
        var dailyPhrase by remember { mutableStateOf("") }

        // Lancer une coroutine pour surveiller les changements du token
        LaunchedEffect(Unit) {
            // Démarrer le rafraîchissement du token
            tokenInfo.startTokenRefresh { newToken ->
                token = newToken // Mettre à jour le token lorsqu'il change
            }
        }

        // URL qui dépend du token
        val infoUrl = remember(token) { "https://vabre.ch/smart_screen/api.php?id_afficheur=$idAfficheur&token=${token}" }
        val weatherUrl = remember(token) { "https://vabre.ch/smart_screen/meteo/weather_api.php" }
        val alertUrl = remember(token) { "https://vabre.ch/smart_screen/alerte.php?token=${token}&action=get_status" }
        val enligne = remember(token) { "https://vabre.ch/smart_screen/change_status.php?id_afficheur=$idAfficheur&token=${token}" }

        var weatherData by remember { mutableStateOf(WeatherData()) } // Données météorologiques
        var infoList by remember { mutableStateOf(listOf<InfoData>()) } // Liste des données d'information
        var currentIndex by remember { mutableStateOf(0) } // Index de l'élément actuel
        var currentTime by remember { mutableStateOf("") } // Heure actuelle
        var alertStatus by remember { mutableStateOf(AlertStatus()) } // État des alertes

        // Fonction pour vérifier les alertes
        fun checkAlertStatus(alert: AlertStatus): Boolean { // Vérifier si une alerte est activée
            return alert.incendie == "1" || alert.intrusion == "1" || alert.gaz == "1" // Retourne vrai si une alerte est activée
        }

        suspend fun fetchDailyPhrase(url: String, onSuccess: (String) -> Unit) {
            withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 20000
                    connection.readTimeout = 20000

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val response = inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)
                        val phrase = json.getString("phrase") // Récupérer la phrase
                        onSuccess(phrase)
                    } else {
                        Log.e("fetchDailyPhrase", "Erreur HTTP: ${connection.responseCode}")
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e("fetchDailyPhrase", "Erreur lors de la récupération de la phrase du jour", e)
                }
            }
        }

        suspend fun checkOnlineStatus(url: String) { // Vérifier l'état en ligne
            withContext(Dispatchers.IO) { // Exécuter en arrière-plan
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection // Ouvrir une connexion HTTP
                    connection.requestMethod = "GET" // Utiliser la méthode GET
                    connection.connectTimeout = 20000
                    connection.readTimeout = 20000

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        // La requête a réussi. Vous pouvez traiter la réponse si nécessaire
                        val inputStream = connection.inputStream // Lire la réponse
                        val response = inputStream.bufferedReader().use { it.readText() } // Convertir en chaîne
                        Log.d("checkOnlineStatus", "Réponse: $response")
                    } else {
                        Log.e("checkOnlineStatus", "Erreur HTTP: ${connection.responseCode}")
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e("checkOnlineStatus", "Erreur lors de la vérification de l'état en ligne", e)
                }
            }
        }

        LaunchedEffect(token) {
            while (token != null) {
                token?.let {
                    val dailyPhraseUrl = "https://vabre.ch/smart_screen/phrase_du_jour?token=$token"
                    fetchDailyPhrase(dailyPhraseUrl) { phrase -> dailyPhrase = phrase }
                }
                delay(10000) // Mise à jour toutes les 10 secondes
            }
        }

        // Lancer une coroutine pour vérifier l'état en ligne toutes les 10 secondes
        LaunchedEffect(token) { // Lancer une coroutine pour surveiller les changements du token
            while (token != null) { // Tant que le token est défini
                token?.let { // Si le token n'est pas nul
                    checkOnlineStatus(enligne) // Appeler la fonction ici
                }
                delay(5000) // Attendre 10 secondes
            }
        }
        // Lancer une coroutine pour actualiser les données toutes les 10 secondes
        LaunchedEffect(token) {
            while (token != null) {
                token?.let {
                    fetchWeatherData(weatherUrl) { data -> weatherData = data }
                    fetchInfoData(infoUrl) { data -> infoList = data }
                    fetchAlertData(alertUrl) { alert -> alertStatus = alert }
                }
                delay(5000) // Mise à jour toutes les 10 secondes
            }
        }

        LaunchedEffect(currentIndex) { // Lancer une coroutine pour changer l'élément actuel
            delay(5000)
            currentIndex = (currentIndex + 1) % infoList.size
        }

        LaunchedEffect(Unit) { // Lancer une coroutine pour mettre à jour l'heure actuelle
            while (true) {
                currentTime = getCurrentTime()
                delay(1000)
            }
        }

        fun getCurrentDay(): String {
            val dayFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
            return dayFormat.format(java.util.Date())
        }


        fun translateDayToFrench(day: String): String {
            return when (day.lowercase()) {
                "monday" -> "Lundi"
                "tuesday" -> "Mardi"
                "wednesday" -> "Mercredi"
                "thursday" -> "Jeudi"
                "friday" -> "Vendredi"
                "saturday" -> "Samedi"
                "sunday" -> "Dimanche"
                else -> day
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

            Box( // Afficher un écran rouge avec le message d'alerte
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
        } else { // Sinon, afficher l'écran normal
            // Dégradé de couleurs animé
            val color1 = remember { Color(0xFF6A7B8A) } // Gris-bleu
            val color2 = remember { Color(0xFF9DA5B1) } // Gris clair


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
                // Ajouter le gif d'arrière-plan
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(R.drawable.backgroundgif) // Assurez-vous que c'est bien un GIF
                        .crossfade(true)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    imageLoader = ImageLoader.Builder(LocalContext.current)
                        .components {
                            add(GifDecoder.Factory()) // Ajout du décoder pour les GIFs
                        }
                        .build()
                )

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
                            painter = rememberAsyncImagePainter(weatherData.icon),
                            contentDescription = "Icône météo",
                            modifier = Modifier.size(60.dp).align(Alignment.CenterHorizontally).padding(top = 8.dp)
                        )
                    }

                    if (infoList.isNotEmpty()) { // Si la liste d'informations n'est pas vide
                        val currentItem = infoList[currentIndex] // Obtenir l'élément actuel
                        Text(
                            text = currentItem.nom_salle,
                            color = Color.White,
                            fontSize = 40.sp,

                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp)) { // Afficher le contenu principal
                    if (infoList.isNotEmpty()) {
                        val currentItem = infoList[currentIndex]
                        if (currentItem.type_info == "message") {
                            Text(
                                text = currentItem.contenu,
                                color = Color.White,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 130.dp)
                            )
                            // Afficher la phrase du jour
                            Text(
                                text = dailyPhrase,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp) // Positionner en bas
                            )
                        } else if (currentItem.type_info == "image") {
                            if (currentItem.contenu.endsWith(".mp4")) { // Si c'est une vidéo
                                val context = LocalContext.current // Obtenir le contexte actuel
                                AndroidView( // Utiliser AndroidView pour afficher la vidéo
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
                                    painter = rememberAsyncImagePainter(currentItem.contenu), // Utiliser Coil pour charger l'image
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(400.dp) // Ajuster la taille de l'image
                                        .clip (RoundedCornerShape(30.dp)) // Appliquer des coins arrondis à l'image
                                )
                            }
                        }
                    }
                }

                Text(
                    text = currentTime,

                    fontFamily = FontFamily(Font(R.font.anurati_regular)),
                    color = Color.White,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
                )
                Text(
                    text = translateDayToFrench(getCurrentDay()), // Utilisez une fonction pour obtenir le jour actuel
                    fontFamily = FontFamily(Font(R.font.anurati_regular)),
                    color = Color.White,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
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
                connection.connectTimeout = 20000
                connection.readTimeout = 20000

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
}