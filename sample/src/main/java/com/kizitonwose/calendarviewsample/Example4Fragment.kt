package com.kizitonwose.calendarviewsample


import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import com.google.android.material.snackbar.Snackbar
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import kotlinx.android.synthetic.main.calendar_day_legend.*
import kotlinx.android.synthetic.main.example_4_calendar_day.view.*
import kotlinx.android.synthetic.main.example_4_calendar_header.view.*
import kotlinx.android.synthetic.main.example_4_fragment.*
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import java.util.*

class Example4Fragment : BaseFragment(), HasToolbar, HasBackButton {

    override val toolbar: Toolbar?
        get() = exFourToolbar

    override val titleRes: Int? = null

    private val today = LocalDate.now()

    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.example_4_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        // Set the First day of week depending on Locale
        val daysOfWeek = daysOfWeekFromLocale()
        legendLayout.children.forEachIndexed { index, view ->
            (view as TextView).apply {
                text = daysOfWeek[index].getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColorRes(R.color.example_4_grey)
            }
        }

        val currentMonth = YearMonth.now()
        exFourCalendar.setup(currentMonth, currentMonth.plusMonths(12), daysOfWeek.first())
        exFourCalendar.scrollToMonth(currentMonth)

        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay // Will be set when this container is bound.
            val textView = view.exFourDayText
            val roundBgView = view.exFourRoundBgView
            val selectedMiddleBackgroundLeft = view.selected_middle_background_left
            val selectedMiddleBackgroundRight = view.selected_middle_background_right
            val selectedStartBackground = view.selected_start_background
            val selectedEndBackground = view.selected_end_background
            val midleCornerStart = view.midle_corner_start
            val midleCornerEnd = view.midle_corner_end

