package com.familyfinance.manager.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions", indices = [Index("date"), Index("category"), Index("type")])
data class FinanceTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val category: String,
    val amount: Double,
    val member: String,
    val account: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val eventId: Long? = null
)
@Entity(tableName = "family_events")
data class FamilyEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val date: Long, val budget: Double, val actual: Double,
    val note: String = "", val isActive: Boolean = true
)
@Entity(tableName = "loans")
data class FamilyLoan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val principal: Double, val annualInterestPercent: Double,
    val tenureMonths: Int, val startDate: Long = System.currentTimeMillis(),
    val emiPaidCount: Int = 0, val note: String = "",
    val interestType: String = "REDUCING", val direction: String = "BORROWED",
    val amountRepaid: Double = 0.0
)
@Entity(tableName = "budgets")
data class CategoryBudget(@PrimaryKey(autoGenerate = true) val id: Long = 0, val category: String, val monthlyLimit: Double)
@Entity(tableName = "accounts")
data class FinanceAccount(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val kind: String, val openingBalance: Double = 0.0)
@Entity(tableName = "family_members")
data class FamilyMember(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val role: String = "Family member")
@Entity(tableName = "categories", indices = [Index(value = ["name", "type"], unique = true)])
data class FinanceCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val isActive: Boolean = true
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC") fun observeTransactions(): Flow<List<FinanceTransaction>>
    @Insert suspend fun addTransaction(item: FinanceTransaction)
    @Update suspend fun updateTransaction(item: FinanceTransaction)
    @Delete suspend fun deleteTransaction(item: FinanceTransaction)
    @Query("SELECT * FROM family_events ORDER BY date DESC") fun observeEvents(): Flow<List<FamilyEvent>>
    @Insert suspend fun addEvent(item: FamilyEvent)
    @Update suspend fun updateEvent(item: FamilyEvent)
    @Delete suspend fun deleteEvent(item: FamilyEvent)
    @Query("SELECT * FROM loans ORDER BY startDate DESC") fun observeLoans(): Flow<List<FamilyLoan>>
    @Insert suspend fun addLoan(item: FamilyLoan)
    @Update suspend fun updateLoan(item: FamilyLoan)
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
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name") fun observeCategories(type: String): Flow<List<FinanceCategory>>
    @Query("SELECT * FROM categories ORDER BY type, name") fun observeAllCategories(): Flow<List<FinanceCategory>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addCategory(item: FinanceCategory)
    @Update suspend fun updateCategory(item: FinanceCategory)
    @Delete suspend fun deleteCategory(item: FinanceCategory)
}

@Database(entities = [FinanceTransaction::class, FamilyEvent::class, FamilyLoan::class, CategoryBudget::class, FinanceAccount::class, FamilyMember::class, FinanceCategory::class], version = 2, exportSchema = false)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun dao(): FinanceDao
    companion object {
        @Volatile private var INSTANCE: FinanceDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE family_events ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE loans ADD COLUMN interestType TEXT NOT NULL DEFAULT 'REDUCING'")
                db.execSQL("ALTER TABLE loans ADD COLUMN direction TEXT NOT NULL DEFAULT 'BORROWED'")
                db.execSQL("ALTER TABLE loans ADD COLUMN amountRepaid REAL NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, isActive INTEGER NOT NULL DEFAULT 1)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name_type ON categories(name, type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)")
            }
        }
        fun get(context: Context): FinanceDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, FinanceDatabase::class.java, "family_finance.db")
                .addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
