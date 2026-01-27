package com.royce.zyora.ui.fragments
// Corrected imports
// ... rest of your imports
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.royce.zyora.R
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.data.models.Habit
import com.royce.zyora.data.models.HabitCategory
import com.royce.zyora.data.models.HabitProgress
import com.royce.zyora.data.models.HabitWithProgress
import com.royce.zyora.databinding.FragmentHabitTrackerBinding
import com.royce.zyora.ui.adapters.HabitAdapter
import com.royce.zyora.utils.HabitNotificationManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Fragment for managing daily wellness habits.
 * Allows users to add, edit, delete habits and track daily progress.
 * Uses AlarmManager for scheduling habit reminders.
 */
class HabitTrackerFragment : Fragment() {
    
    private var _binding: FragmentHabitTrackerBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var habitAdapter: HabitAdapter
    private val habitsList = mutableListOf<HabitWithProgress>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create notification channel
        HabitNotificationManager.createNotificationChannel(requireContext())

        preferencesManager = PreferencesManager(requireContext())
        
        setupRecyclerView()
        setupUI()
        loadHabits()
        // Add some default habits if none exist
        if (preferencesManager.getHabits().isEmpty()) {
            addDefaultHabits()
            loadHabits()
        }
    }
    
    private fun setupRecyclerView() {
        habitAdapter = HabitAdapter(
            habits = habitsList,
            onHabitClick = { habit -> 
                // Handle habit click - could open detail view
            },
            onProgressUpdate = { habit, newProgress ->
                updateHabitProgress(habit, newProgress)
            },
            onEditHabit = { habit ->
                showEditHabitDialog(habit)
            },
            onDeleteHabit = { habit ->
                showDeleteConfirmationDialog(habit)
            }
        )
        
        binding.rvHabits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitAdapter
        }
    }
    
    private fun setupUI() {
        binding.fabAddHabit.setOnClickListener {
            showAddHabitDialog()
        }
    }
    
    /**
     * Loads habits from SharedPreferences and displays them with today's progress.
     * Also schedules notifications for habits with reminders enabled.
     */
    private fun loadHabits() {
        val habits = preferencesManager.getHabits()
        habitsList.clear()

        habits.forEach { habit ->
            val todayProgress = preferencesManager.getHabitProgress().find {
                it.habitId == habit.id && it.date == getCurrentDateString()
            }

            val completionPercentage = if (todayProgress != null) {
                (todayProgress.currentCount.toFloat() / habit.targetCount) * 100
            } else 0f

            habitsList.add(HabitWithProgress(habit, todayProgress, completionPercentage))

            // Schedule notification for habits with reminders enabled
            if (habit.reminderEnabled) {
                HabitNotificationManager.scheduleHabitReminder(requireContext(), habit)
            }
        }

        habitAdapter.notifyDataSetChanged()
        
        // Update empty state
        binding.tvEmptyState.visibility = if (habitsList.isEmpty()) View.VISIBLE else View.GONE
        binding.rvHabits.visibility = if (habitsList.isEmpty()) View.GONE else View.VISIBLE
    }
    
    /**
     * Updates the progress count for a specific habit on the current day.
     * Creates new progress entry if none exists for today.
     * 
     * @param habit The habit being updated
     * @param newProgress The new progress count
     */
    private fun updateHabitProgress(habit: Habit, newProgress: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val allProgress = preferencesManager.getHabitProgress().toMutableList()
        
        // Find existing progress or create new
        val existingProgressIndex = allProgress.indexOfFirst { 
            it.habitId == habit.id && it.date == today 
        }
        
        val updatedProgress = if (existingProgressIndex >= 0) {
            allProgress[existingProgressIndex].copy(
                currentCount = newProgress,
                isCompleted = newProgress >= habit.targetCount,
                completedAt = if (newProgress >= habit.targetCount) System.currentTimeMillis() else null
            )
        } else {
            HabitProgress(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                date = today,
                currentCount = newProgress,
                targetCount = habit.targetCount,
                isCompleted = newProgress >= habit.targetCount,
                completedAt = if (newProgress >= habit.targetCount) System.currentTimeMillis() else null
            )
        }
        
        if (existingProgressIndex >= 0) {
            allProgress[existingProgressIndex] = updatedProgress
        } else {
            allProgress.add(updatedProgress)
        }
        
        preferencesManager.saveHabitProgress(allProgress)
        loadHabits() // Refresh the list
        
        // Update widget
        com.royce.zyora.widget.HabitProgressWidget.updateAllWidgets(requireContext())
    }
    
    /**
     * Adds default wellness habits when the app is first launched.
     * Includes habits for hydration, exercise, meditation, and reading.
     */
    private fun addDefaultHabits() {
        val defaultHabits = listOf(
            Habit(
                id = UUID.randomUUID().toString(),
                name = "Drink Water",
                description = "Stay hydrated throughout the day",
                targetCount = 8,
                unit = "glasses",
                iconName = "water",
                color = "#00BCD4",
                category = HabitCategory.HEALTH
            ),
            Habit(
                id = UUID.randomUUID().toString(),
                name = "Exercise",
                description = "Get your body moving",
                targetCount = 30,
                unit = "minutes",
                iconName = "fitness",
                color = "#4CAF50",
                category = HabitCategory.FITNESS
            ),
            Habit(
                id = UUID.randomUUID().toString(),
                name = "Meditate",
                description = "Practice mindfulness",
                targetCount = 10,
                unit = "minutes",
                iconName = "meditation",
                color = "#9C27B0",
                category = HabitCategory.MINDFULNESS
            ),
            Habit(
                id = UUID.randomUUID().toString(),
                name = "Read",
                description = "Expand your knowledge",
                targetCount = 20,
                unit = "pages",
                iconName = "book",
                color = "#FF9800",
                category = HabitCategory.PERSONAL
            )
        )
        
        preferencesManager.saveHabits(defaultHabits)
    }
    
    private fun showAddHabitDialog() {
        showHabitDialog(null)
    }

    private fun showEditHabitDialog(habit: Habit) {
        showHabitDialog(habit)
    }

    private fun showHabitDialog(existingHabit: Habit?) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_Alert)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_habit)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        // Ensure dialog is properly sized
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etHabitName = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etHabitName)
        val etHabitDescription = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etHabitDescription)
        val etTargetCount = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTargetCount)
        val etUnit = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUnit)
        val chipGroupCategory = dialog.findViewById<ChipGroup>(R.id.chipGroupCategory)
        val chipGroupIcon = dialog.findViewById<ChipGroup>(R.id.chipGroupIcon)
        val chipGroupColor = dialog.findViewById<ChipGroup>(R.id.chipGroupColor)
        val swReminderEnabled = dialog.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swReminderEnabled)
        val llReminderTime = dialog.findViewById<LinearLayout>(R.id.llReminderTime)
        val btnReminderTime = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnReminderTime)

        // Set dialog title
        val titleText = dialog.findViewById<TextView>(R.id.dialog_title)
        val btnCreate = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCreate)

        // Set title and button text based on mode
        if (existingHabit != null) {
            titleText.text = "Edit Habit"
            btnCreate.text = "Update"
        } else {
            titleText.text = "Create New Habit"
            btnCreate.text = "Create"
        }

        // Populate fields if editing
        if (existingHabit != null) {
            etHabitName.setText(existingHabit.name)
            etHabitDescription.setText(existingHabit.description)
            etTargetCount.setText(existingHabit.targetCount.toString())
            etUnit.setText(existingHabit.unit)

            // Select category
            val categoryChipIndex = when (existingHabit.category) {
                HabitCategory.HEALTH -> 0
                HabitCategory.FITNESS -> 1
                HabitCategory.MINDFULNESS -> 2
                HabitCategory.PRODUCTIVITY -> 3
                HabitCategory.PERSONAL -> 4
            }
            chipGroupCategory.check(chipGroupCategory.getChildAt(categoryChipIndex).id)

            // Select icon
            for (i in 0 until chipGroupIcon.childCount) {
                val chip = chipGroupIcon.getChildAt(i)
                if (chip is Chip && chip.tag == existingHabit.iconName) {
                    chipGroupIcon.check(chip.id)
                    break
                }
            }

            // Select color
            for (i in 0 until chipGroupColor.childCount) {
                val chip = chipGroupColor.getChildAt(i)
                if (chip is Chip && chip.tag == existingHabit.color) {
                    chipGroupColor.check(chip.id)
                    break
                }
            }

            // Setup reminder settings for existing habit
            swReminderEnabled.isChecked = existingHabit.reminderEnabled
            llReminderTime.visibility = if (existingHabit.reminderEnabled) View.VISIBLE else View.GONE

            // Set reminder time
            val reminderTimeText = formatTimeForDisplay(existingHabit.reminderHour, existingHabit.reminderMinute)
            btnReminderTime.text = reminderTimeText
        } else {
            // Set default values for new habit
            etTargetCount.setText("1")
            etUnit.setText("times")
            chipGroupCategory.check(chipGroupCategory.getChildAt(0).id)
            chipGroupIcon.check(chipGroupIcon.getChildAt(0).id)
            chipGroupColor.check(chipGroupColor.getChildAt(0).id)
            swReminderEnabled.isChecked = false
            llReminderTime.visibility = View.GONE
        }

        // Setup reminder toggle
        swReminderEnabled.setOnCheckedChangeListener { _, isChecked ->
            llReminderTime.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Setup time picker
        btnReminderTime.setOnClickListener {
            showTimePickerDialog(btnReminderTime)
        }

        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        btnCreate.setOnClickListener {
            val habitName = etHabitName?.text.toString().trim() ?: ""
            val habitDescription = etHabitDescription?.text.toString().trim() ?: ""
            val targetCountStr = etTargetCount?.text.toString().trim() ?: ""
            val unit = etUnit?.text.toString().trim() ?: ""

            // Validation
            if (habitName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a habit name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (targetCountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a target count", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (unit.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a unit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetCount = targetCountStr.toIntOrNull()
            if (targetCount == null || targetCount <= 0) {
                Toast.makeText(requireContext(), "Please enter a valid target count", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get selected category
            val selectedCategoryChip = chipGroupCategory?.findViewById<Chip>(chipGroupCategory.checkedChipId)
            val category = when (selectedCategoryChip?.text) {
                "Health" -> HabitCategory.HEALTH
                "Fitness" -> HabitCategory.FITNESS
                "Mindfulness" -> HabitCategory.MINDFULNESS
                "Productivity" -> HabitCategory.PRODUCTIVITY
                "Personal" -> HabitCategory.PERSONAL
                else -> HabitCategory.HEALTH
            }

            // Get selected icon
            val selectedIconChip = chipGroupIcon?.findViewById<Chip>(chipGroupIcon.checkedChipId)
            val iconName = selectedIconChip?.tag as? String ?: "water"

            // Get selected color
            val selectedColorChip = chipGroupColor?.findViewById<Chip>(chipGroupColor.checkedChipId)
            val color = selectedColorChip?.tag as? String ?: "#00BCD4"

            // Reminder settings
            val reminderEnabled = swReminderEnabled?.isChecked ?: false
            val reminderHour = if (reminderEnabled) {
                // Extract hour from time button text (format: "9:00 AM")
                val timeText = btnReminderTime?.text.toString() ?: "9:00 AM"
                try {
                    val hourMinute = timeText.split(" ")[0].split(":")
                    val hour = hourMinute[0].toIntOrNull() ?: 9
                    // Convert to 24-hour format if PM
                    val amPm = timeText.split(" ")[1]
                    if (amPm == "PM" && hour != 12) hour + 12 else if (amPm == "AM" && hour == 12) 0 else hour
                } catch (e: Exception) {
                    9
                }
            } else 9
            val reminderMinute = if (reminderEnabled) {
                val timeText = btnReminderTime?.text.toString() ?: "9:00 AM"
                try {
                    timeText.split(" ")[0].split(":")[1].toIntOrNull() ?: 0
                } catch (e: Exception) {
                    0
                }
            } else 0

            if (existingHabit != null) {
                // Update existing habit
                updateHabit(existingHabit, habitName, habitDescription, targetCount, unit, iconName, color, category, reminderEnabled, reminderHour, reminderMinute)
            } else {
                // Create new habit
                val newHabit = Habit(
                    id = UUID.randomUUID().toString(),
                    name = habitName,
                    description = habitDescription,
                    targetCount = targetCount,
                    unit = unit,
                    iconName = iconName,
                    color = color,
                    category = category,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute
                )

                // Save habit
                val habits = preferencesManager.getHabits().toMutableList()
                habits.add(newHabit)
                preferencesManager.saveHabits(habits)

                // Schedule notification for the new habit
                if (reminderEnabled) {
                    HabitNotificationManager.scheduleHabitReminder(requireContext(), newHabit)
                }

                Toast.makeText(requireContext(), "Habit created successfully!", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
            loadHabits()
        }

        dialog.show()

        // Ensure buttons are visible and clickable after dialog is shown
        dialog.setOnShowListener {
            btnCreate?.let { button ->
                button.isEnabled = true
                button.isClickable = true
                button.visibility = View.VISIBLE
            }
        }
    }

    private fun deleteHabit(habit: Habit) {
        val habits = preferencesManager.getHabits().toMutableList()
        habits.removeAll { it.id == habit.id }
        preferencesManager.saveHabits(habits)

        // Also delete associated progress
        val progress = preferencesManager.getHabitProgress().toMutableList()
        progress.removeAll { it.habitId == habit.id }
        preferencesManager.saveHabitProgress(progress)

        // Cancel notification for deleted habit
        HabitNotificationManager.cancelHabitReminder(requireContext(), habit.id)
    }
    
    private fun updateHabit(
        existingHabit: Habit,
        name: String,
        description: String,
        targetCount: Int,
        unit: String,
        iconName: String,
        color: String,
        category: HabitCategory,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) {
        val habits = preferencesManager.getHabits().toMutableList()
        val habitIndex = habits.indexOfFirst { it.id == existingHabit.id }

        if (habitIndex >= 0) {
            val updatedHabit = existingHabit.copy(
                name = name,
                description = description,
                targetCount = targetCount,
                unit = unit,
                iconName = iconName,
                color = color,
                category = category,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            habits[habitIndex] = updatedHabit
            preferencesManager.saveHabits(habits)

            // Update notification schedule
            HabitNotificationManager.scheduleHabitReminder(requireContext(), updatedHabit)
        }
    }

    private fun showDeleteConfirmationDialog(habit: Habit) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_Alert)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_delete_confirmation)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        // Ensure dialog is properly sized
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnDelete = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)

        tvMessage?.text = "Are you sure you want to delete \"${habit.name}\"? This action cannot be undone."

        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete?.setOnClickListener {
            deleteHabit(habit)
            Toast.makeText(requireContext(), "Habit deleted successfully!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            loadHabits()
        }

        dialog.show()
    }

    private fun formatTimeForDisplay(hour: Int, minute: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)

        return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(calendar.time)
    }

    private fun showTimePickerDialog(timeButton: com.google.android.material.button.MaterialButton) {
        val calendar = java.util.Calendar.getInstance()

        // Parse current time from button text if possible
        val currentTimeText = timeButton.text.toString()
        if (currentTimeText != "9:00 AM") {
            try {
                val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                val parsedTime = timeFormat.parse(currentTimeText)
                if (parsedTime != null) {
                    calendar.time = parsedTime
                }
            } catch (e: Exception) {
                // Use default time if parsing fails
            }
        }

        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)

        val timePickerDialog = android.app.TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = formatTimeForDisplay(selectedHour, selectedMinute)
                timeButton.text = formattedTime
            },
            hour,
            minute,
            false // 12-hour format
        )

        timePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
