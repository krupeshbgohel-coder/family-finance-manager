package com.familyfinance.manager.data

/**

* Provides a consistent way to find or create a cloud identity

* for a local Room record.

* 

* This helper prepares record identity only.

* It does not upload or download data.
  */
  class SyncIdentityHelper(
  private val dao: FinanceDao
  ) {
  
  suspend fun getOrCreate(
  collection: String,
  localId: Long,
  familyId: String,
  updatedBy: String = ""
  ): SyncIdentity {
  require(collection in ALLOWED_COLLECTIONS) {
  "Unsupported collection: $collection"
  }
  require(localId > 0) {
  "Local record ID must be greater than zero."
  }
  require(familyId.isNotBlank()) {
  "Family ID cannot be blank."
  }
  
   val existing = dao.findSyncIdentity(collection, localId)

 if (existing != null) {
     if (existing.familyId != familyId) {
         val updated = existing.copy(
             familyId = familyId,
             updatedAt = System.currentTimeMillis(),
             isDeleted = false
         )
         dao.updateSyncIdentity(updated)
         return updated
     }

     return existing
 }

 val identity = SyncIdentity(
     collection = collection,
     localId = localId,
     familyId = familyId,
     updatedAt = System.currentTimeMillis(),
     isDeleted = false
 )

 val insertedId = dao.insertSyncIdentity(identity)

 if (insertedId != -1L) {
     return identity.copy(id = insertedId)
 }

 // Another operation may have inserted the same identity.
 return dao.findSyncIdentity(collection, localId)
     ?: throw IllegalStateException(
         "Could not create or retrieve sync identity."
     )
  
  }
  
  companion object {
  val ALLOWED_COLLECTIONS = setOf(
  "transactions",
  "categories",
  "events",
  "loans",
  "budgets",
  "accounts"
  )
  }
  }
