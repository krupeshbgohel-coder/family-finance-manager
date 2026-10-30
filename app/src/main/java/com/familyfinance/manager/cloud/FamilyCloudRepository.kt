package com.familyfinance.manager.cloud

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firestore access layer. Every document is scoped to a familyId and every operation is also
 * protected by firestore.rules. Never accept a familyId from untrusted input without membership checks.
 */
class FamilyCloudRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun signInEmail(email: String, password: String) = auth.signInWithEmailAndPassword(email.trim(), password).await()
    suspend fun registerEmail(email: String, password: String) = auth.createUserWithEmailAndPassword(email.trim(), password).await()
    suspend fun sendPasswordReset(email: String) = auth.sendPasswordResetEmail(email.trim()).await()
    fun signOut() = auth.signOut()
    fun currentUid(): String? = auth.currentUser?.uid

    suspend fun createFamily(familyId: String, displayName: String) {
        val uid = requireNotNull(currentUid()) { "Please sign in first." }
        val member = mapOf("uid" to uid, "email" to auth.currentUser?.email.orEmpty(), "displayName" to displayName, "role" to "ADMIN", "status" to "ACTIVE")
        db.collection("families").document(familyId).set(mapOf("name" to displayName, "createdBy" to uid)).await()
        db.collection("families").document(familyId).collection("members").document(uid).set(member).await()
    }

    /** Admin-only pending invitation. An email delivery function must be configured separately. */
    suspend fun inviteMember(familyId: String, email: String, role: String = "MEMBER") {
        require(role in setOf("MEMBER", "MANAGER")) { "Only non-admin roles can be invited." }
        val uid = requireNotNull(currentUid()) { "Please sign in first." }
        db.collection("families").document(familyId).collection("invitations").document(email.trim().lowercase())
            .set(mapOf("email" to email.trim().lowercase(), "role" to role, "invitedBy" to uid, "status" to "PENDING", "createdAt" to System.currentTimeMillis())).await()
    }

    suspend fun acceptInvitation(familyId: String) {
        val user = requireNotNull(auth.currentUser) { "Please sign in first." }
        val email = requireNotNull(user.email) { "This account has no email. Ask the Admin to invite the account email." }.trim().lowercase()
        val family = db.collection("families").document(familyId)
        val inviteRef = family.collection("invitations").document(email)
        val invite = inviteRef.get().await()
        require(invite.exists() && invite.getString("status") == "PENDING") { "No pending invitation found for this email." }
        val role = invite.getString("role") ?: "MEMBER"
        require(role in setOf("MEMBER", "MANAGER")) { "Invalid invitation role." }
        family.collection("members").document(user.uid).set(mapOf("uid" to user.uid, "email" to email, "displayName" to (user.displayName ?: email.substringBefore("@")), "role" to role, "status" to "ACTIVE")).await()
        inviteRef.update("status", "ACCEPTED", "acceptedBy", user.uid, "acceptedAt", System.currentTimeMillis()).await()
    }

    suspend fun writeRecord(familyId: String, collection: String, recordId: String, data: Map<String, Any?>) {
        require(collection in allowedCollections) { "Unsupported collection." }
        val uid = requireNotNull(currentUid()) { "Please sign in first." }
        val safe = data.toMutableMap().apply { put("updatedBy", uid); put("updatedAt", System.currentTimeMillis()) }
        db.collection("families").document(familyId).collection(collection).document(recordId).set(safe).await()
    }

    suspend fun deleteRecord(familyId: String, collection: String, recordId: String) {
        require(collection in allowedCollections) { "Unsupported collection." }
        db.collection("families").document(familyId).collection(collection).document(recordId).delete().await()
    }

    suspend fun readRecords(familyId: String, collection: String): List<Map<String, Any?>> {
        require(collection in allowedCollections) { "Unsupported collection." }
        return db.collection("families").document(familyId).collection(collection).get().await().documents.mapNotNull { doc ->
            doc.data?.toMutableMap()?.apply { put("_id", doc.id) }
        }
    }

    companion object {
        val allowedCollections = setOf("transactions", "categories", "events", "loans", "budgets", "accounts")
    }
}
