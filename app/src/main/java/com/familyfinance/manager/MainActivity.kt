package com.familyfinance.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

private val Navy = Color(0xFF102A56)
private val Blue = Color(0xFF2864DC)
private val Canvas = Color(0xFFF5F7FB)
private val Green = Color(0xFF16845B)
private val Red = Color(0xFFB42318)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = com.familyfinance.manager.data.FinanceDatabase.get(this).dao()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Canvas, surface = Color.White, secondary = Green)) {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) { delay(1800); showSplash = false }
                if (showSplash) BrandSplash() else FinanceShell(dao)
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
                        Icon(Icons.Default.Home, contentDescription = "Family Finance Manager logo", tint = Navy, modifier = Modifier.size(64.dp))
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

private enum class Screen(val title: String) { Dashboard("Overview"), Activity("Activity"), Events("Events"), Loans("Loans"), Manage("Manage") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun FinanceShell(dao: com.familyfinance.manager.data.FinanceDao) {
    val tx by dao.observeTransactions().collectAsStateWithLifecycle(emptyList())
    val events by dao.observeEvents().collectAsStateWithLifecycle(emptyList())
    val loans by dao.observeLoans().collectAsStateWithLifecycle(emptyList())
    val budgets by dao.observeBudgets().collectAsStateWithLifecycle(emptyList())
    val accounts by dao.observeAccounts().collectAsStateWithLifecycle(emptyList())
    val members by dao.observeMembers().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.Dashboard) }
    var transactionDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("Expense") }
    var form by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = Canvas, topBar = {
        TopAppBar(title = { Column { Text("Family Finance", fontWeight = FontWeight.Bold, color = Navy); Text("Your household, in control", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            actions = { IconButton(onClick = { selectedType = "Income"; transactionDialog = true }) { Icon(Icons.Default.AddCircle, "Add income", tint = Blue) } })
    }, bottomBar = {
        NavigationBar(containerColor = Color.White) {
            Screen.values().forEach { s ->
                NavigationBarItem(selected = screen == s, onClick = { screen = s },
                    icon = { Icon(when(s) { Screen.Dashboard -> Icons.Default.Dashboard; Screen.Activity -> Icons.Default.ReceiptLong; Screen.Events -> Icons.Default.Celebration; Screen.Loans -> Icons.Default.AccountBalance; Screen.Manage -> Icons.Default.Tune }, null) },
                    label = { Text(s.title) })
            }
        }
    }, floatingActionButton = {
        if (screen == Screen.Dashboard || screen == Screen.Activity) FloatingActionButton(onClick = { selectedType = "Expense"; transactionDialog = true }, containerColor = Blue, contentColor = Color.White) { Icon(Icons.Default.Add, "Add transaction") }
    }) { p ->
        when(screen) {
            Screen.Dashboard -> Dashboard(tx, events, loans, budgets, p, onAdd = { selectedType = it; transactionDialog = true })
            Screen.Activity -> ActivityScreen(tx, p, onDelete = { scope.launch { dao.deleteTransaction(it) } })
            Screen.Events -> EventsScreen(events, p, onAdd = { title, budget, actual, note -> scope.launch { dao.addEvent(com.familyfinance.manager.data.FamilyEvent(title=title, date=System.currentTimeMillis(), budget=budget, actual=actual, note=note)) } }, onDelete = { scope.launch { dao.deleteEvent(it) } })
            Screen.Loans -> LoansScreen(loans, p, onAdd = { title, principal, rate, months -> scope.launch { dao.addLoan(com.familyfinance.manager.data.FamilyLoan(title=title, principal=principal, annualInterestPercent=rate, tenureMonths=months)) } }, onDelete = { scope.launch { dao.deleteLoan(it) } })
            Screen.Manage -> ManageScreen(budgets, accounts, members, p, onBudget={c,a->scope.launch{dao.addBudget(com.familyfinance.manager.data.CategoryBudget(category=c,monthlyLimit=a))}}, onAccount={n,k,b->scope.launch{dao.addAccount(com.familyfinance.manager.data.FinanceAccount(name=n,kind=k,openingBalance=b))}}, onMember={n,r->scope.launch{dao.addMember(com.familyfinance.manager.data.FamilyMember(name=n,role=r))}}, onDeleteBudget={scope.launch{dao.deleteBudget(it)}}, onDeleteAccount={scope.launch{dao.deleteAccount(it)}}, onDeleteMember={scope.launch{dao.deleteMember(it)}})
        }
    }
    if(transactionDialog) TransactionForm(selectedType, members.map{it.name}, onDismiss={transactionDialog=false}) { type,title,category,amount,member,account,note ->
        scope.launch { dao.addTransaction(com.familyfinance.manager.data.FinanceTransaction(type=type,title=title,category=category,amount=amount,member=member,account=account,note=note)) }
        transactionDialog=false
    }
}

@Composable private fun Dashboard(tx: List<com.familyfinance.manager.data.FinanceTransaction>, events: List<com.familyfinance.manager.data.FamilyEvent>, loans: List<com.familyfinance.manager.data.FamilyLoan>, budgets: List<com.familyfinance.manager.data.CategoryBudget>, padding: PaddingValues, onAdd:(String)->Unit) {
    val income=tx.filter{it.type=="Income"}.sumOf{it.amount}; val expense=tx.filter{it.type=="Expense"}.sumOf{it.amount}
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
@Composable private fun ActivityScreen(items:List<com.familyfinance.manager.data.FinanceTransaction>,padding:PaddingValues,onDelete:(com.familyfinance.manager.data.FinanceTransaction)->Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { Text("Transactions",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text("Your income and spending history",color=Color.Gray); Spacer(Modifier.height(12.dp))
        if(items.isEmpty()) Empty("No activity yet.")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)) { items(items,key={it.id}) { item -> Card { Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.title,fontWeight=FontWeight.SemiBold); Text("${item.category} • ${item.member}",style=MaterialTheme.typography.bodySmall,color=Color.Gray); Text("${date(item.date)} • ${item.account}",style=MaterialTheme.typography.labelSmall,color=Color.Gray) }; Text((if(item.type=="Income") "+" else "−")+money(item.amount),color=if(item.type=="Income") Green else Red,fontWeight=FontWeight.Bold); IconButton(onClick={onDelete(item)}){Icon(Icons.Default.DeleteOutline,null)} } } } }
    }
}
@Composable private fun TransactionCard(t:com.familyfinance.manager.data.FinanceTransaction) { Card(shape=RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically) { Icon(if(t.type=="Income") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,null,tint=if(t.type=="Income")Green else Red); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)){Text(t.title,fontWeight=FontWeight.SemiBold);Text("${t.category} • ${date(t.date)}",style=MaterialTheme.typography.bodySmall,color=Color.Gray)};Text(money(t.amount),fontWeight=FontWeight.Bold,color=if(t.type=="Income")Green else Red) } } }

