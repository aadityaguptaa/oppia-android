package org.oppia.android.app.settings.profile

import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import dagger.Component
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.oppia.android.R
import org.oppia.android.app.activity.ActivityComponent
import org.oppia.android.app.activity.ActivityComponentFactory
import org.oppia.android.app.application.ApplicationComponent
import org.oppia.android.app.application.ApplicationInjector
import org.oppia.android.app.application.ApplicationInjectorProvider
import org.oppia.android.app.application.ApplicationModule
import org.oppia.android.app.application.ApplicationStartupListenerModule
import org.oppia.android.app.devoptions.DeveloperOptionsModule
import org.oppia.android.app.devoptions.DeveloperOptionsStarterModule
import org.oppia.android.app.shim.ViewBindingShimModule
import org.oppia.android.app.topic.PracticeTabModule
import org.oppia.android.app.translation.testing.ActivityRecreatorTestModule
import org.oppia.android.app.utility.OrientationChangeAction.Companion.orientationLandscape
import org.oppia.android.data.backends.gae.NetworkConfigProdModule
import org.oppia.android.data.backends.gae.NetworkModule
import org.oppia.android.domain.classify.InteractionsModule
import org.oppia.android.domain.classify.rules.continueinteraction.ContinueModule
import org.oppia.android.domain.classify.rules.dragAndDropSortInput.DragDropSortInputModule
import org.oppia.android.domain.classify.rules.fractioninput.FractionInputModule
import org.oppia.android.domain.classify.rules.imageClickInput.ImageClickInputModule
import org.oppia.android.domain.classify.rules.itemselectioninput.ItemSelectionInputModule
import org.oppia.android.domain.classify.rules.multiplechoiceinput.MultipleChoiceInputModule
import org.oppia.android.domain.classify.rules.numberwithunits.NumberWithUnitsRuleModule
import org.oppia.android.domain.classify.rules.numericinput.NumericInputRuleModule
import org.oppia.android.domain.classify.rules.ratioinput.RatioInputModule
import org.oppia.android.domain.classify.rules.textinput.TextInputRuleModule
import org.oppia.android.domain.exploration.lightweightcheckpointing.ExplorationStorageModule
import org.oppia.android.domain.hintsandsolution.HintsAndSolutionConfigModule
import org.oppia.android.domain.hintsandsolution.HintsAndSolutionProdModule
import org.oppia.android.domain.onboarding.ExpirationMetaDataRetrieverModule
import org.oppia.android.domain.oppialogger.LogStorageModule
import org.oppia.android.domain.oppialogger.loguploader.LogUploadWorkerModule
import org.oppia.android.domain.platformparameter.PlatformParameterModule
import org.oppia.android.domain.platformparameter.PlatformParameterSingletonModule
import org.oppia.android.domain.question.QuestionModule
import org.oppia.android.domain.topic.PrimeTopicAssetsControllerModule
import org.oppia.android.domain.workmanager.WorkManagerConfigurationModule
import org.oppia.android.testing.AccessibilityTestRule
import org.oppia.android.testing.TestLogReportingModule
import org.oppia.android.testing.espresso.EditTextInputAction
import org.oppia.android.testing.junit.InitializeDefaultLocaleRule
import org.oppia.android.testing.profile.ProfileTestHelper
import org.oppia.android.testing.robolectric.RobolectricModule
import org.oppia.android.testing.threading.TestCoroutineDispatchers
import org.oppia.android.testing.threading.TestDispatcherModule
import org.oppia.android.testing.time.FakeOppiaClockModule
import org.oppia.android.util.accessibility.AccessibilityTestModule
import org.oppia.android.util.caching.AssetModule
import org.oppia.android.util.caching.testing.CachingTestModule
import org.oppia.android.util.gcsresource.GcsResourceModule
import org.oppia.android.util.locale.LocaleProdModule
import org.oppia.android.util.logging.LoggerModule
import org.oppia.android.util.logging.firebase.FirebaseLogUploaderModule
import org.oppia.android.util.networking.NetworkConnectionDebugUtilModule
import org.oppia.android.util.networking.NetworkConnectionUtilDebugModule
import org.oppia.android.util.parser.html.HtmlParserEntityTypeModule
import org.oppia.android.util.parser.image.GlideImageLoaderModule
import org.oppia.android.util.parser.image.ImageParsingModule
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import javax.inject.Inject
import javax.inject.Singleton

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
  application = ProfileResetPinActivityTest.TestApplication::class,
  qualifiers = "port-xxhdpi"
)
class ProfileResetPinActivityTest {
  @get:Rule
  val initializeDefaultLocaleRule = InitializeDefaultLocaleRule()

