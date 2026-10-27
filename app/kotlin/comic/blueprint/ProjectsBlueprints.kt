package comic.blueprint

import comic.blueprint.core.ScreenBlueprint
import comic.projects.ProjectsBrick
import comic.projects.ProjectsLayoutState

interface ProjectsBlueprints {
    val blueprint: ScreenBlueprint<ProjectsLayoutState, ProjectsBrick>
}
