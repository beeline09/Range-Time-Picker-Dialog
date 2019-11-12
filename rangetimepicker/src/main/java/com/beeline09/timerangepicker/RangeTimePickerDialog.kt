package com.beeline09.timerangepicker

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TimePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout
import java.lang.reflect.Field
import java.util.*

class RangeTimePickerDialog : DialogFragment() {
    private var mAlertDialog: AlertDialog? = null
    private var dialogDismissed = false
    private var tabLayout: TabLayout? = null
    private var tabItemStartTime: TabItem? = null
    private var tabItemEndTime: TabItem? = null
    private var timePickerStart: TimePicker? = null
    private var timePickerEnd: TimePicker? = null
    private var btnPositive: MaterialButton? = null
    private var btnNegative: MaterialButton? = null
    private var startTabIcon = R.drawable.ic_start_time_black_24dp
    private var endTabIcon = R.drawable.ic_end_time_black_24dp
    private var colorTabUnselected = R.color.White
    private var colorTabSelected = R.color.Yellow
    private var colorTextButton = R.color.Yellow
    private var colorBackgroundHeader = R.color.CyanWater
    private var colorBackgroundTimePickerHeader = R.color.CyanWater
    private var is24HourView = true
    private var messageErrorRangeTime = "Error: set a end time greater than start time"
    private var textBtnPositive = "Ok"
    private var textBtnNegative = "Cancel"
    private var textTabStart = "Start time"
    private var textTabEnd = "End time"
    private var radiusDialog = 50 // Default 50
    private var validateRange = true
    private var isMinutesEnabled = true
    private val currentTime = Calendar.getInstance()
    private var initialStarHour = currentTime.get(Calendar.HOUR_OF_DAY)
    private var initialStartMinute = currentTime.get(Calendar.MINUTE)
    private var initialEndHour = currentTime.get(Calendar.HOUR_OF_DAY)
    private var initialEndMinute = currentTime.get(Calendar.MINUTE)
    private var initialOpenedTab = InitialOpenedTab.START_CLOCK_TAB
    private var inputKeyboardAsDefault = false
    private var range = 0
    //minimum time difference b\w start and end time
    fun setMinimumSelectedTimeInMinutes(range: Int, validateRange: Boolean) {
        this.range = range
        this.validateRange = validateRange
    }

    enum class InitialOpenedTab {
        START_CLOCK_TAB, END_CLOCK_TAB
    }

    interface ISelectedTime {
        fun onSelectedTime(hourStart: Int, minuteStart: Int, hourEnd: Int, minuteEnd: Int)
    }

    fun newInstance(): RangeTimePickerDialog {
        return RangeTimePickerDialog()
    }

