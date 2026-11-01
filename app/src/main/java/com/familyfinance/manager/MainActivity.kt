package com.familyfinance.manager

import android.os.Bundle
import android.app.DatePickerDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

private val Navy = Color(0xFF321414)
private val Blue = Color(0xFFC62828)
private val Canvas = Color(0xFFF5F7FB)
private val Green = Color(0xFF16845B)
private val Red = Color(0xFFB42318)

private enum class AppThemeMode(val label: String) { SYSTEM("System default"), LIGHT("Light mode"), DARK("Dark mode") }

private fun loadThemeMode(context: Context): AppThemeMode {
    val saved = context.getSharedPreferences("family_finance_settings", Context.MODE_PRIVATE)
        .getString("theme_mode", AppThemeMode.SYSTEM.name)
    return runCatching { AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name) }.getOrDefault(AppThemeMode.SYSTEM)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = com.familyfinance.manager.data.FinanceDatabase.get(this).dao()
        scheduleEmiReminders()
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 4201)
        }
        val firebaseApp = com.familyfinance.manager.cloud.FirebaseBootstrap.initialize(this)
        setContent {
            var cloudSignedIn by remember { mutableStateOf(firebaseApp == null || com.google.firebase.auth.FirebaseAuth.getInstance(firebaseApp).currentUser != null) }
            var themeMode by remember { mutableStateOf(loadThemeMode(this@MainActivity)) }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val appColors = if (darkTheme) darkColorScheme(
                primary = Color(0xFFFF8A80), onPrimary = Color(0xFF321414),
                secondary = Color(0xFF68D5A8), background = Color(0xFF101318),
                surface = Color(0xFF1B1F27), onSurface = Color(0xFFE5E7EB),
                onBackground = Color(0xFFE5E7EB), error = Color(0xFFFFB4AB)
            ) else lightColorScheme(primary = Blue, background = Canvas, surface = Color.White, secondary = Green)
            MaterialTheme(colorScheme = appColors) {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) { delay(1800); showSplash = false }
                if (showSplash) BrandSplash() else if (firebaseApp != null && !cloudSignedIn) {
                    com.familyfinance.manager.cloud.CloudAuthScreen(onSignedIn = { cloudSignedIn = true })
                } else FinanceShell(
                    dao = dao,
                    cloudEnabled = firebaseApp != null,
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        getSharedPreferences("family_finance_settings", Context.MODE_PRIVATE).edit().putString("theme_mode", mode.name).apply()
                    }
                )
            }
        }
    }
}

@Composable private fun BrandSplash() {
    Surface(modifier = Modifier.fillMaxSize(), color = Navy) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = RoundedCornerShape(30.dp), color = Color.White, modifier = Modifier.size(112.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.ic_family_finance_logo), contentDescription = "Family Finance Manager logo", modifier = Modifier.size(82.dp))
                    }
                }
                Text("Family Finance", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Your household, in control", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(18.dp))
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
    }
}

