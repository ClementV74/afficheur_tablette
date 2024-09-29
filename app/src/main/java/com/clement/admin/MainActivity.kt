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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp




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



    @Composable
    fun SmartDisplayScreen(idAfficheur: String) { // Composable pour afficher l'écran intelligent
        val tokenInfo = remember { TokenInfo() } // Instance de TokenInfo pour gérer le token
        var token by remember { mutableStateOf<String?>(null) } // Token d'authentification

        // Lancer une coroutine pour surveiller les changements du token
        LaunchedEffect(Unit) {
            // Démarrer le rafraîchissement du token
            tokenInfo.startTokenRefresh { newToken ->
                token = newToken // Mettre à jour le token lorsqu'il change
            }
        }

        // URL qui dépend du token
        val infoUrl = remember(token) { "https://feegaffe.fr/smart_screen/api.php?id_afficheur=$idAfficheur&token=${token}" }
        val weatherUrl = remember(token) { "https://feegaffe.fr/smart_screen/meteo/weather_api.php" }
        val alertUrl = remember(token) { "https://feegaffe.fr/smart_screen/alerte.php?token=${token}&action=get_status" }
        val enligne = remember(token) { "https://feegaffe.fr/smart_screen/change_status.php?id_afficheur=$idAfficheur&token=${token}" }

        var weatherData by remember { mutableStateOf(WeatherData()) } // Données météorologiques
        var infoList by remember { mutableStateOf(listOf<InfoData>()) } // Liste des données d'information
        var currentIndex by remember { mutableStateOf(0) } // Index de l'élément actuel
        var currentTime by remember { mutableStateOf("") } // Heure actuelle
        var alertStatus by remember { mutableStateOf(AlertStatus()) } // État des alertes

        // Fonction pour vérifier les alertes
        fun checkAlertStatus(alert: AlertStatus): Boolean { // Vérifier si une alerte est activée
            return alert.incendie == "1" || alert.intrusion == "1" || alert.gaz == "1" // Retourne vrai si une alerte est activée
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

        // Lancer une coroutine pour vérifier l'état en ligne toutes les 10 secondes
        LaunchedEffect(token) { // Lancer une coroutine pour surveiller les changements du token
            while (token != null) { // Tant que le token est défini
                token?.let { // Si le token n'est pas nul
                    checkOnlineStatus(enligne) // Appeler la fonction ici
                }
                delay(10000) // Attendre 10 secondes
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
                delay(10000) // Mise à jour toutes les 10 secondes
            }
        }

        LaunchedEffect(currentIndex) { // Lancer une coroutine pour changer l'élément actuel
            delay(10000)
            currentIndex = (currentIndex + 1) % infoList.size
        }

        LaunchedEffect(Unit) { // Lancer une coroutine pour mettre à jour l'heure actuelle
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
                // Ajouter l'image d'arrière-plan
                Image(
                    painter = painterResource(id = R.drawable.background), // Remplace `background` par ton nom d'image
                    contentDescription = null,
                    contentScale = ContentScale.Crop, // Pour s'adapter à l'écran
                    modifier = Modifier.fillMaxSize() // Remplir toute la taille de l'écran
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
                            painter = rememberImagePainter(weatherData.icon),
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
                                //mets le texte centre en bas de l'écran mets un peu en hauteur quand meme
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 130.dp)

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
                                    painter = rememberImagePainter(currentItem.contenu), // Utiliser Coil pour charger l'image
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp)) // Appliquer des coins arrondis à l'image
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
    }}