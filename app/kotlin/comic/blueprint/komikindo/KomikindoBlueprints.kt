package comic.blueprint.komikindo

import comic.blueprint.BrowseBlueprints
import comic.blueprint.GenresBlueprints
import comic.blueprint.HomeBlueprints
import comic.blueprint.ProfileBlueprints
import comic.blueprint.ProjectsBlueprints
import comic.blueprint.ReaderBlueprints
import comic.blueprint.SettingsBlueprints
import comic.blueprint.SourceBlueprints
import comic.blueprint.UpdatesBlueprints

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
