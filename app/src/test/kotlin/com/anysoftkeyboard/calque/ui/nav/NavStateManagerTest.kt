package com.anysoftkeyboard.calque.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Test

class NavStateManagerTest {

    @Test
    fun testInitialDestination() {
        val manager = NavStateManager()
        assertEquals(NavDestination.READER, manager.currentDestination)
    }

    @Test
    fun testCustomInitialDestination() {
        val manager = NavStateManager(initialDestination = NavDestination.TOOLBOX)
        assertEquals(NavDestination.TOOLBOX, manager.currentDestination)
    }

    @Test
    fun testNavigateToUpdatesDestinationAndNotifiesListener() {
        val manager = NavStateManager()
        val transitions = mutableListOf<NavDestination>()
        manager.addListener { transitions.add(it) }

        manager.navigateTo(NavDestination.TOOLBOX)
        assertEquals(NavDestination.TOOLBOX, manager.currentDestination)
        assertEquals(listOf(NavDestination.TOOLBOX), transitions)

        manager.navigateTo(NavDestination.VAULT)
        assertEquals(NavDestination.VAULT, manager.currentDestination)
        assertEquals(listOf(NavDestination.TOOLBOX, NavDestination.VAULT), transitions)
    }

    @Test
    fun testDuplicateNavigationDoesNotNotifyListener() {
        val manager = NavStateManager()
        var callCount = 0
        manager.addListener { callCount++ }

        manager.navigateTo(NavDestination.READER)
        assertEquals(0, callCount)
    }

    @Test
    fun testRemoveListenerStopsNotifications() {
        val manager = NavStateManager()
        val transitions = mutableListOf<NavDestination>()
        val listener = { dest: NavDestination ->
            transitions.add(dest)
            Unit
        }

        manager.addListener(listener)
        manager.navigateTo(NavDestination.TOOLBOX)
        manager.removeListener(listener)
        manager.navigateTo(NavDestination.VAULT)

        assertEquals(listOf(NavDestination.TOOLBOX), transitions)
    }
}
