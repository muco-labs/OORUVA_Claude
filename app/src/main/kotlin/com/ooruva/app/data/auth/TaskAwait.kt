package com.ooruva.app.data.auth

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Awaits a Play Services [Task] from a coroutine.
 *
 * kotlinx-coroutines-play-services provides this, but pulling in a whole
 * artifact for one twelve-line adapter is not a good trade when Firebase auth
 * is the only caller. If a second subsystem starts needing Task bridging, swap
 * this for the official dependency rather than growing it.
 */
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    // A Task that has already settled never fires a listener, so check first.
    if (isComplete) {
        val e = exception
        if (e != null) cont.resumeWithException(e)
        else if (isCanceled) cont.cancel()
        else @Suppress("UNCHECKED_CAST") cont.resume(result as T)
        return@suspendCancellableCoroutine
    }

    addOnSuccessListener { value -> if (cont.isActive) cont.resume(value) }
    addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    addOnCanceledListener { if (cont.isActive) cont.cancel() }
}
