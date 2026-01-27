package com.royce.zyora.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.royce.zyora.R
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.data.models.ContainerType
import com.royce.zyora.data.models.HydrationEntry
import com.royce.zyora.data.models.HydrationGoal
import com.royce.zyora.data.models.PortionSchedule
import com.royce.zyora.data.models.DailyPortionPlan
import com.royce.zyora.databinding.FragmentHydrationReminderBinding
import com.royce.zyora.databinding.DialogAddWaterBinding
import com.royce.zyora.ui.adapters.HydrationAdapter
import com.royce.zyora.ui.adapters.PortionScheduleAdapter
import com.royce.zyora.utils.HydrationNotificationManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for tracking water intake with scheduled reminders.
 * Uses AlarmManager to send notifications at configured intervals.
 * Supports customizable daily goals, glass sizes, and reminder times.
 */
class HydrationReminderFragment : Fragment() {
    
    private var _binding: FragmentHydrationReminderBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var hydrationAdapter: HydrationAdapter
    private lateinit var portionScheduleAdapter: PortionScheduleAdapter
    private val hydrationList = mutableListOf<HydrationEntry>()
    private val scheduledPortions = mutableListOf<PortionSchedule>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            _binding = FragmentHydrationReminderBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            android.util.Log.e("HydrationFragment", "Error inflating layout", e)
            throw e
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        // Initialize notification system
        HydrationNotificationManager.createNotificationChannel(requireContext())
        
        setupRecyclerView()
        setupPortionScheduleRecyclerView()
        setupUI()
        generateDailyPortionPlan()
        loadHydrationData()
        
