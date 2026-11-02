package com.familyfinance.manager.data

/**

* Finds or creates the stable cloud identity for a local Room record.

* 

* This helper manages identity only. It does not upload or download data.
  */
  class SyncIdentityHelper(
  private val dao: FinanceDao
  ) {
  
  suspend fun getOrCreate(
  collection: String,
  localId: Long,
  familyId: String
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
     require(existing.familyId.isBlank() || existing.familyId == familyId) {
         "This record is already associated with another family."
     }

     if (existing.familyId.isBlank()) {
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

 // A concurrent operation may have inserted this identity.
 val inserted = dao.findSyncIdentity(collection, localId)
     ?: throw IllegalStateException(
         "Could not create or retrieve sync identity."
     )

 require(inserted.familyId.isBlank() || inserted.familyId == familyId) {
     "This record is already associated with another family."
 }

 return inserted
  
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
