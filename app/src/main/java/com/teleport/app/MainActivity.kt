package com.teleport.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.teleport.app.databinding.ActivityMainBinding // Import the generated binding class

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // Set the root view from the binding object

        // Now, access the TextView directly via the binding object.
        // The property name (e.g., welcomeMessageTextView) is derived from the TextView's ID in XML (e.g., welcome_message_text_view).
        // This line is the equivalent of the original crashing line 50.
        binding.welcomeMessageTextView.text = "Welcome to TelePort!"
    }
}
