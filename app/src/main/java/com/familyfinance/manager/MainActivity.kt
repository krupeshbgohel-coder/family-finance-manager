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
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val blue = Color.rgb(25, 75, 150)
    private val dark = Color.rgb(35, 45, 65)
    private val background = Color.rgb(245, 247, 251)
    private val green = Color.rgb(25, 135, 84)
    private val red = Color.rgb(200, 55, 55)

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var summary: LinearLayout
    private lateinit var transactionsBox: LinearLayout
    private lateinit var paymentsBox: LinearLayout

    private val prefs by lazy {
        getSharedPreferences("family_finance_data", Context.MODE_PRIVATE)
    }

    private val transactions = JSONArray()
    private val payments = JSONArray()

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
            val savedTransactions = prefs.getString("transactions", "[]")
            val savedPayments = prefs.getString("payments", "[]")

            val t = JSONArray(savedTransactions)
            for (i in 0 until t.length()) {
                transactions.put(t.getJSONObject(i))
            }

            val p = JSONArray(savedPayments)
            for (i in 0 until p.length()) {
                payments.put(p.getJSONObject(i))
            }
        } catch (_: Exception) {
        }
    }

    private fun saveData() {
        prefs.edit()
            .putString("transactions", transactions.toString())
            .putString("payments", payments.toString())
            .apply()
    }

    private fun money(value: Double): String {
        return "₹" + String.format(Locale("en", "IN"), "%,.2f", value)
            .replace(".00", "")
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

    private fun showDashboard() {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(background)

        root = LinearLayout(this)
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
            showTransactionDialog("Loan")
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
        amount.textSize = 23f
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
        val loans = total("Loan")
        val balance = income - expenses

        addCard(summary, "Total Income", money(income), green)
        addCard(summary, "Family Expenses", money(expenses), red)
        addCard(summary, "Outstanding Loans & Debts Recorded", money(loans))
        addCard(
            summary,
            "Available Balance",
            money(balance),
            if (balance >= 0) green else red
        )
    }

    private fun addAction(parent: LinearLayout, text: String, action: () -> Unit) {
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
        hint: String,
        inputType: Int = android.text.InputType.TYPE_CLASS_TEXT
    ): EditText {
        return EditText(this).apply {
            this.hint = hint
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
                "Income" -> "Income source (e.g. Salary)"
                "Expense" -> "Expense name (e.g. Groceries)"
                else -> "Loan or debt name"
            }
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
            .setTitle(
                when (type) {
                    "Income" -> "Add Income"
                    "Expense" -> "Add Expense"
                    else -> "Add Loan or Debt"
                }
            )
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val title = name.text.toString().trim()
                val value = amount.text.toString().toDoubleOrNull()
                val cat = category.text.toString().trim()
                val enteredDate = date.text.toString().trim()

                if (title.isEmpty() || value == null || value <= 0) {
                    showMessage(
                        "Invalid details",
                        "Enter a name and a valid amount greater than zero."
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

    private fun showPaymentDialog() {
        val form = LinearLayout(this)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(22, 8, 22, 0)

        val name = createInput("Payment name (e.g. Rent, EMI)")
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
                val title = name.text.toString().trim()
                val value = amount.text.toString().toDoubleOrNull()
                val date = dueDate.text.toString().trim()

                if (title.isEmpty() || value == null || value <= 0 ||
                    !isValidDate(date)
                ) {
                    showMessage(
                        "Invalid details",
                        "Enter a payment name, valid amount and date in YYYY-MM-DD format."
                    )
                    return@setPositiveButton
                }

                val item = JSONObject()
                item.put("name", title)
                item.put("amount", value)
                item.put("date", date)

                payments.put(item)
                saveData()
                refreshPayments()
            }
            .show()
    }

    private fun refreshTransactions() {
        transactionsBox.removeAllViews()

        if (transactions.length() == 0) {
            addCard(
                transactionsBox,
                "No transactions yet",
                "Add your first income or expense."
            )
            return
        }

        for (i in transactions.length() - 1 downTo 0) {
            val item = transactions.optJSONObject(i) ?: continue
            val type = item.optString("type")
            val title = item.optString("name")
            val amount = item.optDouble("amount", 0.0)
            val date = item.optString("date")

            addCard(
                transactionsBox,
                "$title • $type",
                "${money(amount)}  |  $date",
                if (type == "Income") green else if (type == "Expense") red else blue
            )
        }
    }

    private fun refreshPayments() {
        paymentsBox.removeAllViews()

        if (payments.length() == 0) {
            addCard(
                paymentsBox,
                "No payments added yet",
                "Add a payment to track its due date."
            )
            return
        }

        val list = mutableListOf<JSONObject>()
        for (i in 0 until payments.length()) {
            payments.optJSONObject(i)?.let { list.add(it) }
        }

        list.sortBy { it.optString("date") }

        for (item in list) {
            val date = item.optString("date")
            val dueText = when {
                date < todayString() -> "OVERDUE"
                date == todayString() -> "DUE TODAY"
                else -> "Due: $date"
            }

            addCard(
                paymentsBox,
                "${item.optString("name")} • $dueText",
                money(item.optDouble("amount", 0.0)),
                if (date <= todayString()) red else blue
            )
        }
    }

    private fun todayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun isValidDate(value: String): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.isLenient = false
            val date = format.parse(value) ?: return false
            format.format(date) == value
        } catch (_: Exception) {
            false
        }
    }

    private fun showMessage(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
