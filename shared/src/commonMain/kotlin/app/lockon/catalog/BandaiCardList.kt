package app.lockon.catalog

/**
 * Reads Bandai's official card list pages for one narrow job: alt-art printings that gcg-api hasn't
 * picked up yet. A new set's parallels reach Bandai's site days before gcg-api's weekly refresh, so
 * sets with no alt arts in gcg-api are looked up here. Bandai has no data feed, so this is HTML
 * reading; anything that doesn't parse is skipped and the gcg-api list stands on its own.
 */
object BandaiCardList {
    private const val BASE_URL = "https://www.gundam-gcg.com/en/"
    /** The search page with no filter lists no cards, only the products to search by. */
    const val SEARCH_URL = BASE_URL + "cards/index.php?search=true"

    fun packageUrl(packageId: String) = "$SEARCH_URL&package=$packageId"
    fun detailUrl(printingId: String) = BASE_URL + "cards/detail.php?detailSearch=$printingId"

    /** A product in the search filter, such as "Stardust Trails [GD06]" (package 616106, code GD06). */
    data class Package(val id: String, val code: String)

    /** A printing in a product's results: its ID ("GD06-002_p1") and its artwork link. */
    data class Listed(val printingId: String, val artworkUrl: String?) {
        val cardNumber: String get() = printingId.substringBefore('_')
        val isStandard: Boolean get() = printingId == cardNumber
    }

    data class Detail(val rarity: String, val whereToGet: String?)

    private val packagePattern = Regex("""data-val="(\d+)">[^<\[]*\[([A-Za-z0-9]+)]""")
    private val cardPattern = Regex("""detailSearch=([A-Za-z0-9_-]+)"[^>]*>\s*<img[^>]*data-src="\.\./([^"?]+)""")
    private val rarityPattern = Regex("""<div class="rarity">([^<]*)</div>""")
    private val whereToGetPattern = Regex("""Where to get it</dt>\s*<dd[^>]*>([^<]*)</dd>""")

    fun parsePackages(html: String): List<Package> =
        packagePattern.findAll(html).map { Package(it.groupValues[1], it.groupValues[2]) }.distinct().toList()

    fun parseListing(html: String): List<Listed> =
        cardPattern.findAll(html).map { Listed(it.groupValues[1], BASE_URL + it.groupValues[2]) }.distinctBy { it.printingId }.toList()

    /** "LR                +" on the page becomes "LR+". */
    fun parseDetail(html: String): Detail? {
        val rarity = rarityPattern.find(html)?.groupValues?.get(1)?.filterNot { it.isWhitespace() }?.takeIf { it.isNotEmpty() } ?: return null
        val whereToGet = whereToGetPattern.find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        return Detail(rarity, whereToGet)
    }

    /** Sets with standard printings but no alt arts at all, which is how a set looks before gcg-api has its parallels. */
    fun setsWithoutAltArts(printings: List<Pair<String, Boolean>>): Set<String> {
        val bySet = printings.groupBy({ it.first }, { it.second })
        return bySet.filterValues { alts -> alts.none { it } }.keys
    }
}