private enum class Screen(val title: String) { Dashboard("Overview"), Activity("Transactions"), Stats("Stats"), Accounts("Accounts"), More("More"), Calendar("Calendar"), Categories("Categories"), Events("Events"), Loans("Loans"), Manage("Manage") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun FinanceShell(dao: com.familyfinance.manager.data.FinanceDao, cloudEnabled: Boolean, themeMode: AppThemeMode, onThemeModeChange: (AppThemeMode) -> Unit) {
    val context = LocalContext.current
    val tx by dao.observeTransactions().collectAsStateWithLifecycle(emptyList())
    val events by dao.observeEvents().collectAsStateWithLifecycle(emptyList())
    val loans by dao.observeLoans().collectAsStateWithLifecycle(emptyList())
    val budgets by dao.observeBudgets().collectAsStateWithLifecycle(emptyList())
    val accounts by dao.observeAccounts().collectAsStateWithLifecycle(emptyList())
    val members by dao.observeMembers().collectAsStateWithLifecycle(emptyList())
    val categories by dao.observeAllCategories().collectAsStateWithLifecycle(emptyList())
    val activeAccounts = accounts.filter { it.isActive }
    LaunchedEffect(Unit) {
        listOf("Salary" to "Income", "Bonus" to "Income", "Interest" to "Income", "Other income" to "Income", "Groceries" to "Expense", "Kitchen" to "Expense", "Rent" to "Expense", "Transport" to "Expense", "Utilities" to "Expense", "Education" to "Expense", "Medical" to "Expense", "Shopping" to "Expense", "Other expense" to "Expense").forEach { (name, type) -> dao.addCategory(com.familyfinance.manager.data.FinanceCategory(name=name, type=type)) }
    }
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.Activity) }
    var transactionDialog by remember { mutableStateOf(false) }
    var cloudDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("Expense") }
    var editingTransaction by remember { mutableStateOf<com.familyfinance.manager.data.FinanceTransaction?>(null) }
    var form by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Column { Text("Family Finance", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text("Your household, in control", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            actions = { if (cloudEnabled) IconButton(onClick = { cloudDialog = true }) { Icon(Icons.Default.Cloud, "Family cloud setup", tint = Blue) }; IconButton(onClick = { selectedType = "Income"; editingTransaction = null; transactionDialog = true }) { Icon(Icons.Default.AddCircle, "Add income", tint = Blue) } })
    }, bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            listOf(Screen.Activity, Screen.Stats, Screen.Accounts, Screen.More).forEach { s ->
                NavigationBarItem(selected = screen == s, onClick = { screen = s },
                    icon = { Icon(when(s) { Screen.Dashboard -> Icons.Default.Dashboard; Screen.Activity -> Icons.Default.ReceiptLong; Screen.Stats -> Icons.Default.BarChart; Screen.Accounts -> Icons.Default.AccountBalanceWallet; Screen.More -> Icons.Default.MoreHoriz; Screen.Calendar -> Icons.Default.CalendarMonth; Screen.Categories -> Icons.Default.Category; Screen.Events -> Icons.Default.Celebration; Screen.Loans -> Icons.Default.AccountBalance; Screen.Manage -> Icons.Default.Tune }, null) },
                    label = { Text(s.title) })
            }
        }
    }, floatingActionButton = {
        if (screen == Screen.Dashboard || screen == Screen.Activity || screen == Screen.Stats) FloatingActionButton(onClick = { selectedType = "Expense"; editingTransaction = null; transactionDialog = true }, containerColor = Blue, contentColor = Color.White) { Icon(Icons.Default.Add, "Add transaction") }
    }) { p ->
        when(screen) {
            Screen.Dashboard -> Dashboard(tx, events, loans, budgets, p, onAdd = { selectedType = it; transactionDialog = true })
            Screen.Stats -> Dashboard(tx, events, loans, budgets, p, onAdd = { selectedType = it; transactionDialog = true })
            Screen.Accounts -> AccountsScreen(accounts, tx, p, onAdd={n,k,b->scope.launch{dao.addAccount(com.familyfinance.manager.data.FinanceAccount(name=n,kind=k,openingBalance=b))}}, onToggle={a->scope.launch{dao.updateAccount(a.copy(isActive=!a.isActive))}}, onDelete={a->scope.launch{dao.deleteAccount(a)}})
            Screen.More -> MoreScreen(p, onNavigate={screen=it})
            Screen.Activity -> ActivityScreen(tx, p, onEdit = { editingTransaction = it; selectedType = it.type; transactionDialog = true }, onDelete = { scope.launch { dao.deleteTransaction(it) } })
            Screen.Calendar -> CalendarScreen(tx, p)
            Screen.Categories -> CategoriesScreen(categories, p, onAdd={name,type,parent->scope.launch{dao.addCategory(com.familyfinance.manager.data.FinanceCategory(name=name,type=type,parentCategory=parent))}}, onUpdate={scope.launch{dao.updateCategory(it)}}, onDelete={scope.launch{dao.deleteCategory(it)}})
            Screen.Events -> EventsScreen(events, tx, p, onAdd = { title, budget, actual, note -> scope.launch { dao.addEvent(com.familyfinance.manager.data.FamilyEvent(title=title, date=System.currentTimeMillis(), budget=budget, actual=0.0, note=note)) } }, onDelete = { scope.launch { dao.deleteEvent(it) } }, onToggle={e->scope.launch{dao.updateEvent(e.copy(isActive=!e.isActive))}})
            Screen.Loans -> LoansScreen(loans, p, onAdd = { title, principal, rate, months, interestType, direction, dueDay -> scope.launch { dao.addLoan(com.familyfinance.manager.data.FamilyLoan(title=title, principal=principal, annualInterestPercent=rate, tenureMonths=months, interestType=interestType, direction=direction, emiDueDay=dueDay.coerceIn(1,31))) } }, onDelete = { scope.launch { dao.deleteLoan(it) } }, onMarkPaid = { loan -> scope.launch { val month = java.text.SimpleDateFormat("yyyy-MM", Locale.US).format(Date()); dao.updateLoan(loan.copy(lastPaidYearMonth=month, emiPaidCount=loan.emiPaidCount+1)) } })
            Screen.Manage -> ManageScreen(budgets, accounts, members, p, themeMode, onThemeModeChange, onBudget={c,a->scope.launch{dao.addBudget(com.familyfinance.manager.data.CategoryBudget(category=c,monthlyLimit=a))}}, onAccount={n,k,b->scope.launch{dao.addAccount(com.familyfinance.manager.data.FinanceAccount(name=n,kind=k,openingBalance=b))}}, onToggleAccount={a->scope.launch{dao.updateAccount(a.copy(isActive=!a.isActive))}}, onMember={n,r->scope.launch{dao.addMember(com.familyfinance.manager.data.FamilyMember(name=n,role=r))}}, onDeleteBudget={scope.launch{dao.deleteBudget(it)}}, onDeleteAccount={scope.launch{dao.deleteAccount(it)}}, onDeleteMember={scope.launch{dao.deleteMember(it)}})
        }
    }
    if (cloudDialog) com.familyfinance.manager.cloud.CloudFamilyAdminDialog(onDismiss = { cloudDialog = false })
    if(transactionDialog) TransactionForm(
        selectedType, members.map{it.name},
        categories.filter{it.isActive},
        events.filter{it.isActive}.map{it.id to it.title},
        activeAccounts.map { it.name },
        initialDate = context.getSharedPreferences("family_finance_settings", Context.MODE_PRIVATE).getLong("last_transaction_date", System.currentTimeMillis()),
        initialItem=editingTransaction, onDismiss={transactionDialog=false; editingTransaction=null}
    ) { type,title,category,subcategory,amount,member,account,note,eventId,transactionDate ->
        scope.launch {
            val old = editingTransaction
            if (old == null) dao.addTransaction(com.familyfinance.manager.data.FinanceTransaction(type=type,title=title,category=category,subcategory=subcategory,amount=amount,member=member,account=account,note=note,eventId=eventId,date=transactionDate))
            else dao.updateTransaction(old.copy(type=type,title=title,category=category,subcategory=subcategory,amount=amount,member=member,account=account,note=note,eventId=eventId,date=transactionDate))
            context.getSharedPreferences("family_finance_settings", Context.MODE_PRIVATE).edit().putLong("last_transaction_date", transactionDate).apply()
        }
        transactionDialog=false; editingTransaction=null
    }
}