        // Schedule notifications based on current settings
        val currentGoal = preferencesManager.getHydrationGoal()
        HydrationNotificationManager.scheduleHydrationReminders(requireContext(), currentGoal)
    }
    
    private fun setupRecyclerView() {
        try {
            hydrationAdapter = HydrationAdapter(
                entries = hydrationList,
                onEntryDelete = { entry ->
                    deleteHydrationEntry(entry)
                }
            )

            binding.rvHydrationEntries.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = hydrationAdapter
            }
        } catch (e: Exception) {
            android.util.Log.e("HydrationFragment", "Error setting up RecyclerView", e)
        }
    }
    
    private fun setupPortionScheduleRecyclerView() {
        try {
            portionScheduleAdapter = PortionScheduleAdapter(
                portions = scheduledPortions,
                onPortionChecked = { portionIndex, isChecked ->
                    handlePortionChecked(portionIndex, isChecked)
                }
            )

            binding.rvScheduledPortions.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = portionScheduleAdapter
            }
        } catch (e: Exception) {
            android.util.Log.e("HydrationFragment", "Error setting up Portion RecyclerView", e)
        }
    }
    
    /**
     * Generates a daily water intake schedule based on user settings.
     * Divides daily goal into portions with specific reminder times.
     * Creates a new plan each day to track completion.
     */
    private fun generateDailyPortionPlan() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val goal = preferencesManager.getHydrationGoal()
        
        // Check if we already have a plan for today
        val existingPlan = preferencesManager.getDailyPortionPlan()
        if (existingPlan != null && existingPlan.date == today) {
            // Load existing plan
            scheduledPortions.clear()
            scheduledPortions.addAll(existingPlan.scheduledPortions)
            portionScheduleAdapter.notifyDataSetChanged()
            return
        }
        
        // Generate new plan for today based on interval
        val portions = mutableListOf<PortionSchedule>()
        val totalGlasses = goal.portionsPerDay // e.g., 8 glasses
        val intervalMinutes = goal.reminderIntervalMinutes // e.g., 45 minutes
        val glassSize = goal.portionSizeMl // e.g., 250ml
        
        // Parse start time
        val startParts = goal.startTime.split(":").map { it.toInt() }
        val startHour = startParts[0]
        val startMinute = startParts[1]
        
        // Create portion schedules based on interval
        for (glassNumber in 1..totalGlasses) {
            // Calculate time for this glass
            val minutesFromStart = (glassNumber - 1) * intervalMinutes
            val totalMinutes = startHour * 60 + startMinute + minutesFromStart
            val hourOfDay = (totalMinutes / 60) % 24 // Handle midnight rollover
            val minuteOfHour = totalMinutes % 60
            val scheduledTime = String.format("%02d:%02d", hourOfDay, minuteOfHour)
            
            portions.add(
                PortionSchedule(
                    portionIndex = glassNumber,
                    scheduledTime = scheduledTime,
                    portionSize = glassSize,
                    isCompleted = false,
                    completedAt = null,
                    date = today
                )
            )
        }
        
        // Save the plan
        val plan = DailyPortionPlan(
            date = today,
            totalGlasses = totalGlasses,
            glassSize = glassSize,
            intervalMinutes = intervalMinutes,
            startTime = goal.startTime,
            scheduledPortions = portions
        )
        preferencesManager.saveDailyPortionPlan(plan)
        
        // Update UI
        scheduledPortions.clear()
        scheduledPortions.addAll(portions)
        portionScheduleAdapter.notifyDataSetChanged()
    }
    
    private fun handlePortionChecked(portionIndex: Int, isChecked: Boolean) {
        // Update in preferences
        preferencesManager.updatePortionCompletion(portionIndex, isChecked)
        
        // Update local list
        val index = scheduledPortions.indexOfFirst { it.portionIndex == portionIndex }
        if (index != -1) {
            scheduledPortions[index] = scheduledPortions[index].copy(
                isCompleted = isChecked,
                completedAt = if (isChecked) System.currentTimeMillis() else null
            )
            portionScheduleAdapter.notifyItemChanged(index)
        }
        
        // If checked, add hydration entry automatically
        if (isChecked) {
            val portion = scheduledPortions.find { it.portionIndex == portionIndex }
            portion?.let {
                addWaterEntry(ContainerType.GLASS, it.portionSize)
            }
        }
        
        // Reload hydration data to update progress
        loadHydrationData()
    }
    
    private fun setupUI() {
        binding.fabAddWater.setOnClickListener {
            showAddWaterDialog()
        }
        
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
        
        // Quick add buttons - update with current portion size
        val goal = preferencesManager.getHydrationGoal()
        updateQuickAddButtons(goal)
    }
    
    private fun updateQuickAddButtons(goal: HydrationGoal) {
        val glassSize = goal.portionSizeMl
        
        binding.btnQuickGlass.apply {
            text = "Glass\n${glassSize}ml"
            setOnClickListener {
                addWaterEntry(ContainerType.GLASS, glassSize)
            }
        }
        
        binding.btnQuickBottle.apply {
            text = "Bottle\n${glassSize * 2}ml"
            setOnClickListener {
                addWaterEntry(ContainerType.BOTTLE, glassSize * 2)
            }
        }
        
        binding.btnQuickCup.apply {
            text = "Cup\n${glassSize / 2}ml"
            setOnClickListener {
                addWaterEntry(ContainerType.CUP, glassSize / 2)
            }
        }
    }
    
    private fun loadHydrationData() {
        try {
            val entries = preferencesManager.getHydrationEntries()
            val goal = preferencesManager.getHydrationGoal()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Filter today's entries
            val todayEntries = entries.filter { it.date == today }
            hydrationList.clear()
            hydrationList.addAll(todayEntries.sortedByDescending { it.timestamp })
            hydrationAdapter.notifyDataSetChanged()

            // Update progress
            updateProgress(todayEntries, goal)

            // Update quick add buttons with current portion size
            updateQuickAddButtons(goal)

            // Update empty state
            val emptyStateView = binding.root.findViewById<LinearLayout>(R.id.tvEmptyState)
            emptyStateView.visibility = if (hydrationList.isEmpty()) View.VISIBLE else View.GONE
            binding.rvHydrationEntries.visibility = if (hydrationList.isEmpty()) View.GONE else View.VISIBLE
        } catch (e: Exception) {
            // Handle any unexpected errors
            android.util.Log.e("HydrationFragment", "Error loading hydration data", e)
            Toast.makeText(requireContext(), "Error loading data. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateProgress(todayEntries: List<HydrationEntry>, goal: HydrationGoal) {
        val totalIntake = todayEntries.sumOf { it.amount }
        val dailyGoal = goal.dailyGoalMl

        // Prevent division by zero
        val progressPercentage = if (dailyGoal > 0) {
            ((totalIntake.toFloat() / dailyGoal.toFloat()) * 100).toInt()
        } else {
            0
        }

        binding.apply {
            tvCurrentIntake.text = "${totalIntake}ml"
            tvDailyGoal.text = "Goal: ${goal.portionsPerDay} glasses × ${goal.portionSizeMl}ml = ${dailyGoal}ml"
            progressHydration.max = dailyGoal
            progressHydration.progress = totalIntake
            tvProgressPercentage.text = "$progressPercentage%"

            // Update portion info based on glasses completed
            val glassesCompleted = scheduledPortions.count { it.isCompleted }
            val remainingGlasses = goal.portionsPerDay - glassesCompleted
            tvPortionInfo.text = if (remainingGlasses > 0) {
                "✅ $glassesCompleted/${goal.portionsPerDay} glasses checked • $remainingGlasses remaining"
            } else {
                "🎉 All ${goal.portionsPerDay} glasses completed for today!"
            }

            // Update status message
            tvStatus.text = when {
                dailyGoal <= 0 -> "⚙️ Set your daily hydration goal in settings"
                progressPercentage >= 100 -> "🎉 Goal achieved! Great job!"
                progressPercentage >= 75 -> "💪 Almost there! Keep going!"
                progressPercentage >= 50 -> "👍 Good progress!"
                progressPercentage >= 25 -> "💧 Keep drinking!"
                else -> "🚰 Time to hydrate!"
            }
        }
    }
    
    private fun showAddWaterDialog() {
        val dialogBinding = DialogAddWaterBinding.inflate(layoutInflater)
        
        // Setup container type buttons
        val containerButtons = listOf(
            dialogBinding.btnGlass to ContainerType.GLASS,
            dialogBinding.btnBottle to ContainerType.BOTTLE,
            dialogBinding.btnCup to ContainerType.CUP,
            dialogBinding.btnLargeBottle to ContainerType.LARGE_BOTTLE,
            dialogBinding.btnCustom to ContainerType.CUSTOM
        )
        
        var selectedContainer: ContainerType = ContainerType.GLASS
        dialogBinding.etAmount.setText(selectedContainer.defaultAmount.toString())
        
        containerButtons.forEach { (button, containerType) ->
            button.text = "${containerType.displayName} (${containerType.defaultAmount}ml)"
            button.setOnClickListener {
                selectedContainer = containerType
                dialogBinding.etAmount.setText(containerType.defaultAmount.toString())
                // Update button states
                containerButtons.forEach { (btn, _) -> 
                    btn.isSelected = btn == button
                }
            }
        }
        
        // Select glass by default
        dialogBinding.btnGlass.isSelected = true
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Water Intake")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val amount = dialogBinding.etAmount.text.toString().toIntOrNull() ?: 0
                if (amount > 0) {
                    addWaterEntry(selectedContainer, amount)
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    private fun addQuickWater(containerType: ContainerType) {
        addWaterEntry(containerType, containerType.defaultAmount)
    }
    
    /**
     * Records a water intake entry with timestamp.
     * Updates daily progress and saves to SharedPreferences.
     * 
     * @param containerType Type of container (glass, bottle, cup, etc.)
     * @param amount Amount of water in milliliters
     */
    private fun addWaterEntry(containerType: ContainerType, amount: Int) {
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val entry = HydrationEntry(
            id = UUID.randomUUID().toString(),
            amount = amount,
            timestamp = currentTime,
            date = dateFormat.format(Date(currentTime)),
            time = timeFormat.format(Date(currentTime)),
            containerType = containerType
        )
        
        val allEntries = preferencesManager.getHydrationEntries().toMutableList()
        allEntries.add(entry)
        preferencesManager.saveHydrationEntries(allEntries)
        
        loadHydrationData()
        Toast.makeText(requireContext(), "Added ${amount}ml of water!", Toast.LENGTH_SHORT).show()
        
        // Update widget
        com.royce.zyora.widget.HabitProgressWidget.updateAllWidgets(requireContext())
    }
    
    private fun deleteHydrationEntry(entry: HydrationEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this water entry?")
            .setPositiveButton("Delete") { _, _ ->
                val allEntries = preferencesManager.getHydrationEntries().toMutableList()
                allEntries.removeAll { it.id == entry.id }
                preferencesManager.saveHydrationEntries(allEntries)
                loadHydrationData()
                Toast.makeText(requireContext(), "Entry deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showSettingsDialog() {
        val goal = preferencesManager.getHydrationGoal()
        
        val items = arrayOf(
            "Set Number of Glasses (${goal.portionsPerDay} glasses)",
            "Set Glass Size (${goal.portionSizeMl}ml per glass)",
            "Set Reminder Interval (Every ${goal.reminderIntervalMinutes} min)",
            "Set Start Time (${goal.startTime})",
            "Toggle Reminders"
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hydration Settings")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showNumberOfGlassesDialog(goal)
                    1 -> showGlassSizeDialog(goal)
                    2 -> showIntervalDialog(goal)
                    3 -> showStartTimeDialog(goal)
                    4 -> showToggleRemindersDialog(goal)
                }
            }
            .show()
    }
    
    private fun showNumberOfGlassesDialog(currentGoal: HydrationGoal) {
        val glasses = arrayOf("4 glasses", "6 glasses", "8 glasses", "10 glasses", "12 glasses")
        val glassValues = arrayOf(4, 6, 8, 10, 12)

        val currentIndex = glassValues.indexOf(currentGoal.portionsPerDay)
        val defaultIndex = if (currentIndex >= 0) currentIndex else 2 // Default to 8 glasses

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("How many glasses per day?")
            .setSingleChoiceItems(glasses, defaultIndex) { dialog, which ->
                val selectedGlasses = glassValues[which]
                val updatedGoal = currentGoal.copy(
                    portionsPerDay = selectedGlasses,
                    dailyGoalMl = selectedGlasses * currentGoal.portionSizeMl // Update total goal
                )
                preferencesManager.saveHydrationGoal(updatedGoal)

                // Update notification schedule
                HydrationNotificationManager.scheduleHydrationReminders(requireContext(), updatedGoal)

                // Regenerate daily portion plan
                generateDailyPortionPlan()
                
                loadHydrationData()
                Toast.makeText(requireContext(), "Set to ${glasses[which]} per day", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showIntervalDialog(currentGoal: HydrationGoal) {
        val intervals = arrayOf("Every 30 min", "Every 45 min", "Every 60 min (1 hour)", "Every 90 min", "Every 120 min (2 hours)")
        val intervalValues = arrayOf(30, 45, 60, 90, 120)

        val currentIndex = intervalValues.indexOf(currentGoal.reminderIntervalMinutes)
        val defaultIndex = if (currentIndex >= 0) currentIndex else 2 // Default to 60 min

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reminder Interval")
            .setSingleChoiceItems(intervals, defaultIndex) { dialog, which ->
                val updatedGoal = currentGoal.copy(
                    reminderIntervalMinutes = intervalValues[which]
                )
                preferencesManager.saveHydrationGoal(updatedGoal)

                // Update notification schedule
                HydrationNotificationManager.scheduleHydrationReminders(requireContext(), updatedGoal)

                // Regenerate daily portion plan
                generateDailyPortionPlan()
                
                Toast.makeText(requireContext(), "Reminder set to ${intervals[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGlassSizeDialog(currentGoal: HydrationGoal) {
        val sizes = arrayOf("Small (200ml)", "Medium (250ml)", "Large (300ml)", "Extra Large (350ml)")
        val sizeValues = arrayOf(200, 250, 300, 350)

        val currentIndex = sizeValues.indexOf(currentGoal.portionSizeMl)
        val defaultIndex = if (currentIndex >= 0) currentIndex else 1 // Default to 250ml

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Glass Size")
            .setSingleChoiceItems(sizes, defaultIndex) { dialog, which ->
                val selectedSize = sizeValues[which]
                val updatedGoal = currentGoal.copy(
                    portionSizeMl = selectedSize,
                    dailyGoalMl = currentGoal.portionsPerDay * selectedSize // Update total goal
                )
                preferencesManager.saveHydrationGoal(updatedGoal)

                // Update notification schedule
                HydrationNotificationManager.scheduleHydrationReminders(requireContext(), updatedGoal)

                // Regenerate daily portion plan
                generateDailyPortionPlan()
                
                loadHydrationData()
                Toast.makeText(requireContext(), "Glass size set to ${sizes[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showToggleRemindersDialog(currentGoal: HydrationGoal) {
        val message = if (currentGoal.isReminderEnabled) {
            "Reminders are currently ON. Turn them off?"
        } else {
            "Reminders are currently OFF. Turn them on?"
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hydration Reminders")
            .setMessage(message)
            .setPositiveButton(if (currentGoal.isReminderEnabled) "Turn OFF" else "Turn ON") { _, _ ->
                val updatedGoal = currentGoal.copy(isReminderEnabled = !currentGoal.isReminderEnabled)
                preferencesManager.saveHydrationGoal(updatedGoal)
                
                // Update notification schedule
                HydrationNotificationManager.scheduleHydrationReminders(requireContext(), updatedGoal)
                
                val status = if (updatedGoal.isReminderEnabled) "enabled" else "disabled"
                Toast.makeText(requireContext(), "Hydration reminders $status", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showStartTimeDialog(currentGoal: HydrationGoal) {
        val calendar = Calendar.getInstance()

        // Parse current start time
        val timeParts = currentGoal.startTime.split(":").map { it.toIntOrNull() ?: 0 }
        calendar.set(Calendar.HOUR_OF_DAY, timeParts.getOrElse(0) { 8 })
        calendar.set(Calendar.MINUTE, timeParts.getOrElse(1) { 0 })

        val timePickerDialog = android.app.TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val timeString = String.format("%02d:%02d", hourOfDay, minute)
                val updatedGoal = currentGoal.copy(startTime = timeString)

                preferencesManager.saveHydrationGoal(updatedGoal)

                // Update notification schedule with new start time
                HydrationNotificationManager.scheduleHydrationReminders(requireContext(), updatedGoal)

                // Regenerate daily portion plan
                generateDailyPortionPlan()
                
                Toast.makeText(requireContext(), "First reminder will start at $timeString", Toast.LENGTH_SHORT).show()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24-hour format
        )

        timePickerDialog.setTitle("When should reminders start?")
        timePickerDialog.show()
    }
    
    private fun calculateNextPortionTime(goal: HydrationGoal): String {
        try {
            // Parse start and end times
            val startParts = goal.startTime.split(":").map { it.toInt() }
            val endParts = goal.endTime.split(":").map { it.toInt() }

            val startHour = startParts[0]
            val startMinute = startParts[1]
            val endHour = endParts[0]
            val endMinute = endParts[1]

            val startTimeInMinutes = startHour * 60 + startMinute
            val endTimeInMinutes = endHour * 60 + endMinute
            val availableMinutes = endTimeInMinutes - startTimeInMinutes

            // Calculate interval between portions
            val intervalMinutes = if (goal.portionsPerDay > 1) {
                availableMinutes / (goal.portionsPerDay - 1)
            } else {
                120 // Default 2 hours
            }

            // Get current time
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute

            // Get today's intake to see which portion we're on
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayEntries = preferencesManager.getHydrationEntries().filter { it.date == today }
            val totalIntake = todayEntries.sumOf { it.amount }
            val portionsCompleted = totalIntake / goal.portionSizeMl

            // Calculate next portion index
            val nextPortionIndex = portionsCompleted + 1
            if (nextPortionIndex > goal.portionsPerDay) {
                return "Done for today"
            }

            // Calculate next portion time
            val portionTimeInMinutes = startTimeInMinutes + (nextPortionIndex - 1) * intervalMinutes
            val portionHour = portionTimeInMinutes / 60
            val portionMinute = portionTimeInMinutes % 60

            // If next portion time has passed, schedule for tomorrow
            val nextPortionTimeInMinutes = portionHour * 60 + portionMinute
            val isTomorrow = nextPortionTimeInMinutes < currentTimeInMinutes

            return String.format("%02d:%02d", portionHour, portionMinute) + if (isTomorrow) " (tomorrow)" else ""

        } catch (e: Exception) {
            return "TBD"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
