package com.familyfinance.manager.cloud

import com.familyfinance.manager.data.FinanceDao
import com.familyfinance.manager.data.SyncIdentityHelper

/**

* Uploads local Room records to the selected family's Firestore collections.

* 

* This implementation is upload-only.

* It does not download remote data or propagate local deletions.
  */
  class FinanceSyncRepository(
  private val dao: FinanceDao,
  private val cloud: FamilyCloudRepository = FamilyCloudRepository(),
  private val syncManager: SyncManager = SyncManager(),
  private val identityHelper: SyncIdentityHelper =
  SyncIdentityHelper(dao)
  ) {
  
  data class UploadResult(
  val transactions: Int,
  val categories: Int,
  val events: Int,
  val loans: Int,
  val budgets: Int,
  val accounts: Int
  ) {
  val totalUploaded: Int
  get() = transactions + categories + events +
  loans + budgets + accounts
  }
  
  suspend fun uploadAll(familyId: String): UploadResult {
  require(familyId.isNotBlank()) {
  "Family ID cannot be blank."
  }
  
   check(cloud.currentUid() != null) {
     "Please sign in before syncing."
 }

 var result: UploadResult? = null

 syncManager.runSync {
     result = uploadOnce(familyId)
 }

 return requireNotNull(result)
  
  }
  
  private suspend fun uploadOnce(
  familyId: String
  ): UploadResult {
  var transactionCount = 0
  var categoryCount = 0
  var eventCount = 0
  var loanCount = 0
  var budgetCount = 0
  var accountCount = 0
  
   // Upload transactions.
 for (item in dao.getAllTransactions()) {
     val identity = identityHelper.getOrCreate(
         collection = "transactions",
         localId = item.id,
         familyId = familyId
     )

     cloud.writeRecord(
         familyId = familyId,
         collection = "transactions",
         recordId = identity.syncId,
         data = mapOf(
             "localId" to item.id,
             "type" to item.type,
             "title" to item.title,
             "category" to item.category,
             "amount" to item.amount,
             "member" to item.member,
             "account" to item.account,
             "date" to item.date,
             "note" to item.note,
             "eventId" to item.eventId,
             "subcategory" to item.subcategory,
             "isDeleted" to false
         )
     )
     transactionCount++
 }

 // Upload categories.
 for (item in dao.getAllCategories()) {
     val identity = identityHelper.getOrCreate(
         collection = "categories",
         localId = item.id,
         familyId = familyId
     )

     cloud.writeRecord(
         familyId = familyId,
         collection = "categories",
         recordId = identity.syncId,
         data = mapOf(
             "localId" to item.id,
             "name" to item.name,
             "type" to item.type,
             "isActive" to item.isActive,
             "parentCategory" to item.parentCategory,
             "isDeleted" to false
         )
     )
     categoryCount++
 }

 // Upload family events.
 for (item in dao.getAllEvents()) {
     val identity = identityHelper.getOrCreate(
         collection = "events",
         localId = item.id,
         familyId = familyId
     )

     cloud.writeRecord(
         familyId = familyId,
         collection = "events",
         recordId = identity.syncId,
         data = mapOf(
             "localId" to item.id,
             "title" to item.title,
             "date" to item.date,
             "budget" to item.budget,
             "actual" to item.actual,
             "note" to item.note,
             "isActive" to item.isActive,
             "isDeleted" to false
         )
     )
     eventCount++
 }

 // Upload loans.
 for (item in dao.getAllLoans()) {
     val identity = identityHelper.getOrCreate(
         collection = "loans",
         localId = item.id,
         familyId = familyId
     )

     cloud.writeRecord(
         familyId = familyId,
         collection = "loans",
         recordId = identity.syncId,
         data = mapOf(
             "localId" to item.id,
             "title" to item.title,
             "principal" to item.principal,
             "annualInterestPercent" to
                 item.annualInterestPercent,
             "tenureMonths" to item.tenureMonths,
             "startDate" to item.startDate,
             "emiPaidCount" to item.emiPaidCount,
             "note" to item.note,
             "interestType" to item.interestType,
             "direction" to item.direction,
             "amountRepaid" to item.amountRepaid,
             "emiDueDay" to item.emiDueDay,
             "lastPaidYearMonth" to
                 item.lastPaidYearMonth,
             "isDeleted" to false
         )
     )
     loanCount++
 }

 // Upload budgets.
 for (item in dao.getAllBudgets()) {
     val identity = identityHelper.getOrCreate(
         collection = "budgets",
         localId = item.id,
         familyId = familyId
     )

     cloud.writeRecord(
         familyId = familyId,
         collection = "budgets",
         recordId = identity.syncId,
         data = mapOf(
             "localId" to item.id,
             "category" to item.category,
             "monthlyLimit" to item.monthlyLimit,
             "isDeleted" to false
         )
     )
     budgetCount++
 }

 // Upload accounts.
 for (item in dao.getAllAccounts()) {
     val identity = identityHelper.getOrCreate(
         collection = "accounts",
         localId = item.id,
         familyId = familyId
     )

     cloud.writeRecord(
         familyId = familyId,
         collection = "accounts",
         recordId = identity.syncId,
         data = mapOf(
             "localId" to item.id,
             "name" to item.name,
             "kind" to item.kind,
             "openingBalance" to item.openingBalance,
             "isActive" to item.isActive,
             "isDeleted" to false
         )
     )
     accountCount++
 }

 return UploadResult(
     transactions = transactionCount,
     categories = categoryCount,
     events = eventCount,
     loans = loanCount,
     budgets = budgetCount,
     accounts = accountCount
 )
  
  }
  }