@Composable private fun Dashboard(tx: List<com.familyfinance.manager.data.FinanceTransaction>, events: List<com.familyfinance.manager.data.FamilyEvent>, loans: List<com.familyfinance.manager.data.FamilyLoan>, budgets: List<com.familyfinance.manager.data.CategoryBudget>, padding: PaddingValues, onAdd:(String)->Unit) {
    val income=tx.filter{it.type=="Income"}.sumOf{it.amount}; val expense=tx.filter{it.type=="Expense"}.sumOf{it.amount}
    val cal=java.util.Calendar.getInstance(); val month=cal.get(java.util.Calendar.MONTH); val year=cal.get(java.util.Calendar.YEAR)
    val monthTx=tx.filter{val c=java.util.Calendar.getInstance().apply{timeInMillis=it.date};c.get(java.util.Calendar.MONTH)==month&&c.get(java.util.Calendar.YEAR)==year}
    val yearTx=tx.filter{java.util.Calendar.getInstance().apply{timeInMillis=it.date}.get(java.util.Calendar.YEAR)==year}
    val monthIncome=monthTx.filter{it.type=="Income"}.sumOf{it.amount}; val monthExpense=monthTx.filter{it.type=="Expense"}.sumOf{it.amount}
    val yearIncome=yearTx.filter{it.type=="Income"}.sumOf{it.amount}; val yearExpense=yearTx.filter{it.type=="Expense"}.sumOf{it.amount}
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Card(shape=RoundedCornerShape(26.dp), colors=CardDefaults.cardColors(containerColor=Navy)) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text("TOTAL CASH FLOW", color=Color.White.copy(alpha=.72f), style=MaterialTheme.typography.labelMedium)
                Text(money(income-expense), color=Color.White, style=MaterialTheme.typography.headlineLarge, fontWeight=FontWeight.Bold)
                Text("Recorded income minus recorded expenses", color=Color.White.copy(alpha=.72f), style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Button(onClick={onAdd("Income")}, modifier=Modifier.weight(1f), colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Navy)) { Icon(Icons.Default.ArrowDownward,null); Text(" Income") }
                    Button(onClick={onAdd("Expense")}, modifier=Modifier.weight(1f), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFE7E4),contentColor=Red)) { Icon(Icons.Default.ArrowUpward,null); Text(" Expense") }
                }
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { Metric("Income",money(income),Green,Modifier.weight(1f),Icons.Default.TrendingUp); Metric("Expenses",money(expense),Red,Modifier.weight(1f),Icons.Default.TrendingDown) }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { Metric("Events",events.size.toString(),Blue,Modifier.weight(1f),Icons.Default.Celebration); Metric("Loans",loans.size.toString(),Navy,Modifier.weight(1f),Icons.Default.AccountBalance) }
        Card(shape=RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Monthly analytics",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            Text("Income ${money(monthIncome)}",color=Green); LinearProgressIndicator(progress={if(maxOf(monthIncome,monthExpense)>0)(monthIncome/maxOf(monthIncome,monthExpense)).toFloat() else 0f},modifier=Modifier.fillMaxWidth(),color=Green)
            Text("Expenses ${money(monthExpense)}",color=Red); LinearProgressIndicator(progress={if(maxOf(monthIncome,monthExpense)>0)(monthExpense/maxOf(monthIncome,monthExpense)).toFloat() else 0f},modifier=Modifier.fillMaxWidth(),color=Red)
            Text("Year-to-date • Income ${money(yearIncome)} • Expenses ${money(yearExpense)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        Card(shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Budget vs Actual • This month", style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.Bold)
                if (budgets.isEmpty()) Text("Add monthly budgets from Manage to compare planned and actual expenses.", color=Color.Gray)
                budgets.forEach { b ->
                    val actual = monthTx.filter { it.type == "Expense" && it.category.equals(b.category, true) }.sumOf { it.amount }
                    val ratio = if (b.monthlyLimit > 0) (actual / b.monthlyLimit).toFloat() else 0f
                    Row(verticalAlignment=Alignment.CenterVertically) { Text(b.category, modifier=Modifier.weight(1f), fontWeight=FontWeight.SemiBold); Text("${money(actual)} / ${money(b.monthlyLimit)}", color=if(actual>b.monthlyLimit) Red else MaterialTheme.colorScheme.onSurface, style=MaterialTheme.typography.bodySmall) }
                    LinearProgressIndicator(progress={ratio.coerceIn(0f,1f)}, modifier=Modifier.fillMaxWidth(), color=if(actual>b.monthlyLimit) Red else Green)
                    Text(if(actual>b.monthlyLimit) "Over budget by ${money(actual-b.monthlyLimit)}" else "Remaining ${money(b.monthlyLimit-actual)}", color=if(actual>b.monthlyLimit) Red else Green, style=MaterialTheme.typography.labelSmall)
                }
            }
        }
        Card(shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Quick snapshot", style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.Bold)
                SnapshotLine(Icons.Default.Event, "Family events", "${events.size} tracked")
                SnapshotLine(Icons.Default.Savings, "Monthly budgets", "${budgets.size} categories")
                SnapshotLine(Icons.Default.ReceiptLong, "Transactions", "${tx.size} records")
                SnapshotLine(Icons.Default.CloudOff, "Backup status", "Local device only")
            }
        }
        Text("Recent activity", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold)
        if(tx.isEmpty()) Empty("Your dashboard is ready. Add your first income or expense.")
        tx.take(5).forEach { TransactionCard(it) }
        Text("Financial figures reflect only entries you have recorded. Loan estimates are illustrative.", style=MaterialTheme.typography.bodySmall, color=Color.Gray)
    }
}
@Composable private fun Metric(title:String,value:String,color:Color,modifier:Modifier,icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier, shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(15.dp)) { Icon(icon,null,tint=color); Spacer(Modifier.height(8.dp)); Text(title,style=MaterialTheme.typography.labelMedium,color=Color.Gray); Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=color) } }
}
@Composable private fun SnapshotLine(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,value:String) { Row(verticalAlignment=Alignment.CenterVertically) { Icon(icon,null,tint=Blue); Spacer(Modifier.width(10.dp)); Text(title,modifier=Modifier.weight(1f)); Text(value,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold) } }
@Composable private fun AccountsScreen(accounts:List<com.familyfinance.manager.data.FinanceAccount>, tx:List<com.familyfinance.manager.data.FinanceTransaction>, padding:PaddingValues, onAdd:(String,String,Double)->Unit, onToggle:(com.familyfinance.manager.data.FinanceAccount)->Unit, onDelete:(com.familyfinance.manager.data.FinanceAccount)->Unit) {
    var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) { Text("Accounts", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f)); Button(onClick={show=true}) { Icon(Icons.Default.Add,null); Text(" Add") } }
        Text("Manage cash, bank, savings and credit accounts.", color=Color.Gray); Spacer(Modifier.height(12.dp))
        if(accounts.isEmpty()) Empty("Add your first account.")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) { items(accounts,key={it.id}) { a ->
            val balance = a.openingBalance + tx.filter { it.account == a.name }.sumOf { if(it.type=="Income") it.amount else if(it.type=="Expense") -it.amount else 0.0 }
            Card(shape=RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(a.name,fontWeight=FontWeight.Bold); Text("${a.kind} • ${if(a.isActive)"Active" else "Inactive"}",color=Color.Gray,style=MaterialTheme.typography.bodySmall) }; Text(money(balance),fontWeight=FontWeight.Bold,color=if(balance<0)Red else Green) }
                Row(horizontalArrangement=Arrangement.End, modifier=Modifier.fillMaxWidth()) { TextButton(onClick={onToggle(a)}) { Text(if(a.isActive)"Deactivate" else "Activate") }; TextButton(onClick={onDelete(a)}) { Text("Delete") } }
            } }
        } }
    }
    if(show) FormDialog("Add account",listOf("Account name","Type: Cash/Bank/UPI/Savings/Credit Card","Opening balance ₹"),{show=false}) { v -> if(v[0].isNotBlank()) onAdd(v[0].trim(),v.getOrNull(1).orEmpty().ifBlank{"Cash"},v.getOrNull(2)?.toDoubleOrNull()?:0.0); show=false }
}

