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
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext

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