    private var mCallback: ISelectedTime? = null
    /**
     * Create a new instance with own attributes (All color MUST BE in this format "R.color.my_color")
     *
     * @param colorBackgroundHeader Color of Background header dialog and timePicker
     * @param colorTabUnselected    Color of tab when unselected
     * @param colorTabSelected      Color of tab when selected
     * @param colorTextButton       Text color of button
     * @param is24HourView          Indicates if the format should be 24 hours
     */
    fun newInstance(colorBackgroundHeader: Int, colorTabUnselected: Int, colorTabSelected: Int, colorTextButton: Int, is24HourView: Boolean): RangeTimePickerDialog {
        val f = RangeTimePickerDialog()
        this.colorTabUnselected = colorTabUnselected
        this.colorBackgroundHeader = colorBackgroundHeader
        colorBackgroundTimePickerHeader = colorBackgroundHeader
        this.colorTabSelected = colorTabSelected
        this.colorTextButton = colorTextButton
        this.is24HourView = is24HourView
        return f
    }


    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog { // Use the Builder class for convenient dialog construction
        val builder = MaterialAlertDialogBuilder(activity!!)
        // Get the layout inflater
        val inflater = activity!!.layoutInflater
        // Inflate and set the layout for the dialog
// Pass null as the parent view because its going in the dialog layout
        @SuppressLint("InflateParams") val dialogView = inflater.inflate(R.layout.layout_custom_dialog, null)
        builder.setView(dialogView)
        tabLayout = dialogView.findViewById(R.id.tabLayout)
        tabItemStartTime = dialogView.findViewById(R.id.tabStartTime)
        tabItemEndTime = dialogView.findViewById(R.id.tabEndTime)
        timePickerStart = dialogView.findViewById(R.id.timePickerStart)
        timePickerEnd = dialogView.findViewById(R.id.timePickerEnd)
        btnPositive = dialogView.findViewById(R.id.btnPositiveDialog)
        btnNegative = dialogView.findViewById(R.id.btnNegativeDialog)
        val cardView: MaterialCardView = dialogView.findViewById(R.id.ly_root)
        // Set TimePicker header background color
        setTimePickerHeaderBackgroundColor(this, ResourcesCompat.getColor(activity!!.resources, colorBackgroundTimePickerHeader, null), "timePickerStart")
        setTimePickerHeaderBackgroundColor(this, ResourcesCompat.getColor(activity!!.resources, colorBackgroundTimePickerHeader, null), "timePickerEnd")
        // Set radius of dialog
        cardView.radius = radiusDialog.toFloat()
        setColorTabLayout(colorTabSelected, colorTabUnselected, colorBackgroundHeader)
        timePickerStart?.setIs24HourView(is24HourView)
        timePickerEnd?.setIs24HourView(is24HourView)
        // Set initial clock values
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePickerStart?.hour = initialStarHour
            timePickerStart?.minute = initialStartMinute
        } else {
            timePickerStart?.currentHour = initialStarHour
            timePickerStart?.currentMinute = initialStartMinute
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePickerEnd?.hour = initialEndHour
            timePickerEnd?.minute = initialEndMinute
        } else {
            timePickerEnd?.currentHour = initialEndHour
            timePickerEnd?.currentMinute = initialEndMinute
        }
        // Set icon tabs
        tabLayout?.getTabAt(0)?.setIcon(startTabIcon)
        tabLayout?.getTabAt(1)?.setIcon(endTabIcon)
        // Set initial opened tab
        if (initialOpenedTab == InitialOpenedTab.START_CLOCK_TAB) {
            tabLayout?.getTabAt(0)?.select()
            val tabIconColor = ContextCompat.getColor(activity!!, colorTabSelected)
            val tabIconColorUnselect = ContextCompat.getColor(activity!!, colorTabUnselected)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tabLayout?.getTabAt(0)?.icon?.colorFilter = BlendModeColorFilter(tabIconColor, BlendMode.SRC_IN)
                tabLayout?.getTabAt(1)?.icon?.colorFilter = BlendModeColorFilter(tabIconColorUnselect, BlendMode.SRC_IN)
            } else {
                tabLayout?.getTabAt(0)?.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
                tabLayout?.getTabAt(1)?.icon?.setColorFilter(tabIconColorUnselect, PorterDuff.Mode.SRC_IN)
            }


