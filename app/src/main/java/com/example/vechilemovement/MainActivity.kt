package com.example.vechilemovement

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.vechilemovement.ui.theme.VechileMovementTheme
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var carImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        carImage = findViewById(R.id.carImage)

        // Scale animation on launch
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
        carImage.startAnimation(pulseAnim)

        carImage.setOnClickListener {
            val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce)
            carImage.startAnimation(bounce)

            // Start MapActivity after animation
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MapActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }, 500)
        }
    }
}