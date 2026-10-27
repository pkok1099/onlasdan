package comic.browse

/**
 * String-based brick identifiers for BrowseScreen.
 */
object BrowseBrickIds {
    const val KOMIKU_HEADER = "komiku:header"
    const val KOMIKINDO_HEADER = "komikindo:header"
    const val SHINIGAMI_HEADER = "shinigami:header"
    const val DOUJINDESU_HEADER = "doujindesu:header"
    const val LAYOUT_ROW = "generic:layout_row"
    const val RESULTS = "generic:results"
    const val PAGINATION = "generic:pagination"
    const val KOMIKU_FILTER_SHEET = "komiku:filter_sheet"
    const val KOMIKINDO_FILTER_SHEET = "komikindo:filter_sheet"
    const val SHINIGAMI_FILTER_SHEET = "shinigami:filter_sheet"
    const val DOUJINDESU_FILTER_SHEET = "doujindesu:filter_sheet"
}

/**
 * Extension to map BrowseBrick enum to string IDs for backward compatibility.
 */
val BrowseBrick.id: String
    get() = when (this) {
        BrowseBrick.KomikuHeader -> BrowseBrickIds.KOMIKU_HEADER
        BrowseBrick.KomikindoHeader -> BrowseBrickIds.KOMIKINDO_HEADER
        BrowseBrick.ShinigamiHeader -> BrowseBrickIds.SHINIGAMI_HEADER
        BrowseBrick.DoujindesuHeader -> BrowseBrickIds.DOUJINDESU_HEADER
        BrowseBrick.LayoutRow -> BrowseBrickIds.LAYOUT_ROW
        BrowseBrick.Results -> BrowseBrickIds.RESULTS
        BrowseBrick.Pagination -> BrowseBrickIds.PAGINATION
        BrowseBrick.KomikuFilterSheet -> BrowseBrickIds.KOMIKU_FILTER_SHEET
        BrowseBrick.KomikindoFilterSheet -> BrowseBrickIds.KOMIKINDO_FILTER_SHEET
        BrowseBrick.ShinigamiFilterSheet -> BrowseBrickIds.SHINIGAMI_FILTER_SHEET
        BrowseBrick.DoujindesuFilterSheet -> BrowseBrickIds.DOUJINDESU_FILTER_SHEET
    }
