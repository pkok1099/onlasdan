package com.onlasdan.comic.ui.screens.genres

data class GenresListLayoutState(
    val isLoading: Boolean,
    val hasError: Boolean,
    val hasGenres: Boolean,
    val hasDemographics: Boolean,
    val hasThemes: Boolean,
    val hasContent: Boolean,
)

enum class GenresListBrick {
    TopBar,
    Loading,
    Error,
    GenresSection,
    DemographicsSection,
    ThemesSection,
    ContentSection,
}

data class GenreDetailLayoutState(val isLoading: Boolean, val hasError: Boolean, val hasComics: Boolean)

enum class GenreDetailBrick {
    TopBar,
    Loading,
    Error,
    Empty,
    Results,
    Pagination,
}
