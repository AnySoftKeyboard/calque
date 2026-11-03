package com.anysoftkeyboard.calque.ui.nav

/** Manages navigation state and destination changes across the primary app shell. */
class NavStateManager(
    initialDestination: NavDestination = NavDestination.READER,
) {
    var currentDestination: NavDestination = initialDestination
        private set

    private val listeners = mutableListOf<(NavDestination) -> Unit>()

    fun navigateTo(destination: NavDestination) {
        if (currentDestination != destination) {
            currentDestination = destination
            for (listener in listeners) {
                listener(destination)
            }
        }
    }

    fun addListener(listener: (NavDestination) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (NavDestination) -> Unit) {
        listeners.remove(listener)
    }
}
