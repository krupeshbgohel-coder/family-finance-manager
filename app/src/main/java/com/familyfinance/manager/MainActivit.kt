package com.familyfinance.manager

import android.app.DatePickerDialog
import android.app.AlertDialog
import android.os.Bundle
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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

class MainActivity : android.app.Activity() {
    private val prefsName = "family_finance_manager"
    private val dataKey = "loans_stage2"
    private val loans = mutableListOf<Loan>()
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var summary: TextView
    private val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val backgroundColorValue = 0xFFF4F7FB.toInt()
    private val blue = 0xFF1957A6.toInt()
    private val dark = 0xFF172B4D.toInt()
    private val green = 0xFF16845B.toInt()
    private val red = 0xFFC0392B.toInt()

    data class Loan(
        val id: Long,
        val name: String,
        val principal: Double,
        val annualRate: Double,
        val months: Int,
        val type: String,
        val startDate: Long,
        var paidInstallments: Int = 0,
        var paidAmount: Double = 0.0
    ) {
        fun emi(): Double {
            val r = annualRate / 1200.0
            if (months <= 0) return 0.0
            return if (type == "Flat") {
                (principal + principal * annualRate / 100.0 * months / 12.0) / months
            } else if (r == 0.0) {
                principal / months
            } else {
                val factor = (1 + r).pow(months)
                principal * r * factor / (factor - 1)
            }
        }
        fun totalPayable(): Double = emi() * months
        fun totalInterest(): Double = totalPayable() - principal
        fun remainingPayments(): Int = (months - paidInstallments).coerceAtLeast(0)
        fun remainingAmount(): Double = (totalPayable() - paidAmount).coerceAtLeast(0.0)
        fun nextDueDate(): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = startDate
            c.add(Calendar.MONTH, paidInstallments + 1)
            return c.timeInMillis
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLoans()
        buildUi()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColorValue)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(18))
            setBackgroundColor(blue)
        }
        header.addView(TextView(this).apply {
            text = "Family Finance Manager"
            textSize = 23f
            setTextColor(-1)
            setTypeface(null, Typeface.BOLD)
        })
        header.addView(TextView(this).apply {
            text = "Loan & EMI Management"
            textSize = 15f
            setTextColor(0xFFE2ECFF.toInt())
            setPadding(0, dp(5), 0, 0)
        })
        root.addView(header)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(28))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        addButton(content, "+ Add New Loan", blue) { showAddLoanDialog() }
        summary = TextView(this).apply {
            textSize = 15f
            setTextColor(dark)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(-1)
        }
        val summaryParams = LinearLayout.LayoutParams(-1, -2)
        summaryParams.setMargins(0, dp(12), 0, dp(12))
        content.addView(summary, summaryParams)
        refreshLoanList()
    }

    private fun refreshLoanList() {
        while (content.childCount > 2) content.removeViewAt(2)
        val totalPrincipal = loans.sumOf { it.principal }
        val totalRemaining = loans.sumOf { it.remainingAmount() }
        val totalEmi = loans.sumOf { if (it.remainingPayments() > 0) it.emi() else 0.0 }
        summary.text = "TOTAL LOANS: ${loans.size}\nOriginal loan amount: ${money(totalPrincipal)}\nOutstanding payable: ${money(totalRemaining)}\nMonthly EMI (active loans): ${money(totalEmi)}"
        if (loans.isEmpty()) {
            content.addView(TextView(this).apply {
                text = "No loans added yet.\nTap “+ Add New Loan” to calculate and track your first EMI."
                textSize = 16f
                setTextColor(dark)
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(32), dp(18), dp(32))
            })
            return
        }
        loans.forEach { loan ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                setBackgroundColor(-1)
            }
            val params = LinearLayout.LayoutParams(-1, -2)
            params.setMargins(0, 0, 0, dp(12))
            content.addView(card, params)
            card.addView(TextView(this).apply {
                text = loan.name
                textSize = 19f
                setTextColor(dark)
                setTypeface(null, Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                text = "${loan.type} interest • ${loan.annualRate}% p.a. • ${loan.months} months"
                textSize = 13f
                setTextColor(0xFF65758B.toInt())
                setPadding(0, dp(4), 0, dp(10))
            })
            card.addView(detail("Loan amount", money(loan.principal)))
            card.addView(detail("Monthly EMI", money(loan.emi())))
            card.addView(detail("Total interest", money(loan.totalInterest())))
            card.addView(detail("Outstanding payable", money(loan.remainingAmount())))
            card.addView(detail("Installments paid", "${loan.paidInstallments} / ${loan.months}"))
            card.addView(detail("Next EMI due", if (loan.remainingPayments() > 0) dateFormat.format(Date(loan.nextDueDate())) else "Loan completed"))
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            card.addView(actions)
            val payButton = Button(this).apply {
                text = "Record EMI Paid"
                isEnabled = loan.remainingPayments() > 0
                setOnClickListener { confirmPayment(loan) }
            }
            actions.addView(payButton, LinearLayout.LayoutParams(0, -2, 1f))
            val deleteButton = Button(this).apply {
                text = "Delete"
                setOnClickListener { confirmDelete(loan) }
            }
            actions.addView(deleteButton, LinearLayout.LayoutParams(0, -2, 0.7f))
        }
    }

    private fun showAddLoanDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(4))
        }
        val name = EditText(this).apply { hint = "Loan name (e.g. Personal Loan)" }
        val amount = EditText(this).apply {
            hint = "Loan amount (₹)"
            inputType = 8194
        }
        val rate = EditText(this).apply {
            hint = "Annual interest rate (%)"
            inputType = 8194
        }
        val months = EditText(this).apply {
            hint = "Tenure in months"
            inputType = 2
        }
        val type = Spinner(this)
        type.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Reducing Balance", "Flat Interest"))
        val startDate = Button(this).apply { text = "Select first EMI date" }
        var selectedDate = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, 1) }
        startDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate.set(y, m, d)
                startDate.text = "First EMI date: ${dateFormat.format(selectedDate.time)}"
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }
        box.addView(name); box.addView(amount); box.addView(rate); box.addView(months)
        box.addView(TextView(this).apply {
            text = "Interest calculation method"
            setPadding(0, dp(8), 0, dp(3))
        })
        box.addView(type); box.addView(startDate)
        AlertDialog.Builder(this)
            .setTitle("Add New Loan")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Calculate & Save", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val n = name.text.toString().trim()
                        val p = amount.text.toString().toDoubleOrNull()
                        val r = rate.text.toString().toDoubleOrNull()
                        val m = months.text.toString().toIntOrNull()
                        if (n.isBlank() || p == null || p <= 0 || r == null || r < 0 || m == null || m !in 1..600) {
                            Toast.makeText(this, "Enter a loan name, amount > 0, rate ≥ 0, and tenure 1–600 months.", Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        val kind = if (type.selectedItemPosition == 0) "Reducing Balance" else "Flat"
                        val loan = Loan(System.currentTimeMillis(), n, p, r, m, kind, selectedDate.timeInMillis)
                        loans.add(0, loan)
                        saveLoans()
                        refreshLoanList()
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }

    private fun confirmPayment(loan: Loan) {
        if (loan.remainingPayments() <= 0) return
        AlertDialog.Builder(this)
            .setTitle("Record EMI payment")
            .setMessage("Record ${money(loan.emi())} as paid for “${loan.name}”?\nThis will mark installment ${loan.paidInstallments + 1} of ${loan.months} as paid.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm payment") { _, _ ->
                loan.paidInstallments += 1
                loan.paidAmount = (loan.paidAmount + loan.emi()).coerceAtMost(loan.totalPayable())
                saveLoans()
                refreshLoanList()
                Toast.makeText(this, "EMI payment recorded.", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun confirmDelete(loan: Loan) {
        AlertDialog.Builder(this)
            .setTitle("Delete loan?")
            .setMessage("Remove “${loan.name}” and its payment tracking from this app?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                loans.removeAll { it.id == loan.id }
                saveLoans()
                refreshLoanList()
            }.show()
    }

    private fun detail(label: String, value: String): TextView = TextView(this).apply {
        text = "$label: $value"
        textSize = 14f
        setTextColor(dark)
        setPadding(0, dp(3), 0, dp(3))
    }

    private fun addButton(parent: LinearLayout, label: String, color: Int, action: () -> Unit) {
        val button = Button(this).apply {
            text = label
            setTextColor(-1)
            setBackgroundColor(color)
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(-1, -2))
    }

    private fun money(value: Double): String = currency.format(value.roundToLong())
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun saveLoans() {
        val array = JSONArray()
        loans.forEach { loan ->
            array.put(JSONObject().apply {
                put("id", loan.id); put("name", loan.name); put("principal", loan.principal)
                put("annualRate", loan.annualRate); put("months", loan.months); put("type", loan.type)
                put("startDate", loan.startDate); put("paidInstallments", loan.paidInstallments)
                put("paidAmount", loan.paidAmount)
            })
        }
        getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().putString(dataKey, array.toString()).apply()
    }

    private fun loadLoans() {
        val raw = getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString(dataKey, null) ?: return
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                loans.add(Loan(
                    o.getLong("id"), o.getString("name"), o.getDouble("principal"),
                    o.getDouble("annualRate"), o.getInt("months"), o.getString("type"),
                    o.getLong("startDate"), o.optInt("paidInstallments", 0), o.optDouble("paidAmount", 0.0)
                ))
            }
        } catch (_: Exception) {
            loans.clear()
        }
    }
}
