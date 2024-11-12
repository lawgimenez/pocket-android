package com.pocket.repository

import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.thing.RecentSearches
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sync.source.bindLocalAsFlow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Enables a user to search their saved items.
 *
 * For local/client side searching, see {saves}. Remote and premium search is powered by {get}.
 *
 * Free search only searches an item's title and url.
 *
 * Premium search searches article text, tags, topics, authors, and potentially other values.
 *
 * https://help.getpocket.com/article/894-pocket-premium-full-text-search is a help article that may be useful to read.
 *
 * ### Query
 * The search term is provided via {get.search}. See that field's docs for more details.
 *
 * ### My List / Archive / All Items
 * Most apps have UI that separates search into 3 main tabs: My List, Archive and All Items.
 * All Items is only available for Premium.
 *
 * The parameter that is used to control this is {get.state}.
 *
 * ### Sorting
 * A user can choose a sort for their sort. Typically the options offered in the UI are {ItemSortKey.newest}, {ItemSortKey.oldest}, and {ItemSortKey.relevance} (the last one is premium only)
 *
 * The parameter that is used to control this is {get.sort}. See that field's docs for options and more details.
 *
 * ### Paging
 * Apps typically only ask for a few items at a time (30 is typical) and load more as they scroll down.
 *
 * The parameters used for this are {get.offset} and {get.count}.
 *
 * Apps know when they have reached the end when no results are returned.
 *
 * ### Context / Filters
 * A search may contain a "context", or additional limits on a search.
 * Such as searching only amongst items that have a tag or are favorited.
 * See the field docs for more details:
 * * {get.favorite}
 * * {get.tag}
 * * {get.contentType}
 * * {get.shared}
 *
 * ### Recent Searches
 * Premium search also remembers your most recent searches and shows them as options for quick access. See {RecentSearchFeature} for more details.
 *
 * ### Search Highlighting
 * For premium search, when the apps display a list of results, it will highlight what parts of the item matched the search.
 * {get} returns a `highlights` field on each of its items. This contains a {SearchMatch} which describes what part of the item was matched.
 * For example it might highlight the search term in the title, or if a tag matched, it will return that tag.
 * See {get.search_matches} for the remapped version of this info.
 *
 * ## Examples
 * Note, these example queries contain a lot of parameters not mentioned here, that are part of {get}'s ability to specify what fields you want back. These are the standard ones typically included with mobile app requests.
 * These are using the `a70` QA test account which has premium search.
 *
 * ### Queries / Special Operators
 * Examples using special operators on {get.search}. That is the only parameter changing between these examples:
 * * OR `dog OR cat` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595875922758&oauth_nonce=VzdpX4c5pGSUATIK&sig_hash=b577a1c55f8e31e12d7c5f06f17f6727&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=dog%20OR%20cat&meta=1&state=unread&authors=1
 * * AND `dog AND cat` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595875922758&oauth_nonce=VzdpX4c5pGSUATIK&sig_hash=b577a1c55f8e31e12d7c5f06f17f6727&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=dog%20AND%20cat&meta=1&state=unread&authors=1
 * * NOT `cats NOT tiger` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595876001059&oauth_nonce=NFmI1v71t8vsZK4J&sig_hash=496f838d12ad1683ae4da77dbb933c9b&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=cats%20NOT%20tigers&meta=1&state=unread&authors=1
 * * quotes `"computers to learn to identify cats"` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595876111524&oauth_nonce=oG7g2EnMhAL9QCwb&sig_hash=4bd7d6b2a8237aae8e0da078ad9e8b4d&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=%22computers%20to%20learn%20to%20identify%20cats%22&meta=1&state=unread&authors=1
 * * exclude `dogs -cats` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595882820076&oauth_nonce=BcMFt9zyGDyypvQo&sig_hash=8c29aeb211ba1e26512d25b51614e8de&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=dogs%20-cats&meta=1&state=all&authors=1
 * * include `dogs +cats` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595882890535&oauth_nonce=SkFw0NhVvAtAsPB9&sig_hash=260adcbd6a87d5e42d8097f6f86d35db&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=dogs%20%2Bcats&meta=1&state=all&authors=1
 * * tag `#economy` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595883175683&oauth_nonce=u8jRJZiTlrTxAr8I&sig_hash=e53ea1a9f168347ab03adca87f371f2e&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=%23economy&meta=1&state=all&authors=1
 * * multiple tags `#economy #internet` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595883415276&oauth_nonce=XEJogUUlxOnlzq08&sig_hash=6bd34838aa3f1db5bf02aefe58acb65f&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=%23economy%20%23internet&meta=1&state=all&authors=1
 * * multi-word tag `#"multi word"` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595883585962&oauth_nonce=oE27ShfE808qc5Qb&sig_hash=c534540f7b069213e0215e21da92dfda&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=%23%22multi%20word%22&meta=1&state=all&authors=1
 * * alt tag syntax `tag:economy` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595883468708&oauth_nonce=Mm2keU8qvSBoolIM&sig_hash=17d2649c6dae87737abc3e17a01716d4&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=tag%3Aeconomy&meta=1&state=all&authors=1
 *
 * ### My List, Archive and All Items
 * {get.state} is the only parameter changing between these examples:
 * * Searching My List for `the` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595875648412&oauth_nonce=aiGfmUdreeSZWbPt&sig_hash=29e4a04cc815fe35aed3343b6f5cebf4&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=the&meta=1&state=unread&authors=1
 * * Searching Archive for `the` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595874963456&oauth_nonce=5znSGT5N83Cpz8vW&sig_hash=93c7267edb02ac86fbc2d8db2e77d92d&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=the&meta=1&state=archive&authors=1
 * * Searching All Items for `the` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595874963456&oauth_nonce=5znSGT5N83Cpz8vW&sig_hash=93c7267edb02ac86fbc2d8db2e77d92d&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=the&meta=1&state=all&authors=1
 *
 * ### Content Type
 * {get.contentType} is the only parameter changing between these examples:
 * * Articles : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595883786092&oauth_nonce=AN3oE3fVzgs5LEge&sig_hash=069ab0307384a5b51ab7af814f992d52&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=cat&meta=1&state=all&contentType=article&authors=1
 * * Videos : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595883821823&oauth_nonce=JHdWhXns7OcU6YhX&sig_hash=0a63d53a575137a09ea50bed125d8b83&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=cat&meta=1&state=all&contentType=video&authors=1
 *
 * ### Tags
 * {get.tag} is the only parameter changing between these examples:
 * * Tagged with `economy` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595883896213&oauth_nonce=XIA3ztT2LOY8PDZS&sig_hash=14bea64f26899bc6b98962a0636aa5e2&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=internet&meta=1&state=all&tag=economy&authors=1
 * * Does not have a tag : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595883918307&oauth_nonce=4BQfcMRp5pnq75BV&sig_hash=1e20f1e63ab12e03d7aea138ddd6e326&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=cat&meta=1&state=all&tag=_untagged_&authors=1
 *
 * ### Favorites
 * {get.favorite} is the only parameter changing between these examples:
 * * Only favorites : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595883957881&oauth_nonce=oN56hazMb1W8Qq46&sig_hash=b48387be2acfed3e921f61a2204cc185&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=internet&meta=1&state=all&favorite=1&authors=1
 *
 * ### Sorting
 * {get.sort} is the only parameter changing between these examples:
 * * `relevance` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595884046916&oauth_nonce=5GckM6NPJeCauNa2&sig_hash=866390494019da704cda1d5ac0826f70&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=relevance&posts=1&tags=1&shares=1&search=cat&meta=1&state=all&authors=1
 * * `newest` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595884062662&oauth_nonce=yz9L7uqWBPwAxKsq&sig_hash=85c23ae84615e958a207e30be852e6b3&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=newest&posts=1&tags=1&shares=1&search=cat&meta=1&state=all&authors=1
 * * `oldest` : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595884062662&oauth_nonce=yz9L7uqWBPwAxKsq&sig_hash=85c23ae84615e958a207e30be852e6b3&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=oldest&posts=1&tags=1&shares=1&search=cat&meta=1&state=all&authors=1
 *
 * ### Paging
 * {get.count} and {get.offset} are the important params here:
 * * The first 30 : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595884161234&oauth_nonce=RoimfRQ2IgBbksrx&sig_hash=4daa05cbc99a7aafa8f480231e87a23b&images=1&offset=0&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=newest&posts=1&tags=1&shares=1&search=cat&meta=1&state=all&authors=1
 * * The next 30 : https://getpocket.com/v3/get?locale_lang=en-US&consumer_key=5513-8646141fb5902c766272e74d&guid=f2fd9pb8T9358E38e9Aa7aKD90g2T9eD953fb7G929Lr79L6a011cj8cO10Ir2c5&access_token=227c350e-5100-620c-8d99-ea85f2&oauth_timestamp=1595884169642&oauth_nonce=miD4oWXiwwC8vidD&sig_hash=95a4696bcb5ff9604391e5dcf5d38b9f&images=1&offset=30&count=30&annotations=1&cxt_search_type=new&positions=1&rediscovery=1&videos=1&sort=newest&posts=1&tags=1&shares=1&search=cat&meta=1&state=all&authors=1
 */
class SearchRepository @Inject constructor(
    private val pocket: Pocket,
) {

    fun getRecentSearches(): Flow<RecentSearches> =
        pocket.bindLocalAsFlow(
            pocket.spec().things()
                .recentSearches()
                .build()
        )

    fun addRecentSearch(text: String) {
        pocket.sync(
            null,
            pocket.spec().actions().recent_search()
                .search(text)
                .time(Timestamp.now())
                .build()
        )
    }
}