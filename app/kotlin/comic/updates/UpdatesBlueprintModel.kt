package comic.updates

data class UpdatesLayoutState(
    val isLoading: Boolean,
    val hasError: Boolean,
    val hasData: Boolean,
    val hasResults: Boolean,
)

enum class UpdatesBrick {
    TopBar,
    ControlsRow,
    Loading,
    Error,
    Empty,
    Results,
    Pagination,
}
