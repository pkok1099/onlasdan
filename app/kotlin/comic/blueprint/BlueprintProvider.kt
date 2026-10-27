package comic.blueprint

import comic.blueprint.doujindesu.DoujindesuBlueprints
import comic.blueprint.komikindo.KomikindoBlueprints
import comic.blueprint.komiku.KomikuBlueprints
import comic.blueprint.shinigami.ShinigamiBlueprints

/**
 * Registry for comic source blueprints.
 * New sources can be added here without modifying the core screens.
 */
object BlueprintProvider {
    private val registry = mutableMapOf<String, SourceBlueprints>()

    init {
        register("komiku", KomikuBlueprints)
        register("komikindo", KomikindoBlueprints)
        register("shinigami", ShinigamiBlueprints)
        register("doujindesu", DoujindesuBlueprints)
    }

    fun register(source: String, blueprints: SourceBlueprints) {
        registry[source.lowercase()] = blueprints
    }

    fun forSource(source: String): SourceBlueprints? = registry[source.lowercase()]

    fun getAvailableSources(): List<String> = registry.keys.toList()
}

/**
 * Container for per-screen blueprints.
 *
 * Each source must provide all required screens.
 */
interface SourceBlueprints {
    val home: HomeBlueprints
    val browse: BrowseBlueprints
    val reader: ReaderBlueprints
    val genres: GenresBlueprints
    val updates: UpdatesBlueprints
    val settings: SettingsBlueprints
    val profile: ProfileBlueprints
    val projects: ProjectsBlueprints
}
