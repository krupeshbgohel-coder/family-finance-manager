package com.familyfinance.manager.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class FinanceTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val category: String,
    val amount: Double,
    val member: String,
    val account: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)
@Entity(tableName = "family_events")
data class FamilyEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val date: Long, val budget: Double, val actual: Double, val note: String = ""
)
@Entity(tableName = "loans")
data class FamilyLoan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val principal: Double, val annualInterestPercent: Double,
    val tenureMonths: Int, val startDate: Long = System.currentTimeMillis(), val emiPaidCount: Int = 0, val note: String = ""
)
@Entity(tableName = "budgets")
data class CategoryBudget(@PrimaryKey(autoGenerate = true) val id: Long = 0, val category: String, val monthlyLimit: Double)
@Entity(tableName = "accounts")
data class FinanceAccount(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val kind: String, val openingBalance: Double = 0.0)
@Entity(tableName = "family_members")
data class FamilyMember(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val role: String = "Family member")

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC") fun observeTransactions(): Flow<List<FinanceTransaction>>
    @Insert suspend fun addTransaction(item: FinanceTransaction)
    @Delete suspend fun deleteTransaction(item: FinanceTransaction)
    @Query("SELECT * FROM family_events ORDER BY date DESC") fun observeEvents(): Flow<List<FamilyEvent>>
    @Insert suspend fun addEvent(item: FamilyEvent)
    @Delete suspend fun deleteEvent(item: FamilyEvent)
    @Query("SELECT * FROM loans ORDER BY startDate DESC") fun observeLoans(): Flow<List<FamilyLoan>>
    @Insert suspend fun addLoan(item: FamilyLoan)
    @Delete suspend fun deleteLoan(item: FamilyLoan)
    @Query("SELECT * FROM budgets ORDER BY category") fun observeBudgets(): Flow<List<CategoryBudget>>
    @Insert suspend fun addBudget(item: CategoryBudget)
    @Delete suspend fun deleteBudget(item: CategoryBudget)
    @Query("SELECT * FROM accounts ORDER BY name") fun observeAccounts(): Flow<List<FinanceAccount>>
    @Insert suspend fun addAccount(item: FinanceAccount)
    @Delete suspend fun deleteAccount(item: FinanceAccount)
    @Query("SELECT * FROM family_members ORDER BY name") fun observeMembers(): Flow<List<FamilyMember>>
    @Insert suspend fun addMember(item: FamilyMember)
    @Delete suspend fun deleteMember(item: FamilyMember)
}
@Database(entities = [FinanceTransaction::class, FamilyEvent::class, FamilyLoan::class, CategoryBudget::class, FinanceAccount::class, FamilyMember::class], version = 1, exportSchema = false)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun dao(): FinanceDao
    companion object {
        @Volatile private var INSTANCE: FinanceDatabase? = null
        fun get(context: Context): FinanceDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, FinanceDatabase::class.java, "family_finance.db")
                .build().also { INSTANCE = it }
        }
    }
}
