package lat.pam.laporkampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import lat.pam.laporkampus.navigation.LaporKampusNavHost
import lat.pam.laporkampus.ui.theme.LaporKampusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaporKampusTheme {
                LaporKampusNavHost()
            }
        }
    }
}
