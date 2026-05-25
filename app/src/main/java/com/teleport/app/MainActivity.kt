package com.teleport.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.teleport.app.databinding.ActivityMainBinding // This class will be auto-generated

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Line 50: Access the TextView using the binding object.
        // This assumes there is a TextView in activity_main.xml with android:id="@+id/welcome_message_text_view"
        // If the TextView is missing or has a different ID, the build will likely fail,
        // or the property won't exist, preventing a runtime NullPointerException.
        binding.welcomeMessageTextView.setText("Welcome to TelePort!")
    }
}
