package com.onlasdan.comic.ui.blueprint.shinigami

import com.onlasdan.comic.ui.blueprint.GenresBlueprints
import com.onlasdan.comic.ui.blueprint.core.ScreenSlot
import com.onlasdan.comic.ui.blueprint.core.blueprint
import com.onlasdan.comic.ui.screens.genres.GenreDetailBrick
import com.onlasdan.comic.ui.screens.genres.GenreDetailLayoutState
import com.onlasdan.comic.ui.screens.genres.GenresListBrick
import com.onlasdan.comic.ui.screens.genres.GenresListLayoutState

object ShinigamiGenresBlueprints : GenresBlueprints {
    override val list = blueprint<GenresListLayoutState, GenresListBrick> {
        slot(ScreenSlot.Header) { place(GenresListBrick.TopBar) }
        slot(ScreenSlot.Body) {
            place(GenresListBrick.Loading, predicate = { it.isLoading })
            place(GenresListBrick.Error, predicate = { it.hasError })
            place(GenresListBrick.GenresSection, predicate = { it.hasGenres && !it.isLoading && !it.hasError })
            place(GenresListBrick.DemographicsSection, predicate = {
                it.hasDemographics && !it.isLoading && !it.hasError
            })
            place(GenresListBrick.ThemesSection, predicate = { it.hasThemes && !it.isLoading && !it.hasError })
            place(GenresListBrick.ContentSection, predicate = { it.hasContent && !it.isLoading && !it.hasError })
        }
    }

    override val detail = blueprint<GenreDetailLayoutState, GenreDetailBrick> {
        slot(ScreenSlot.Header) { place(GenreDetailBrick.TopBar) }
        slot(ScreenSlot.Body) {
            place(GenreDetailBrick.Loading, predicate = { it.isLoading })
            place(GenreDetailBrick.Error, predicate = { it.hasError })
            place(GenreDetailBrick.Empty, predicate = { !it.isLoading && !it.hasError && !it.hasComics })
            place(GenreDetailBrick.Results, predicate = { !it.isLoading && !it.hasError && it.hasComics })
            place(GenreDetailBrick.Pagination, predicate = { !it.isLoading && !it.hasError && it.hasComics })
        }
    }
}
