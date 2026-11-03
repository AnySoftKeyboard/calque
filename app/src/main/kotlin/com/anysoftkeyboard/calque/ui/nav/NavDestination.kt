package com.anysoftkeyboard.calque.ui.nav

/** Primary navigation destinations for Calque defined in docs/PAGES_AND_FLOWS.md. */
enum class NavDestination(
    val id: String,
    val title: String,
    val subtitle: String,
) {
    READER(
        id = "reader",
        title = "Reader",
        subtitle = "Active reading with live interlinear glosses",
    ),
    TOOLBOX(
        id = "toolbox",
        title = "Toolbox",
        subtitle = "Linguistic sandbox & toys",
    ),
    VAULT(
        id = "vault",
        title = "Vault",
        subtitle = "Saved vocabulary, lessons, and custom texts",
    ),
}
