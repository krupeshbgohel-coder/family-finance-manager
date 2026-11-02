package com.familyfinance.manager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**

* Stores the mapping between a local Room record and its stable cloud ID.

* 

* This entity is not active until it is registered in FinanceDatabase

* and the corresponding database migration is added.
  */
  @Entity(
  tableName = "sync_identities",
  indices = [
  Index(
  value = ["collection", "localId"],
  unique = true
  ),
  Index(
  value = ["syncId"],
  unique = true
  )
  ]
  )
  data class SyncIdentity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  
  val collection: String,
  
  val localId: Long,
  
  val syncId: String = UUID.randomUUID().toString(),
  
  val familyId: String = "",
  
  val updatedAt: Long = System.currentTimeMillis(),
  
  val isDeleted: Boolean = false
  )
