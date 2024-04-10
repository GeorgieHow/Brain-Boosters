package com.example.brainboosters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.Locale
import java.text.SimpleDateFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.*
import java.util.concurrent.TimeUnit

class StatisticsFragmentActivity : Fragment() {

    private var mAuth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    private lateinit var moodChart: BarChart
    private lateinit var quizResultsLineChart: LineChart
    private lateinit var quizCount: TextView
    private var count: Int = 0

    private val quizResults = mutableMapOf<Long, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.statistics_fragment, container, false).apply {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        moodChart = view.findViewById(R.id.barChart)
        quizCount = view.findViewById(R.id.total_quiz_text)
        quizResultsLineChart = view.findViewById(R.id.lineChart)
        getFirebaseDataForMood()
        getFirebaseDataForLongTermQuestions()
    }

    private fun getFirebaseDataForMood(){
        val moodCounts = mutableMapOf(
            "Happy" to 0,
            "Sad" to 0,
            "Angry" to 0,
            "Confused" to 0,
            "Calm" to 0
        )

        mAuth.currentUser?.uid?.let { currentUserUID ->
            db.collection("quizzes")
                .whereEqualTo("uid", currentUserUID)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        count++
                        val mood = document.getString("mood") ?: "Unknown"
                        moodCounts[mood] = moodCounts.getOrDefault(mood, 0) + 1
                    }
                    displayBarChart(moodCounts, count)
                }
                .addOnFailureListener { exception ->
                }
        }
    }



    private fun displayBarChart(moodCounts: Map<String, Int>, quizTotalCount: Int) {
        val entries = moodCounts.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val dataSet = BarDataSet(entries, "Mood Counts").apply{
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry?): String {
                    // Assuming you always have an integer value for the bars, return as an integer string
                    return barEntry?.y?.toInt().toString()
                }
            }
        }
        val barData = BarData(dataSet)
        moodChart.data = barData

        moodChart.apply{
            setExtraOffsets(10f, 0f, 10f, 63f)
        }

        moodChart.axisLeft.apply {
            setDrawGridLines(true)
            granularity = 1f
            isGranularityEnabled = true
            axisMinimum = 0f
        }
        moodChart.axisRight.isEnabled = false

        val moods = moodCounts.keys.toList()
        moodChart.xAxis.apply {
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            isGranularityEnabled = true
            labelCount = moods.size
            valueFormatter = IndexAxisValueFormatter(moods)
            textSize = 20f
            labelRotationAngle = 45f
        }

        moodChart.setFitBars(true) // Make the x-axis fit exactly all bars
        moodChart.setTouchEnabled(true)
        moodChart.isDragEnabled = true
        moodChart.setScaleEnabled(true)
        moodChart.setPinchZoom(false)

        moodChart.description.isEnabled = false
        moodChart.legend.isEnabled = false

        moodChart.invalidate() // Refresh the chart

        quizCount.text = quizTotalCount.toString()
    }

    private fun getFirebaseDataForLongTermQuestions() {
        mAuth.currentUser?.uid?.let { currentUserUID ->
            db.collection("quizzes")
                .whereEqualTo("uid", currentUserUID)
                .get()
                .addOnSuccessListener { quizDocuments ->
                    if (quizDocuments.isEmpty) {
                        Log.d("Quiz Results", "No quizzes found for the user.")
                        return@addOnSuccessListener
                    }

                    val quizCount = quizDocuments.size()
                    var processedQuizzes = 0

                    for (quiz in quizDocuments) {
                        val quizId = quiz.id
                        val dateString = quiz.getString("date")
                        val quizDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateString)
                        val quizTimestamp = quizDate?.time ?: continue

                        db.collection("questions")
                            .whereEqualTo("quizId", quizId)
                            .whereEqualTo("questionType", "LONG_TERM")
                            .get()
                            .addOnSuccessListener { questionDocuments ->
                                var correctLongTermCount = 0

                                for (question in questionDocuments) {
                                    if (question.getBoolean("correct") == true) {
                                        correctLongTermCount++
                                    }
                                }

                                quizResults[quizTimestamp] = correctLongTermCount
                                processedQuizzes++

                                // Check if all quizzes have been processed
                                if (processedQuizzes == quizCount) {
                                    Log.d("Quiz Results", "$quizResults")
                                    plotLineChart(quizResults)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase Error", "Error getting questions", exception)
                                processedQuizzes++

                                // Still check if we need to plot due to other successes
                                if (processedQuizzes == quizCount) {
                                    plotLineChart(quizResults)
                                    Log.d("Quiz Results", "$quizResults")
                                }
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase Error", "Error getting quizzes", exception)
                }
        }
    }



    private fun plotLineChart(quizResults: Map<Long, Int>) {
        val calendar = Calendar.getInstance()
        // Set to the start of this week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)
        val startOfWeekMillis = calendar.timeInMillis

        // End of the week (Sunday)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfWeekMillis = calendar.timeInMillis

        // Filter the results to include only quizzes within the current week
        val currentWeekQuizResults = quizResults.filterKeys {
            it in startOfWeekMillis..endOfWeekMillis
        }

        // Prepare the entries using a map to store counts for each day
        val entriesMap = mutableMapOf<Long, Entry>()

        // Initialize the entries map with zeros for each day of the current week
        for (i in 0 until 7) {
            val dayMillis = startOfWeekMillis + TimeUnit.DAYS.toMillis(i.toLong())
            entriesMap[dayMillis] = Entry(dayMillis.toFloat(), 0f)
        }

        // Populate the entries map with actual quiz results, rounded to the nearest day
        currentWeekQuizResults.forEach { (timestamp, count) ->
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.clear(Calendar.MINUTE)
            calendar.clear(Calendar.SECOND)
            calendar.clear(Calendar.MILLISECOND)
            val roundedDayMillis = calendar.timeInMillis
            entriesMap[roundedDayMillis]?.y = (entriesMap[roundedDayMillis]?.y ?: 0f) + count.toFloat()
        }

        val entries = entriesMap.values.sortedBy { it.x }
        val lineDataSet = LineDataSet(entries, "Long Term Question Correct Count").apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString() // Display whole numbers
                }
            }
        }

        val lineData = LineData(lineDataSet)
        quizResultsLineChart.data = lineData

        // Configure the x-axis
        quizResultsLineChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                private val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
                override fun getFormattedValue(value: Float): String {
                    return dateFormat.format(Date(value.toLong()))
                }
            }
            granularity = TimeUnit.DAYS.toMillis(1).toFloat() // Only allow intervals of one day
            setLabelCount(7, true) // Display a label for each day
        }

        // Configure the y-axis
        quizResultsLineChart.axisLeft.apply {
            axisMinimum = 0f // Start at zero
            granularity = 1f // Interval of 1
            isGranularityEnabled = true
        }
        quizResultsLineChart.axisRight.isEnabled = false

        // Refresh the chart
        quizResultsLineChart.invalidate()
    }

    class DateValueFormatter : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val millis = value.toLong()
            return dateFormat.format(Date(millis))
        }
    }

    companion object {
        fun newInstance() = StatisticsFragmentActivity()
    }
}