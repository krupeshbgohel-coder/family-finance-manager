package com.familyfinance.manager.cloud

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates sync operations safely.
 *
 * Features:
 * - Prevents overlapping sync operations.
 * - Retries temporary failures.
 * - Uses exponential backoff between attempts.
 * - Preserves coroutine cancellation.
 *
 * IMPORTANT:
 * This class provides the sync coordination foundation.
 * Local Room and remote Firestore data synchronization
 * must be connected separately.
 */
class SyncManager(
    private val maxAttempts: Int = 3,
    private val initialRetryDelayMs: Long = 1_000L,
    private val maxRetryDelayMs: Long = 8_000L
) {

    private val syncMutex = Mutex()

    init {
        require(maxAttempts >= 1) {
            "maxAttempts must be at least 1."
        }

        require(initialRetryDelayMs >= 0L) {
            "initialRetryDelayMs cannot be negative."
        }

        require(maxRetryDelayMs >= initialRetryDelayMs) {
            "maxRetryDelayMs must be greater than or equal to initialRetryDelayMs."
        }
    }

    /**
     * Executes one sync operation at a time.
     *
     * The supplied action must contain the actual
     * Room-to-Firestore synchronization logic.
     *
     * Returns normally when synchronization succeeds.
     * Throws the last error when all attempts fail.
     */
    suspend fun runSync(
        syncAction: suspend () -> Unit
    ) {
        syncMutex.withLock {
            var lastError: Exception? = null

            repeat(maxAttempts) { attempt ->
                try {
                    syncAction()
                    return
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    lastError = error

                    val isLastAttempt = attempt == maxAttempts - 1

                    if (!isLastAttempt) {
                        val multiplier = 1L shl attempt.coerceAtMost(20)
                        val retryDelay = (
                            initialRetryDelayMs * multiplier
                        ).coerceAtMost(maxRetryDelayMs)

                        delay(retryDelay)
                    }
                }
            }

            throw requireNotNull(lastError) {
                "Synchronization failed without an error."
            }
        }
    }
}
