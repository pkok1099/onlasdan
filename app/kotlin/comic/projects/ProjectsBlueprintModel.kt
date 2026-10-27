package comic.projects

data class ProjectsLayoutState(val hasProjects: Boolean)

enum class ProjectsBrick {
    TopBar,
    Grid,
    Empty,
}