  @get:Rule
  val accessibilityTestRule = AccessibilityTestRule()

  @get:Rule
  val activityTestRule: ActivityTestRule<ProfileResetPinActivity> = ActivityTestRule(
    ProfileResetPinActivity::class.java, /* initialTouchMode= */ true, /* launchActivity= */ false
  )

  @Inject
  lateinit var context: Context

  @Inject
  lateinit var profileTestHelper: ProfileTestHelper

  @Inject
  lateinit var testCoroutineDispatchers: TestCoroutineDispatchers

  @Inject
  lateinit var editTextInputAction: EditTextInputAction

  private val internalProfileId = 0

  @Before
  fun setUp() {
    Intents.init()
    setUpTestApplicationComponent()
    testCoroutineDispatchers.registerIdlingResource()
    profileTestHelper.initializeProfiles()
  }

  @Test
  fun testProfileResetPinActivity_hasCorrectActivityLabel() {
    activityTestRule.launchActivity(
      createProfileResetPinActivityIntent(
        internalProfileId,
        isAdmin = true,
      )
    )
    val title = activityTestRule.activity.title

    // Verify that the activity label is correct as a proxy to verify TalkBack will announce the
    // correct string when it's read out.
    assertThat(title).isEqualTo(context.getString(R.string.profile_reset_pin_activity_title))
  }

  private fun createProfileResetPinActivityIntent(
    internalProfileId: Int,
    isAdmin: Boolean
  ): Intent {
    return ProfileResetPinActivity.createProfileResetPinActivity(
      ApplicationProvider.getApplicationContext(), internalProfileId, isAdmin
    )
  }

  @After
  fun tearDown() {
    testCoroutineDispatchers.unregisterIdlingResource()
    Intents.release()
  }

  private fun setUpTestApplicationComponent() {
    ApplicationProvider.getApplicationContext<TestApplication>().inject(this)
  }

  @Test
  fun testProfileResetPin_withAdmin_inputBothPin_save_opensProfileEditActivity() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("12345"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("12345"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      testCoroutineDispatchers.runCurrent()
      intended(hasComponent(ProfileEditActivity::class.java.name))
    }
  }

