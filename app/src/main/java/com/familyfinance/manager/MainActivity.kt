package com.familyfinance.manager

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

private val blue = Color.rgb(25, 75, 150)
private val background = Color.rgb(245, 247, 251)

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window.statusBarColor = blue
    window.navigationBarColor = background
    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

    val scroll = ScrollView(this)
    scroll.setBackgroundColor(background)

    val root = LinearLayout(this)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(20, 24, 20, 24)

    scroll.addView(root)

    val title = TextView(this)
    title.text = "Family Finance Manager"
    title.textSize = 25f
    title.setTextColor(blue)
    title.gravity = Gravity.CENTER_VERTICAL
    root.addView(title)

    val subtitle = TextView(this)
    subtitle.text = "Your family's money, organized."
    subtitle.textSize = 14f
    subtitle.setTextColor(Color.DKGRAY)
    root.addView(subtitle)

    addHeading(root, "Financial Overview")

    addCard(root, "Monthly Income", "₹0")
    addCard(root, "Family Expenses", "₹0")
    addCard(root, "Outstanding Loans & Debts", "₹0")
    addCard(root, "Available Balance", "₹0")

    addHeading(root, "Upcoming Payments")

    addCard(
        root,
        "No payments added yet",
        "Add a payment to receive reminders."
    )

    addHeading(root, "Quick Actions")

    addCard(root, "Income", "Record money received")
    addCard(root, "Expenses", "Record family spending")
    addCard(root, "Loans & Debts", "Track repayments")
    addCard(root, "Payment Reminders", "View upcoming due dates")

    setContentView(scroll)
}

private fun addHeading(parent: LinearLayout, text: String) {
    val heading = TextView(this)
    heading.text = text
    heading.textSize = 19f
    heading.setTextColor(Color.rgb(35, 45, 65))
    heading.setPadding(0, 24, 0, 10)
    parent.addView(heading)
}

private fun addCard(
    parent: LinearLayout,
    title: String,
    value: String
) {
    val card = LinearLayout(this)
    card.orientation = LinearLayout.VERTICAL
    card.setPadding(18, 16, 18, 16)
    card.setBackgroundColor(Color.WHITE)

    val titleView = TextView(this)
    titleView.text = title
    titleView.textSize = 15f
    titleView.setTextColor(Color.DKGRAY)
    card.addView(titleView)

    val valueView = TextView(this)
    valueView.text = value
    valueView.textSize = 21f
    valueView.setTextColor(blue)
    valueView.setPadding(0, 8, 0, 0)
    card.addView(valueView)

    val params = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    params.bottomMargin = 12
    parent.addView(card, params)
}

}
