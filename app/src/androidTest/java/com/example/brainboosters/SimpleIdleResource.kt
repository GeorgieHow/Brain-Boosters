package com.example.brainboosters

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

class SimpleIdlingResource : IdlingResource {
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private val isIdleNow = AtomicBoolean(true)

    override fun getName(): String = SimpleIdlingResource::class.java.name

    override fun isIdleNow(): Boolean = isIdleNow.get()

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        resourceCallback = callback
    }

    fun setIdleState(isIdle: Boolean) {
        isIdleNow.set(isIdle)
        if (isIdle && resourceCallback != null) {
            resourceCallback?.onTransitionToIdle()
        }
    }
}