package com.clement.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clement.admin.Screen
import com.clement.admin.TokenInfo
import com.clement.admin.fetchScreensData

@Composable
fun ViewScreen(onScreenSelected: (String) -> Unit) {
    val tokenInfo = remember { TokenInfo() }
    val token = produceState<String?>(initialValue = null) {
        value = try {
            tokenInfo.getToken()
        } catch (e: Exception) {
            // Gestion des erreurs lors de la récupération du token
            null
        }
    }

    var screens by remember { mutableStateOf(listOf<Screen>()) }

    LaunchedEffect(token.value) {
        token.value?.let {
            val screensUrl = "https://feegaffe.fr/smart_screen/ecraninfo.php?token=${it}"
            fetchScreensData(screensUrl) { data ->
                screens = data
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),  // Définit une couleur de fond
        contentAlignment = Alignment.Center  // Centre le contenu dans la vue
    ) {
        Column {
            screens.forEach { screen ->
                Button(
                    onClick = { onScreenSelected(screen.id_afficheur) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = screen.nom_salle,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}
