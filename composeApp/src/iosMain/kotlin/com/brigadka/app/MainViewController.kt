package com.brigadka.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arkivanov.decompose.DefaultComponentContext
import com.brigadka.app.di.initKoin
import com.brigadka.app.presentation.root.RootComponent
import com.brigadka.app.presentation.root.RootContent
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform.getKoin
import platform.UIKit.UIViewController
import com.arkivanov.essenty.lifecycle.Lifecycle as EssentyLifecycle

@Composable
fun App() {
    // Create the component context
    val rootContext = DefaultComponentContext(
        lifecycle = LocalLifecycleOwner.current.lifecycle.asEssentyLifecycle()
    )

    // Get the root component from Koin
    val rootComponent = getKoin().get<RootComponent> { parametersOf(rootContext) }

    // Return the compose view controller with the root component
    RootContent(rootComponent, modifier = Modifier.fillMaxSize())
}

fun MainViewController(): UIViewController {
    initKoin()
    // Return the compose view controller with the root component
    return ComposeUIViewController {
        App()
    }
}


/**
 * Converts AndroidX [Lifecycle] to Essenty [Lifecycle][EssentyLifecycle]
 */
fun Lifecycle.asEssentyLifecycle(): EssentyLifecycle = EssentyLifecycleInterop(this)

/**
 * Converts AndroidX [Lifecycle] to Essenty [Lifecycle][EssentyLifecycle]
 */
fun LifecycleOwner.essentyLifecycle(): EssentyLifecycle = lifecycle.asEssentyLifecycle()

private class EssentyLifecycleInterop(
    private val delegate: Lifecycle
) : EssentyLifecycle {

    private val observerMap = HashMap<EssentyLifecycle.Callbacks, LifecycleObserver>()

    override val state: EssentyLifecycle.State get() = delegate.currentState.toEssentyLifecycleState()

    override fun subscribe(callbacks: EssentyLifecycle.Callbacks) {
        check(callbacks !in observerMap) { "Already subscribed" }

        val observer = AndroidLifecycleObserver(delegate = callbacks, onDestroy = { observerMap -= callbacks })
        observerMap[callbacks] = observer
        delegate.addObserver(observer)
    }

    override fun unsubscribe(callbacks: EssentyLifecycle.Callbacks) {
        observerMap.remove(callbacks)?.also {
            delegate.removeObserver(it)
        }
    }
}

private fun Lifecycle.State.toEssentyLifecycleState(): EssentyLifecycle.State =
    when (this) {
        Lifecycle.State.DESTROYED -> EssentyLifecycle.State.DESTROYED
        Lifecycle.State.INITIALIZED -> EssentyLifecycle.State.INITIALIZED
        Lifecycle.State.CREATED -> EssentyLifecycle.State.CREATED
        Lifecycle.State.STARTED -> EssentyLifecycle.State.STARTED
        Lifecycle.State.RESUMED -> EssentyLifecycle.State.RESUMED
    }

private class AndroidLifecycleObserver(
    private val delegate: EssentyLifecycle.Callbacks,
    private val onDestroy: () -> Unit,
) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        delegate.onCreate()
    }

    override fun onStart(owner: LifecycleOwner) {
        delegate.onStart()
    }

    override fun onResume(owner: LifecycleOwner) {
        delegate.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        delegate.onPause()
    }

    override fun onStop(owner: LifecycleOwner) {
        delegate.onStop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        delegate.onDestroy()
        onDestroy.invoke()
    }
}
