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

/**
 * A fragment which displays all statistics relevant to the user, in the form of graphs.
 * Graphs are implemented via MPAndroidChart.
 */
class StatisticsFragmentActivity : Fragment() {

    // Gets authentication and database.
    private var mAuth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Late initialises all the statistics aspects of the fragment.
    private lateinit var moodChart: BarChart
    private lateinit var longTermLineChart: LineChart
    private lateinit var shortTermLineChart: LineChart
    private lateinit var quizCount: TextView
    private var count: Int = 0

    // Creates maps for the results.
    private val longTermQuizResults = mutableMapOf<Long, Pair<Int, Int>>()
    private val shortTermQuizResults = mutableMapOf<Long, Pair<Int, Int>>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.statistics_fragment, container, false).apply {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gets graphs from layout.
        longTermLineChart = view.findViewById(R.id.long_term_line_chart)
        shortTermLineChart = view.findViewById(R.id.short_term_line_chart)

        // Sets up spinner for long term graph.
        val longTermlineChartSpinner = view.findViewById<Spinner>(R.id.long_term_line_chart_spinner)
        val longTermAdapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.time_options,
            R.layout.spinner_item
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
                // Changes what data is loaded via the spinner.
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

        // Sets up spinner for short term graph.
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
                // Changes what data is loaded via the spinner.
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

        // Loads up bar chart and quiz count, before fetching data for them.
        moodChart = view.findViewById(R.id.mood_bar_chart)
        quizCount = view.findViewById(R.id.total_quiz_text)
        getFirebaseDataForMood()
    }

    /**
     * A method to map the mood bar chart.
     */
    private fun getFirebaseDataForMood(){

        // Creates a mutable map to keep count.
        val moodCounts = mutableMapOf(
            "Happy" to 0,
            "Sad" to 0,
            "Angry" to 0,
            "Confused" to 0,
            "Calm" to 0
        )

        // Searches through users quizzes.
        mAuth.currentUser?.uid?.let { currentUserUID ->
            db.collection("quizzes")
                .whereEqualTo("uid", currentUserUID)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        // Increment count for each quiz found, as well as moods.
                        count++
                        val mood = document.getString("mood") ?: "Unknown"
                        moodCounts[mood] = moodCounts.getOrDefault(mood, 0) + 1
                    }
                    // Display the data found.
                    displayBarChart(moodCounts, count)
                }
                .addOnFailureListener { exception ->
                }
        }
    }


    /**
     * A method to display the bar chart to the screen.
     *
     * @param moodCounts a map with the counts of the mood.
     * @param quizTotalCount total amount of quizzes.
     */
    private fun displayBarChart(moodCounts: Map<String, Int>, quizTotalCount: Int) {
        // Gets entries and sets them as the mood count map.
        val entries = moodCounts.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        // Gives bars different colours.
        val barColors = listOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#F44336"),
            Color.parseColor("#FFEB3B"),
            Color.parseColor("#9C27B0")
        )

        // Uses the entries to create a dataset.
        val dataSet = BarDataSet(entries, "Mood Counts").apply{
            setColors(barColors)
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry?): String {
                    // Sets y axis as an integer
                    return barEntry?.y?.toInt().toString()
                }
            }
        }
        val barData = BarData(dataSet)

        // Gives graph the data.
        moodChart.data = barData

        // Formats chart.
        moodChart.apply{
            setExtraOffsets(10f, 0f, 10f, 63f)
        }

        // Formats y axis.
        moodChart.axisLeft.apply {
            setDrawGridLines(true)
            granularity = 1f
            isGranularityEnabled = true
            axisMinimum = 0f
        }
        moodChart.axisRight.isEnabled = false

        // Formats x axis, and makes each bar display mood name underneath.
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

        // Make the bar chart fit and format nicely.
        moodChart.setFitBars(true)
        moodChart.setTouchEnabled(true)
        moodChart.isDragEnabled = true
        moodChart.setScaleEnabled(true)
        moodChart.setPinchZoom(false)

        moodChart.description.isEnabled = false
        moodChart.legend.isEnabled = false

        // Refresh chart and display.
        moodChart.invalidate()
        quizCount.text = quizTotalCount.toString()
    }

    /**
     * A method to get data for long term questions.
     *
     * @param plotFunction parses through which plot method to use.
     */
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

                    // Looks through all quizzes and keeps track of how many carried out.
                    val quizCount = quizDocuments.size()
                    var processedQuizzes = 0

                    for (quiz in quizDocuments) {
                        val quizId = quiz.id
                        val dateString = quiz.getString("date")
                        val quizDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateString)
                        val quizTimestamp = quizDate?.time ?: continue

                        // Uses quiz details to look through questions collection for long term
                        // questions.
                        db.collection("questions")
                            .whereEqualTo("quizId", quizId)
                            .whereEqualTo("questionType", "LONG_TERM")
                            .get()
                            .addOnSuccessListener { questionDocuments ->
                                var correctLongTermCount = 0
                                var incorrectLongTermCount = 0

                                //Increment counts based on whats found.
                                for (question in questionDocuments) {
                                    if (question.getBoolean("correct") == true) {
                                        correctLongTermCount++
                                    } else {
                                        incorrectLongTermCount++
                                    }
                                }

                                // Pair the correct and incorrect totals together for the same quiz
                                // and move on to next quiz.
                                longTermQuizResults[quizTimestamp] = Pair(correctLongTermCount, incorrectLongTermCount)
                                processedQuizzes++

                                // Check if all quizzes have been processed and plot afterwards.
                                if (processedQuizzes == quizCount) {
                                    Log.d("Quiz Results", "$longTermQuizResults")
                                    plotFunction(longTermQuizResults)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase Error", "Error getting questions", exception)
                                processedQuizzes++

                                // Still check if we need to plot due to other successes.
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

    /**
     * A method to get data for short term questions.
     *
     * @param plotFunction parses through which plot method to use.
     */
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

                    // Looks through all quizzes and keeps track of how many carried out.
                    val quizCount = quizDocuments.size()
                    var processedQuizzes = 0

                    for (quiz in quizDocuments) {
                        val quizId = quiz.id
                        val dateString = quiz.getString("date")
                        val quizDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateString)
                        val quizTimestamp = quizDate?.time ?: continue

                        // Uses quiz details to look through questions collection for short term
                        // questions.
                        db.collection("questions")
                            .whereEqualTo("quizId", quizId)
                            .whereEqualTo("questionType", "SHORT_TERM")
                            .get()
                            .addOnSuccessListener { questionDocuments ->
                                var correctLongTermCount = 0
                                var incorrectLongTermCount = 0

                                //Increment counts based on whats found.
                                for (question in questionDocuments) {
                                    if (question.getBoolean("correct") == true) {
                                        correctLongTermCount++
                                    } else {
                                        incorrectLongTermCount++
                                    }
                                }

                                // Pair the correct and incorrect totals together for the same quiz
                                // and move on to next quiz.
                                shortTermQuizResults[quizTimestamp] = Pair(correctLongTermCount, incorrectLongTermCount)
                                processedQuizzes++

                                // Check if all quizzes have been processed and plot afterwards.
                                if (processedQuizzes == quizCount) {
                                    Log.d("Quiz Results", "$shortTermQuizResults")
                                    plotFunction(shortTermQuizResults)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase Error", "Error getting questions", exception)
                                processedQuizzes++

                                // Still check if we need to plot due to other successes.
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

    /**
     * A method to plot a weekly line chart of the quiz details.
     *
     * @param quizResults A map with the timestamp and a pair representing answers right/wrong.
     * @param lineChart The line chart to update.
     */
    private fun plotWeeklyLineChart(quizResults: Map<Long, Pair<Int,Int>>, lineChart: LineChart) {
        val calendar = Calendar.getInstance()

        // Gets the start of the week.
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)
        val startOfWeekMillis = calendar.timeInMillis

        // Gets the end of the week.
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfWeekMillis = calendar.timeInMillis

        // Prepare the entries using a map to store counts for each day.
        val correctEntriesMap = mutableMapOf<Long, Entry>()
        val incorrectEntriesMap = mutableMapOf<Long, Entry>()
        val entriesMap = mutableMapOf<Long, Entry>()

        // Initialize the entries map with zeros for each day of the current week.
        for (i in 0 until 7) {
            val dayMillis = startOfWeekMillis + TimeUnit.DAYS.toMillis(i.toLong())
            // Set to 0 so if no quiz results found for day, data still displays.
            correctEntriesMap[dayMillis] = Entry(dayMillis.toFloat(), 0f)
            incorrectEntriesMap[dayMillis] = Entry(dayMillis.toFloat(), 0f)
        }

        // Filter the results to include only quizzes within the current week.
        val currentWeekQuizResults = quizResults.filterKeys {
            it in startOfWeekMillis..endOfWeekMillis
        }

        // Create entries map with actual quiz results, rounded to the nearest day.
        currentWeekQuizResults.forEach { (timestamp, counts) ->

            // Gets the day and rounds here.
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.clear(Calendar.MINUTE)
            calendar.clear(Calendar.SECOND)
            calendar.clear(Calendar.MILLISECOND)
            val roundedDayMillis = calendar.timeInMillis

            // Creates two points for correct and incorrect entry
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

        // Creates a line data set for correct answers.
        val correctLineDataSet = LineDataSet(correctEntriesMap.values.toList().sortedBy { it.x }, "Correct Answers").apply {
            // Green to differentiate the lines.
            color = Color.GREEN
            valueTextColor = Color.BLACK
            valueTextSize = 12f

            // Set the value formatter for the data set to ensure whole numbers.
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        // Creates a line data set for incorrect answers.
        val incorrectLineDataSet = LineDataSet(incorrectEntriesMap.values.toList().sortedBy { it.x }, "Incorrect Answers").apply {
            // Redto differentiate the lines.
            color = Color.RED
            valueTextColor = Color.BLACK
            valueTextSize = 12f

            // Set the value formatter for the data set to ensure whole numbers.
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        // Plot both lines on the chart.
        val lineData = LineData(correctLineDataSet, incorrectLineDataSet)
        lineChart.data = lineData

        // St up the x-axis, using dates as each point.
        lineChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                private val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
                override fun getFormattedValue(value: Float): String {
                    return dateFormat.format(Date(value.toLong()))
                }
            }

            // Set intervals to one day and have 7 labels for each day.
            granularity = TimeUnit.DAYS.toMillis(1).toFloat()
            setLabelCount(7, true)
        }

        // Set up the y-axis.
        lineChart.axisLeft.apply {
            // Start it at 0, incrementing in 1.
            axisMinimum = 0f
            granularity = 1f
            isGranularityEnabled = true
        }
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false

        // Refresh the chart
        lineChart.invalidate()
    }

    /**
     * A method to plot a monthly line chart of the quiz details.
     *
     * @param quizResults A map with the timestamp and a pair representing answers right/wrong.
     * @param lineChart The line chart to update.
     */
    private fun plotCurrentMonthWeeklyChart(quizResults: Map<Long, Pair<Int, Int>>, lineChart: LineChart) {
        val calendar = Calendar.getInstance()

        // Get the current month were in from the start.
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)

        // Create maps for correct and incorrect entries.
        val correctEntriesMap = mutableMapOf<Int, Entry>()
        val incorrectEntriesMap = mutableMapOf<Int, Entry>()

        // Initialize entries for each week of the month, so 4.
        for (week in 0 until 4) {
            correctEntriesMap[week] = Entry(week.toFloat(), 0f)
            incorrectEntriesMap[week] = Entry(week.toFloat(), 0f)
        }

        // Go through each result.
        quizResults.forEach { (timestamp, counts) ->
            calendar.timeInMillis = timestamp

            // Index starts from 0, so minus one from week its in.
            val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH) - 1

            // Loop through each value, and add to the counts for each week.
            if (weekOfMonth in 0..3) {
                correctEntriesMap[weekOfMonth]?.let {
                    it.y += counts.first.toFloat()
                }
                incorrectEntriesMap[weekOfMonth]?.let {
                    it.y += counts.second.toFloat()
                }
            }
        }

        // Creates a line data set for correct answers.
        val correctLineDataSet = LineDataSet(correctEntriesMap.values.toList(), "Correct Answers").apply {
            //Green to differentiate.
            color = Color.GREEN
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        // Creates a line data set for incorrect answers.
        val incorrectLineDataSet = LineDataSet(incorrectEntriesMap.values.toList(), "Incorrect Answers").apply {
            //Red to differentiate.
            color = Color.RED
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        // Plots the line chart.
        val lineData = LineData(correctLineDataSet, incorrectLineDataSet)
        lineChart.data = lineData

        // Sets up the x-axis, so it shows which week its on.
        lineChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "Week ${(value.toInt() + 1)}" // Need to add one due to 0 index.
                }
            }
            setLabelCount(4, true)
        }

        // Set up left axis, and increment it in 1's.
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


    /**
     * A method to plot a 6 month period line chart of the quiz details.
     *
     * @param quizResults A map with the timestamp and a pair representing answers right/wrong.
     * @param lineChart The line chart to update.
     */
    private fun plotMonthlyLineChart(quizResults: Map<Long, Pair<Int, Int>>, lineChart: LineChart) {
        val calendar = Calendar.getInstance()

        // Set to the first day of six months ago.
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)

        // Create maps for correct and incorrect entries.
        val correctEntriesMap = mutableMapOf<Long, Entry>()
        val incorrectEntriesMap = mutableMapOf<Long, Entry>()

        // Loop over each month, from six months ago to the current month
        for (i in 0 until 6) {
            val monthMillis = calendar.timeInMillis
            correctEntriesMap[monthMillis] = Entry(i.toFloat(), 0f)
            incorrectEntriesMap[monthMillis] = Entry(i.toFloat(), 0f)
            calendar.add(Calendar.MONTH, 1)
        }

        // Fill entries with actual quiz results
        quizResults.forEach { (timestamp, counts) ->

            // Round the timestamp down to the first of each month
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

        // Creates a line data set for correct answers.
        val correctLineDataSet = LineDataSet(correctEntriesMap.values.toList().sortedBy { it.x }, "Correct Answers").apply {
            color = Color.GREEN
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            // Set the value formatter for the data set to ensure whole numbers.
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }


        // Creates a line data set for incorrect answers.
        val incorrectLineDataSet = LineDataSet(incorrectEntriesMap.values.toList().sortedBy { it.x }, "Incorrect Answers").apply {
            color = Color.RED
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            // Set the value formatter for the data set to ensure whole numbers.
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        // Plots both lines on the line chart.
        val lineData = LineData(correctLineDataSet, incorrectLineDataSet)
        lineChart.data = lineData

        // Configure the x-axis with labels for each month
        lineChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val calendar = Calendar.getInstance()

                    // Go back six months and add each month.
                    calendar.add(Calendar.MONTH, -6)
                    calendar.add(Calendar.MONTH, value.toInt())

                    // Format so it shows month and year.
                    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    return dateFormat.format(calendar.time)
                }
            }
            position = XAxis.XAxisPosition.BOTTOM
            setLabelCount(6, true)
        }

        // Set up y axis on line chart.
        lineChart.axisLeft.apply {
            // Increment in 1's from 0
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