@Composable private fun MoreScreen(padding:PaddingValues, onNavigate:(Screen)->Unit) {
    val destinations = listOf(Screen.Events to Icons.Default.Celebration, Screen.Loans to Icons.Default.AccountBalance, Screen.Categories to Icons.Default.Category, Screen.Calendar to Icons.Default.CalendarMonth, Screen.Manage to Icons.Default.Settings)
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("More", style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Manage your household finance tools",color=Color.Gray); Spacer(Modifier.height(12.dp))
        destinations.forEach { (screen, icon) -> OutlinedButton(onClick={onNavigate(screen)}, modifier=Modifier.fillMaxWidth().padding(bottom=9.dp)) { Icon(icon,null,tint=Blue); Spacer(Modifier.width(12.dp)); Text(screen.title,modifier=Modifier.weight(1f)); Icon(Icons.Default.ChevronRight,null) } }
    }
}

@Composable private fun ActivityScreen(items:List<com.familyfinance.manager.data.FinanceTransaction>,padding:PaddingValues,onEdit:(com.familyfinance.manager.data.FinanceTransaction)->Unit,onDelete:(com.familyfinance.manager.data.FinanceTransaction)->Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { Text("Transactions",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text("Your income and spending history",color=Color.Gray); Spacer(Modifier.height(12.dp))
        if(items.isEmpty()) Empty("No activity yet.")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)) { items(items,key={it.id}) { item -> Card { Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.title,fontWeight=FontWeight.SemiBold); Text("${item.category} • ${item.member}",style=MaterialTheme.typography.bodySmall,color=Color.Gray); Text("${date(item.date)} • ${item.account}",style=MaterialTheme.typography.labelSmall,color=Color.Gray) }; Text((if(item.type=="Income") "+" else "−")+money(item.amount),color=if(item.type=="Income") Green else Red,fontWeight=FontWeight.Bold); IconButton(onClick={onEdit(item)}){Icon(Icons.Default.Edit,null)}; IconButton(onClick={onDelete(item)}){Icon(Icons.Default.DeleteOutline,null)} } } } }
    }
}
@Composable private fun TransactionCard(t:com.familyfinance.manager.data.FinanceTransaction) { Card(shape=RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically) { Icon(if(t.type=="Income") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,null,tint=if(t.type=="Income")Green else Red); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)){Text(t.title,fontWeight=FontWeight.SemiBold);Text("${t.category} • ${date(t.date)}",style=MaterialTheme.typography.bodySmall,color=Color.Gray)};Text(money(t.amount),fontWeight=FontWeight.Bold,color=if(t.type=="Income")Green else Red) } } }

