package com.onlasdan.comic.ui.blueprint.doujindesu

import com.onlasdan.comic.ui.blueprint.*
import com.onlasdan.comic.ui.blueprint.shinigami.*

object DoujindesuBlueprints : SourceBlueprints {
    override val home: HomeBlueprints = ShinigamiHomeBlueprints
    override val browse: BrowseBlueprints = DoujindesuBrowseBlueprints
    override val reader: ReaderBlueprints = ShinigamiReaderBlueprints
    override val genres: GenresBlueprints = ShinigamiGenresBlueprints
    override val updates: UpdatesBlueprints = ShinigamiUpdatesBlueprints
    override val settings: SettingsBlueprints = ShinigamiSettingsBlueprints
    override val profile: ProfileBlueprints = ShinigamiProfileBlueprints
    override val projects: ProjectsBlueprints = ShinigamiProjectsBlueprints
}