@Composable private fun EventsScreen(items:List<com.familyfinance.manager.data.FamilyEvent>,padding:PaddingValues,onAdd:(String,Double,Double,String)->Unit,onDelete:(com.familyfinance.manager.data.FamilyEvent)->Unit) {
    var show by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { Row(verticalAlignment=Alignment.CenterVertically){Text("Family events",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Button(onClick={show=true}){Icon(Icons.Default.Add,null);Text(" Add")}};Text("Plan celebrations and track actual spending.",color=Color.Gray);Spacer(Modifier.height(12.dp))
        if(items.isEmpty())Empty("Add weddings, birthdays, festivals or trips.")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(items,key={it.id}){e->Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(e.title,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick={onDelete(e)}){Icon(Icons.Default.DeleteOutline,null)}};Text("Budget ${money(e.budget)} • Spent ${money(e.actual)}");LinearProgressIndicator(progress={if(e.budget>0)(e.actual/e.budget).toFloat().coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth());Text("Remaining ${money(e.budget-e.actual)}",color=if(e.actual>e.budget)Red else Green);if(e.note.isNotBlank())Text(e.note)}}}}
    }
    if(show) FormDialog("New family event",listOf("Event name","Budget ₹","Actual spent ₹","Notes"),{show=false}){v->if(v[0].isNotBlank())onAdd(v[0],v[1].toDoubleOrNull()?:0.0,v[2].toDoubleOrNull()?:0.0,v[3]);show=false}
}
@Composable private fun LoansScreen(items:List<com.familyfinance.manager.data.FamilyLoan>,padding:PaddingValues,onAdd:(String,Double,Double,Int)->Unit,onDelete:(com.familyfinance.manager.data.FamilyLoan)->Unit) {
    var show by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Loans & EMI",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Button(onClick={show=true}){Icon(Icons.Default.Add,null);Text(" Add")}};Text("Estimated monthly repayment • reducing balance",color=Color.Gray);Spacer(Modifier.height(12.dp))
        if(items.isEmpty())Empty("Add a loan to see an indicative EMI estimate.")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(items,key={it.id}){l->val r=l.annualInterestPercent/1200.0;val emi=if(l.tenureMonths>0&&r>0)l.principal*r*(1+r).pow(l.tenureMonths)/((1+r).pow(l.tenureMonths)-1) else if(l.tenureMonths>0)l.principal/l.tenureMonths else 0.0;Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(l.title,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick={onDelete(l)}){Icon(Icons.Default.DeleteOutline,null)}};Text("Principal ${money(l.principal)}");Text("${l.annualInterestPercent}% p.a. • ${l.tenureMonths} months");Spacer(Modifier.height(6.dp));Text("Estimated EMI ${money(emi)}",style=MaterialTheme.typography.titleMedium,color=Blue,fontWeight=FontWeight.Bold);Text("Estimated total ${money(emi*l.tenureMonths)}",color=Color.Gray)}}}}
    }
    if(show)FormDialog("Add loan",listOf("Loan name","Principal ₹","Annual interest %","Tenure months"),{show=false}){v->val p=v[1].toDoubleOrNull()?:0.0;val r=v[2].toDoubleOrNull()?:0.0;val m=v[3].toIntOrNull()?:0;if(v[0].isNotBlank()&&p>0&&m>0)onAdd(v[0],p,r,m);show=false}
}
@Composable private fun ManageScreen(budgets:List<com.familyfinance.manager.data.CategoryBudget>,accounts:List<com.familyfinance.manager.data.FinanceAccount>,members:List<com.familyfinance.manager.data.FamilyMember>,padding:PaddingValues,onBudget:(String,Double)->Unit,onAccount:(String,String,Double)->Unit,onMember:(String,String)->Unit,onDeleteBudget:(com.familyfinance.manager.data.CategoryBudget)->Unit,onDeleteAccount:(com.familyfinance.manager.data.FinanceAccount)->Unit,onDeleteMember:(com.familyfinance.manager.data.FamilyMember)->Unit) {
    var form by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Family setup",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Budgets, accounts and household members",color=Color.Gray);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton(onClick={form="Budget"},modifier=Modifier.weight(1f)){Text("Budget")};OutlinedButton(onClick={form="Account"},modifier=Modifier.weight(1f)){Text("Account")};OutlinedButton(onClick={form="Member"},modifier=Modifier.weight(1f)){Text("Member")}}
        Text("Monthly budgets",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);if(budgets.isEmpty())Empty("No budget categories yet.");budgets.forEach{b->ManageRow(b.category,"Monthly limit ${money(b.monthlyLimit)}"){onDeleteBudget(b)}}
        Text("Accounts",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);if(accounts.isEmpty())Empty("Add Cash, Bank or UPI accounts.");accounts.forEach{a->ManageRow(a.name,"${a.kind} • Opening ${money(a.openingBalance)}"){onDeleteAccount(a)}}
        Text("Family members",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);if(members.isEmpty())Empty("Add names for transaction assignment.");members.forEach{m->ManageRow(m.name,m.role){onDeleteMember(m)}}
        Text("Data & security",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("This build stores data locally on this device. Login, encrypted cloud sync and remote backup are not enabled. Do not rely on it as your only financial record.",color=Color.Gray)
    }
    when(form){"Budget"->FormDialog("Add budget",listOf("Category","Monthly limit ₹"),{form=null}){v->if(v[0].isNotBlank())onBudget(v[0],v[1].toDoubleOrNull()?:0.0);form=null};"Account"->FormDialog("Add account",listOf("Account name","Type: Cash/Bank/UPI","Opening balance ₹"),{form=null}){v->if(v[0].isNotBlank())onAccount(v[0],v[1].ifBlank{"Cash"},v[2].toDoubleOrNull()?:0.0);form=null};"Member"->FormDialog("Add family member",listOf("Name","Role"),{form=null}){v->if(v[0].isNotBlank())onMember(v[0],v[1].ifBlank{"Family member"});form=null}}
}
@Composable private fun ManageRow(title:String,subtitle:String,onDelete:()->Unit){Card{Row(Modifier.fillMaxWidth().padding(start=12.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f).padding(vertical=10.dp)){Text(title,fontWeight=FontWeight.SemiBold);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=Color.Gray)};IconButton(onClick=onDelete){Icon(Icons.Default.DeleteOutline,null)}}}}
@Composable private fun TransactionForm(initial:String,members:List<String>,onDismiss:()->Unit,onSave:(String,String,String,Double,String,String,String)->Unit){
    var type by remember{mutableStateOf(initial)};var title by remember{mutableStateOf("")};var category by remember{mutableStateOf(if(initial=="Income")"Salary" else "Groceries")};var amount by remember{mutableStateOf("")};var member by remember{mutableStateOf(members.firstOrNull()?:"Family")};var account by remember{mutableStateOf("Cash")};var note by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Add transaction")},text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){Row{FilterChip(type=="Income",{type="Income";category="Salary"},{Text("Income")});Spacer(Modifier.width(8.dp));FilterChip(type=="Expense",{type="Expense";category="Groceries"},{Text("Expense")})};Field(title,{title=it},"Title");Field(category,{category=it},"Category");Field(amount,{amount=it},"Amount ₹");Field(member,{member=it},"Family member");Field(account,{account=it},"Account/method");Field(note,{note=it},"Notes (optional)")}},confirmButton={Button(enabled=title.isNotBlank()&&(amount.toDoubleOrNull()?:0.0)>0,onClick={onSave(type,title,category,amount.toDouble(),member,account,note)}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable private fun FormDialog(title:String,labels:List<String>,onDismiss:()->Unit,onSave:(List<String>)->Unit){val v=remember(title){labels.map{""}.toMutableStateList()};AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){labels.forEachIndexed{i,label->Field(v[i],{v[i]=it},label)}}},confirmButton={Button(onClick={onSave(v.toList())}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}
@Composable private fun Field(value:String,onValueChange:(String)->Unit,label:String){OutlinedTextField(value,onValueChange,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())}
@Composable private fun Empty(text:String){Text(text,Modifier.fillMaxWidth().padding(vertical=14.dp),color=Color.Gray)}
private fun money(v:Double)="₹"+String.format(Locale("en","IN"),"%,.2f",v)
private fun date(v:Long)=SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(Date(v))
