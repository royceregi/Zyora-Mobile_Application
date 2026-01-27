package com.royce.zyora.ui.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.royce.zyora.MainActivity
import com.royce.zyora.R
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager(requireContext())

        setupUI()
        loadDashboardData()
    }

    private fun setupUI() {
        // Set current date
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        binding.tvCurrentDate.text = dateFormat.format(Date())

        // Set greeting based on time
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }

        val user = preferencesManager.getCurrentUser()
        binding.tvGreeting.text = "$greeting, ${user?.username ?: "User"}!"

        // Setup quick action buttons - Navigate to respective fragments
        binding.btnLogMood.setOnClickListener {
            navigateToFragment(MoodJournalFragment(), R.id.nav_mood)
        }

        binding.btnAddWater.setOnClickListener {
            navigateToFragment(HydrationReminderFragment(), R.id.nav_hydration)
        }
    }
    
    /**
     * Helper function to navigate to a fragment and update bottom navigation
     */
    private fun navigateToFragment(fragment: Fragment, navItemId: Int) {
        // Get MainActivity and update fragment
        (activity as? MainActivity)?.let { mainActivity ->
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
            
            // Update bottom navigation selection
            mainActivity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
                ?.selectedItemId = navItemId
        }
    }

    private fun loadDashboardData() {
        loadHabitsOverview()
        loadMoodOverview()
        loadHydrationOverview()
        loadWeeklyChart()
        loadAchievements()
    }

    private fun loadHabitsOverview() {
        val habits = preferencesManager.getHabits()
        val habitProgress = preferencesManager.getHabitProgress()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayProgress = habitProgress.filter { it.date == today }

        val completedHabits = todayProgress.count { it.isCompleted }
        val totalHabits = habits.size

        binding.tvHabitsCompleted.text = "$completedHabits"
        binding.tvHabitsTotal.text = "of $totalHabits habits"

        val progressPercentage = if (totalHabits > 0) {
            (completedHabits.toFloat() / totalHabits.toFloat() * 100).toInt()
        } else 0

        binding.progressHabits.progress = progressPercentage
        binding.tvHabitsProgress.text = "$progressPercentage%"

        // Calculate habit streak
        val habitStreak = calculateHabitStreak()
        binding.tvHabitsStreak.text = if (habitStreak > 0) "🔥 $habitStreak day streak!" else ""
    }

    private fun loadMoodOverview() {
        val moodEntries = preferencesManager.getMoodEntries()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayMood = moodEntries.find { it.date == today }

        if (todayMood != null) {
            binding.tvMoodStatus.text = "Today: ${todayMood.mood.displayName}"
            binding.tvMoodEmoji.text = todayMood.mood.emoji
        } else {
            binding.tvMoodStatus.text = "No mood logged today"
            binding.tvMoodEmoji.text = "😐"
        }

        // Show recent mood trend
        val recentEntries = moodEntries.takeLast(7)
        val averageMood = if (recentEntries.isNotEmpty()) {
            recentEntries.map { it.intensity }.average()
        } else 3.0

        binding.tvMoodTrend.text = when {
            averageMood >= 4.0 -> "Trending positive ↗"
            averageMood >= 3.0 -> "Stable mood →"
            else -> "Needs attention ↘"
        }

        // Weekly count
        val thisWeekEntries = recentEntries.count()
        binding.tvMoodWeeklyCount.text = "$thisWeekEntries entries this week"
    }

    private fun loadHydrationOverview() {
        val hydrationEntries = preferencesManager.getHydrationEntries()
        val hydrationGoal = preferencesManager.getHydrationGoal()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayIntake = hydrationEntries
            .filter { it.date == today }
            .sumOf { it.amount }

        val progressPercentage = ((todayIntake.toFloat() / hydrationGoal.dailyGoalMl.toFloat()) * 100).toInt()

        binding.tvHydrationAmount.text = "${todayIntake}ml"
        binding.tvHydrationGoal.text = "of ${hydrationGoal.dailyGoalMl}ml"
        binding.progressHydration.progress = progressPercentage.coerceAtMost(100)
        binding.tvHydrationProgress.text = "$progressPercentage%"

        // Calculate hydration streak
        val hydrationStreak = calculateHydrationStreak()
        binding.tvHydrationStreak.text = if (hydrationStreak > 0) "🔥 $hydrationStreak day streak!" else ""
    }

    private fun loadWeeklyChart() {
        try {
            val habitProgress = preferencesManager.getHabitProgress()
            val hydrationEntries = preferencesManager.getHydrationEntries()
            val habits = preferencesManager.getHabits()

            val calendar = Calendar.getInstance()
            val dayViews = listOf(
                binding.barDay1 to binding.tvDay1Value,
                binding.barDay2 to binding.tvDay2Value,
                binding.barDay3 to binding.tvDay3Value,
                binding.barDay4 to binding.tvDay4Value,
                binding.barDay5 to binding.tvDay5Value,
                binding.barDay6 to binding.tvDay6Value,
                binding.barDay7 to binding.tvDay7Value
            )

        var totalCompleted = 0
        var totalPossible = 0
        var bestDay = ""
        var bestScore = 0

        // Calculate for each of the last 7 days
        for (i in 0..6) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -(6 - i))
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val dayProgress = habitProgress.filter { it.date == date }
            val completed = dayProgress.count { it.isCompleted }
            val total = habits.size

            totalCompleted += completed
            totalPossible += total

            val percentage = if (total > 0) (completed.toFloat() / total.toFloat() * 100).toInt() else 0

            if (percentage > bestScore) {
                bestScore = percentage
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                bestDay = dayNames[i]
            }

            // Update bar height (max 40dp, min 4dp)
            val barHeight = if (total > 0) (percentage / 100f * 40f).toInt().coerceAtLeast(4) else 4
            val params = dayViews[i].first.layoutParams
            params.height = barHeight
            dayViews[i].first.layoutParams = params

            // Update color based on completion
            val color = when {
                percentage >= 80 -> Color.parseColor("#4CAF50") // Green
                percentage >= 60 -> Color.parseColor("#FF9800") // Orange
                percentage >= 40 -> Color.parseColor("#FFC107") // Yellow
                else -> Color.parseColor("#E0E0E0") // Light gray
            }

            // Safely update background color
            try {
                when (val drawable = dayViews[i].first.background) {
                    is GradientDrawable -> drawable.setColor(color)
                    else -> {
                        // Create a new GradientDrawable if the background isn't one
                        val newDrawable = GradientDrawable()
                        newDrawable.setColor(color)
                        newDrawable.cornerRadius = 8f
                        dayViews[i].first.background = newDrawable
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error setting bar color", e)
            }

            // Update text
            dayViews[i].second.text = "$completed/$total"
        }

        // Weekly summary
        val weeklyAverage = if (totalPossible > 0) {
            (totalCompleted.toFloat() / totalPossible.toFloat() * 7).toInt() / 7f
        } else 0f

        val weeklyPercentage = if (totalPossible > 0) {
            (totalCompleted.toFloat() / totalPossible.toFloat() * 100).toInt()
        } else 0

            binding.tvWeeklySummary.text = "Avg: ${String.format("%.1f", weeklyAverage)}/${habits.size} habits • Best day: $bestDay"
            binding.tvWeeklyScore.text = "$weeklyPercentage%"
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Error loading weekly chart", e)
            // Set fallback values
            binding.tvWeeklySummary.text = "Weekly data unavailable"
            binding.tvWeeklyScore.text = "0%"
        }
    }

    private fun loadAchievements() {
        val habitStreak = calculateHabitStreak()
        val hydrationGoalDays = calculateHydrationGoalDays()
        val moodTrackingDays = calculateMoodTrackingDays()

        // Habit streak achievement
        binding.tvHabitStreakBadge.text = when {
            habitStreak >= 30 -> "🏆"
            habitStreak >= 14 -> "🔥"
            habitStreak >= 7 -> "💪"
            habitStreak >= 3 -> "👍"
            else -> "🎯"
        }
        binding.tvHabitStreakCount.text = if (habitStreak > 0) "$habitStreak days" else "Start today!"

        // Hydration achievement
        binding.tvHydrationBadge.text = when {
            hydrationGoalDays >= 30 -> "🏆"
            hydrationGoalDays >= 14 -> "💧"
            hydrationGoalDays >= 7 -> "🌊"
            hydrationGoalDays >= 3 -> "💦"
            else -> "🚰"
        }
        binding.tvHydrationGoalCount.text = if (hydrationGoalDays > 0) "$hydrationGoalDays days" else "Drink up!"

        // Mood tracking achievement
        binding.tvMoodBadge.text = when {
            moodTrackingDays >= 30 -> "🌟"
            moodTrackingDays >= 14 -> "😊"
            moodTrackingDays >= 7 -> "🙂"
            moodTrackingDays >= 3 -> "😐"
            else -> "📝"
        }
        binding.tvMoodStreakCount.text = if (moodTrackingDays > 0) "$moodTrackingDays days" else "Track mood!"
    }

    private fun calculateHabitStreak(): Int {
        val habitProgress = preferencesManager.getHabitProgress()
        val habits = preferencesManager.getHabits()

        if (habits.isEmpty()) return 0

        var streak = 0
        val calendar = Calendar.getInstance()

        // Check each day going backwards until we find a day with incomplete habits
        while (true) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val dayProgress = habitProgress.filter { it.date == date }
            val completedCount = dayProgress.count { it.isCompleted }

            if (completedCount == habits.size && completedCount > 0) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }

    private fun calculateHydrationStreak(): Int {
        val hydrationEntries = preferencesManager.getHydrationEntries()
        val hydrationGoal = preferencesManager.getHydrationGoal()

        if (hydrationGoal.dailyGoalMl <= 0) return 0

        var streak = 0
        val calendar = Calendar.getInstance()

        // Check each day going backwards until we find a day that didn't meet the goal
        while (true) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val dayIntake = hydrationEntries
                .filter { it.date == date }
                .sumOf { it.amount }

            if (dayIntake >= hydrationGoal.dailyGoalMl) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }

    private fun calculateHydrationGoalDays(): Int {
        val hydrationEntries = preferencesManager.getHydrationEntries()
        val hydrationGoal = preferencesManager.getHydrationGoal()

        if (hydrationGoal.dailyGoalMl <= 0) return 0

        var goalDays = 0
        val calendar = Calendar.getInstance()

        // Count days that met hydration goal in the last 30 days
        for (i in 0..29) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val dayIntake = hydrationEntries
                .filter { it.date == date }
                .sumOf { it.amount }

            if (dayIntake >= hydrationGoal.dailyGoalMl) {
                goalDays++
            }
        }

        return goalDays
    }

    private fun calculateMoodTrackingDays(): Int {
        val moodEntries = preferencesManager.getMoodEntries()
        val uniqueDates = moodEntries.map { it.date }.distinct()

        // Count unique days with mood entries in the last 30 days
        var trackingDays = 0
        val calendar = Calendar.getInstance()

        for (i in 0..29) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            if (uniqueDates.contains(date)) {
                trackingDays++
            }
        }

        return trackingDays
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData() // Refresh data when returning to fragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
