package comic.blueprint.komiku

import comic.blueprint.BrowseBlueprints
import comic.blueprint.GenresBlueprints
import comic.blueprint.HomeBlueprints
import comic.blueprint.ProfileBlueprints
import comic.blueprint.ProjectsBlueprints
import comic.blueprint.ReaderBlueprints
import comic.blueprint.SettingsBlueprints
import comic.blueprint.SourceBlueprints
import comic.blueprint.UpdatesBlueprints

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
