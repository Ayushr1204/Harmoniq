package com.harmoniq.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harmoniq.app.R
import com.harmoniq.app.ui.theme.BackgroundDark

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit
) {
    // Auto-dismiss after 1.5 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        onAnimationComplete()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_harmoniq),
                contentDescription = "Harmoniq Logo",
                modifier = Modifier
                    .size(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Harmoniq",
                style = MaterialTheme.typography.headlineLarge,
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