  @Test
  fun testProfileResetPin_withAdmin_inputBothPin_imeAction_opensProfileEditActivity() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("12345"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("12345"),
        pressImeActionButton()
      )
      testCoroutineDispatchers.runCurrent()
      intended(hasComponent(ProfileEditActivity::class.java.name))
    }
  }

  @Test
  fun testProfileResetPin_withAdmin_configChange_inputBothPin_save_opensProfileEditActivity() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(scrollTo())
        .perform(
          editTextInputAction.appendText("12345"),
          closeSoftKeyboard()
        )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        scrollTo()
      ).perform(
        editTextInputAction.appendText("12345"),
        closeSoftKeyboard()
      )

      onView(withId(R.id.profile_reset_save_button)).perform(scrollTo()).perform(click())
      testCoroutineDispatchers.runCurrent()
      intended(hasComponent(ProfileEditActivity::class.java.name))
    }
  }

  @Test
  fun testProfileResetPin_withUser_inputBothPin_save_opensProfileEditActivity() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("123"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("123"),
        pressImeActionButton()
      )
      testCoroutineDispatchers.runCurrent()
      intended(hasComponent(ProfileEditActivity::class.java.name))
    }
  }

  @Test
  fun testProfileResetPin_withAdmin_inputShortPin_save_pinLengthErrorIsShown() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("1234"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("1234"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      onView(withId(R.id.profile_reset_input_pin))
        .check(matches(hasErrorText(R.string.profile_reset_pin_error_admin_pin_length)))
    }
  }

  @Test
  fun testProfileResetPin_withAdmin_inputShortPin_save_configChange_pinLengthErrorIsShown() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("1234"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("1234"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(scrollTo()).perform(click())
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_reset_input_pin))
        .check(matches(hasErrorText(R.string.profile_reset_pin_error_admin_pin_length)))
    }
  }

  @Test
  fun testProfileResetPin_withAdmin_inputShortPin_save_inputPin_errorIsCleared() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("1234"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("5"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_input_pin))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testProfileResetPin_withAdmin_inputShortPin_save_inputPin_configChange_errorIsCleared() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("1234"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("5"),
        closeSoftKeyboard()
      )
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_reset_input_pin))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testProfileResetPin_withAdmin_inputWrongConfirmPin_save_confirmWrongErrorIsShown() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("12345"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("1234"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      onView(withId(R.id.profile_reset_input_confirm_pin))
        .check(matches(hasErrorText(R.string.add_profile_error_pin_confirm_wrong)))
    }
  }

  @Test
  fun testProfileResetPin_withAdmin_inputWrongConfirmPin_configChange_confirmWrongErrorIsShown() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("12345"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("1234"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_reset_input_confirm_pin))
        .perform(scrollTo())
        .check(matches(hasErrorText(R.string.add_profile_error_pin_confirm_wrong)))
    }
  }

  @Test
  fun testProfileResetPin_inputPin_configChange_inputFieldsExist_saveButtonIsClickable() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("12345"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("12345"),
        closeSoftKeyboard()
      )
      onView(isRoot()).perform(orientationLandscape())
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(scrollTo())
        .check(matches(withText("12345")))
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(scrollTo()).check(matches(withText("12345")))
      onView(withId(R.id.profile_reset_save_button)).perform(scrollTo())
        .check(matches(isClickable()))
    }
  }

  @Test
  fun testProfileResetPin_withAdmin_wrongConfirmPin_save_inputConfirmPin_errorIsCleared() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 0,
        isAdmin = true
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("12345"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("1234"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("5"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_input_confirm_pin))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testProfileResetPin_withUser_inputShortPin_save_pinLengthErrorIsShown() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("12"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("12"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      onView(withId(R.id.profile_reset_input_pin))
        .check(matches(hasErrorText(R.string.profile_reset_pin_error_user_pin_length)))
    }
  }

  @Test
  fun testProfileResetPin_withUser_inputShortPin_save_inputPin_errorIsCleared() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("12"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("3"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_input_pin))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testProfileResetPin_withUser_inputWrongConfirmPin_save_confirmWrongErrorIsShown() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("123"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("12"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      onView(withId(R.id.profile_reset_input_confirm_pin))
        .check(matches(hasErrorText(R.string.add_profile_error_pin_confirm_wrong)))
    }
  }

  @Test
  fun testProfileResetPin_withUser_inputWrongConfirmPin_save_inputConfirmPin_errorIsCleared() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("123"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("12"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).perform(click())
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("3"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_input_confirm_pin))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testProfileResetPin_default_saveButtonIsNotClickable() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      onView(withId(R.id.profile_reset_save_button)).check(matches(not(isClickable())))
    }
  }

  @Test
  fun testProfileResetPin_default_configChange_saveButtonIsNotClickable() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_reset_save_button)).perform(scrollTo())
        .check(matches(not(isClickable())))
    }
  }

  @Test
  fun testProfileResetPin_inputPin_saveButtonIsNotClickable() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("123"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).check(matches(not(isClickable())))
    }
  }

  @Test
  fun testProfileResetPin_inputPin_configChange_saveButtonIsNotClickable() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("123"),
        closeSoftKeyboard()
      )
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_reset_save_button)).perform(scrollTo())
        .check(matches(not(isClickable())))
    }
  }

  @Test
  fun testProfileResetPin_inputPin_inputConfirmPin_saveButtonIsClickable() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("123"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("12"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).check(matches(isClickable()))
    }
  }

  @Test
  fun testProfileResetPin_inputPin_clickableSaveButton_clearPin_saveButtonIsNotClickable() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("123"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("12"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).check(matches(isClickable()))
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        clearText(),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).check(matches(not(isClickable())))
    }
  }

  @Test
  fun testProfileResetPin_withUser_inputWrongConfirmPin_saveButtonIsNotClickable() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("123"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("12"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).check(matches(isClickable()))
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        clearText(),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).check(matches(not(isClickable())))
    }
  }

  @Test
  fun testProfileResetPin_withUser_inputWrongConfirmPin_configChange_saveButtonIsNotClickable() {
    launch<ProfileResetPinActivity>(
      ProfileResetPinActivity.createProfileResetPinActivity(
        context = context,
        profileId = 1,
        isAdmin = false
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_reset_input_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("123"),
        closeSoftKeyboard()
      )
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        editTextInputAction.appendText("12"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.profile_reset_save_button)).check(matches(isClickable()))
      onView(
        allOf(
          withId(R.id.profile_reset_input_confirm_pin_edit_text),
          isDescendantOfA(withId(R.id.profile_reset_input_confirm_pin))
        )
      ).perform(
        clearText(),
        closeSoftKeyboard()
      )
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.profile_reset_save_button)).perform(scrollTo())
        .check(matches(not(isClickable())))
    }
  }

  private fun hasErrorText(@StringRes expectedErrorTextId: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
      override fun matchesSafely(view: View): Boolean {
        val expectedErrorText = context.resources.getString(expectedErrorTextId)
        return (view as TextInputLayout).error == expectedErrorText
      }

      override fun describeTo(description: Description) {
        description.appendText("TextInputLayout's error")
      }
    }
  }

  private fun hasNoErrorText(): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
      override fun matchesSafely(view: View): Boolean {
        return (view as TextInputLayout).error.isNullOrEmpty()
      }

      override fun describeTo(description: Description) {
        description.appendText("")
      }
    }
  }

  // TODO(#59): Figure out a way to reuse modules instead of needing to re-declare them.
  @Singleton
  @Component(
    modules = [
      RobolectricModule::class, PlatformParameterModule::class, TestDispatcherModule::class,
      ApplicationModule::class, LoggerModule::class, ContinueModule::class,
      FractionInputModule::class, ItemSelectionInputModule::class, MultipleChoiceInputModule::class,
      NumberWithUnitsRuleModule::class, NumericInputRuleModule::class, TextInputRuleModule::class,
      DragDropSortInputModule::class, ImageClickInputModule::class, InteractionsModule::class,
      GcsResourceModule::class, GlideImageLoaderModule::class, ImageParsingModule::class,
      HtmlParserEntityTypeModule::class, QuestionModule::class, TestLogReportingModule::class,
      AccessibilityTestModule::class, LogStorageModule::class, CachingTestModule::class,
      PrimeTopicAssetsControllerModule::class, ExpirationMetaDataRetrieverModule::class,
      ViewBindingShimModule::class, RatioInputModule::class, WorkManagerConfigurationModule::class,
      ApplicationStartupListenerModule::class, LogUploadWorkerModule::class,
      HintsAndSolutionConfigModule::class, HintsAndSolutionProdModule::class,
      FirebaseLogUploaderModule::class, FakeOppiaClockModule::class, PracticeTabModule::class,
      DeveloperOptionsStarterModule::class, DeveloperOptionsModule::class,
      ExplorationStorageModule::class, NetworkModule::class, NetworkConfigProdModule::class,
      NetworkConnectionUtilDebugModule::class, NetworkConnectionDebugUtilModule::class,
      AssetModule::class, LocaleProdModule::class, ActivityRecreatorTestModule::class,
      PlatformParameterSingletonModule::class
    ]
  )
  interface TestApplicationComponent : ApplicationComponent {
    @Component.Builder
    interface Builder : ApplicationComponent.Builder

    fun inject(profileResetPinActivityTest: ProfileResetPinActivityTest)
  }

  class TestApplication : Application(), ActivityComponentFactory, ApplicationInjectorProvider {
    private val component: TestApplicationComponent by lazy {
      DaggerProfileResetPinActivityTest_TestApplicationComponent.builder()
        .setApplication(this)
        .build() as TestApplicationComponent
    }

    fun inject(profileResetPinActivityTest: ProfileResetPinActivityTest) {
      component.inject(profileResetPinActivityTest)
    }

    override fun createActivityComponent(activity: AppCompatActivity): ActivityComponent {
      return component.getActivityComponentBuilderProvider().get().setActivity(activity).build()
    }

    override fun getApplicationInjector(): ApplicationInjector = component
  }
}
