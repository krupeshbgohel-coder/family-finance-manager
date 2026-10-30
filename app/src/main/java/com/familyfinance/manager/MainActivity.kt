package com.familyfinance.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
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
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        loadData()
        showDashboard()
    }

    private fun loadData() {
        try {
            val savedT = JSONArray(prefs.getString("transactions", "[]") ?: "[]")
            for (i in 0 until savedT.length()) transactions.put(savedT.getJSONObject(i))
            val savedP = JSONArray(prefs.getString("payments", "[]") ?: "[]")
            for (i in 0 until savedP.length()) payments.put(savedP.getJSONObject(i))
        } catch (_: Exception) { }
    }

    private fun saveData() {
        prefs.edit().putString("transactions", transactions.toString())
            .putString("payments", payments.toString()).apply()
    }

    private fun money(value: Double): String =
        "₹" + String.format(Locale("en", "IN"), "%,.2f", value).replace(".00", "")

    private fun total(type: String): Double {
        var result = 0.0
        for (i in 0 until transactions.length()) {
            val item = transactions.optJSONObject(i) ?: continue
            if (item.optString("type") == type) result += item.optDouble("amount", 0.0)
        }
        return result
    }

    private fun showDashboard() {
        val scroll = ScrollView(this).apply { setBackgroundColor(background) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 22, 18, 24)
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "Family Finance Manager"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(blue)
        })
        root.addView(TextView(this).apply {
            text = "Your family's money, organized."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 5, 0, 18)
        })

        sectionTitle(root, "Financial Overview")
        summary = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(summary)
        refreshSummary()

        sectionTitle(root, "Quick Actions")
        addAction(root, "➕ Add Income") { showTransactionDialog("Income") }
        addAction(root, "➖ Add Expense") { showTransactionDialog("Expense") }
        addAction(root, "🏦 Loans & Debts") { showLoanMenu() }
        addAction(root, "📅 Add Payment Reminder") { showPaymentDialog() }
        addAction(root, "📊 Monthly Report") { showMonthlyReport() }

        sectionTitle(root, "Upcoming Payments")
        paymentsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(paymentsBox)
        refreshPayments()

        sectionTitle(root, "Recent Transactions")
        transactionsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(transactionsBox)
        refreshTransactions()

        root.addView(TextView(this).apply {
            text = "Your data is stored locally on this phone. Keep a separate backup before changing phones."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 22, 0, 8)
        })
        setContentView(scroll)
    }

    private fun sectionTitle(parent: LinearLayout, textValue: String) {
        parent.addView(TextView(this).apply {
            text = textValue
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(dark)
            setPadding(0, 20, 0, 10)
        })
    }

    private fun addCard(parent: LinearLayout, title: String, value: String, valueColor: Int = blue) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(Color.WHITE)
        }
        card.addView(TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.DKGRAY)
        })
        card.addView(TextView(this).apply {
            text = value
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            setTextColor(valueColor)
            setPadding(0, 6, 0, 0)
        })
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = 10
        parent.addView(card, params)
    }

    private fun refreshSummary() {
        summary.removeAllViews()
        val income = total("Income")
        val expenses = total("Expense")
        val loans = total("Loan")
        addCard(summary, "Total Income", money(income), green)
        addCard(summary, "Family Expenses", money(expenses), red)
        addCard(summary, "Outstanding Loans & Debts Recorded", money(loans))
        addCard(summary, "Available Balance", money(income - expenses), if (income >= expenses) green else red)
    }

    private fun addAction(parent: LinearLayout, label: String, action: () -> Unit) {
        val button = TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(blue)
            gravity = android.view.Gravity.CENTER
            setPadding(12, 18, 12, 18)
            setBackgroundColor(Color.WHITE)
            isClickable = true
            isFocusable = true
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = 8
        parent.addView(button, params)
        button.setOnClickListener { action() }
    }

    private fun createInput(hintText: String, inputType: Int = android.text.InputType.TYPE_CLASS_TEXT): EditText =
        EditText(this).apply {
            hint = hintText
            this.inputType = inputType
            textSize = 16f
            setSingleLine(true)
        }

    private fun showTransactionDialog(type: String) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 8, 22, 0)
        }
        val name = createInput(when (type) {
            "Income" -> "Income source (e.g. Salary)"
            "Expense" -> "Expense name (e.g. Groceries)"
            else -> "Loan or debt name"
        })
        val amount = createInput("Amount in ₹", android.text.InputType.TYPE_CLASS_NUMBER or
            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val category = createInput("Category (e.g. Home, Food, Salary)")
        val date = createInput("Date (YYYY-MM-DD)").apply { setText(todayString()) }
        form.addView(name)
        form.addView(amount)
        form.addView(category)
        form.addView(date)

        AlertDialog.Builder(this)
            .setTitle(when (type) {
                "Income" -> "Add Income"
                "Expense" -> "Add Expense"
                else -> "Add Loan or Debt"
            })
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val title = name.text.toString().trim()
                val value = amount.text.toString().toDoubleOrNull()
                val cat = category.text.toString().trim()
                val enteredDate = date.text.toString().trim()
                if (title.isEmpty() || value == null || value <= 0 || !isValidDate(enteredDate)) {
                    showMessage("Invalid details", "Enter a name, amount greater than zero, and date as YYYY-MM-DD.")
                    return@setPositiveButton
                }
                transactions.put(JSONObject().apply {
                    put("name", title)
                    put("amount", value)
                    put("category", cat)
                    put("date", enteredDate)
                    put("type", type)
                })
                saveData()
                refreshSummary()
                refreshTransactions()
            }.show()
    }

    private fun showLoanMenu() {
        val options = arrayOf("Add loan or debt", "Add EMI payment reminder", "View loan/debt list")
        AlertDialog.Builder(this).setTitle("Loans & Debts").setItems(options) { _, which ->
            when (which) {
                0 -> showTransactionDialog("Loan")
                1 -> showPaymentDialog()
                2 -> showLoanList()
            }
        }.setNegativeButton("Close", null).show()
    }

    private fun showLoanList() {
        val items = mutableListOf<JSONObject>()
        for (i in 0 until transactions.length()) {
            val item = transactions.optJSONObject(i) ?: continue
            if (item.optString("type") == "Loan") items.add(item)
        }
        if (items.isEmpty()) {
            showMessage("Loans & Debts", "No loans or debts have been recorded yet.")
            return
        }
        val lines = items.joinToString("\n\n") {
            "${it.optString("name")}\n${money(it.optDouble("amount", 0.0))} • ${it.optString("category")}\nDate: ${it.optString("date")}"
        }
        showMessage("Loans & Debts", lines)
    }

    private fun showPaymentDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 8, 22, 0)
        }
        val name = createInput("Payment name (e.g. Rent, EMI)")
        val amount = createInput("Amount in ₹", android.text.InputType.TYPE_CLASS_NUMBER or
            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val dueDate = createInput("Due date (YYYY-MM-DD)").apply { setText(todayString()) }
        form.addView(name)
        form.addView(amount)
        form.addView(dueDate)

        AlertDialog.Builder(this).setTitle("Add Payment Reminder").setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val title = name.text.toString().trim()
                val value = amount.text.toString().toDoubleOrNull()
                val date = dueDate.text.toString().trim()
                if (title.isEmpty() || value == null || value <= 0 || !isValidDate(date)) {
                    showMessage("Invalid details", "Enter a payment name, amount greater than zero, and valid date YYYY-MM-DD.")
                    return@setPositiveButton
                }
                payments.put(JSONObject().apply {
                    put("name", title)
                    put("amount", value)
                    put("date", date)
                })
                saveData()
                refreshPayments()
            }.show()
    }

    private fun refreshTransactions() {
        transactionsBox.removeAllViews()
        if (transactions.length() == 0) {
            addCard(transactionsBox, "No transactions yet", "Add your first income or expense.")
            return
        }
        for (i in transactions.length() - 1 downTo 0) {
            val item = transactions.optJSONObject(i) ?: continue
            val type = item.optString("type")
            val color = when (type) {
                "Income" -> green
                "Expense" -> red
                else -> blue
            }
            addCard(
                transactionsBox,
                "${item.optString("name")} • $type",
                "${money(item.optDouble("amount", 0.0))}  |  ${item.optString("date")}",
                color
            )
        }
    }

    private fun refreshPayments() {
        paymentsBox.removeAllViews()
        if (payments.length() == 0) {
            addCard(paymentsBox, "No payments added yet", "Add a payment to track its due date.")
            return
        }
        val items = mutableListOf<JSONObject>()
        for (i in 0 until payments.length()) payments.optJSONObject(i)?.let { items.add(it) }
        items.sortBy { it.optString("date") }
        for (item in items) {
            val date = item.optString("date")
            val dueText = when {
                date < todayString() -> "OVERDUE • Due: $date"
                date == todayString() -> "DUE TODAY"
                else -> "Due: $date"
            }
            addCard(paymentsBox, "${item.optString("name")} • $dueText",
                money(item.optDouble("amount", 0.0)), if (date <= todayString()) red else blue)
        }
    }

    private fun showMonthlyReport() {
        val month = todayString().substring(0, 7)
        var income = 0.0
        var expenses = 0.0
        var loans = 0.0
        for (i in 0 until transactions.length()) {
            val item = transactions.optJSONObject(i) ?: continue
            if (!item.optString("date").startsWith(month)) continue
            when (item.optString("type")) {
                "Income" -> income += item.optDouble("amount", 0.0)
                "Expense" -> expenses += item.optDouble("amount", 0.0)
                "Loan" -> loans += item.optDouble("amount", 0.0)
            }
        }
        showMessage("Monthly Report • $month",
            "Income: ${money(income)}\nExpenses: ${money(expenses)}\nLoans/debts recorded: ${money(loans)}\nMonthly balance: ${money(income - expenses)}")
    }

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun isValidDate(value: String): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.isLenient = false
            val parsed = format.parse(value) ?: return false
            format.format(parsed) == value
        } catch (_: Exception) {
            false
        }
    }

    private fun showMessage(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton("OK", null).show()
    }
}
