package com.familyfinance.manager

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val blue = Color.rgb(25, 75, 150)
    private val background = Color.rgb(245, 247, 251)
    private val prefs by lazy {
        getSharedPreferences("family_finance_data", MODE_PRIVATE)
    }

    private lateinit var root: LinearLayout
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = blue
        window.navigationBarColor = background
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        showDashboard()
    }

    private fun showDashboard() {
        scroll = ScrollView(this)
        scroll.setBackgroundColor(background)

        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(18, 22, 18, 30)
        scroll.addView(root)

        val title = TextView(this)
        title.text = "Family Finance Manager"
        title.textSize = 27f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(blue)
        root.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "Your family's money, organized."
        subtitle.textSize = 15f
        subtitle.setTextColor(Color.DKGRAY)
        subtitle.setPadding(0, 5, 0, 12)
        root.addView(subtitle)

        heading("Financial Overview")

        val transactions = getArray("transactions")
        var income = 0.0
        var expenses = 0.0

        for (i in 0 until transactions.length()) {
            val item = transactions.optJSONObject(i) ?: continue
            val amount = item.optDouble("amount", 0.0)

            if (item.optString("type") == "Income") {
                income += amount
            } else {
                expenses += amount
            }
        }

        val loans = getArray("loans")
        var outstanding = 0.0

        for (i in 0 until loans.length()) {
            outstanding += loans.optJSONObject(i)
                ?.optDouble("balance", 0.0) ?: 0.0
        }

        card("Monthly Income", money(income))
        card("Family Expenses", money(expenses))
        card("Outstanding Loans & Debts", money(outstanding))
        card("Available Balance", money(income - expenses))

        heading("Quick Actions")

        actionButton("➕  Add Income") {
            showTransactionDialog("Income")
        }

        actionButton("➖  Add Expense") {
            showTransactionDialog("Expense")
        }

        actionButton("🏦  Add Loan or Debt") {
            showLoanDialog()
        }

        actionButton("📅  Add Payment Reminder") {
            showReminderDialog()
        }

        actionButton("📜  Transaction History") {
            showTransactions()
        }

        actionButton("📋  Loans & Debts") {
            showLoans()
        }

        actionButton("⏰  Payment Reminders") {
            showReminders()
        }

        heading("Upcoming Payments")

        val reminders = getArray("reminders")
        val today = todayString()
        var upcomingCount = 0

        for (i in 0 until reminders.length()) {
            val item = reminders.optJSONObject(i) ?: continue

            if (item.optString("date") >= today) {
                card(
                    item.optString("title"),
                    money(item.optDouble("amount", 0.0)) +
                        " • Due: " + item.optString("date")
                )
                upcomingCount++
            }
        }

        if (upcomingCount == 0) {
            card(
                "No upcoming payments",
                "Tap Add Payment Reminder to add one."
            )
        }

        setContentView(scroll)
    }

    private fun heading(text: String) {
        val view = TextView(this)
        view.text = text
        view.textSize = 20f
        view.setTypeface(null, Typeface.BOLD)
        view.setTextColor(Color.rgb(35, 45, 65))
        view.setPadding(0, 24, 0, 12)
        root.addView(view)
    }

    private fun card(title: String, value: String) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(18, 15, 18, 15)
        layout.setBackgroundColor(Color.WHITE)

        val titleView = TextView(this)
        titleView.text = title
        titleView.textSize = 15f
        titleView.setTextColor(Color.DKGRAY)
        layout.addView(titleView)

        val valueView = TextView(this)
        valueView.text = value
        valueView.textSize = 21f
        valueView.setTextColor(blue)
        valueView.setPadding(0, 7, 0, 0)
        layout.addView(valueView)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = 10
        root.addView(layout, params)
    }

    private fun actionButton(text: String, action: () -> Unit) {
        val button = Button(this)
        button.text = text
        button.textSize = 16f
        button.setTextColor(blue)
        button.setBackgroundColor(Color.WHITE)
        button.gravity = Gravity.CENTER_VERTICAL
        button.setPadding(16, 8, 16, 8)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = 8

        root.addView(button, params)
        button.setOnClickListener { action() }
    }

    private fun createInput(
        hint: String,
        inputType: Int = android.text.InputType.TYPE_CLASS_TEXT
    ): EditText {
        return EditText(this).apply {
            this.hint = hint
            this.inputType = inputType
            textSize = 16f
            singleLine = true
        }
    }

    private fun showTransactionDialog(type: String) {
        val form = LinearLayout(this)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(24, 8, 24, 0)

        val name = createInput(
            if (type == "Income") "Income source (e.g. Salary)"
            else "Expense name (e.g. Groceries)"
        )

        val amount = createInput(
            "Amount in ₹",
            android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )

        val category = createInput("Category (e.g. Home, Food, Salary)")

        val date = createInput("Date (YYYY-MM-DD)")
        date.setText(todayString())

        form.addView(name)
        form.addView(amount)
        form.addView(category)
        form.addView(date)

        AlertDialog.Builder(this)
            .setTitle(if (type == "Income") "Add Income" else "Add Expense")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {
                            val title = name.text.toString().trim()
                            val value = amount.text.toString().toDoubleOrNull()
                            val cat = category.text.toString().trim()
                            val enteredDate = date.text.toString().trim()

                            if (title.isEmpty() ||
                                value == null || value <= 0 ||
                                !validDate(enteredDate)
                            ) {
                                toast("Enter a name, valid amount and date.")
                                return@setOnClickListener
                            }

                            val item = JSONObject()
                            item.put("type", type)
                            item.put("title", title)
                            item.put("amount", value)
                            item.put("category", cat)
                            item.put("date", enteredDate)

                            val list = getArray("transactions")
                            list.put(item)
                            saveArray("transactions", list)

                            dialog.dismiss()
                            showDashboard()
                            toast("$type saved successfully")
                        }
                }
                dialog.show()
            }
    }

    private fun showLoanDialog() {
        val form = LinearLayout(this)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(24, 8, 24, 0)

        val name = createInput("Loan name / lender")
        val total = createInput(
            "Original loan amount ₹",
            android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val balance = createInput(
            "Current outstanding amount ₹",
            android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val emi = createInput(
            "Monthly EMI ₹ (optional)",
            android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )

        form.addView(name)
        form.addView(total)
        form.addView(balance)
        form.addView(emi)

        AlertDialog.Builder(this)
            .setTitle("Add Loan or Debt")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {
                            val loanName = name.text.toString().trim()
                            val original = total.text.toString()
                                .toDoubleOrNull()
                            val outstanding = balance.text.toString()
                                .toDoubleOrNull()
                            val monthlyEmi = emi.text.toString()
                                .toDoubleOrNull() ?: 0.0

                            if (loanName.isEmpty() ||
                                original == null || original <= 0 ||
                                outstanding == null ||
                                outstanding < 0 ||
                                outstanding > original ||
                                monthlyEmi < 0
                            ) {
                                toast("Check the loan amounts and try again.")
                                return@setOnClickListener
                            }

                            val item = JSONObject()
                            item.put("title", loanName)
                            item.put("original", original)
                            item.put("balance", outstanding)
                            item.put("emi", monthlyEmi)

                            val list = getArray("loans")
                            list.put(item)
                            saveArray("loans", list)

                            dialog.dismiss()
                            showDashboard()
                            toast("Loan saved successfully")
                        }
                }
                dialog.show()
            }
    }

    private fun showReminderDialog() {
        val form = LinearLayout(this)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(24, 8, 24, 0)

        val name = createInput("Payment name (e.g. Rent / EMI)")
        val amount = createInput(
            "Amount ₹",
            android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val date = createInput("Due date (YYYY-MM-DD)")
        date.setText(todayString())

        form.addView(name)
        form.addView(amount)
        form.addView(date)

        AlertDialog.Builder(this)
            .setTitle("Add Payment Reminder")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {
                            val title = name.text.toString().trim()
                            val value = amount.text.toString().toDoubleOrNull()
                            val dueDate = date.text.toString().trim()

                            if (title.isEmpty() ||
                                value == null || value <= 0 ||
                                !validDate(dueDate)
                            ) {
                                toast("Enter a name, amount and valid date.")
                                return@setOnClickListener
                            }

                            val item = JSONObject()
                            item.put("title", title)
                            item.put("amount", value)
                            item.put("date", dueDate)

                            val list = getArray("reminders")
                            list.put(item)
                            saveArray("reminders", list)

                            dialog.dismiss()
                            showDashboard()
                            toast("Reminder saved")
                        }
                }
                dialog.show()
            }
    }

    private fun showTransactions() {
        val list = getArray("transactions")
        val lines = mutableListOf<String>()

        for (i in list.length() - 1 downTo 0) {
            val item = list.optJSONObject(i) ?: continue
            lines.add(
                "${item.optString("date")} • ${item.optString("type")}\n" +
                    "${item.optString("title")} — " +
                    money(item.optDouble("amount", 0.0))
            )
        }

        showListDialog(
            "Transaction History",
            lines,
            "No transactions added yet."
        )
    }

    private fun showLoans() {
        val list = getArray("loans")
        val lines = mutableListOf<String>()

        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            lines.add(
                "${item.optString("title")}\n" +
                    "Outstanding: ${money(item.optDouble("balance", 0.0))}\n" +
                    "Monthly EMI: ${money(item.optDouble("emi", 0.0))}"
            )
        }

        showListDialog(
            "Loans & Debts",
            lines,
            "No loans added yet."
        )
    }

    private fun showReminders() {
        val list = getArray("reminders")
        val lines = mutableListOf<String>()

        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            lines.add(
                "${item.optString("title")}\n" +
                    "${money(item.optDouble("amount", 0.0))} • " +
                    "Due: ${item.optString("date")}"
            )
        }

        showListDialog(
            "Payment Reminders",
            lines,
            "No payment reminders added yet."
        )
    }

    private fun showListDialog(
        title: String,
        lines: List<String>,
        emptyMessage: String
    ) {
        val message = if (lines.isEmpty()) {
            emptyMessage
        } else {
            lines.joinToString("\n\n────────────────\n\n")
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Back") { _, _ ->
                showDashboard()
            }
            .show()
    }

    private fun getArray(key: String): JSONArray {
        return try {
            JSONArray(prefs.getString(key, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveArray(key: String, array: JSONArray) {
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun money(value: Double): String {
        return "₹" + String.format(Locale("en", "IN"), "%,.2f", value)
    }

    private fun todayString(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date())
    }

    private fun validDate(value: String): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.isLenient = false
            val parsed = format.parse(value)
            parsed != null && format.format(parsed) == value
        } catch (_: Exception) {
            false
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