            timePickerStart?.visibility = View.VISIBLE
            timePickerEnd?.visibility = View.GONE
        } else if (initialOpenedTab == InitialOpenedTab.END_CLOCK_TAB) {
            tabLayout?.getTabAt(1)?.select()
            val tabIconColor = ContextCompat.getColor(activity!!, colorTabSelected)
            val tabIconColorUnselect = ContextCompat.getColor(activity!!, colorTabUnselected)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tabLayout?.getTabAt(1)?.icon?.colorFilter = BlendModeColorFilter(tabIconColor, BlendMode.SRC_ATOP)
                tabLayout?.getTabAt(0)?.icon?.colorFilter = BlendModeColorFilter(tabIconColorUnselect, BlendMode.SRC_ATOP)
            } else {
                tabLayout?.getTabAt(1)?.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
                tabLayout?.getTabAt(0)?.icon?.setColorFilter(tabIconColorUnselect, PorterDuff.Mode.SRC_IN)
            }


            timePickerEnd?.visibility = View.VISIBLE
            timePickerStart?.visibility = View.GONE
        }
        btnPositive?.setTextColor(ContextCompat.getColor(activity!!, colorTextButton))
        btnNegative?.setTextColor(ContextCompat.getColor(activity!!, colorTextButton))
        btnPositive?.text = textBtnPositive
        btnNegative?.text = textBtnNegative
        tabLayout?.getTabAt(0)?.text = textTabStart
        tabLayout?.getTabAt(1)?.text = textTabEnd
        // Set keyboard input as default
        if (inputKeyboardAsDefault) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setInputKeyboardAsDefault("timePickerStart")
                setInputKeyboardAsDefault("timePickerEnd")
            }
        }
        // Enable/Disable minutes
        if (!isMinutesEnabled) {
            setMinutesEnabled(this, isMinutesEnabled, "timePickerStart")
            setMinutesEnabled(this, isMinutesEnabled, "timePickerEnd")
        }
        // Create the AlertDialog object and return it
        mAlertDialog = builder.create()
        mAlertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mAlertDialog?.setOnShowListener {
            tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    val tabIconColor = ContextCompat.getColor(activity!!, colorTabSelected)


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tab.icon?.colorFilter = BlendModeColorFilter(tabIconColor, BlendMode.SRC_ATOP)
                    } else {
                        tab.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
                    }

                    //tab.getIcon().setTint(Color.YELLOW);
                    if (tab.position == 0) {
                        timePickerStart?.visibility = View.VISIBLE
                        timePickerEnd?.visibility = View.GONE
                    } else {
                        timePickerStart?.visibility = View.GONE
                        timePickerEnd?.visibility = View.VISIBLE
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    val tabIconColor = ContextCompat.getColor(activity!!, colorTabUnselected)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tab.icon?.colorFilter = BlendModeColorFilter(tabIconColor, BlendMode.SRC_ATOP)
                    } else {
                        tab.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
            btnNegative?.setOnClickListener { dismiss() }
            btnPositive?.setOnClickListener {
                val flagCorrect: Boolean
                val hourStart: Int
                val minuteStart: Int
                val hourEnd: Int
                val minuteEnd: Int
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    hourStart = timePickerStart?.hour ?: 0
                    minuteStart = timePickerStart?.minute ?: 0
                    hourEnd = timePickerEnd?.hour ?: 0
                    minuteEnd = timePickerEnd?.minute ?: 0
                } else {
                    hourStart = timePickerStart?.currentHour ?: 0
                    minuteStart = timePickerStart?.currentMinute ?: 0
                    hourEnd = timePickerEnd?.currentHour ?: 0
                    minuteEnd = timePickerEnd?.currentMinute ?: 0
                }
                flagCorrect = if (validateRange) {
                    if (hourEnd > hourStart) {
                        true
                    } else hourEnd == hourStart && minuteEnd > minuteStart
                } else {
                    true
                }
                if (flagCorrect) { /*check if range is not 0 , for time validate range must be true*/
                    if (range == 0 || !validateRange) { // Check if this dialog was called by a fragment
                        if (targetFragment != null) { // Return value to Fragment
                            val bundle = Bundle()
                            bundle.putInt(HOUR_START, hourStart)
                            bundle.putInt(MINUTE_START, minuteStart)
                            bundle.putInt(HOUR_END, hourEnd)
                            bundle.putInt(MINUTE_END, minuteEnd)
                            val intent = Intent().putExtras(bundle)
                            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                        } else { // Return value to activity
                            mCallback?.onSelectedTime(hourStart, minuteStart, hourEnd, minuteEnd)
                        }
                        dismiss()
                    } else {
                        if (checkdiffernce(hourStart, minuteStart, hourEnd, minuteEnd) >= range) { // Check if this dialog was called by a fragment
                            if (targetFragment != null) { // Return value to Fragment
                                val bundle = Bundle()
                                bundle.putInt(HOUR_START, hourStart)
                                bundle.putInt(MINUTE_START, minuteStart)
                                bundle.putInt(HOUR_END, hourEnd)
                                bundle.putInt(MINUTE_END, minuteEnd)
                                val intent = Intent().putExtras(bundle)
                                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                            } else { // Return value to activity
                                mCallback?.onSelectedTime(hourStart, minuteStart, hourEnd, minuteEnd)
                            }
                            dismiss()
                        } else {
                            Toast.makeText(activity,
                                    "Error: Selected time cannot be less than $range minutes.",
                                    Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(activity, messageErrorRangeTime, Toast.LENGTH_SHORT).show()
                }
            }
        }
        return mAlertDialog!!
    }

    /*instantiate the Calander instances and sending it to
     * function which returns difference in minutes
     */
    private fun checkdiffernce(hourStart: Int, minuteStart: Int, hourEnd: Int, minuteEnd: Int): Long { /*Preparing instance for Start time to get difference of time*/
        val calStart = Calendar.getInstance()
        calStart.time = Date()
        calStart[Calendar.HOUR_OF_DAY] = hourStart
        calStart[Calendar.MINUTE] = minuteStart
        calStart[Calendar.SECOND] = 0
        calStart[Calendar.MILLISECOND] = 0
        val dateStart = Date(calStart.timeInMillis)
        /*Preparing instance for End time to get difference of time*/
        val calEnd = Calendar.getInstance()
        calEnd.time = Date()
        calEnd[Calendar.HOUR_OF_DAY] = hourEnd
        calEnd[Calendar.MINUTE] = minuteEnd
        calEnd[Calendar.SECOND] = 0
        calEnd[Calendar.MILLISECOND] = 0
        val dateEnd = Date(calEnd.timeInMillis)
        /*Final cal to get difference of time in minutes*/return getDifferenceInMinutes(dateStart, dateEnd)
    }

    /*returns difference in minutes*/
    private fun getDifferenceInMinutes(dateStart: Date, dateEnd: Date): Long {
        val diff = dateEnd.time - dateStart.time
        val seconds = diff / 1000
        return seconds / 60
    }

    override fun onDismiss(dialog: DialogInterface) {
        dialogDismissed = true
    }

    override fun onResume() {
        super.onResume()
        if (dialogDismissed && mAlertDialog != null) {
            mAlertDialog?.dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mCallback = activity as ISelectedTime
        } catch (e: ClassCastException) {
            Log.d("MyDialog", "Activity doesn't implement the interface")
        }
    }

    /**
     * Set color of tab item when it is unselected
     *
     * @param colorTabUnselected (eg. R.color.my_color)
     */
    fun setColorTabUnselected(colorTabUnselected: Int) {
        this.colorTabUnselected = colorTabUnselected
    }

    /**
     * Set color of tab item when it is selected
     *
     * @param colorTabSelected (eg. R.color.my_color)
     */
    fun setColorTabSelected(colorTabSelected: Int) {
        this.colorTabSelected = colorTabSelected
    }

    /**
     * Set button text color
     *
     * @param colorTextButton (eg. R.color.my_color)
     */
    fun setColorTextButton(colorTextButton: Int) {
        this.colorTextButton = colorTextButton
    }

    /**
     * Set background color of header dialog
     *
     * @param colorBackgroundHeader (eg. R.color.my_color)
     */
    fun setColorBackgroundHeader(colorBackgroundHeader: Int) {
        this.colorBackgroundHeader = colorBackgroundHeader
    }

    /**
     * Set true if you want see time into 24 hour format
     *
     * @param is24HourView true = 24 hour format, false = am/pm format
     */
    fun setIs24HourView(is24HourView: Boolean) {
        this.is24HourView = is24HourView
    }

    /**
     * Set message error that appears when you select a end time greater than start time (only if validateRange is true)
     *
     * @param messageErrorRangeTime String
     */
    fun setMessageErrorRangeTime(messageErrorRangeTime: String) {
        this.messageErrorRangeTime = messageErrorRangeTime
    }

    /**
     * Set positive button text
     *
     * @param textBtnPositive (eg. Ok or Accept)
     */
    fun setTextBtnPositive(textBtnPositive: String) {
        this.textBtnPositive = textBtnPositive
    }

    /**
     * Set negative button text
     *
     * @param textBtnNegative (eg. Cancel or Close)
     */
    fun setTextBtnNegative(textBtnNegative: String) {
        this.textBtnNegative = textBtnNegative
    }

    /**
     * Set dialog radius (default is 50)
     *
     * @param radiusDialog Set to 0 if you want remove radius
     */
    fun setRadiusDialog(radiusDialog: Int) {
        this.radiusDialog = radiusDialog
    }

    /**
     * Set tab start text
     *
     * @param textTabStart (eg. Start time)
     */
    fun setTextTabStart(textTabStart: String) {
        this.textTabStart = textTabStart
    }

    /**
     * Set tab end text
     *
     * @param textTabEnd (eg. End time)
     */
    fun setTextTabEnd(textTabEnd: String) {
        this.textTabEnd = textTabEnd
    }

    /**
     * Set true if you want validate the range time (start time < end time). Set false if you want select any time
     *
     * @param validateRange true = validation, false = no validation
     */
    fun setValidateRange(validateRange: Boolean) {
        this.validateRange = validateRange
    }

    /**
     * Set background color of header timePicker
     *
     * @param colorBackgroundTimePickerHeader (eg. R.color.my_color)
     */
    fun setColorBackgroundTimePickerHeader(colorBackgroundTimePickerHeader: Int) {
        this.colorBackgroundTimePickerHeader = colorBackgroundTimePickerHeader
    }

    @Suppress("DEPRECATION")
    private fun setColorTabLayout(colorTabSelected: Int, colorTabUnselected: Int, colorBackgroundHeader: Int) {
        tabLayout?.setBackgroundColor(ContextCompat.getColor(activity!!, colorBackgroundHeader))
        // Set color header TabLayout
        tabLayout?.setTabTextColors(ContextCompat.getColor(activity!!, colorTabUnselected), ContextCompat.getColor(activity!!, colorTabSelected))
        tabLayout?.setSelectedTabIndicatorColor(ContextCompat.getColor(activity!!, colorTabSelected))
        // Use setColorFilter to avoid setTint because setTint is for API >= 21
        var tabIconColor = ContextCompat.getColor(activity!!, colorTabSelected)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tabLayout?.getTabAt(0)?.icon?.colorFilter = BlendModeColorFilter(tabIconColor, BlendMode.SRC_IN)
        } else {
            tabLayout?.getTabAt(0)?.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
        }

        //tabLayout?.getTabAt(0).getIcon().setTint(Color.YELLOW);
        tabIconColor = ContextCompat.getColor(activity!!, colorTabUnselected)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tabLayout?.getTabAt(1)?.icon?.colorFilter = BlendModeColorFilter(tabIconColor, BlendMode.SRC_IN)
        } else {
            tabLayout?.getTabAt(1)?.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
        }
        //tabLayout?.getTabAt(1).getIcon().setTint(Color.WHITE);
    }

    /**
     * Set color of timePicker'header
     *
     * @param rangeTimePickerDialog Dialog where is located the timePicker
     * @param color                 Color to set
     * @param nameTimePicker        id of timePicker declared into xml (eg. my_time_picker [android:id="@+id/my_time_picker"])
     */
    private fun setTimePickerHeaderBackgroundColor(rangeTimePickerDialog: RangeTimePickerDialog, color: Int, nameTimePicker: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val mTimePickerField: Field = RangeTimePickerDialog::class.java.getDeclaredField(nameTimePicker)
                mTimePickerField.isAccessible = true
                val mTimePicker = mTimePickerField[rangeTimePickerDialog] as TimePicker
                val headerId = Resources.getSystem().getIdentifier("time_header", "id", "android")
                val header = mTimePicker.findViewById<View>(headerId)
                header.setBackgroundColor(color)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val headerTextId = Resources.getSystem().getIdentifier("input_header", "id", "android")
                    val headerText = mTimePicker.findViewById<View>(headerTextId)
                    headerText.setBackgroundColor(color)
                    headerText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Method to enable/disable minutes into range time dialog
     *
     * @param value true = minutes enabled; false = minutes disabled
     */
    fun enableMinutes(value: Boolean) {
        isMinutesEnabled = value
    }

    @Suppress("DEPRECATION")
    private fun setMinutesEnabled(rangeTimePickerDialog: RangeTimePickerDialog, value: Boolean, nameTimePicker: String) {
        try {
            val mTimePickerField: Field = RangeTimePickerDialog::class.java.getDeclaredField(nameTimePicker)
            mTimePickerField.isAccessible = true
            val mTimePicker = mTimePickerField[rangeTimePickerDialog] as TimePicker
            val minutesId: Int
            val hoursId: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                minutesId = Resources.getSystem().getIdentifier("minutes", "id", "android")
                hoursId = Resources.getSystem().getIdentifier("hours", "id", "android")
            } else {
                minutesId = Resources.getSystem().getIdentifier("minute", "id", "android")
                hoursId = Resources.getSystem().getIdentifier("hour", "id", "android")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val toggleModeId = Resources.getSystem().getIdentifier("toggle_mode", "id", "android")
                val toggleModeView = mTimePicker.findViewById<View>(toggleModeId)
                toggleModeView.callOnClick()
                toggleModeView.visibility = View.GONE
            }
            val minutesView = mTimePicker.findViewById<View>(minutesId)
            val hoursView = mTimePicker.findViewById<View>(hoursId)
            minutesView.isEnabled = value

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mTimePicker.minute = 0
            } else {
                @Suppress("DEPRECATION")
                mTimePicker.currentMinute = 0
            }


            mTimePicker.setOnTimeChangedListener { _, _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mTimePicker.minute = 0
                } else {
                    mTimePicker.currentMinute = 0
                }
                hoursView.isSoundEffectsEnabled = false
                hoursView.performClick()
                hoursView.isSoundEffectsEnabled = true
            }
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Method to set initial start clock
     *
     * @param hour   Initial hour
     * @param minute Initial minute
     */
    fun setInitialStartClock(hour: Int, minute: Int) {
        initialStarHour = hour
        initialStartMinute = minute
    }

    /**
     * Method to set initial end clock
     *
     * @param hour   Initial hour
     * @param minute Initial minute
     */
    fun setInitialEndClock(hour: Int, minute: Int) {
        initialEndHour = hour
        initialEndMinute = minute
    }

    /**
     * Method to change start tab icon
     *
     * @param startTabIcon Resource ID of start tab icon
     */
    fun setStartTabIcon(startTabIcon: Int) {
        this.startTabIcon = startTabIcon
    }

    /**
     * Method to change end tab icon
     *
     * @param endTabIcon Resource ID of end tab icon
     */
    fun setEndTabIcon(endTabIcon: Int) {
        this.endTabIcon = endTabIcon
    }

    /**
     * Method to select which tab are selected on open
     *
     * @param initialOpenedTab START_CLOCK_TAB or END_CLOCK_TAB
     */
    fun setInitialOpenedTab(initialOpenedTab: InitialOpenedTab) {
        this.initialOpenedTab = initialOpenedTab
    }

    /**
     * Method to set keyboard input as default (Only on Oreo device)
     *
     * @param inputKeyboardAsDefault true = keyboard set as default, false: otherwise
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    fun setInputKeyboardAsDefault(inputKeyboardAsDefault: Boolean) {
        this.inputKeyboardAsDefault = inputKeyboardAsDefault
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setInputKeyboardAsDefault(timePickerName: String) {
        val mTimePickerField: Field
        try {
            mTimePickerField = RangeTimePickerDialog::class.java.getDeclaredField(timePickerName)
            mTimePickerField.isAccessible = true
            val mTimePicker = mTimePickerField[this@RangeTimePickerDialog] as TimePicker
            val toggleModeId = Resources.getSystem().getIdentifier("toggle_mode", "id", "android")
            val toggleModeView = mTimePicker.findViewById<View>(toggleModeId)
            toggleModeView.callOnClick()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    companion object {
        var HOUR_START = "hourStart"
        var MINUTE_START = "minuteStart"
        var HOUR_END = "hourEnd"
        var MINUTE_END = "minuteEnd"
    }
}