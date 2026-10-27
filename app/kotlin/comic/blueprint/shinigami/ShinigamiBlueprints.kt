package comic.blueprint.shinigami

import comic.blueprint.BrowseBlueprints
import comic.blueprint.GenresBlueprints
import comic.blueprint.HomeBlueprints
import comic.blueprint.ProfileBlueprints
import comic.blueprint.ProjectsBlueprints
import comic.blueprint.ReaderBlueprints
import comic.blueprint.SettingsBlueprints
import comic.blueprint.SourceBlueprints
import comic.blueprint.UpdatesBlueprints

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
