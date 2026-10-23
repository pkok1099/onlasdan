package com.onlasdan.comic.ui.blueprint

import com.onlasdan.comic.ui.blueprint.doujindesu.DoujindesuBlueprints
import com.onlasdan.comic.ui.blueprint.komikindo.KomikindoBlueprints
import com.onlasdan.comic.ui.blueprint.komiku.KomikuBlueprints
import com.onlasdan.comic.ui.blueprint.shinigami.ShinigamiBlueprints

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
