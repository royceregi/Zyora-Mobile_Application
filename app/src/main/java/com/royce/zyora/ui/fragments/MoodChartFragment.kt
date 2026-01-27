package com.royce.zyora.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.royce.zyora.R
import com.royce.zyora.data.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment that displays a mood trend chart using MPAndroidChart library.
 * Shows mood intensity over the past 7 days to visualize emotional patterns.
 */
class MoodChartFragment : Fragment() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var lineChart: LineChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mood_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        lineChart = view.findViewById(R.id.moodLineChart)
        
        setupChart()
        loadMoodData()
    }

    /**
     * Configures the chart appearance and behavior
     */
    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            // Configure X-axis (dates)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = -45f
            }
            
            // Configure Y-axis (mood intensity 1-5)
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 6f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    /**
     * Loads mood data from the last 7 days and populates the chart
     */
    private fun loadMoodData() {
        val moodEntries = preferencesManager.getMoodEntries()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Create entries for the last 7 days
        val entries = mutableListOf<Entry>()
        val dateLabels = mutableListOf<String>()
        
        // Go through last 7 days
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(calendar.time)
            
            // Get mood for this date
            val dayMood = moodEntries.find { it.date == date }
            val intensity = dayMood?.intensity?.toFloat() ?: 0f
            
            // Add entry (x = day index, y = intensity)
            // Only add non-zero values to avoid flat line at bottom
            if (intensity > 0f) {
                entries.add(Entry((6 - i).toFloat(), intensity))
            }
            
            // Add date label (short format)
            val shortDateFormat = SimpleDateFormat("EEE", Locale.getDefault())
            dateLabels.add(shortDateFormat.format(calendar.time))
        }
        
        // If no data exists, show empty state message
        if (entries.isEmpty()) {
            android.widget.Toast.makeText(
                requireContext(),
                "No mood data available. Start logging your moods!",
                android.widget.Toast.LENGTH_LONG
            ).show()
            // Add a single point at 0 to show the chart structure
            entries.add(Entry(3f, 0f))
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, "Mood Intensity (1=Low, 5=High)").apply {
            color = Color.parseColor("#9C27B0")
            setCircleColor(Color.parseColor("#9C27B0"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
            setDrawFilled(true)
            fillColor = Color.parseColor("#E1BEE7")
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        // Set custom X-axis labels
        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value.toInt() in dateLabels.indices) {
                    dateLabels[value.toInt()]
                } else ""
            }
        }
        
        // Apply data to chart
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate() // Refresh chart
    }

    override fun onResume() {
        super.onResume()
        // Refresh chart data when returning to fragment
        loadMoodData()
    }
}
