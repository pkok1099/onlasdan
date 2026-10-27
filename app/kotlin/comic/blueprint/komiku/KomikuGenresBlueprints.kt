package comic.blueprint.komiku

import comic.blueprint.GenresBlueprints
import comic.blueprint.core.ScreenSlot
import comic.blueprint.core.blueprint
import comic.genres.GenreDetailBrick
import comic.genres.GenreDetailLayoutState
import comic.genres.GenresListBrick
import comic.genres.GenresListLayoutState

object KomikuGenresBlueprints : GenresBlueprints {
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
