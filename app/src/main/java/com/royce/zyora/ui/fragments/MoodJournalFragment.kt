package com.royce.zyora.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.data.models.MoodEntry
import com.royce.zyora.data.models.MoodType
import com.royce.zyora.databinding.FragmentMoodJournalBinding
import com.royce.zyora.databinding.DialogAddMoodBinding
import com.royce.zyora.ui.adapters.MoodAdapter
import com.royce.zyora.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for logging and viewing mood entries.
 * Supports emoji-based mood selection and data export via intents.
 */
class MoodJournalFragment : Fragment() {
    
    private var _binding: FragmentMoodJournalBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var moodAdapter: MoodAdapter
    private val moodList = mutableListOf<MoodEntry>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoodJournalBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupRecyclerView()
        setupUI()
        loadMoodEntries()
    }
    
    private fun setupRecyclerView() {
        moodAdapter = MoodAdapter(
            moods = moodList,
            onMoodClick = { mood -> 
                // Handle mood click - could show details or edit
            },
            onMoodDelete = { mood ->
                deleteMoodEntry(mood)
            }
        )
        
        binding.rvMoods.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moodAdapter
        }
    }
    
    private fun setupUI() {
        binding.fabAddMood.setOnClickListener {
            showAddMoodDialog()
        }
        
        binding.btnViewChart.setOnClickListener {
            showMoodChart()
        }
        
        binding.btnExport.setOnClickListener {
            exportMoodData()
        }
    }
    
    /**
     * Shows the mood trend chart using MPAndroidChart library
     */
    private fun showMoodChart() {
        // Replace current fragment with MoodChartFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MoodChartFragment())
            .addToBackStack("mood_chart")
            .commit()
    }
    
    private fun loadMoodEntries() {
        val moods = preferencesManager.getMoodEntries()
        moodList.clear()
        moodList.addAll(moods.sortedByDescending { it.timestamp })
        moodAdapter.notifyDataSetChanged()
        
        // Update empty state
        binding.tvEmptyState.visibility = if (moodList.isEmpty()) View.VISIBLE else View.GONE
        binding.rvMoods.visibility = if (moodList.isEmpty()) View.GONE else View.VISIBLE
        binding.btnExport.visibility = if (moodList.isEmpty()) View.GONE else View.VISIBLE
    }
    
    private fun showAddMoodDialog() {
        val dialogBinding = DialogAddMoodBinding.inflate(layoutInflater)
        
        // Setup mood buttons
        val moodButtons = listOf(
            dialogBinding.btnVeryHappy to MoodType.VERY_HAPPY,
            dialogBinding.btnHappy to MoodType.HAPPY,
            dialogBinding.btnNeutral to MoodType.NEUTRAL,
            dialogBinding.btnSad to MoodType.SAD,
            dialogBinding.btnVerySad to MoodType.VERY_SAD,
            dialogBinding.btnAngry to MoodType.ANGRY,
            dialogBinding.btnAnxious to MoodType.ANXIOUS,
            dialogBinding.btnExcited to MoodType.EXCITED,
            dialogBinding.btnCalm to MoodType.CALM,
            dialogBinding.btnTired to MoodType.TIRED
        )
        
        var selectedMood: MoodType? = null
        
        moodButtons.forEach { (button, moodType) ->
            button.text = "${moodType.emoji} ${moodType.displayName}"
            button.setOnClickListener {
                selectedMood = moodType
                // Update button states
                moodButtons.forEach { (btn, _) -> 
                    btn.isSelected = btn == button
                }
            }
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("How are you feeling?")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                selectedMood?.let { mood ->
                    saveMoodEntry(mood, dialogBinding.etNote.text.toString())
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // Disable save button initially
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
        
        // Enable save button when mood is selected
        moodButtons.forEach { (button, _) ->
            button.setOnClickListener {
                selectedMood = moodButtons.find { it.first == button }?.second
                moodButtons.forEach { (btn, _) -> 
                    btn.isSelected = btn == button
                }
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        }
    }
    
    /**
     * Saves a mood entry to SharedPreferences with current timestamp.
     * 
     * @param moodType The selected mood type with emoji
     * @param note Optional text note about the mood
     */
    private fun saveMoodEntry(moodType: MoodType, note: String) {
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val moodEntry = MoodEntry(
            id = UUID.randomUUID().toString(),
            mood = moodType,
            intensity = getMoodIntensity(moodType),
            note = note.trim(),
            date = dateFormat.format(Date(currentTime)),
            time = timeFormat.format(Date(currentTime)),
            timestamp = currentTime
        )
        
        val allMoods = preferencesManager.getMoodEntries().toMutableList()
        allMoods.add(moodEntry)
        preferencesManager.saveMoodEntries(allMoods)
        
        loadMoodEntries()
        Toast.makeText(requireContext(), "Mood logged successfully!", Toast.LENGTH_SHORT).show()
        
        // Update widget
        com.royce.zyora.widget.HabitProgressWidget.updateAllWidgets(requireContext())
    }
    
    private fun getMoodIntensity(moodType: MoodType): Int {
        return when (moodType) {
            MoodType.VERY_HAPPY -> 5
            MoodType.HAPPY, MoodType.EXCITED, MoodType.CALM -> 4
            MoodType.NEUTRAL -> 3
            MoodType.SAD, MoodType.TIRED -> 2
            MoodType.VERY_SAD, MoodType.ANGRY, MoodType.ANXIOUS -> 1
        }
    }
    
    private fun deleteMoodEntry(mood: MoodEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Mood Entry")
            .setMessage("Are you sure you want to delete this mood entry?")
            .setPositiveButton("Delete") { _, _ ->
                val allMoods = preferencesManager.getMoodEntries().toMutableList()
                allMoods.removeAll { it.id == mood.id }
                preferencesManager.saveMoodEntries(allMoods)
                loadMoodEntries()
                Toast.makeText(requireContext(), "Mood entry deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Exports mood data as CSV format using implicit intent.
     * Allows sharing via email, messaging apps, etc.
     */
    private fun exportMoodData() {
        val moods = preferencesManager.getMoodEntries()
        if (moods.isEmpty()) {
            Toast.makeText(requireContext(), "No mood data to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        val csvContent = buildString {
            append("Date,Time,Mood,Intensity,Note\n")
            moods.forEach { mood ->
                append("${mood.date},${mood.time},${mood.mood.displayName},${mood.intensity},\"${mood.note}\"\n")
            }
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/csv"
            putExtra(Intent.EXTRA_TEXT, csvContent)
            putExtra(Intent.EXTRA_SUBJECT, "Zyora Mood Journal Export")
        }
        
        startActivity(Intent.createChooser(shareIntent, "Export Mood Data"))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
