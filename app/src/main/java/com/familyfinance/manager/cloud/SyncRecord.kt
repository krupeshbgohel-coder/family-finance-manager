package com.familyfinance.manager.cloud

import java.util.UUID

/**

* Stable identifier and sync metadata for a finance record.

* 

* This is a sync foundation only. It does not modify Room entities

* or upload/download records by itself.
  */
  data class SyncRecord(
  val id: String = UUID.randomUUID().toString(),
  val familyId: String,
  val collection: String,
  val updatedAt: Long = System.currentTimeMillis(),
  val updatedBy: String = "",
  val isDeleted: Boolean = false
  ) {
  init {
  require(id.isNotBlank()) { "Record ID cannot be blank." }
  require(familyId.isNotBlank()) { "Family ID cannot be blank." }
  require(collection in ALLOWED_COLLECTIONS) {
  "Unsupported collection: $collection"
  }
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
