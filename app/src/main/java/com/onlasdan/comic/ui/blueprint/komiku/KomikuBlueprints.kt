package com.onlasdan.comic.ui.blueprint.komiku

import com.onlasdan.comic.ui.blueprint.BrowseBlueprints
import com.onlasdan.comic.ui.blueprint.GenresBlueprints
import com.onlasdan.comic.ui.blueprint.HomeBlueprints
import com.onlasdan.comic.ui.blueprint.ProfileBlueprints
import com.onlasdan.comic.ui.blueprint.ProjectsBlueprints
import com.onlasdan.comic.ui.blueprint.ReaderBlueprints
import com.onlasdan.comic.ui.blueprint.SettingsBlueprints
import com.onlasdan.comic.ui.blueprint.SourceBlueprints
import com.onlasdan.comic.ui.blueprint.UpdatesBlueprints

object KomikuBlueprints : SourceBlueprints {
    override val home: HomeBlueprints = KomikuHomeBlueprints
    override val browse: BrowseBlueprints = KomikuBrowseBlueprints
    override val reader: ReaderBlueprints = KomikuReaderBlueprints
    override val genres: GenresBlueprints = KomikuGenresBlueprints
    override val updates: UpdatesBlueprints = KomikuUpdatesBlueprints
    override val settings: SettingsBlueprints = KomikuSettingsBlueprints
    override val profile: ProfileBlueprints = KomikuProfileBlueprints
    override val projects: ProjectsBlueprints = KomikuProjectsBlueprints
}
