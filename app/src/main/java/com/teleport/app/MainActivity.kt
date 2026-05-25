package com.teleport.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.teleport.app.databinding.ActivityMainBinding // Import the generated binding class

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // Declare the binding object

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // Inflate the layout
        setContentView(binding.root) // Set the root view of the binding

        // Line 50 (or equivalent, now using binding)
        // Access the TextView directly from the binding object.
        // If 'myTextView' is the ID in activity_main.xml, it becomes binding.myTextView
        binding.myTextView.setText("Welcome to TelePort!") // Example text
        // Ensure 'myTextView' exists in activity_main.xml with the correct ID.
    }
}
