package comic.genres

/**
 * String-based brick identifiers for GenresScreen.
 */
object GenresListBrickIds {
    const val TOP_BAR = "komiku:topbar"
    const val LOADING = "komiku:loading"
    const val ERROR = "komiku:error"
    const val GENRES_SECTION = "komiku:genres_section"
    const val DEMOGRAPHICS_SECTION = "komiku:demographics_section"
    const val THEMES_SECTION = "komiku:themes_section"
    const val CONTENT_SECTION = "komiku:content_section"
}

val GenresListBrick.id: String
    get() = when (this) {
        GenresListBrick.TopBar -> GenresListBrickIds.TOP_BAR
        GenresListBrick.Loading -> GenresListBrickIds.LOADING
        GenresListBrick.Error -> GenresListBrickIds.ERROR
        GenresListBrick.GenresSection -> GenresListBrickIds.GENRES_SECTION
        GenresListBrick.DemographicsSection -> GenresListBrickIds.DEMOGRAPHICS_SECTION
        GenresListBrick.ThemesSection -> GenresListBrickIds.THEMES_SECTION
        GenresListBrick.ContentSection -> GenresListBrickIds.CONTENT_SECTION
    }

/**
 * String-based brick identifiers for GenreDetailScreen.
 */
object GenreDetailBrickIds {
    const val TOP_BAR = "komiku:topbar"
    const val LOADING = "komiku:loading"
    const val ERROR = "komiku:error"
    const val EMPTY = "komiku:empty"
    const val RESULTS = "komiku:results"
    const val PAGINATION = "komiku:pagination"
}

val GenreDetailBrick.id: String
    get() = when (this) {
        GenreDetailBrick.TopBar -> GenreDetailBrickIds.TOP_BAR
        GenreDetailBrick.Loading -> GenreDetailBrickIds.LOADING
        GenreDetailBrick.Error -> GenreDetailBrickIds.ERROR
        GenreDetailBrick.Empty -> GenreDetailBrickIds.EMPTY
        GenreDetailBrick.Results -> GenreDetailBrickIds.RESULTS
        GenreDetailBrick.Pagination -> GenreDetailBrickIds.PAGINATION
    }
