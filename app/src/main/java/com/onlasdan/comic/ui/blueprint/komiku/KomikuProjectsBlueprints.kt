package com.onlasdan.comic.ui.blueprint.komiku

import com.onlasdan.comic.ui.blueprint.ProjectsBlueprints
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.blueprint.core.blueprint
import com.onlasdan.comic.ui.screens.projects.ProjectsBrick
import com.onlasdan.comic.ui.screens.projects.ProjectsLayoutState

object KomikuProjectsBlueprints : ProjectsBlueprints {
    override val blueprint = blueprint<ProjectsLayoutState, ProjectsBrick> {
        slot(ScreenSlot.Header) { place(ProjectsBrick.TopBar, predicate = { true }) }
        slot(ScreenSlot.Body) {
            place(ProjectsBrick.Empty) { !it.hasProjects }
            place(ProjectsBrick.Grid) { it.hasProjects }
        }
    }
}
