package com.example.brainboosters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StatisticsFragmentActivity : Fragment() {

    private var mAuth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    private lateinit var moodChart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.statistics_fragment, container, false).apply {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        moodChart = view.findViewById(R.id.barChart)
        getFirebaseData()
    }

    private fun getFirebaseData(){
        val moodCounts = mutableMapOf<String, Int>()

        mAuth.currentUser?.uid?.let { currentUserUID ->
            db.collection("quizzes")
                .whereEqualTo("uid", currentUserUID)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val mood = document.getString("mood") ?: "Unknown"
                        moodCounts[mood] = moodCounts.getOrDefault(mood, 0) + 1
                    }
                    displayBarChart(moodCounts)
                }
                .addOnFailureListener { exception ->
                }
        }

    }
    private fun displayBarChart(moodCounts: Map<String, Int>) {
        val entries = moodCounts.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val dataSet = BarDataSet(entries, "Mood Counts")
        val barData = BarData(dataSet)
        moodChart.data = barData

        moodChart.axisLeft.apply {
            setDrawGridLines(true)
            granularity = 1f
            isGranularityEnabled = true
            axisMinimum = 0f


        }
        moodChart.axisRight.isEnabled = false

        moodChart.xAxis.apply {
            setDrawGridLines(false) // Remove grid lines
            position = XAxis.XAxisPosition.BOTTOM // Set labels to the bottom
            valueFormatter = IndexAxisValueFormatter(moodCounts.keys.toList())
        }

        moodChart.setTouchEnabled(false)
        moodChart.isDragEnabled = false
        moodChart.setScaleEnabled(false)
        moodChart.setPinchZoom(false)

        // Customize the chart as needed
        moodChart.description.text = "Quiz Mood Distribution"
        moodChart.xAxis.valueFormatter = IndexAxisValueFormatter(moodCounts.keys.toList())

        moodChart.invalidate() // Refresh the chart
    }

    companion object {
        fun newInstance() = StatisticsFragmentActivity()
    }
}