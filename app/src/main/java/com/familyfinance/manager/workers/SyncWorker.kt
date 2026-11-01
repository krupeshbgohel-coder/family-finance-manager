package com.familyfinance.manager.workers
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
/** Starter worker; actual synchronization is not implemented. */
class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) { override fun doWork(): Result = Result.success() }
