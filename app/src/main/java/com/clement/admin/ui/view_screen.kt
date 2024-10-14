package com.clement.admin.ui
import com.clement.admin.TokenInfo

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
import com.clement.admin.fetchScreensData
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults.buttonElevation


@Composable
fun ViewScreen(onScreenSelected: (String) -> Unit) {
    val tokenInfo = remember { TokenInfo() }
    val token = produceState<String?>(initialValue = null) {
        value = try {
            tokenInfo.getToken()
        } catch (e: Exception) {
            null
        }
    }

    var screens by remember { mutableStateOf(listOf<Screen>()) }

    LaunchedEffect(token.value) {
        token.value?.let {
            val screensUrl = "https://vabre.ch/smart_screen/ecraninfo.php?token=${it}"
            fetchScreensData(screensUrl) { data ->
                screens = data
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            screens.forEach { screen ->
                Button(
                    onClick = { onScreenSelected(screen.id_afficheur) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                    shape = RoundedCornerShape(8.dp), // Correct importation de RoundedCornerShape
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp), // Correction de l'élévation avec buttonElevation
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = screen.nom_salle,
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}