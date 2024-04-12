package com.example.brainboosters

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
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
    private lateinit var longTermLineChart: LineChart
    private lateinit var shortTermLineChart: LineChart
    private lateinit var quizCount: TextView
    private var count: Int = 0

    private val longTermQuizResults = mutableMapOf<Long, Pair<Int, Int>>()
    private val shortTermQuizResults = mutableMapOf<Long, Pair<Int, Int>>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.statistics_fragment, container, false).apply {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        longTermLineChart = view.findViewById(R.id.long_term_line_chart)
        shortTermLineChart = view.findViewById(R.id.short_term_line_chart)

        val longTermlineChartSpinner = view.findViewById<Spinner>(R.id.long_term_line_chart_spinner)

        val longTermAdapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.time_options, // The array resource containing your items
            R.layout.spinner_item // Custom layout for items
        )
        longTermAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        longTermlineChartSpinner.adapter = longTermAdapter

        longTermlineChartSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> getFirebaseDataForLongTermQuestions { results ->
                        plotWeeklyLineChart(results, longTermLineChart)
                    }
                    1 -> getFirebaseDataForLongTermQuestions { results ->
                        plotCurrentMonthWeeklyChart(results, longTermLineChart)
                    }
                    2 -> getFirebaseDataForLongTermQuestions { results ->
                        plotMonthlyLineChart(results, longTermLineChart)
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                getFirebaseDataForLongTermQuestions { results ->
                    plotWeeklyLineChart(results, longTermLineChart)
                }
            }
        }

        val shortTermlineChartSpinner = view.findViewById<Spinner>(R.id.short_term_line_chart_spinner)

        val shortTermAdapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.time_options, // The array resource containing your items
            R.layout.spinner_item // Custom layout for items
        )
        shortTermAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        shortTermlineChartSpinner.adapter = shortTermAdapter

        shortTermlineChartSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> getFirebaseDataForShortTermQuestions { results ->
                        plotWeeklyLineChart(results, shortTermLineChart)
                    }
                    1 -> getFirebaseDataForShortTermQuestions { results ->
                        plotCurrentMonthWeeklyChart(results, shortTermLineChart)
                    }
                    2 -> getFirebaseDataForShortTermQuestions { results ->
                        plotMonthlyLineChart(results, shortTermLineChart)
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                getFirebaseDataForShortTermQuestions { results ->
                    plotWeeklyLineChart(results, shortTermLineChart)
                }
            }
        }

        moodChart = view.findViewById(R.id.mood_bar_chart)
        quizCount = view.findViewById(R.id.total_quiz_text)

        getFirebaseDataForMood()
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

        val barColors = listOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#F44336"),
            Color.parseColor("#FFEB3B"),
            Color.parseColor("#9C27B0")
        )

        val dataSet = BarDataSet(entries, "Mood Counts").apply{
            setColors(barColors)
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

    private fun getFirebaseDataForLongTermQuestions(plotFunction: (Map<Long, Pair<Int, Int>>) -> Unit) {
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
                                var incorrectLongTermCount = 0

                                for (question in questionDocuments) {
                                    if (question.getBoolean("correct") == true) {
                                        correctLongTermCount++
                                    } else {
                                        incorrectLongTermCount++
                                    }
                                }

                                longTermQuizResults[quizTimestamp] = Pair(correctLongTermCount, incorrectLongTermCount)
                                processedQuizzes++

                                // Check if all quizzes have been processed
                                if (processedQuizzes == quizCount) {
                                    Log.d("Quiz Results", "$longTermQuizResults")
                                    plotFunction(longTermQuizResults)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase Error", "Error getting questions", exception)
                                processedQuizzes++

                                // Still check if we need to plot due to other successes
                                if (processedQuizzes == quizCount) {
                                    plotFunction(longTermQuizResults)
                                    Log.d("Quiz Results", "$longTermQuizResults")
                                }
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase Error", "Error getting quizzes", exception)
                }
        }
    }

    private fun getFirebaseDataForShortTermQuestions(plotFunction: (Map<Long, Pair<Int, Int>>) -> Unit) {
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
                            .whereEqualTo("questionType", "SHORT_TERM")
                            .get()
                            .addOnSuccessListener { questionDocuments ->
                                var correctLongTermCount = 0
                                var incorrectLongTermCount = 0

                                for (question in questionDocuments) {
                                    if (question.getBoolean("correct") == true) {
                                        correctLongTermCount++
                                    } else {
                                        incorrectLongTermCount++
                                    }
                                }

                                shortTermQuizResults[quizTimestamp] = Pair(correctLongTermCount, incorrectLongTermCount)
                                processedQuizzes++

                                // Check if all quizzes have been processed
                                if (processedQuizzes == quizCount) {
                                    Log.d("Quiz Results", "$shortTermQuizResults")
                                    plotFunction(shortTermQuizResults)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase Error", "Error getting questions", exception)
                                processedQuizzes++

                                // Still check if we need to plot due to other successes
                                if (processedQuizzes == quizCount) {
                                    plotFunction(shortTermQuizResults)
                                    Log.d("Quiz Results", "$shortTermQuizResults")
                                }
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase Error", "Error getting quizzes", exception)
                }
        }
    }

    private fun plotWeeklyLineChart(quizResults: Map<Long, Pair<Int,Int>>, lineChart: LineChart) {
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

        val correctEntriesMap = mutableMapOf<Long, Entry>()
        val incorrectEntriesMap = mutableMapOf<Long, Entry>()

        // Prepare the entries using a map to store counts for each day
        val entriesMap = mutableMapOf<Long, Entry>()

        // Initialize the entries map with zeros for each day of the current week
        for (i in 0 until 7) {
            val dayMillis = startOfWeekMillis + TimeUnit.DAYS.toMillis(i.toLong())
            correctEntriesMap[dayMillis] = Entry(dayMillis.toFloat(), 0f)   // Set zero for correct answers
            incorrectEntriesMap[dayMillis] = Entry(dayMillis.toFloat(), 0f) // Set zero for incorrect answers
        }

        // Filter the results to include only quizzes within the current week
        val currentWeekQuizResults = quizResults.filterKeys {
            it in startOfWeekMillis..endOfWeekMillis
        }

        // Populate the entries map with actual quiz results, rounded to the nearest day
        currentWeekQuizResults.forEach { (timestamp, counts) ->
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.clear(Calendar.MINUTE)
            calendar.clear(Calendar.SECOND)
            calendar.clear(Calendar.MILLISECOND)
            val roundedDayMillis = calendar.timeInMillis

            val correctEntry = correctEntriesMap.getOrPut(roundedDayMillis) { Entry(roundedDayMillis.toFloat(), 0f) }
            correctEntry.y += counts.first.toFloat()

            val incorrectEntry = incorrectEntriesMap.getOrPut(roundedDayMillis) { Entry(roundedDayMillis.toFloat(), 0f) }
            incorrectEntry.y += counts.second.toFloat()
        }

        val entries = entriesMap.values.sortedBy { it.x }
        val lineDataSet = LineDataSet(entries, "Long Term Question Correct Count").apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString() // Display whole numbers
                }
            }
        }

        val correctLineDataSet = LineDataSet(correctEntriesMap.values.toList().sortedBy { it.x }, "Correct Answers").apply {
            color = Color.GREEN
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            // Set the value formatter for the data set to ensure whole numbers
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        val incorrectLineDataSet = LineDataSet(incorrectEntriesMap.values.toList().sortedBy { it.x }, "Incorrect Answers").apply {
            color = Color.RED
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            // Set the value formatter for the data set to ensure whole numbers
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        val lineData = LineData(correctLineDataSet, incorrectLineDataSet)
        lineChart.data = lineData

        // Configure the x-axis
        lineChart.xAxis.apply {
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
        lineChart.axisLeft.apply {
            axisMinimum = 0f // Start at zero
            granularity = 1f // Interval of 1
            isGranularityEnabled = true
        }
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false

        // Refresh the chart
        lineChart.invalidate()
    }

    private fun plotCurrentMonthWeeklyChart(quizResults: Map<Long, Pair<Int, Int>>, lineChart: LineChart) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1) // First day of the current month
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)

        val correctEntriesMap = mutableMapOf<Int, Entry>()
        val incorrectEntriesMap = mutableMapOf<Int, Entry>()

        // Initialize entries for each week of the month
        for (week in 0 until 4) {
            correctEntriesMap[week] = Entry(week.toFloat(), 0f)
            incorrectEntriesMap[week] = Entry(week.toFloat(), 0f)
        }

        // Process quiz results
        quizResults.forEach { (timestamp, counts) ->
            calendar.timeInMillis = timestamp
            val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH) - 1 // 0-based index

            if (weekOfMonth in 0..3) {
                correctEntriesMap[weekOfMonth]?.let {
                    it.y += counts.first.toFloat()
                }
                incorrectEntriesMap[weekOfMonth]?.let {
                    it.y += counts.second.toFloat()
                }
            }
        }

        // Create data sets for the chart
        val correctLineDataSet = LineDataSet(correctEntriesMap.values.toList(), "Correct Answers").apply {
            color = Color.GREEN
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        val incorrectLineDataSet = LineDataSet(incorrectEntriesMap.values.toList(), "Incorrect Answers").apply {
            color = Color.RED
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        val lineData = LineData(correctLineDataSet, incorrectLineDataSet)
        lineChart.data = lineData

        // Configure the x-axis
        lineChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "Week ${(value.toInt() + 1)}"
                }
            }
            setLabelCount(4, true)
        }

        // Configure the rest of the chart
        lineChart.axisLeft.apply {
            axisMinimum = 0f
            granularity = 1f
            isGranularityEnabled = true
        }
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false

        // Refresh the chart
        lineChart.invalidate()
    }

    private fun plotMonthlyLineChart(quizResults: Map<Long, Pair<Int, Int>>, lineChart: LineChart) {
        val calendar = Calendar.getInstance()
        // Set to the first day of six months ago
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)

        // Initialize maps for correct and incorrect answers
        val correctEntriesMap = mutableMapOf<Long, Entry>()
        val incorrectEntriesMap = mutableMapOf<Long, Entry>()

        // Iterate over the months, from six months ago to the current month
        for (i in 0 until 6) {
            val monthMillis = calendar.timeInMillis
            correctEntriesMap[monthMillis] = Entry(i.toFloat(), 0f) // X value is the index for simplicity
            incorrectEntriesMap[monthMillis] = Entry(i.toFloat(), 0f)
            calendar.add(Calendar.MONTH, 1)
        }

        // Populate the entries with actual quiz results
        quizResults.forEach { (timestamp, counts) ->
            // Round the timestamp down to the first of the month
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.clear(Calendar.MINUTE)
            calendar.clear(Calendar.SECOND)
            calendar.clear(Calendar.MILLISECOND)
            val roundedMonthMillis = calendar.timeInMillis

            val index = ((roundedMonthMillis - correctEntriesMap.keys.first()) / (1000L * 60 * 60 * 24 * 30)).toFloat()
            correctEntriesMap[roundedMonthMillis]?.apply { y = counts.first.toFloat() }
            incorrectEntriesMap[roundedMonthMillis]?.apply { y = counts.second.toFloat() }
        }

        // Create datasets and set them to the chart
        val correctLineDataSet = LineDataSet(correctEntriesMap.values.toList().sortedBy { it.x }, "Correct Answers").apply {
            color = Color.GREEN
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            // Set the value formatter for the data set to ensure whole numbers
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }
        val incorrectLineDataSet = LineDataSet(incorrectEntriesMap.values.toList().sortedBy { it.x }, "Incorrect Answers").apply {
            color = Color.RED
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            // Set the value formatter for the data set to ensure whole numbers
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }
        val lineData = LineData(correctLineDataSet, incorrectLineDataSet)
        lineChart.data = lineData

        // Configure the x-axis with labels for each month
        lineChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.MONTH, -6)
                    calendar.add(Calendar.MONTH, value.toInt()) // Add the number of months corresponding to the entry's x value
                    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    return dateFormat.format(calendar.time)
                }
            }
            position = XAxis.XAxisPosition.BOTTOM
            setLabelCount(6, true)
        }

        // Rest of the chart configuration...
        lineChart.axisLeft.apply {
            axisMinimum = 0f
            granularity = 1f
            isGranularityEnabled = true
        }
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false

        // Refresh the chart
        lineChart.invalidate()
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