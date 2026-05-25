package com.teleport.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.teleport.app.databinding.ActivityMainBinding // Import the generated binding class

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // Declare the binding object

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Set the root view of the binding as the content view
        setContentView(binding.root)

        // Assuming the TextView that was causing the crash has an ID like 'teleport_status_text'
        // It can now be accessed directly and safely via the binding object.
        // This line replaces the problematic line 50.
        binding.teleportStatusText.text = "TelePort Status: Connected"
        // If the TextView had a different ID, e.g., 'my_status_label', it would be binding.myStatusLabel.text
    }
}
