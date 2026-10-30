package com.familyfinance.manager.cloud

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

@Composable
fun CloudAuthScreen(onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var mode by remember { mutableStateOf("EMAIL") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    fun finish(task: com.google.android.gms.tasks.Task<AuthResult>) {
        task.addOnCompleteListener { t -> busy = false; if (t.isSuccessful) onSignedIn() else message = t.exception?.localizedMessage ?: "Authentication failed." }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Family Finance Manager", style = MaterialTheme.typography.headlineSmall)
            Text("Sign in to enable family cloud access", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "EMAIL", onClick = { mode = "EMAIL" }, label = { Text("Email / Password") })
                FilterChip(selected = mode == "PHONE", onClick = { mode = "PHONE" }, label = { Text("Mobile OTP") })
            }
            if (mode == "EMAIL") {
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy && email.contains("@") && password.isNotBlank(), onClick = { busy = true; finish(auth.signInWithEmailAndPassword(email.trim(), password)) }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Please wait…" else "Sign in") }
                TextButton(enabled = email.contains("@"), onClick = { auth.sendPasswordResetEmail(email.trim()).addOnSuccessListener { message = "Password reset email sent." }.addOnFailureListener { message = it.localizedMessage ?: "Could not send reset email." } }) { Text("Forgot password?") }
            } else {
                OutlinedTextField(phone, { phone = it }, label = { Text("Mobile number with country code, e.g. +91…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy && verificationId == null && phone.startsWith("+") && phone.length >= 10, onClick = {
                    val activity = context as? Activity
                    if (activity == null) { message = "Could not access the current Activity." } else {
                        busy = true
                        val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phone.trim()).setTimeout(60L, TimeUnit.SECONDS).setActivity(activity)
                            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(credential: PhoneAuthCredential) { auth.signInWithCredential(credential).addOnCompleteListener { t -> busy = false; if (t.isSuccessful) onSignedIn() else message = t.exception?.localizedMessage ?: "OTP sign-in failed." } }
                                override fun onVerificationFailed(e: FirebaseException) { busy = false; message = e.localizedMessage ?: "Phone verification failed." }
                                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) { busy = false; verificationId = id; message = "OTP sent. Enter the code." }
                            }).build()
                        PhoneAuthProvider.verifyPhoneNumber(options)
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Sending…" else "Send OTP") }
                verificationId?.let { id ->
                    OutlinedTextField(otp, { otp = it }, label = { Text("OTP code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(enabled = !busy && otp.length >= 6, onClick = { busy = true; finish(auth.signInWithCredential(PhoneAuthProvider.getCredential(id, otp.trim()))) }, modifier = Modifier.fillMaxWidth()) { Text("Verify OTP") }
                }
            }
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
            Text("If Firebase is configured, only users with family membership should access shared family data.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CloudFamilyAdminDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { FamilyCloudRepository() }
    var familyId by remember { mutableStateOf("") }
    var familyName by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MEMBER") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Family cloud setup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text("Create a unique family ID once. Other members must use the same ID after they accept an invitation.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(familyId, { familyId = it.trim().replace(" ", "-").lowercase() }, label = { Text("Family ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(familyName, { familyName = it }, label = { Text("Family name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy && familyId.length >= 3 && familyName.isNotBlank(), onClick = {
                    busy = true
                    scope.launch { runCatching { repo.createFamily(familyId, familyName.trim()) }.onSuccess { status = "Family created. Keep the Family ID safe." }.onFailure { status = it.localizedMessage ?: "Could not create family." }; busy = false }
                }, modifier = Modifier.fillMaxWidth()) { Text("Create family as Admin") }
                HorizontalDivider()
                Button(enabled = !busy && familyId.isNotBlank(), onClick = {
                    busy = true
                    scope.launch { runCatching { repo.acceptInvitation(familyId) }.onSuccess { status = "Invitation accepted. Family membership is active." }.onFailure { status = it.localizedMessage ?: "Could not accept invitation." }; busy = false }
                }, modifier = Modifier.fillMaxWidth()) { Text("Accept invitation for signed-in email") }
                HorizontalDivider()
                OutlinedTextField(inviteEmail, { inviteEmail = it }, label = { Text("Invite member email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(role, { role = it.uppercase() }, label = { Text("Role: MEMBER or MANAGER") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy && familyId.isNotBlank() && inviteEmail.contains("@") && role in setOf("MEMBER", "MANAGER"), onClick = {
                    busy = true
                    scope.launch { runCatching { repo.inviteMember(familyId, inviteEmail, role) }.onSuccess { status = "Invitation record saved. Email delivery/acceptance flow must be configured separately." }.onFailure { status = it.localizedMessage ?: "Invitation failed. Admin membership is required." }; busy = false }
                }, modifier = Modifier.fillMaxWidth()) { Text("Save admin invitation") }
                if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
