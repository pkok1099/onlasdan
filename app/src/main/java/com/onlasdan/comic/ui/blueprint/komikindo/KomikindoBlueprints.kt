package com.onlasdan.comic.ui.blueprint.komikindo

import com.onlasdan.comic.ui.blueprint.BrowseBlueprints
import com.onlasdan.comic.ui.blueprint.GenresBlueprints
import com.onlasdan.comic.ui.blueprint.HomeBlueprints
import com.onlasdan.comic.ui.blueprint.ProfileBlueprints
import com.onlasdan.comic.ui.blueprint.ProjectsBlueprints
import com.onlasdan.comic.ui.blueprint.ReaderBlueprints
import com.onlasdan.comic.ui.blueprint.SettingsBlueprints
import com.onlasdan.comic.ui.blueprint.SourceBlueprints
import com.onlasdan.comic.ui.blueprint.UpdatesBlueprints

object KomikindoBlueprints : SourceBlueprints {
    override val home: HomeBlueprints = KomikindoHomeBlueprints
    override val browse: BrowseBlueprints = KomikindoBrowseBlueprints
    override val reader: ReaderBlueprints = KomikindoReaderBlueprints
    override val genres: GenresBlueprints = KomikindoGenresBlueprints
    override val updates: UpdatesBlueprints = KomikindoUpdatesBlueprints
    override val settings: SettingsBlueprints = KomikindoSettingsBlueprints
    override val profile: ProfileBlueprints = KomikindoProfileBlueprints
    override val projects: ProjectsBlueprints = KomikindoProjectsBlueprints
}