@Composable private fun EventsScreen(items:List<com.familyfinance.manager.data.FamilyEvent>,transactions:List<com.familyfinance.manager.data.FinanceTransaction>,padding:PaddingValues,onAdd:(String,Double,Double,String)->Unit,onDelete:(com.familyfinance.manager.data.FamilyEvent)->Unit,onToggle:(com.familyfinance.manager.data.FamilyEvent)->Unit) {
    var show by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { Row(verticalAlignment=Alignment.CenterVertically){Text("Family events",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Button(onClick={show=true}){Icon(Icons.Default.Add,null);Text(" Add")}};Text("Plan celebrations and track actual spending.",color=Color.Gray);Spacer(Modifier.height(12.dp))
        if(items.isEmpty())Empty("Add weddings, birthdays, festivals or trips.")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(items,key={it.id}){e->Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(e.title,fontWeight=FontWeight.Bold);Text(if(e.isActive)"Active" else "Inactive",style=MaterialTheme.typography.labelSmall,color=if(e.isActive)Green else Red)};TextButton(onClick={onToggle(e)}){Text(if(e.isActive)"Deactivate" else "Activate")};IconButton(onClick={onDelete(e)}){Icon(Icons.Default.DeleteOutline,null)}};val spent = transactions.filter { it.eventId == e.id && it.type == "Expense" }.sumOf { it.amount }; Text("Budget ${money(e.budget)} • Spent ${money(spent)}");LinearProgressIndicator(progress={if(e.budget>0)(spent/e.budget).toFloat().coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth());Text("Remaining ${money(e.budget-spent)}",color=if(spent>e.budget)Red else Green);val eventTransactions = transactions.filter { it.eventId == e.id && it.type == "Expense" }.sortedByDescending { it.date };if(eventTransactions.isNotEmpty()){Spacer(Modifier.height(8.dp));Text("Event transactions",fontWeight=FontWeight.SemiBold);eventTransactions.forEach { t -> Text("• ${t.title} — ${date(t.date)} — ${money(t.amount)}",style=MaterialTheme.typography.bodySmall) }};if(e.note.isNotBlank())Text(e.note)}}}}
    }
    if(show) FormDialog("New family event",listOf("Event name","Budget ₹","Notes"),{show=false}){v->if(v[0].isNotBlank())onAdd(v[0],v[1].toDoubleOrNull()?:0.0,0.0,v[2]);show=false}
}
@Composable private fun LoansScreen(items:List<com.familyfinance.manager.data.FamilyLoan>,padding:PaddingValues,onAdd:(String,Double,Double,Int,String,String,Int)->Unit,onDelete:(com.familyfinance.manager.data.FamilyLoan)->Unit,onMarkPaid:(com.familyfinance.manager.data.FamilyLoan)->Unit) {
    var show by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Loans & EMI",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Button(onClick={show=true}){Icon(Icons.Default.Add,null);Text(" Add")}};Text("Estimated monthly repayment • reducing balance",color=Color.Gray);Spacer(Modifier.height(12.dp))
        if(items.isEmpty())Empty("Add a loan to see an indicative EMI estimate.")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(items,key={it.id}){l->val r=l.annualInterestPercent/1200.0;val emi=if(l.tenureMonths<=0)0.0 else if(l.interestType=="FLAT") (l.principal + l.principal*(l.annualInterestPercent/100.0)*(l.tenureMonths/12.0))/l.tenureMonths else if(r>0)l.principal*r*(1+r).pow(l.tenureMonths)/((1+r).pow(l.tenureMonths)-1) else l.principal/l.tenureMonths;Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(l.title,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick={onDelete(l)}){Icon(Icons.Default.DeleteOutline,null)}};Text("${if(l.direction=="LENT")"Money lent" else "Money borrowed"} • Principal ${money(l.principal)}");Text("${l.annualInterestPercent}% p.a. • ${l.tenureMonths} months • ${if(l.interestType=="FLAT")"Flat" else "Reducing balance"}");Text("EMI due date: ${l.emiDueDay} of every month", color=Color.Gray);Text("EMIs marked paid: ${l.emiPaidCount}", color=Color.Gray);Spacer(Modifier.height(6.dp));Text("Estimated EMI ${money(emi)}",style=MaterialTheme.typography.titleMedium,color=Blue,fontWeight=FontWeight.Bold);Text("Estimated total ${money(emi*l.tenureMonths)}",color=Color.Gray);Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={onMarkPaid(l)}, modifier=Modifier.weight(1f)){Text("Mark this month paid")};IconButton(onClick={onDelete(l)}){Icon(Icons.Default.DeleteOutline,"Delete loan")}}}}}}
    }
    if(show)FormDialog("Add loan",listOf("Loan name","Principal ₹","Annual interest % (0 for interest-free)","Tenure months","Interest type: REDUCING or FLAT","Direction: BORROWED or LENT","EMI due day (1-31)"),{show=false}){v->val p=v[1].toDoubleOrNull()?:0.0;val r=v[2].toDoubleOrNull()?:0.0;val m=v[3].toIntOrNull()?:0;val interest=if(v.getOrNull(4).orEmpty().trim().equals("flat",true))"FLAT" else "REDUCING";val direction=if(v.getOrNull(5).orEmpty().trim().equals("lent",true))"LENT" else "BORROWED";val dueDay=v.getOrNull(6)?.toIntOrNull()?:5;if(v[0].isNotBlank()&&p>0&&m>0)onAdd(v[0],p,r,m,interest,direction,dueDay);show=false}
}
@Composable private fun CalendarScreen(items:List<com.familyfinance.manager.data.FinanceTransaction>,padding:PaddingValues){
    var period by remember{mutableStateOf("Month")}; val now=java.util.Calendar.getInstance(); val start=java.util.Calendar.getInstance().apply{when(period){"Day"->{set(java.util.Calendar.HOUR_OF_DAY,0);set(java.util.Calendar.MINUTE,0);set(java.util.Calendar.SECOND,0);set(java.util.Calendar.MILLISECOND,0)};"Week"->{set(java.util.Calendar.DAY_OF_WEEK,firstDayOfWeek);set(java.util.Calendar.HOUR_OF_DAY,0);set(java.util.Calendar.MINUTE,0);set(java.util.Calendar.SECOND,0)};else->{set(java.util.Calendar.DAY_OF_MONTH,1);set(java.util.Calendar.HOUR_OF_DAY,0);set(java.util.Calendar.MINUTE,0);set(java.util.Calendar.SECOND,0)}}}.timeInMillis;val filtered=items.filter{it.date>=start && it.date<=now.timeInMillis};val expense=filtered.filter{it.type=="Expense"}.sumOf{it.amount};val income=filtered.filter{it.type=="Income"}.sumOf{it.amount}
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)){Text("Expense calendar",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(vertical=12.dp)){listOf("Day","Week","Month").forEach{FilterChip(selected=period==it,onClick={period=it},label={Text(it)})}};Card{Column(Modifier.fillMaxWidth().padding(16.dp)){Text("${period} summary",fontWeight=FontWeight.Bold);Text("Income ${money(income)}",color=Green);Text("Expenses ${money(expense)}",color=Red);Text("Net ${money(income-expense)}",fontWeight=FontWeight.Bold)}};Spacer(Modifier.height(12.dp));if(filtered.isEmpty())Empty("No transactions in this period.");LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(filtered,key={it.id}){TransactionCard(it)}}}
}
@Composable private fun CategoriesScreen(items:List<com.familyfinance.manager.data.FinanceCategory>,padding:PaddingValues,onAdd:(String,String,String)->Unit,onUpdate:(com.familyfinance.manager.data.FinanceCategory)->Unit,onDelete:(com.familyfinance.manager.data.FinanceCategory)->Unit){
    var show by remember{mutableStateOf(false)}; var editing by remember{mutableStateOf<com.familyfinance.manager.data.FinanceCategory?>(null)}
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Manage categories",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Button(onClick={show=true}){Icon(Icons.Default.Add,null);Text(" Add")}};Text("Add, edit, activate or delete categories used in transaction dropdowns.",color=Color.Gray);Spacer(Modifier.height(10.dp));items.forEach{c->Card(Modifier.padding(bottom=8.dp)){Row(Modifier.fillMaxWidth().padding(start=12.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f).padding(vertical=10.dp)){Text(c.name,fontWeight=FontWeight.SemiBold);Text(c.type + if(c.parentCategory.isNotBlank()) " • Subcategory of ${c.parentCategory}" else " • Main category",style=MaterialTheme.typography.bodySmall,color=Color.Gray)};TextButton(onClick={onUpdate(c.copy(isActive=!c.isActive))}){Text(if(c.isActive)"Active" else "Inactive")};IconButton(onClick={editing=c}){Icon(Icons.Default.Edit,null)};IconButton(onClick={onDelete(c)}){Icon(Icons.Default.DeleteOutline,null)}}}}
    }
    if(show)FormDialog("Add category",listOf("Category name","Type: Income or Expense","Parent category (optional; blank = main category)"),{show=false}){v->val t=if(v.getOrNull(1).orEmpty().trim().equals("income",true))"Income" else "Expense";if(v[0].isNotBlank())onAdd(v[0].trim(),t,v.getOrNull(2).orEmpty().trim());show=false}
    editing?.let{c->FormDialog("Edit category",listOf("Category name"),{editing=null}){v->if(v[0].isNotBlank())onUpdate(c.copy(name=v[0].trim()));editing=null}}
}
@Composable private fun ManageScreen(budgets:List<com.familyfinance.manager.data.CategoryBudget>,accounts:List<com.familyfinance.manager.data.FinanceAccount>,members:List<com.familyfinance.manager.data.FamilyMember>,padding:PaddingValues,themeMode:AppThemeMode,onThemeModeChange:(AppThemeMode)->Unit,onBudget:(String,Double)->Unit,onAccount:(String,String,Double)->Unit,onToggleAccount:(com.familyfinance.manager.data.FinanceAccount)->Unit,onMember:(String,String)->Unit,onDeleteBudget:(com.familyfinance.manager.data.CategoryBudget)->Unit,onDeleteAccount:(com.familyfinance.manager.data.FinanceAccount)->Unit,onDeleteMember:(com.familyfinance.manager.data.FamilyMember)->Unit) {
    var form by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Family setup",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Budgets, accounts and household members",color=Color.Gray);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton(onClick={form="Budget"},modifier=Modifier.weight(1f)){Text("Budget")};OutlinedButton(onClick={form="Account"},modifier=Modifier.weight(1f)){Text("Account")};OutlinedButton(onClick={form="Member"},modifier=Modifier.weight(1f)){Text("Member")}}
        Text("Monthly budgets",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);if(budgets.isEmpty())Empty("No budget categories yet.");budgets.forEach{b->ManageRow(b.category,"Monthly limit ${money(b.monthlyLimit)}"){onDeleteBudget(b)}}
        Text("Accounts",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);if(accounts.isEmpty())Empty("Add Cash, Bank or UPI accounts.");accounts.forEach{a->Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(a.name,fontWeight=FontWeight.SemiBold);Text("${a.kind} • Opening ${money(a.openingBalance)} • ${if(a.isActive)"Active" else "Inactive"}",style=MaterialTheme.typography.bodySmall,color=Color.Gray)};TextButton(onClick={onToggleAccount(a)}){Text(if(a.isActive)"Deactivate" else "Activate")};IconButton(onClick={onDeleteAccount(a)}){Icon(Icons.Default.DeleteOutline,null)}}}
        Text("Family members",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);if(members.isEmpty())Empty("Add names for transaction assignment.");members.forEach{m->ManageRow(m.name,m.role){onDeleteMember(m)}}
        Text("Appearance",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Text("Choose how the app looks. System default follows your phone's light/dark setting.", color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp), modifier=Modifier.fillMaxWidth()) {
            AppThemeMode.values().forEach { mode ->
                FilterChip(selected = themeMode == mode, onClick = { onThemeModeChange(mode) }, label = { Text(when(mode) { AppThemeMode.SYSTEM -> "System"; AppThemeMode.LIGHT -> "Light"; AppThemeMode.DARK -> "Dark" }) }, modifier = Modifier.weight(1f))
            }
        }
        Text("Data & security",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("This build stores data locally on this device. Login, encrypted cloud sync and remote backup are not enabled. Do not rely on it as your only financial record.",color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    when(form){"Budget"->FormDialog("Add budget",listOf("Category","Monthly limit ₹"),{form=null}){v->if(v[0].isNotBlank())onBudget(v[0],v[1].toDoubleOrNull()?:0.0);form=null};"Account"->FormDialog("Add account",listOf("Account name","Type: Cash/Bank/UPI","Opening balance ₹"),{form=null}){v->if(v[0].isNotBlank())onAccount(v[0],v[1].ifBlank{"Cash"},v[2].toDoubleOrNull()?:0.0);form=null};"Member"->FormDialog("Add family member",listOf("Name","Role"),{form=null}){v->if(v[0].isNotBlank())onMember(v[0],v[1].ifBlank{"Family member"});form=null}}
}
@Composable private fun ManageRow(title:String,subtitle:String,onDelete:()->Unit){Card{Row(Modifier.fillMaxWidth().padding(start=12.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f).padding(vertical=10.dp)){Text(title,fontWeight=FontWeight.SemiBold);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=Color.Gray)};IconButton(onClick=onDelete){Icon(Icons.Default.DeleteOutline,null)}}}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TransactionForm(
    initial:String,
    members:List<String>,
    categories:List<com.familyfinance.manager.data.FinanceCategory>,
    events:List<Pair<Long,String>>,
    accounts:List<String>,
    initialDate:Long,
    initialItem:com.familyfinance.manager.data.FinanceTransaction?=null,
    onDismiss:()->Unit,
    onSave:(String,String,String,String,Double,String,String,String,Long?,Long)->Unit
){
    var type by remember(initialItem?.id){mutableStateOf(initialItem?.type?:initial)}
    var title by remember(initialItem?.id){mutableStateOf(initialItem?.title?:"")}
    var category by remember(initialItem?.id){mutableStateOf(initialItem?.category?:categories.firstOrNull{it.type==initial && it.parentCategory.isBlank()}?.name?:if(initial=="Income")"Salary" else "Groceries")}
    var subcategory by remember(initialItem?.id){mutableStateOf(initialItem?.subcategory?:"")}
    var amount by remember(initialItem?.id){mutableStateOf(initialItem?.amount?.toString()?:"")}
    var member by remember(initialItem?.id){mutableStateOf(initialItem?.member?:members.firstOrNull()?:"Family")}
    var account by remember(initialItem?.id){mutableStateOf(initialItem?.account?:accounts.firstOrNull()?:"Cash")}
    var note by remember(initialItem?.id){mutableStateOf(initialItem?.note?:"")}
    var selectedDate by remember(initialItem?.id){mutableLongStateOf(initialItem?.date?:initialDate)}
    var categoryExpanded by remember{mutableStateOf(false)}
    var subcategoryExpanded by remember{mutableStateOf(false)}
    var memberExpanded by remember{mutableStateOf(false)}
    var accountExpanded by remember{mutableStateOf(false)}
    var eventId by remember(initialItem?.id){mutableStateOf(initialItem?.eventId)}
    val context = androidx.compose.ui.platform.LocalContext.current
    val categoryOptions = remember(categories, events, type) {
        categories.filter { it.type == type && it.parentCategory.isBlank() }.map { it.name to (null as Long?) } +
            if (type == "Expense") events.map { it.second to it.first } else emptyList()
    }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(if(initialItem==null)"Add transaction" else "Edit transaction")},
        text={Column(Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row { FilterChip(type=="Income",{type="Income";eventId=null;category=categories.firstOrNull{it.type=="Income" && it.parentCategory.isBlank()}?.name?:"Salary"},{Text("Income")});Spacer(Modifier.width(8.dp));FilterChip(type=="Expense",{type="Expense";category=categories.firstOrNull{it.type=="Expense" && it.parentCategory.isBlank()}?.name?:"Groceries"},{Text("Expense")}) }
            Field(title,{title=it},"Title")
            ExposedDropdownMenuBox(expanded=categoryExpanded,onExpandedChange={categoryExpanded=it}) {
                OutlinedTextField(value=category,onValueChange={},readOnly=true,label={Text("Category / Active Event")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded)},modifier=Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded=categoryExpanded,onDismissRequest={categoryExpanded=false}) { categoryOptions.forEach { (name, linkedEventId) -> DropdownMenuItem(text={Text(if(linkedEventId!=null) "🎉 $name" else name)},onClick={category=name;subcategory="";eventId=linkedEventId;categoryExpanded=false}) } }
            }
            val subcategoryOptions = categories.filter { it.isActive && it.type == type && it.parentCategory == category }.map { it.name }
            if (subcategoryOptions.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded=subcategoryExpanded,onExpandedChange={subcategoryExpanded=it}) {
                    OutlinedTextField(value=subcategory,onValueChange={},readOnly=true,label={Text("Subcategory")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(subcategoryExpanded)},modifier=Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded=subcategoryExpanded,onDismissRequest={subcategoryExpanded=false}) {
                        DropdownMenuItem(text={Text("None")},onClick={subcategory="";subcategoryExpanded=false})
                        subcategoryOptions.forEach { sub -> DropdownMenuItem(text={Text(sub)},onClick={subcategory=sub;subcategoryExpanded=false}) }
                    }
                }
            } else Field(subcategory,{subcategory=it},"Subcategory (optional)")
            Field(amount,{amount=it},"Amount ₹")
            ExposedDropdownMenuBox(expanded=memberExpanded,onExpandedChange={memberExpanded=it}) {
                OutlinedTextField(value=member,onValueChange={},readOnly=true,label={Text("Family member")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(memberExpanded)},modifier=Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded=memberExpanded,onDismissRequest={memberExpanded=false}) {(members.ifEmpty{listOf("Family")}).forEach{m->DropdownMenuItem(text={Text(m)},onClick={member=m;memberExpanded=false})}}
            }
            ExposedDropdownMenuBox(expanded=accountExpanded,onExpandedChange={accountExpanded=it}) {
                OutlinedTextField(value=account,onValueChange={},readOnly=true,label={Text("Account")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded)},modifier=Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded=accountExpanded,onDismissRequest={accountExpanded=false}) {
                    (accounts.ifEmpty{listOf("Cash")}).forEach { a -> DropdownMenuItem(text={Text(a)},onClick={account=a;accountExpanded=false}) }
                }
            }
            OutlinedButton(onClick={
                val c=java.util.Calendar.getInstance().apply{timeInMillis=selectedDate}
                DatePickerDialog(context,
                    { _, year, month, day -> selectedDate=java.util.Calendar.getInstance().apply{set(year,month,day,12,0,0);set(java.util.Calendar.MILLISECOND,0)}.timeInMillis },
                    c.get(java.util.Calendar.YEAR),c.get(java.util.Calendar.MONTH),c.get(java.util.Calendar.DAY_OF_MONTH)
                ).show()
            },modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.CalendarMonth,null);Spacer(Modifier.width(8.dp));Text("Date: ${date(selectedDate)} • Tap to change") }
            Field(note,{note=it},"Notes (optional)")
        }},
        confirmButton={Button(enabled=title.isNotBlank()&&(amount.toDoubleOrNull()?:0.0)>0,onClick={onSave(type,title,category,subcategory,amount.toDouble(),member,account,note,eventId,selectedDate)}){Text(if(initialItem==null)"Save" else "Update")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}}
    )
}
@Composable private fun FormDialog(title:String,labels:List<String>,onDismiss:()->Unit,onSave:(List<String>)->Unit){val v=remember(title){labels.map{""}.toMutableStateList()};AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){labels.forEachIndexed{i,label->Field(v[i],{v[i]=it},label)}}},confirmButton={Button(onClick={onSave(v.toList())}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}
@Composable private fun Field(value:String,onValueChange:(String)->Unit,label:String){OutlinedTextField(value,onValueChange,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())}
@Composable private fun Empty(text:String){Text(text,Modifier.fillMaxWidth().padding(vertical=14.dp),color=Color.Gray)}
private fun money(v:Double)="₹"+String.format(Locale("en","IN"),"%,.2f",v)
private fun date(v:Long)=SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(Date(v))

private fun android.content.Context.scheduleEmiReminders() {
    val request = androidx.work.PeriodicWorkRequestBuilder<com.familyfinance.manager.workers.EmiReminderWorker>(24, java.util.concurrent.TimeUnit.HOURS).build()
    androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork("family_finance_emi_reminders", androidx.work.ExistingPeriodicWorkPolicy.UPDATE, request)
}
