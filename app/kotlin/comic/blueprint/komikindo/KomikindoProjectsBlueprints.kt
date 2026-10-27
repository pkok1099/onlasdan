package comic.blueprint.komikindo

import comic.blueprint.ProjectsBlueprints
import comic.blueprint.core.ScreenSlot
import comic.blueprint.core.blueprint
import comic.projects.ProjectsBrick
import comic.projects.ProjectsLayoutState

object KomikindoProjectsBlueprints : ProjectsBlueprints {
    override val blueprint = blueprint<ProjectsLayoutState, ProjectsBrick> {
        slot(ScreenSlot.Header) { place(ProjectsBrick.TopBar, predicate = { true }) }
        slot(ScreenSlot.Body) {
            place(ProjectsBrick.Empty) { !it.hasProjects }
            place(ProjectsBrick.Grid) { it.hasProjects }
        }
    }
}
