package com.familyfinance.manager

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

class MainActivity : Activity() {

    private val prefsName = "family_finance_manager_integrated"
    private val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private val blue = 0xFF1957A6.toInt()
    private val navy = 0xFF172B4D.toInt()
    private val pageBg = 0xFFF3F6FB.toInt()
    private val red = 0xFFC0392B.toInt()

    private val records = mutableListOf<Record>()
    private val loans = mutableListOf<Loan>()

    private var currentTab = "Dashboard"

    private lateinit var body: LinearLayout
    private lateinit var nav: LinearLayout

    data class Record(
        val id: Long,
        val kind: String,
        val title: String,
        val category: String,
        val amount: Double,
        val date: Long,
        val note: String
    )

    data class Loan(
        val id: Long,
        val name: String,
        val principal: Double,
        val annualRate: Double,
        val months: Int,
        val method: String,
        val firstDueDate: Long,
        var paidInstallments: Int = 0,
        var paidAmount: Double = 0.0
    ) {
        fun emi(): Double {
            if (months <= 0) return 0.0

            val r = annualRate / 1200.0

            return when {
                method == "Flat Interest" ->
                    (principal +
                        principal * annualRate / 100.0 *
                        months / 12.0) / months

                r == 0.0 -> principal / months

                else -> {
                    val factor = (1 + r).pow(months)
                    principal * r * factor / (factor - 1)
                }
            }
        }

        fun totalPayable() = emi() * months

        fun interest() =
            (totalPayable() - principal).coerceAtLeast(0.0)

        fun remaining() =
            (totalPayable() - paidAmount).coerceAtLeast(0.0)

        fun remainingInstallments() =
            (months - paidInstallments).coerceAtLeast(0)

        fun nextDue(): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = firstDueDate
            c.add(Calendar.MONTH, paidInstallments)
            return c.timeInMillis
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAll()
        buildShell()
        showTab("Dashboard")
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(16))
            setBackgroundColor(blue)
        }

        header.addView(TextView(this).apply {
            text = "Family Finance Manager"
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, Typeface.BOLD)
        })

        header.addView(TextView(this).apply {
            text = "Family budget • Loans • Events • Savings"
            textSize = 13f
            setTextColor(0xFFE4EEFF.toInt())
            setPadding(0, dp(5), 0, 0)
        })

        root.addView(header)

        val scroll = ScrollView(this)

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(24))
        }

        scroll.addView(body)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        root.addView(nav)
        setContentView(root)
        buildNav()
    }

    private fun buildNav() {
        nav.removeAllViews()

        val tabs = listOf(
            "Dashboard",
            "Income",
            "Expenses",
            "Events",
            "Loans"
        )

        tabs.forEach { tab ->
            val b = TextView(this).apply {
                text = tab
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(12), dp(3), dp(12))

                setTextColor(
                    if (currentTab == tab) blue else navy
                )

                if (currentTab == tab) {
                    setTypeface(null, Typeface.BOLD)
                }

                setOnClickListener {
                    currentTab = tab
                    buildNav()
                    showTab(tab)
                }
            }

            nav.addView(
                b,
                LinearLayout.LayoutParams(0, -2, 1f)
            )
        }
    }

    private fun showTab(tab: String) {
        currentTab = tab
        body.removeAllViews()

        when (tab) {
            "Dashboard" -> dashboard()
            "Income" -> recordPage("Income")
            "Expenses" -> recordPage("Expense")
            "Events" -> eventPage()
            "Loans" -> loanPage()
        }
    }

    private fun dashboard() {
        val income = records
            .filter { it.kind == "Income" }
            .sumOf { it.amount }

        val expenses = records
            .filter { it.kind == "Expense" }
            .sumOf { it.amount }

        val eventExpenses = records
            .filter { it.kind == "Event" }
            .sumOf { it.amount }

        val loanOutstanding = loans.sumOf { it.remaining() }

        val monthlyEmi = loans
            .filter { it.remainingInstallments() > 0 }
            .sumOf { it.emi() }

        addHeading("Family Dashboard")

        addSummaryCard(
            "Total income recorded",
            money(income),
            "Regular expenses: ${money(expenses)}\n" +
                "Family event expenses: ${money(eventExpenses)}\n" +
                "Balance before loan repayments: " +
                money(income - expenses - eventExpenses)
        )

        addSummaryCard(
            "Loan overview",
            money(loanOutstanding),
            "Outstanding payable: ${money(loanOutstanding)}\n" +
                "Monthly EMI for active loans: ${money(monthlyEmi)}\n" +
                "Active loans: " +
                loans.count { it.remainingInstallments() > 0 }
        )

        addHeading("Quick Actions")

        actionButton("+ Add Income") {
            showRecordDialog("Income")
        }

        actionButton("+ Add Household Expense") {
            showRecordDialog("Expense")
        }

        actionButton("+ Add Family Event Expense") {
            showRecordDialog("Event")
        }

        actionButton("+ Add Loan / EMI") {
            showLoanDialog()
        }

        addHeading("Recent Activity")

        val recent = records
            .sortedByDescending { it.date }
            .take(5)

        if (recent.isEmpty()) {
            addText("No income or expenses recorded yet.")
        }

        recent.forEach { r ->
            addSummaryCard(
                "${r.title} • ${r.kind}",
                money(r.amount),
                "${dateFormat.format(Date(r.date))} • ${r.category}"
            )
        }

        if (loans.isNotEmpty()) {
            addHeading("Upcoming EMI")

            loans
                .filter { it.remainingInstallments() > 0 }
                .sortedBy { it.nextDue() }
                .take(3)
                .forEach { loan ->
                    addSummaryCard(
                        loan.name,
                        money(loan.emi()),
                        "Next due: " +
                            dateFormat.format(Date(loan.nextDue()))
                    )
                }
        }
    }

    private fun recordPage(kind: String) {
        val label =
            if (kind == "Income") "Income"
            else "Household Expenses"

        addHeading(label)

        actionButton(
            if (kind == "Income") "+ Add Income"
            else "+ Add Expense"
        ) {
            showRecordDialog(kind)
        }

        val list = records
            .filter { it.kind == kind }
            .sortedByDescending { it.date }

        val total = list.sumOf { it.amount }

        addSummaryCard(
            "Total $label",
            money(total),
            "Entries: ${list.size}"
        )

        if (list.isEmpty()) {
            addText(
                "No records yet. Tap the button above " +
                    "to add your first entry."
            )
        }

        list.forEach { r ->
            addSummaryCard(
                r.title,
                money(r.amount),
                "${r.category} • ${dateFormat.format(Date(r.date))}" +
                    if (r.note.isNotBlank()) "\n${r.note}" else ""
            )

            smallAction("Delete: ${r.title}") {
                deleteRecord(r)
            }
        }
    }

    private fun eventPage() {
        addHeading("Family Event Budget")

        val eventRecords = records.filter {
            it.kind == "Event"
        }

        val total = eventRecords.sumOf { it.amount }

        addSummaryCard(
            "Total family event expenses",
            money(total),
            "Event expense entries: ${eventRecords.size}"
        )

        actionButton("+ Add Event Expense") {
            showRecordDialog("Event")
        }

        addText(
            "Use separate entries for weddings, birthdays, festivals, " +
                "vacations, school functions or other family occasions."
        )

        eventRecords
            .sortedByDescending { it.date }
            .forEach { r ->
                addSummaryCard(
                    r.title,
                    money(r.amount),
                    "${r.category} • ${dateFormat.format(Date(r.date))}" +
                        if (r.note.isNotBlank()) "\n${r.note}" else ""
                )

                smallAction("Delete: ${r.title}") {
                    deleteRecord(r)
                }
            }

        if (eventRecords.isEmpty()) {
            addText("No event expenses yet.")
        }
    }

    private fun loanPage() {
        addHeading("Loans & EMI Management")

        actionButton("+ Add New Loan") {
            showLoanDialog()
        }

        val principal = loans.sumOf { it.principal }
        val outstanding = loans.sumOf { it.remaining() }

        val emi = loans
            .filter { it.remainingInstallments() > 0 }
            .sumOf { it.emi() }

        addSummaryCard(
            "Loan summary",
            "Loans: ${loans.size}",
            "Original principal: ${money(principal)}\n" +
                "Outstanding scheduled payable: ${money(outstanding)}\n" +
                "Monthly EMI: ${money(emi)}"
        )

        if (loans.isEmpty()) {
            addText("No loans added yet.")
        }

        loans.forEach { loan ->
            addSummaryCard(
                loan.name,
                "EMI ${money(loan.emi())}",
                "${loan.method} • ${loan.annualRate}% p.a. • " +
                    "${loan.months} months\n" +
                    "Principal: ${money(loan.principal)}\n" +
                    "Total interest: ${money(loan.interest())}\n" +
                    "Total scheduled payable: ${money(loan.totalPayable())}\n" +
                    "Outstanding scheduled payable: ${money(loan.remaining())}\n" +
                    "Installments paid: ${loan.paidInstallments}/${loan.months}\n" +
                    "Next EMI due: " +
                    if (loan.remainingInstallments() > 0)
                        dateFormat.format(Date(loan.nextDue()))
                    else "Completed"
            )

            if (loan.remainingInstallments() > 0) {
                actionButton("Record EMI Paid • ${loan.name}") {
                    confirmLoanPayment(loan)
                }
            }

            smallAction("Delete loan: ${loan.name}") {
                confirmDeleteLoan(loan)
            }
        }
    }

    private fun showRecordDialog(kind: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(4))
        }

        val title = EditText(this).apply {
            hint = when (kind) {
                "Income" -> "Income source (e.g. Salary)"
                "Event" -> "Event name (e.g. Wedding)"
                else -> "Expense name (e.g. Groceries)"
            }
        }

        val amount = EditText(this).apply {
            hint = "Amount (₹)"
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val categories = when (kind) {
            "Income" -> listOf(
                "Salary",
                "Bonus",
                "Business",
                "Interest",
                "Other"
            )

            "Event" -> listOf(
                "Wedding",
                "Birthday",
                "Festival",
                "Travel",
                "School event",
                "Religious event",
                "Other"
            )

            else -> listOf(
                "Groceries",
                "Rent",
                "Utilities",
                "School",
                "Transport",
                "Medical",
                "Shopping",
                "Other"
            )
        }

        val categoryOptions =
            categories.filter { it != "Other" } +
                listOf("Other / Enter custom")

        val category = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                categoryOptions
            )
        }

        val customCategory = EditText(this).apply {
            hint = "Type your own category"
            visibility = android.view.View.GONE
        }

        category.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {
                }

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    customCategory.visibility =
                        if (categoryOptions[position] == "Other / Enter custom")
                            android.view.View.VISIBLE
                        else
                            android.view.View.GONE
                }
            }

        val note = EditText(this).apply {
            hint = "Note (optional)"
        }

        val dateButton = Button(this).apply {
            text = "Select date"
        }

        val date = Calendar.getInstance()

        dateButton.setOnClickListener {
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    date.set(y, m, d)
                    dateButton.text = dateFormat.format(date.time)
                },
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        box.addView(title)
        box.addView(amount)
        box.addView(category)
        box.addView(customCategory)
        box.addView(note)
        box.addView(dateButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                when (kind) {
                    "Income" -> "Add Income"
                    "Event" -> "Add Family Event Expense"
                    else -> "Add Household Expense"
                }
            )
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val t = title.text.toString().trim()
                    val a = amount.text.toString().toDoubleOrNull()

                    if (t.isBlank() || a == null || a <= 0) {
                        Toast.makeText(
                            this,
                            "Enter a title and amount greater than zero.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    val selectedCategory =
                        category.selectedItem.toString()

                    val finalCategory =
                        if (selectedCategory == "Other / Enter custom")
                            customCategory.text.toString().trim()
                        else
                            selectedCategory

                    if (finalCategory.isBlank()) {
                        Toast.makeText(
                            this,
                            "Please enter a custom category.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    records.add(
                        0,
                        Record(
                            System.currentTimeMillis(),
                            kind,
                            t,
                            finalCategory,
                            a,
                            date.timeInMillis,
                            note.text.toString().trim()
                        )
                    )

                    saveAll()
                    dialog.dismiss()
                    showTab(currentTab)
                }
        }

        dialog.show()
    }

    private fun showLoanDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(4))
        }

        val name = EditText(this).apply {
            hint = "Loan name (e.g. Personal Loan)"
        }

        val principal = EditText(this).apply {
            hint = "Loan amount (₹)"
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val rate = EditText(this).apply {
            hint = "Annual interest rate (%)"
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val months = EditText(this).apply {
            hint = "Tenure in months"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val loanTypes = listOf(
            "Personal Loan",
            "Home Loan",
            "Car Loan",
            "Bike Loan",
            "Gold Loan",
            "Education Loan",
            "Credit Card",
            "Other / Enter custom"
        )

        val loanType = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                loanTypes
            )
        }

        val customLoanType = EditText(this).apply {
            hint = "Enter custom loan type"
            visibility = android.view.View.GONE
        }

        loanType.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {
                }

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    customLoanType.visibility =
                        if (loanTypes[position] == "Other / Enter custom")
                            android.view.View.VISIBLE
                        else
                            android.view.View.GONE
                }
            }

        val method = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Reducing Balance", "Flat Interest")
            )
        }

        val dateButton = Button(this).apply {
            text = "Select first EMI date"
        }

        val due = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
        }

        dateButton.setOnClickListener {
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    due.set(y, m, d)
                    dateButton.text =
                        "First EMI: ${dateFormat.format(due.time)}"
                },
                due.get(Calendar.YEAR),
                due.get(Calendar.MONTH),
                due.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        box.addView(name)

        box.addView(
            TextView(this).apply {
                text = "Loan type"
                setPadding(0, dp(8), 0, dp(3))
            }
        )

        box.addView(loanType)
        box.addView(customLoanType)
        box.addView(principal)
        box.addView(rate)
        box.addView(months)

        box.addView(
            TextView(this).apply {
                text = "Interest calculation method"
                setPadding(0, dp(8), 0, dp(3))
            }
        )

        box.addView(method)
        box.addView(dateButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Loan")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Calculate & Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val n = name.text.toString().trim()
                    val p = principal.text.toString().toDoubleOrNull()
                    val r = rate.text.toString().toDoubleOrNull()
                    val m = months.text.toString().toIntOrNull()

                    if (
                        n.isBlank() ||
                        p == null || p <= 0 ||
                        r == null || r < 0 ||
                        m == null || m !in 1..600
                    ) {
                        Toast.makeText(
                            this,
                            "Enter valid details. Tenure must be 1–600 months.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    val chosenLoanType =
                        loanType.selectedItem.toString()

                    val finalLoanName =
                        if (chosenLoanType == "Other / Enter custom") {
                            val custom =
                                customLoanType.text.toString().trim()

                            if (custom.isBlank()) {
                                Toast.makeText(
                                    this,
                                    "Please enter a custom loan type.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@setOnClickListener
                            }

                            "$n ($custom)"
                        } else {
                            "$n ($chosenLoanType)"
                        }

                    val loan = Loan(
                        System.currentTimeMillis(),
                        finalLoanName,
                        p,
                        r,
                        m,
                        method.selectedItem.toString(),
                        due.timeInMillis
                    )

                    loans.add(0, loan)
                    saveAll()
                    dialog.dismiss()
                    showTab("Loans")
                }
        }

        dialog.show()
    }

    private fun confirmLoanPayment(loan: Loan) {
        AlertDialog.Builder(this)
            .setTitle("Record EMI payment")
            .setMessage(
                "Record ${money(loan.emi())} as paid for ${loan.name}? " +
                    "This marks installment ${loan.paidInstallments + 1} " +
                    "of ${loan.months}."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->

                loan.paidInstallments =
                    (loan.paidInstallments + 1).coerceAtMost(loan.months)

                loan.paidAmount =
                    (loan.paidAmount + loan.emi())
                        .coerceAtMost(loan.totalPayable())

                saveAll()
                showTab("Loans")

                Toast.makeText(
                    this,
                    "EMI payment recorded.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun confirmDeleteLoan(loan: Loan) {
        AlertDialog.Builder(this)
            .setTitle("Delete loan?")
            .setMessage(
                "Delete ${loan.name} and its EMI tracking?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->

                loans.removeAll { it.id == loan.id }
                saveAll()
                showTab("Loans")
            }
            .show()
    }

    private fun deleteRecord(record: Record) {
        AlertDialog.Builder(this)
            .setTitle("Delete record?")
            .setMessage(
                "Delete ${record.title} (${money(record.amount)})?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->

                records.removeAll { it.id == record.id }
                saveAll()
                showTab(currentTab)
            }
            .show()
    }

    private fun addHeading(text: String) {
        body.addView(
            TextView(this).apply {
                this.text = text
                textSize = 21f
                setTextColor(navy)
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(12))
            }
        )
    }

    private fun addText(text: String) {
        body.addView(
            TextView(this).apply {
                this.text = text
                textSize = 14f
                setTextColor(navy)
                setPadding(dp(4), dp(8), dp(4), dp(12))
            }
        )
    }

    private fun addSummaryCard(
        title: String,
        value: String,
        details: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 0, 0, dp(10))
        body.addView(card, params)

        card.addView(
            TextView(this).apply {
                text = title
                textSize = 15f
                setTextColor(navy)
                setTypeface(null, Typeface.BOLD)
            }
        )

        card.addView(
            TextView(this).apply {
                text = value
                textSize = 20f
                setTextColor(blue)
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(5), 0, dp(4))
            }
        )

        card.addView(
            TextView(this).apply {
                text = details
                textSize = 13f
                setTextColor(navy)
            }
        )
    }

    private fun actionButton(
        label: String,
        action: () -> Unit
    ) {
        val b = Button(this).apply {
            text = label
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(blue)
            setOnClickListener { action() }
        }

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 0, 0, dp(8))
        body.addView(b, params)
    }

    private fun smallAction(
        label: String,
        action: () -> Unit
    ) {
        val b = Button(this).apply {
            text = label
            setTextColor(red)
            setOnClickListener { action() }
        }

        body.addView(b, LinearLayout.LayoutParams(-1, -2))
    }

    private fun money(value: Double): String {
        return currency.format(value.roundToLong())
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    private fun saveAll() {
        val recordsJson = JSONArray()

        records.forEach { r ->
            recordsJson.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("kind", r.kind)
                    put("title", r.title)
                    put("category", r.category)
                    put("amount", r.amount)
                    put("date", r.date)
                    put("note", r.note)
                }
            )
        }

        val loansJson = JSONArray()

        loans.forEach { l ->
            loansJson.put(
                JSONObject().apply {
                    put("id", l.id)
                    put("name", l.name)
                    put("principal", l.principal)
                    put("annualRate", l.annualRate)
                    put("months", l.months)
                    put("method", l.method)
                    put("firstDueDate", l.firstDueDate)
                    put("paidInstallments", l.paidInstallments)
                    put("paidAmount", l.paidAmount)
                }
            )
        }

        getSharedPreferences(
            prefsName,
            Context.MODE_PRIVATE
        ).edit()
            .putString("records", recordsJson.toString())
            .putString("loans", loansJson.toString())
            .apply()
    }

    private fun loadAll() {
        val prefs = getSharedPreferences(
            prefsName,
            Context.MODE_PRIVATE
        )

        try {
            val r = JSONArray(
                prefs.getString("records", "[]")
            )

            for (i in 0 until r.length()) {
                val o = r.getJSONObject(i)

                records.add(
                    Record(
                        o.getLong("id"),
                        o.getString("kind"),
                        o.getString("title"),
                        o.getString("category"),
                        o.getDouble("amount"),
                        o.getLong("date"),
                        o.optString("note", "")
                    )
                )
            }

            val l = JSONArray(
                prefs.getString("loans", "[]")
            )

            for (i in 0 until l.length()) {
                val o = l.getJSONObject(i)

                loans.add(
                    Loan(
                        o.getLong("id"),
                        o.getString("name"),
                        o.getDouble("principal"),
                        o.getDouble("annualRate"),
                        o.getInt("months"),
                        o.getString("method"),
                        o.getLong("firstDueDate"),
                        o.optInt("paidInstallments", 0),
                        o.optDouble("paidAmount", 0.0)
                    )
                )
            }
        } catch (_: Exception) {
            records.clear()
            loans.clear()
        }
    }
}
