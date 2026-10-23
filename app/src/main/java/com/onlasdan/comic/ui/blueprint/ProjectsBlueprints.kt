package com.onlasdan.comic.ui.blueprint

import com.onlasdan.comic.ui.blueprint.core.ScreenBlueprint
import com.onlasdan.comic.ui.screens.projects.ProjectsBrick
import com.onlasdan.comic.ui.screens.projects.ProjectsLayoutState

interface ProjectsBlueprints {
    val blueprint: ScreenBlueprint<ProjectsLayoutState, ProjectsBrick>
}
