package com.onlasdan.comic.ui.blueprint.shinigami

import com.onlasdan.comic.ui.blueprint.BrowseBlueprints
import com.onlasdan.comic.ui.blueprint.GenresBlueprints
import com.onlasdan.comic.ui.blueprint.HomeBlueprints
import com.onlasdan.comic.ui.blueprint.ProfileBlueprints
import com.onlasdan.comic.ui.blueprint.ProjectsBlueprints
import com.onlasdan.comic.ui.blueprint.ReaderBlueprints
import com.onlasdan.comic.ui.blueprint.SettingsBlueprints
import com.onlasdan.comic.ui.blueprint.SourceBlueprints
import com.onlasdan.comic.ui.blueprint.UpdatesBlueprints

object ShinigamiBlueprints : SourceBlueprints {
    override val home: HomeBlueprints = ShinigamiHomeBlueprints
    override val browse: BrowseBlueprints = ShinigamiBrowseBlueprints
    override val reader: ReaderBlueprints = ShinigamiReaderBlueprints
    override val genres: GenresBlueprints = ShinigamiGenresBlueprints
    override val updates: UpdatesBlueprints = ShinigamiUpdatesBlueprints
    override val settings: SettingsBlueprints = ShinigamiSettingsBlueprints
    override val profile: ProfileBlueprints = ShinigamiProfileBlueprints
    override val projects: ProjectsBlueprints = ShinigamiProjectsBlueprints
}
