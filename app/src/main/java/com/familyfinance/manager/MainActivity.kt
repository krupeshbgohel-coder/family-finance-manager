package com.familyfinance.manager

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val blue = Color.rgb(25, 75, 150)
    private val dark = Color.rgb(35, 45, 65)
    private val background = Color.rgb(245, 247, 251)
    private val green = Color.rgb(25, 135, 84)
    private val red = Color.rgb(200, 55, 55)

    private lateinit var summary: LinearLayout
    private lateinit var transactionsBox: LinearLayout
    private lateinit var paymentsBox: LinearLayout

    private val prefs by lazy {
        getSharedPreferences("family_finance_data", Context.MODE_PRIVATE)
    }

    private val transactions = JSONArray()
    private val payments = JSONArray()
    private val loans = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = blue
        window.navigationBarColor = background
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        loadData()
        showDashboard()
    }

    private fun loadData() {
        try {
            val t = JSONArray(prefs.getString("transactions", "[]"))
            for (i in 0 until t.length()) {
                transactions.put(t.getJSONObject(i))
            }

            val p = JSONArray(prefs.getString("payments", "[]"))
            for (i in 0 until p.length()) {
                payments.put(p.getJSONObject(i))
            }

            val l = JSONArray(prefs.getString("loans", "[]"))
            for (i in 0 until l.length()) {
                loans.put(l.getJSONObject(i))
            }
        } catch (_: Exception) {
        }
    }

    private fun saveData() {
        prefs.edit()
            .putString("transactions", transactions.toString())
            .putString("payments", payments.toString())
            .putString("loans", loans.toString())
            .apply()
    }

    private fun money(value: Double): String {
        return "₹" + String.format(
            Locale("en", "IN"), "%,.2f", value
        ).replace(".00", "")
    }

    private fun total(type: String): Double {
        var result = 0.0
        for (i in 0 until transactions.length()) {
            val item = transactions.optJSONObject(i) ?: continue
            if (item.optString("type") == type) {
                result += item.optDouble("amount", 0.0)
            }
        }
        return result
    }

    private fun outstandingLoans(): Double {
        var result = 0.0

        // Count old loan records unless they have been
        // entered into the new loan manager.
        for (i in 0 until transactions.length()) {
            val item = transactions.optJSONObject(i) ?: continue
            if (item.optString("type") != "Loan") continue

            val name = item.optString("name")
            var managed = false

            for (j in 0 until loans.length()) {
                val loan = loans.optJSONObject(j) ?: continue
                if (loan.optString("name") == name) {
                    managed = true
                    break
                }
            }

            if (!managed) {
                result += item.optDouble("amount", 0.0)
            }
        }

        for (i in 0 until loans.length()) {
            val loan = loans.optJSONObject(i) ?: continue
            result += loan.optDouble("balance", 0.0)
        }

        return result
    }

    private fun showDashboard() {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(background)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(18, 22, 18, 24)
        scroll.addView(root)

        val title = TextView(this)
        title.text = "Family Finance Manager"
        title.textSize = 26f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(blue)
        root.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "Your family's money, organized."
        subtitle.textSize = 14f
        subtitle.setTextColor(Color.DKGRAY)
        subtitle.setPadding(0, 5, 0, 18)
        root.addView(subtitle)

        sectionTitle(root, "Financial Overview")

        summary = LinearLayout(this)
        summary.orientation = LinearLayout.VERTICAL
        root.addView(summary)
        refreshSummary()

        sectionTitle(root, "Quick Actions")

        addAction(root, "➕ Add Income") {
            showTransactionDialog("Income")
        }

        addAction(root, "➖ Add Expense") {
            showTransactionDialog("Expense")
        }

        addAction(root, "🏦 Loans & Debts") {
            showLoanManager()
        }

        addAction(root, "📅 Add Payment Reminder") {
            showPaymentDialog()
        }

        sectionTitle(root, "Upcoming Payments")

        paymentsBox = LinearLayout(this)
        paymentsBox.orientation = LinearLayout.VERTICAL
        root.addView(paymentsBox)
        refreshPayments()

        sectionTitle(root, "Recent Transactions")

        transactionsBox = LinearLayout(this)
        transactionsBox.orientation = LinearLayout.VERTICAL
        root.addView(transactionsBox)
        refreshTransactions()

        val note = TextView(this)
        note.text = "Your data is stored locally on this phone."
        note.textSize = 12f
        note.setTextColor(Color.GRAY)
        note.setPadding(0, 22, 0, 8)
        root.addView(note)

        setContentView(scroll)
    }

    private fun sectionTitle(parent: LinearLayout, text: String) {
        val heading = TextView(this)
        heading.text = text
        heading.textSize = 20f
        heading.setTypeface(null, Typeface.BOLD)
        heading.setTextColor(dark)
        heading.setPadding(0, 20, 0, 10)
        parent.addView(heading)
    }

    private fun addCard(
        parent: LinearLayout,
        title: String,
        value: String,
        valueColor: Int = blue
    ) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(16, 14, 16, 14)
        card.setBackgroundColor(Color.WHITE)

        val heading = TextView(this)
        heading.text = title
        heading.textSize = 14f
        heading.setTextColor(Color.DKGRAY)
        card.addView(heading)

        val amount = TextView(this)
        amount.text = value
        amount.textSize = 22f
        amount.setTypeface(null, Typeface.BOLD)
        amount.setTextColor(valueColor)
        amount.setPadding(0, 6, 0, 0)
        card.addView(amount)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = 10
        parent.addView(card, params)
    }

    private fun refreshSummary() {
        summary.removeAllViews()

        val income = total("Income")
        val expenses = total("Expense")
        val balance = income - expenses

        addCard(summary, "Total Income", money(income), green)
        addCard(summary, "Family Expenses", money(expenses), red)
        addCard(
            summary,
            "Outstanding Loans & Debts",
            money(outstandingLoans())
        )
        addCard(
            summary,
            "Available Balance",
            money(balance),
            if (balance >= 0) green else red
        )
    }

    private fun addAction(
        parent: LinearLayout,
        text: String,
        action: () -> Unit
    ) {
        val button = Button(this)
        button.text = text
        button.textSize = 15f
        button.setTextColor(blue)
        button.setBackgroundColor(Color.WHITE)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = 8
        parent.addView(button, params)

        button.setOnClickListener { action() }
    }

    private fun createInput(
        hintText: String,
        inputType: Int = android.text.InputType.TYPE_CLASS_TEXT
    ): EditText {
        return EditText(this).apply {
            hint = hintText
            this.inputType = inputType
            textSize = 16f
            setSingleLine(true)
        }
    }

    private fun showTransactionDialog(type: String) {
        val form = LinearLayout(this)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(22, 8, 22, 0)

        val name = createInput(
            when (type) {
                "Income" -> "Income source, e.g. Salary"
                else -> "Expense name, e.g. Groceries"
            }
        )

        val amount = createInput(
            "Amount in ₹",
            android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )

        val category = createInput("Category")
        val date = createInput("Date (YYYY-MM-DD)")
        date.setText(todayString())

        form.addView(name)
        form.addView(amount)
        form.addView(category)
        form.addView(date)

        AlertDialog.Builder(this)
            .setTitle("Add $type")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val title = name.text.toString().trim()
                val value = amount.text.toString().toDoubleOrNull()
                val cat = category.text.toString().trim()
                val enteredDate = date.text.toString().trim()

                if (title.isEmpty() || value == null ||
                    value <= 0 || !isValidDate(enteredDate)
                ) {
                    showMessage(
                        "Invalid details",
                        "Enter a name, valid amount and date."
                    )
                    return@setPositiveButton
                }

                val item = JSONObject()
                item.put("name", title)
                item.put("amount", value)
                item.put("category", cat)
                item.put("date", enteredDate)
                item.put("type", type)

                transactions.put(item)
                saveData()
                refreshSummary()
                refreshTransactions()
            }
            .show()
    }

    // LOAN MANAGEMENT

    private fun showLoanManager() {
        val options = arrayOf(
            "Add New Loan",
            "View Loans & Outstanding Balances",
            "Mark EMI as Paid"
        )

        AlertDialog.Builder(this)
            .setTitle("Loans & Debts")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddLoanDialog()
                    1 -> showLoanList()
                    2 -> chooseLoanForPayment()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAddLoanDialog() {
        val form = LinearLayout(this)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(22, 8, 22, 0)

        val name = createInput("Loan name, e.g. Bike Loan")
        val principal = createInput(
            "Original loan amount in ₹",
            android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val emi = createInput(
            "Monthly EMI in ₹",
            android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val rate = createInput(
            "Annual interest rate %, e.g. 12",
            android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val date = createInput("Next EMI date (YYYY-MM-DD)")
        date.setText(todayString())

        form.addView(name)
        form.addView(principal)
        form.addView(emi)
        form.addView(rate)
        form.addView(date)

        AlertDialog.Builder(this)
            .setTitle("Add New Loan")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val title = name.text.toString().trim()
                val original = principal.text.toString().toDoubleOrNull()
                val monthlyEmi = emi.text.toString().toDoubleOrNull()
                val annualRate = rate.text.toString().toDoubleOrNull()
                val nextDate = date.text.toString().trim()

                if (title.isEmpty() ||
                    original == null || original <= 0 ||
                    monthlyEmi == null || monthlyEmi <= 0 ||
                    annualRate == null || annualRate < 0 ||
                    annualRate > 100 ||
                    !isValidDate(nextDate)
                ) {
                    showMessage(
                        "Invalid details",
                        "Check the loan amount, EMI, interest rate and date."
                    )
                    return@setPositiveButton
                }

                val loan = JSONObject()
                loan.put("name", title)
                loan.put("original", original)
                loan.put("balance", original)
                loan.put("emi", monthlyEmi)
                loan.put("rate", annualRate)
                loan.put("nextDate", nextDate)
                loan.put("paidCount", 0)

                loans.put(loan)

                saveData()
                refreshSummary()
                refreshTransactions()
                showLoanList()
            }
            .show()
    }

    private fun showLoanList() {
        if (loans.length() == 0) {
            showMessage(
                "No managed loans",
                "Select Add New Loan to record a loan."
            )
            return
        }

        val text = StringBuilder()

        for (i in 0 until loans.length()) {
            val loan = loans.optJSONObject(i) ?: continue
            text.append("Loan: ")
                .append(loan.optString("name"))
                .append("\nOriginal: ")
                .append(money(loan.optDouble("original")))
                .append("\nOutstanding: ")
                .append(money(loan.optDouble("balance")))
                .append("\nMonthly EMI: ")
                .append(money(loan.optDouble("emi")))
                .append("\nAnnual interest: ")
                .append(loan.optDouble("rate"))
                .append("%\nNext EMI: ")
                .append(loan.optString("nextDate"))
                .append("\nEMIs paid: ")
                .append(loan.optInt("paidCount"))
                .append("\n\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Loan Details")
            .setMessage(text.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun chooseLoanForPayment() {
        if (loans.length() == 0) {
            showMessage(
                "No loans available",
                "Add a loan before recording an EMI payment."
            )
            return
        }

        val names = mutableListOf<String>()
        val indexes = mutableListOf<Int>()

        for (i in 0 until loans.length()) {
            val loan = loans.optJSONObject(i) ?: continue
            if (loan.optDouble("balance") > 0) {
                names.add(
                    "${loan.optString("name")} — " +
                        money(loan.optDouble("emi"))
                )
                indexes.add(i)
            }
        }

        if (indexes.isEmpty()) {
            showMessage("All loans cleared", "There are no outstanding managed loans.")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select Loan — EMI Paid")
            .setItems(names.toTypedArray()) { _, which ->
                confirmEmiPayment(indexes[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmEmiPayment(index: Int) {
        val loan = loans.optJSONObject(index) ?: return
        val name = loan.optString("name")
        val balance = loan.optDouble("balance")
        val emi = loan.optDouble("emi")
        val annualRate = loan.optDouble("rate")
        val nextDate = loan.optString("nextDate")

        val interest = balance * annualRate / 1200.0
        val principalPaid = (emi - interest).coerceAtLeast(0.0)
            .coerceAtMost(balance)
        val remaining = (balance - principalPaid).coerceAtLeast(0.0)

        val message = buildString {
            append("Loan: ").append(name)
            append("\nEMI amount: ").append(money(emi))
            append("\nEstimated interest: ").append(money(interest))
            append("\nPrincipal reduction: ").append(money(principalPaid))
            append("\nNew outstanding: ").append(money(remaining))
            append("\n\nConfirm that this EMI has been paid?")
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm EMI Payment")
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm Paid") { _, _ ->
                if (emi <= interest && remaining > 0) {
                    showMessage(
                        "EMI too low",
                        "This EMI does not cover the estimated monthly interest. " +
                            "Check the EMI or interest rate before proceeding."
                    )
                    return@setPositiveButton
                }

                loan.put("balance", remaining)
                loan.put("paidCount", loan.optInt("paidCount") + 1)

                val next = nextMonthDate(nextDate)
                loan.put("nextDate", next)

                val item = JSONObject()
                item.put("name", "$name EMI")
                item.put("amount", emi)
                item.put("category", "Loan Repayment")
                item.put("date", todayString())
                item.put("type", "Expense")
                transactions.put(item)

                saveData()
                refreshSummary()
                refreshTransactions()
                showLoanList()
            }
            .show()
    }

    private fun nextMonthDate(value: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.isLenient = false

        val calendar = Calendar.getInstance()
        calendar.time = format.parse(value) ?: Date()
        calendar.add(Calendar.MONTH, 1)

        return format.format(calendar.time)
    }

    // PAYMENT REMINDERS

    private fun showPaymentDialog() {
        val form = LinearLayout(this)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(22, 8, 22, 0)

        val name = createInput("Payment name, e.g. Rent or EMI")
        val amount = createInput(
            "Amount in ₹",
            android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val dueDate = createInput("Due date (YYYY-MM-DD)")
        dueDate.setText(todayString())

        form.addView(name)
        form.addView(amount)
        form.addView(dueDate)

        AlertDialog.Builder(this)
            .setTitle("Add Payment Reminder")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val title = name.text.
