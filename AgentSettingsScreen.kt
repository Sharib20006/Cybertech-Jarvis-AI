// Updated AgentSettingsScreen.kt

import androidx.compose.runtime.*
import androidx.compose.material.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AgentSettingsScreen() {
    var customVoice by remember { mutableStateOf(false) }
    var responseSpeed by remember { mutableStateOf(1) }
    val developersCredits = listOf(
        "Sharib20006 - Lead Developer",
        "YourName - UI/UX Design",
        "AnotherDev - Backend Development"
    )

    Surface(
        elevation = 4.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Agent Settings", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Custom Voice Settings:")
            Switch(
                checked = customVoice,
                onCheckedChange = { customVoice = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Response Speed (1-5):")
            Slider(
                value = responseSpeed.toFloat(),
                onValueChange = { responseSpeed = it.toInt() },
                valueRange = 1f..5f
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Developer Credits:")
            developersCredits.forEach { credit ->
                Text(text = credit)
            }
            Text(text = "Contact us on Telegram: @YourTelegramHandle")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AgentSettingsScreen()
}