            init {
                view.setOnClickListener {
                    if (day.owner == DayOwner.THIS_MONTH && (day.date == today || day.date.isAfter(today))) {
                        val date = day.date
                        if (startDate != null) {
                            if (date < startDate || endDate != null) {
                                startDate = date
                                endDate = null
                            } else if (date != startDate) {
                                endDate = date
                            }
                        } else {
                            startDate = date
                        }
                        exFourCalendar.notifyCalendarChanged()
                    }
                }
            }
        }
        exFourCalendar.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                val textView = container.textView
                val roundBgView = container.roundBgView
                val selectedMiddleBackgroundLeft = container.selectedMiddleBackgroundLeft
                val selectedMiddleBackgroundRight = container.selectedMiddleBackgroundRight
                var selectedStartBackground = container.selectedStartBackground
                var selectedEndBackground = container.selectedEndBackground
                var midleCornerStart = container.midleCornerStart
                var midleCornerEnd = container.midleCornerEnd

                textView.text = null
                textView.background = null
                roundBgView.makeInVisible()
                selectedStartBackground.makeInVisible()
                selectedEndBackground.makeInVisible()
                selectedMiddleBackgroundLeft.makeInVisible()
                selectedMiddleBackgroundRight.makeInVisible()
                midleCornerStart.makeInVisible()
                midleCornerEnd.makeInVisible()

                if (day.owner == DayOwner.THIS_MONTH) {
                    textView.text = day.day.toString()

                    if (day.date.isBefore(today)) {
                        textView.setTextColorRes(R.color.example_4_grey_past)
                    } else {
                        when {
                            startDate == day.date && endDate == null -> {
                                textView.setTextColorRes(R.color.black)
                                roundBgView.makeVisible()
                                selectedMiddleBackgroundLeft.makeInVisible()
                                selectedMiddleBackgroundRight.makeInVisible()
                                selectedStartBackground.makeInVisible()
                                selectedEndBackground.makeInVisible()
                                roundBgView.setBackgroundResource(R.drawable.example_4_single_selected_bg)
                            }
                            day.date == startDate -> {
                                textView.setTextColorRes(R.color.black)
                                selectedStartBackground.makeVisible()
                                selectedMiddleBackgroundRight.makeVisible()
                            }
                            startDate != null && endDate != null && (day.date > startDate && day.date < endDate) -> {
                                textView.setTextColorRes(R.color.white)
                                selectedMiddleBackgroundLeft.makeVisible()
                                selectedMiddleBackgroundRight.makeVisible()

                                if (day.date.dayOfWeek == DayOfWeek.SATURDAY) {
                                    midleCornerEnd.makeVisible()
                                    selectedMiddleBackgroundRight.makeInVisible()
                                }
                                if (day.date.dayOfWeek == DayOfWeek.SUNDAY) {
                                    midleCornerStart.makeVisible()
                                    selectedMiddleBackgroundLeft.makeInVisible()
                                }
                            }
                            day.date == endDate -> {
                                textView.setTextColorRes(R.color.black)
                                selectedMiddleBackgroundLeft.makeVisible()
                                selectedEndBackground.makeVisible()
                            }
                            else -> textView.setTextColorRes(R.color.example_4_grey)
                        }
                    }
                } else {

                    // This part is to make the coloured selection background continuous
                    // on the blank in and out dates across various months and also on dates(months)
                    // between the start and end dates if the selection spans across multiple months.

                    val startDate = startDate
                    val endDate = endDate
                    if (startDate != null && endDate != null) {
                        // Mimic selection of inDates that are less than the startDate.
                        // Example: When 26 Feb 2019 is startDate and 5 Mar 2019 is endDate,
                        // this makes the inDates in Mar 2019 for 24 & 25 Feb 2019 look selected.
                        if ((day.owner == DayOwner.PREVIOUS_MONTH
                                    && startDate.monthValue == day.date.monthValue
                                    && endDate.monthValue != day.date.monthValue) ||
                            // Mimic selection of outDates that are greater than the endDate.
                            // Example: When 25 Apr 2019 is startDate and 2 May 2019 is endDate,
                            // this makes the outDates in Apr 2019 for 3 & 4 May 2019 look selected.
                            (day.owner == DayOwner.NEXT_MONTH
                                    && startDate.monthValue != day.date.monthValue
                                    && endDate.monthValue == day.date.monthValue) ||

                            // Mimic selection of in and out dates of intermediate
                            // months if the selection spans across multiple months.
                            (startDate < day.date && endDate > day.date
                                    && startDate.monthValue != day.date.monthValue
                                    && endDate.monthValue != day.date.monthValue)
                        ) {
                            selectedMiddleBackgroundLeft.makeVisible()
                            selectedMiddleBackgroundRight.makeVisible()
                        }
                    }
                }
            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view) {
            val textView = view.exFourHeaderText
        }
        exFourCalendar.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                val monthTitle = "${month.yearMonth.month.name.toLowerCase().capitalize()} ${month.year}"
                container.textView.text = monthTitle
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.example_4_menu, menu)
        exFourToolbar.post {
            // Configure menu text to match what is in the Airbnb app.
            exFourToolbar.findViewById<TextView>(R.id.menuItemClear).apply {
                setTextColor(requireContext().getColorCompat(R.color.example_4_grey))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                isAllCaps = false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuItemClear) {
            startDate = null
            endDate = null
            exFourCalendar.notifyCalendarChanged()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        val closeIndicator = requireContext().getDrawableCompat(R.drawable.ic_close)?.apply {
            setColorFilter(requireContext().getColorCompat(R.color.example_4_grey), PorterDuff.Mode.SRC_ATOP)
        }
        (activity as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(closeIndicator)
        requireActivity().window.apply {
            // Update statusbar color to match toolbar color.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                statusBarColor = requireContext().getColorCompat(R.color.white)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                statusBarColor = Color.GRAY
            }
        }
    }

    override fun onStop() {
        super.onStop()
        requireActivity().window.apply {
            // Reset statusbar color.
            statusBarColor = requireContext().getColorCompat(R.color.colorPrimaryDark)
            decorView.systemUiVisibility = 0
        }
    }
}
