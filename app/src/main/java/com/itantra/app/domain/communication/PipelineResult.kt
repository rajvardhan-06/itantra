package com.itantra.app.domain.communication

/**
 * Outcome of an end-to-end communication pipeline operation.
 */
sealed class PipelineResult<out T> {

    data class Success<out T>(val value: T) : PipelineResult<T>()

    data class Cancelled(val reason: String) : PipelineResult<Nothing>()

    data class Failed(
        val message: String,
        val cause: Throwable? = null,
        val canRetry: Boolean = true
    ) : PipelineResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isCancelled: Boolean get() = this is Cancelled
    val isFailed: Boolean get() = this is Failed

    fun getOrNull(): T? = (this as? Success)?.value

    fun exceptionOrNull(): Throwable? = (this as? Failed)?.cause
}
