package org.wordpress.android.ui.mysite.cards.dashboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ActivityCard.ActivityCardWithItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DashboardDomainCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.DashboardPlansCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.PromoteWithBlazeCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardDomainBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardPlansBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.PromoteWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainTransferCardBuilderParams
import org.wordpress.android.ui.mysite.cards.blaze.BlazeCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.activity.ActivityCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.CREATE_FIRST
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.DRAFT
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.domain.DashboardDomainCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer.DomainTransferCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.plans.PlansCardBuilder
import org.wordpress.android.ui.utils.UiString.UiStringText

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CardsBuilderTest : BaseUnitTest() {
    @Mock
    lateinit var todaysStatsCardBuilder: TodaysStatsCardBuilder

    @Mock
    lateinit var postCardBuilder: PostCardBuilder

    @Mock
    lateinit var bloggingPromptCardsBuilder: BloggingPromptCardBuilder

    @Mock
    lateinit var blazeCardBuilder: BlazeCardBuilder

    @Mock
    lateinit var dashboardDomainCardBuilder: DashboardDomainCardBuilder

    @Mock
    lateinit var dashboardPlansCardBuilder: PlansCardBuilder

    @Mock
    lateinit var pagesCardBuilder: PagesCardBuilder

    @Mock
    lateinit var activityCardBuilder: ActivityCardBuilder

    @Mock
    lateinit var mDomainTransferCardBuilder: DomainTransferCardBuilder

    private lateinit var cardsBuilder: CardsBuilder

    @Before
    fun setUp() {
        cardsBuilder = CardsBuilder(
            todaysStatsCardBuilder,
            postCardBuilder,
            bloggingPromptCardsBuilder,
            mDomainTransferCardBuilder,
            blazeCardBuilder,
            dashboardDomainCardBuilder,
            dashboardPlansCardBuilder,
            pagesCardBuilder,
            activityCardBuilder
        )
    }

    @Test
    fun `given no stats, when cards are built, then todays stat card is not built`() {
        val cards = buildDashboardCards(hasTodaysStats = false)

        assertThat(cards.findTodaysStatsCard()).isNull()
    }

    @Test
    fun `given stats, when cards are built, then todays stat card is built`() {
        val cards = buildDashboardCards(hasTodaysStats = true)

        assertThat(cards.findTodaysStatsCard()).isNotNull
    }

    /* POST CARD */

    @Test
    fun `given no posts, when cards are built, then post card is not built`() {
        val cards = buildDashboardCards(hasPostsForPostCard = false)

        assertThat(cards.findPostCardWithPosts()).isNull()
    }

    @Test
    fun `given posts, when cards are built, then post card is built`() {
        val cards = buildDashboardCards(hasPostsForPostCard = true)

        assertThat(cards.findPostCardWithPosts()).isNotNull
    }

    /* BLOGGING PROMPT CARD */

    @Test
    fun `given no blogging prompt, when cards are built, then blogging prompt card is not built`() {
        val cards = buildDashboardCards(hasBlogginPrompt = false)

        assertThat(cards.findBloggingPromptCard()).isNull()
    }

    @Test
    fun `given blogging prompt, when cards are built, then blogging prompt card is built`() {
        val cards = buildDashboardCards(hasBlogginPrompt = true)

        assertThat(cards.findBloggingPromptCard()).isNotNull
    }

    /* BLOGGING PROMPT AND POST CARD */

    @Test
    fun `given blogging prompt and posts, both prompt and post cards are visible`() {
        val cards = buildDashboardCards(hasBlogginPrompt = true, hasPostsForPostCard = true)

        assertThat(cards.findBloggingPromptCard()).isNotNull
        assertThat(cards.findPostCardWithPosts()).isNotNull
    }

    @Test
    fun `given blogging prompt and no posts, prompt card is visible while post and next post cards are not`() {
        val cards = buildDashboardCards(hasBlogginPrompt = true, hasPostsForPostCard = false)

        assertThat(cards.findBloggingPromptCard()).isNotNull
        assertThat(cards.findPostCardWithPosts()).isNull()
        assertThat(cards.findNextPostCard()).isNull()
    }

    @Test
    fun `given no blogging prompt and no posts, next post card is visible and prompt card is not`() {
        val cards = buildDashboardCards(hasBlogginPrompt = false, hasPostsForPostCard = false)

        assertThat(cards.findBloggingPromptCard()).isNull()
        assertThat(cards.findPostCardWithPosts()).isNull()
        assertThat(cards.findNextPostCard()).isNotNull
    }

    @Test
    fun `given no blogging prompt and posts, next post card is not visible and prompt card is visible`() {
        val cards = buildDashboardCards(hasBlogginPrompt = false, hasPostsForPostCard = true)

        assertThat(cards.findBloggingPromptCard()).isNull()
        assertThat(cards.findPostCardWithPosts()).isNotNull
        assertThat(cards.findNextPostCard()).isNull()
    }

    /* ERROR CARD */

    @Test
    fun `given no show error, when cards are built, then error card is not built`() {
        val cards = buildDashboardCards(showErrorCard = false)

        assertThat(cards.findErrorCard()).isNull()
    }

    @Test
    fun `given show error, when cards are built, then error card is built`() {
        val cards = buildDashboardCards(showErrorCard = true)

        assertThat(cards.findErrorCard()).isNotNull
    }

    @Test
    fun `given is not eligible for blaze, when cards are built, then blaze card is not built`() {
        val cards = buildDashboardCards(isEligibleForBlaze = false)

        assertThat(cards.findPromoteWithBlazeCard()).isNull()
    }

    @Test
    fun `given is eligible for blaze, when cards are built, then blaze card is built`() {
        val cards = buildDashboardCards(isEligibleForBlaze = true)

        assertThat(cards.findPromoteWithBlazeCard()).isNotNull
    }

    @Test
    fun `given is not eligible for domain, when cards are built, then domain card is not built`() {
        val cards = buildDashboardCards(isEligibleForDomainCard = false)

        assertThat(cards.findDashboardDomainCard()).isNull()
    }

    @Test
    fun `given is eligible for domain, when cards are built, then domain card is built`() {
        val cards = buildDashboardCards(isEligibleForDomainCard = true)

        assertThat(cards.findDashboardDomainCard()).isNotNull
    }

    /* PLANS CARD */
    @Test
    fun `when is eligible for plans card, then plans card is built`() {
        val cards = buildDashboardCards(isEligibleForPlansCard = true)

        assertThat(cards.findDashboardPlansCard()).isNotNull
    }

    @Test
    fun `when is not eligible for plans card, then plans card is not built`() {
        val cards = buildDashboardCards(isEligibleForPlansCard = false)

        assertThat(cards.findDashboardPlansCard()).isNull()
    }

    @Test
    fun `given has pages, when cards are built, then pages card is not built`() {
        val cards = buildDashboardCards(hasPagesCard = false)

        assertThat(cards.findPagesCard()).isNull()
    }

    @Test
    fun `given has pages, when cards are built, then pages card is built`() {
        val cards = buildDashboardCards(hasPagesCard = true)

        assertThat(cards.findPagesCard()).isNotNull
    }

    @Test
    fun `given no activities, when cards are built, then activity card is not built`() {
        val cards = buildDashboardCards(hasActivityCard = false)

        assertThat(cards.findActivityCard()).isNull()
    }

    @Test
    fun `given has activities, when cards are built, then activity card is built`() {
        val cards = buildDashboardCards(hasActivityCard = true)

        assertThat(cards.findActivityCard()).isNotNull
    }

    private fun DashboardCards.findTodaysStatsCard() =
        this.cards.find { it is TodaysStatsCardWithData } as? TodaysStatsCardWithData

    private fun DashboardCards.findPostCardWithPosts() =
        this.cards.find { it is PostCardWithPostItems } as? PostCardWithPostItems

    private fun DashboardCards.findNextPostCard() =
        this.cards.find { it is PostCardWithoutPostItems } as? PostCardWithoutPostItems

    private fun DashboardCards.findBloggingPromptCard() =
        this.cards.find { it is BloggingPromptCard } as? BloggingPromptCard

    private fun DashboardCards.findPromoteWithBlazeCard() =
        this.cards.find { it is PromoteWithBlazeCard } as? PromoteWithBlazeCard

    private fun DashboardCards.findDashboardDomainCard() =
        this.cards.find { it is DashboardDomainCard } as? DashboardDomainCard

    private fun DashboardCards.findDashboardPlansCard() =
        this.cards.find { it is DashboardPlansCard } as? DashboardPlansCard

    private fun DashboardCards.findPagesCard() =
        this.cards.find { it is PagesCardWithData } as? PagesCardWithData

    private fun DashboardCards.findActivityCard() =
        this.cards.find { it is ActivityCardWithItems } as? ActivityCardWithItems

    private fun DashboardCards.findErrorCard() = this.cards.find { it is ErrorCard } as? ErrorCard

    private val todaysStatsCard = mock<TodaysStatsCardWithData>()

    private val blogingPromptCard = mock<BloggingPromptCardWithData>()

    private val promoteWithBlazeCard = mock<PromoteWithBlazeCard>()

    private val dashboardDomainCard = mock<DashboardDomainCard>()

    private val dashboardPlansCard = mock<DashboardPlansCard>()

    private val pagesCard = mock<PagesCardWithData>()

    private val activityCard = mock<ActivityCardWithItems>()

    private fun createPostCards() = listOf(
        PostCardWithPostItems(
            postCardType = DRAFT,
            title = UiStringText(""),
            postItems = emptyList(),
            footerLink = FooterLink(UiStringText(""), onClick = mock())
        )
    )

    private fun createPostPromptCards() = listOf(
        PostCardWithoutPostItems(
            postCardType = CREATE_FIRST,
            title = UiStringText(""),
            excerpt = UiStringText(""),
            imageRes = 0,
            footerLink = FooterLink(UiStringText(""), onClick = mock()),
            onClick = mock()
        )
    )

    private fun buildDashboardCards(
        hasTodaysStats: Boolean = false,
        hasPostsForPostCard: Boolean = false,
        hasBlogginPrompt: Boolean = false,
        showErrorCard: Boolean = false,
        isEligibleForDomainTransferCard: Boolean = false,
        isEligibleForBlaze: Boolean = false,
        isEligibleForDomainCard: Boolean = false,
        isEligibleForPlansCard: Boolean = false,
        hasPagesCard: Boolean = false,
        hasActivityCard: Boolean = false
    ): DashboardCards {
        doAnswer { if (hasTodaysStats) todaysStatsCard else null }.whenever(todaysStatsCardBuilder).build(any())
        doAnswer { if (hasPostsForPostCard) createPostCards() else createPostPromptCards() }.whenever(postCardBuilder)
            .build(any())
        doAnswer { if (hasBlogginPrompt) blogingPromptCard else null }.whenever(bloggingPromptCardsBuilder).build(any())
        doAnswer { if (isEligibleForBlaze) promoteWithBlazeCard else null }.whenever(blazeCardBuilder)
            .build(any())
        doAnswer { if (isEligibleForDomainCard) dashboardDomainCard else null }.whenever(dashboardDomainCardBuilder)
            .build(any())
        doAnswer { if (isEligibleForPlansCard) dashboardPlansCard else null }.whenever(dashboardPlansCardBuilder)
            .build(any())
        doAnswer { if (hasPagesCard) pagesCard else null }.whenever(pagesCardBuilder).build(any())
        doAnswer { if (hasActivityCard) activityCard else null }.whenever(activityCardBuilder).build(any())
        return cardsBuilder.build(
            dashboardCardsBuilderParams = DashboardCardsBuilderParams(
                showErrorCard = showErrorCard,
                onErrorRetryClick = { },
                todaysStatsCardBuilderParams = TodaysStatsCardBuilderParams(mock(), mock(), mock(), mock()),
                postCardBuilderParams = PostCardBuilderParams(mock(), mock(), mock()),
                bloggingPromptCardBuilderParams = BloggingPromptCardBuilderParams(
                    mock(), false, false, false, mock(), mock(), mock(), mock(), mock(), mock()
                ),
                domainTransferCardBuilderParams = DomainTransferCardBuilderParams(
                    isEligibleForDomainTransferCard, mock(), mock(), mock()
                ),
                blazeCardBuilderParams = PromoteWithBlazeCardBuilderParams(
                    mock(),
                    mock(),
                    mock()
                ),
                dashboardCardDomainBuilderParams = DashboardCardDomainBuilderParams(
                    isEligibleForDomainCard, mock(), mock(), mock()
                ),
                dashboardCardPlansBuilderParams = DashboardCardPlansBuilderParams(
                    isEligibleForPlansCard, mock(), mock(), mock()
                ),
                pagesCardBuilderParams = MySiteCardAndItemBuilderParams.PagesCardBuilderParams(
                    mock(),
                    mock(),
                    mock()
                ),
                activityCardBuilderParams = MySiteCardAndItemBuilderParams.ActivityCardBuilderParams(
                    mock(),
                    mock(),
                    mock()
                )
            )
        )
    }
}
