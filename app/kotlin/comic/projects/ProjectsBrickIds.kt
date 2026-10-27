package comic.projects

/**
 * String-based brick identifiers for ProjectsScreen.
 */
object ProjectsBrickIds {
    const val TOP_BAR = "komiku:topbar"
    const val GRID = "komiku:grid"
    const val EMPTY = "komiku:empty"
}

/**
 * Extension to map ProjectsBrick enum to string IDs for backward compatibility.
 */
val ProjectsBrick.id: String
    get() = when (this) {
        ProjectsBrick.TopBar -> ProjectsBrickIds.TOP_BAR
        ProjectsBrick.Grid -> ProjectsBrickIds.GRID
        ProjectsBrick.Empty -> ProjectsBrickIds.EMPTY
    }
