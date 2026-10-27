package comic.home

data class HomeLayoutState(
    val source: String,
    val hasPopular: Boolean,
    val hasHotManga: Boolean,
    val hasHotManhwa: Boolean,
    val hasHotManhua: Boolean,
    val hasGenreCards: Boolean,
    val hasStats: Boolean,
    val hasLatest: Boolean,
)

enum class HomeBrick {
    Hero,
    GenericHeader,
    PopularRow,
    HotMangaRow,
    HotManhwaRow,
    HotManhuaRow,
    GenreCardsRow,
    Stats,
    LatestControls,
    LatestGrid,
    ExploreGenresCard,
    Spacer,
}
