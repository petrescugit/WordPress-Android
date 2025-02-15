package org.wordpress.android.ui.sitecreation.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore.NewSiteError
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType.SITE_NAME_EXISTS
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceManager.SiteCreationServiceManagerListener
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.ui.sitecreation.usecases.CreateSiteUseCase

private const val LANGUAGE_ID = "lang_id"
private const val TIMEZONE_ID = "timezone_id"
private const val NEW_SITE_REMOTE_ID = 1234L
private const val NEW_SITE_REMOTE_URL = "new.site.url"

private val DUMMY_SITE_DATA: SiteCreationServiceData = SiteCreationServiceData(
    123,
    "slug",
    "domain",
    null,
    true,
)

private val IDLE_STATE = SiteCreationServiceState(IDLE)
private val CREATE_SITE_STATE = SiteCreationServiceState(CREATE_SITE)
private val SUCCESS_STATE = SiteCreationServiceState(SUCCESS, Pair(NEW_SITE_REMOTE_ID, NEW_SITE_REMOTE_URL))
private val FAILURE_STATE = SiteCreationServiceState(FAILURE)

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteCreationServiceManagerTest : BaseUnitTest() {
    @Mock
    lateinit var useCase: CreateSiteUseCase

    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var serviceListener: SiteCreationServiceManagerListener

    @Mock
    lateinit var tracker: SiteCreationTracker

    private lateinit var manager: SiteCreationServiceManager

    private lateinit var successEvent: OnNewSiteCreated
    private val genericErrorEvent = OnNewSiteCreated()
    private lateinit var siteExistsErrorEvent: OnNewSiteCreated

    @Before
    fun setUp() {
        manager = SiteCreationServiceManager(
            useCase,
            dispatcher,
            tracker,
            testDispatcher()
        )
        successEvent = OnNewSiteCreated(newSiteRemoteId = NEW_SITE_REMOTE_ID, url = NEW_SITE_REMOTE_URL)
        siteExistsErrorEvent = OnNewSiteCreated(newSiteRemoteId = NEW_SITE_REMOTE_ID, url = NEW_SITE_REMOTE_URL)
        genericErrorEvent.error = NewSiteError(GENERIC_ERROR, "")
        siteExistsErrorEvent.error = NewSiteError(SITE_NAME_EXISTS, "")
    }

    @Test
    fun verifyServiceStateIsBeingUpdated() = test {
        setSuccessfulResponses()
        startFlow()

        argumentCaptor<SiteCreationServiceState>().apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(SUCCESS_STATE)
        }
    }

    @Test
    fun verifyServiceStateUpdateToFailureOnError() = test {
        setGenericErrorResponses()
        startFlow()

        argumentCaptor<SiteCreationServiceState>().apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(SiteCreationServiceState(FAILURE))
        }
    }

    @Test
    fun verifyServicePropagatesCurrentStateWhenFails() = test {
        setGenericErrorResponses()
        val stateBeforeFailure = CREATE_SITE_STATE
        whenever(serviceListener.getCurrentState()).thenReturn(stateBeforeFailure)

        startFlow()

        argumentCaptor<SiteCreationServiceState>().apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(FAILURE_STATE.copy(payload = stateBeforeFailure))
        }
    }

    @Test
    fun verifyRetryWorksWhenTheSiteWasCreatedInPreviousAttempt() = test {
        setSiteExistsErrorResponses()
        retryFlow(previousState = CREATE_SITE_STATE.stepName)

        val argumentCaptor = argumentCaptor<SiteCreationServiceState>()
        argumentCaptor.apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(SUCCESS_STATE)
        }
    }

    @Test
    fun verifyRetryWorksWhenCreateSiteRequestFailed() = test {
        setGenericErrorResponses()
        startFlow()

        setSuccessfulResponses()
        retryFlow(previousState = CREATE_SITE_STATE.stepName)

        val argumentCaptor = argumentCaptor<SiteCreationServiceState>()
        argumentCaptor.apply {
            verify(serviceListener, times(6)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(FAILURE_STATE)

            assertThat(allValues[3]).isEqualTo(IDLE_STATE)
            assertThat(allValues[4]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[5]).isEqualTo(SUCCESS_STATE)
        }
    }

    @Test
    fun verifyUseCaseRegisteredToDispatcherOnCreate() {
        manager.onCreate()
        argumentCaptor<CreateSiteUseCase>().apply {
            verify(dispatcher).register(capture())
            assertThat(allValues[0]).isEqualTo(useCase)
        }
    }

    @Test
    fun verifyUseCaseUnregisteredFromDispatcherOnDestroy() {
        manager.onDestroy()
        argumentCaptor<CreateSiteUseCase>().apply {
            verify(dispatcher).unregister(capture())
            assertThat(allValues[0]).isEqualTo(useCase)
        }
    }

    @Test
    fun verifyDispatcherRegistrationHandledCorrectly() = test {
        setGenericErrorResponses()
        manager.onCreate()
        startFlow()
        setSuccessfulResponses()
        retryFlow(previousState = CREATE_SITE_STATE.stepName)
        manager.onDestroy()
        argumentCaptor<CreateSiteUseCase>().apply {
            verify(dispatcher).register(capture())
            assertThat(allValues[0]).isEqualTo(useCase)
        }
        argumentCaptor<CreateSiteUseCase>().apply {
            verify(dispatcher).unregister(capture())
            assertThat(allValues[0]).isEqualTo(useCase)
        }
    }

    @Test
    fun verifyIllegalStateExceptionInUseCaseResultsInServiceErrorState() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID))
            .thenThrow(IllegalStateException("Error"))
        startFlow()
        argumentCaptor<SiteCreationServiceState>().apply {
            verify(serviceListener, times(3)).updateState(capture())
            assertThat(allValues[0]).isEqualTo(IDLE_STATE)
            assertThat(allValues[1]).isEqualTo(CREATE_SITE_STATE)
            assertThat(allValues[2]).isEqualTo(SiteCreationServiceState(FAILURE))
        }
    }

    private fun startFlow() {
        manager.onStart(LANGUAGE_ID, TIMEZONE_ID, null, DUMMY_SITE_DATA, serviceListener)
    }

    private fun retryFlow(previousState: String) {
        manager.onStart(LANGUAGE_ID, TIMEZONE_ID, previousState, DUMMY_SITE_DATA, serviceListener)
    }

    private suspend fun setSuccessfulResponses() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID))
            .thenReturn(successEvent)
    }

    private suspend fun setGenericErrorResponses() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID))
            .thenReturn(genericErrorEvent)
    }

    private suspend fun setSiteExistsErrorResponses() = test {
        whenever(useCase.createSite(DUMMY_SITE_DATA, LANGUAGE_ID, TIMEZONE_ID))
            .thenReturn(siteExistsErrorEvent)
    }
}
