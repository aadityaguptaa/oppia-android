package org.oppia.android.app.profile

import android.app.Application
import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withInputType
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import dagger.Component
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
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
import org.oppia.android.app.home.HomeActivity
import org.oppia.android.app.shim.ViewBindingShimModule
import org.oppia.android.app.topic.PracticeTabModule
import org.oppia.android.app.translation.testing.ActivityRecreatorTestModule
import org.oppia.android.app.utility.EspressoTestsMatchers.withDrawable
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
  application = PinPasswordActivityTest.TestApplication::class,
  qualifiers = "port-xxhdpi"
)
class PinPasswordActivityTest {
  @get:Rule
  val initializeDefaultLocaleRule = InitializeDefaultLocaleRule()

  // TODO(#3362): Use AccessibilityTestRule

  @get:Rule
  val activityTestRule: ActivityTestRule<PinPasswordActivity> = ActivityTestRule(
    PinPasswordActivity::class.java, /* initialTouchMode= */ true, /* launchActivity= */ false
  )

  @Inject lateinit var context: Context

  @Inject
  lateinit var profileTestHelper: ProfileTestHelper

  @Inject
  lateinit var testCoroutineDispatchers: TestCoroutineDispatchers

  @Inject
  lateinit var editTextInputAction: EditTextInputAction

  private val adminPin = "12345"
  private val adminId = 0
  private val userId = 1

  @Before
  fun setUp() {
    Intents.init()
    setUpTestApplicationComponent()
    profileTestHelper.initializeProfiles()
    testCoroutineDispatchers.registerIdlingResource()
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
  fun testPinPassword_withAdmin_keyboardIsVisibleByDefault() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text)).check(matches(hasFocus()))
    }
  }

  @Test
  fun testPinPassword_pinView_hasContentDescription() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text)).check(
        matches(
          withContentDescription(
            context.resources.getString(
              R.string.enter_your_pin
            )
          )
        )
      )
    }
  }

  @Test
  fun testPinPassword_configChange_pinView_hasContentDescription() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.pin_password_input_pin_edit_text)).check(
        matches(
          withContentDescription(
            context.resources.getString(
              R.string.enter_your_pin
            )
          )
        )
      )
    }
  }

  @Test
  fun testPinPassword_withAdmin_inputCorrectPin_opensHomeActivity() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText("12345"))
      testCoroutineDispatchers.runCurrent()
      intended(hasComponent(HomeActivity::class.java.name))
    }
  }

  @Test
  fun testPinPassword_withUser_inputCorrectPin_opensHomeActivity() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText("123"))
      testCoroutineDispatchers.runCurrent()
      intended(hasComponent(HomeActivity::class.java.name))
    }
  }

  @Test
  fun testPinPassword_withAdmin_inputWrongPin_incorrectPinShows() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      closeSoftKeyboard()
      onView(withId(R.id.pin_password_input_pin_edit_text)).perform(closeSoftKeyboard())
        .perform(editTextInputAction.appendText("54321"), closeSoftKeyboard())
      onView(withText(context.getString(R.string.pin_password_incorrect_pin))).check(
        matches(
          isDisplayed()
        )
      )
    }
  }

  @Test
  fun testPinPasswordActivity_hasCorrectActivityLabel() {
    activityTestRule.launchActivity(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId,
      )
    )
    val title = activityTestRule.activity.title

    // Verify that the activity label is correct as a proxy to verify TalkBack will announce the
    // correct string when it's read out.
    assertThat(title).isEqualTo(context.getString(R.string.pin_password_activity_title))
  }

  @Test
  fun testPinPassword_withUser_inputWrongPin_incorrectPinShows() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.pin_password_input_pin_edit_text)).perform(
        editTextInputAction.appendText("321"), closeSoftKeyboard()
      )
      onView(withText(context.getString(R.string.pin_password_incorrect_pin))).check(
        matches(
          isDisplayed()
        )
      )
    }
  }

  @Test
  fun testPinPassword_withAdmin_forgot_opensAdminForgotDialog() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.pin_password_input_pin_edit_text)).perform(
        editTextInputAction.appendText(""),
        closeSoftKeyboard()
      )
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(withText(getPinPasswordForgotMessage()))
        .inRoot(isDialog())
        .check(matches(isDisplayed()))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputWrongAdminPin_wrongAdminPinError() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.forgot_pin)).perform(click())
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("1234"), closeSoftKeyboard())

      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasErrorText(R.string.admin_settings_incorrect)))
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("5"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPinAndShortPin_pinLengthError() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("12345"), closeSoftKeyboard())

      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())

      onView(
        allOf(
          withId(R.id.reset_pin_input_pin_edit_text),
          isDescendantOfA(withId(R.id.reset_pin_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("32"), closeSoftKeyboard())

      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(withId(R.id.reset_pin_input_pin))
        .check(matches(hasErrorText(R.string.add_profile_error_pin_length)))
      onView(
        allOf(
          withId(R.id.reset_pin_input_pin_edit_text),
          isDescendantOfA(withId(R.id.reset_pin_input_pin))
        )
      ).perform(
        editTextInputAction.appendText("1"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.reset_pin_input_pin))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPinAndNewPinAndOldPin_wrongPinError() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("12345"), closeSoftKeyboard())

      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(
        allOf(
          withId(R.id.reset_pin_input_pin_edit_text),
          isDescendantOfA(withId(R.id.reset_pin_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("321"), closeSoftKeyboard())

      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())

      testCoroutineDispatchers.runCurrent()
      onView(withText(context.getString(R.string.pin_password_close)))
        .inRoot(isDialog())
        .perform(click())
      onView(withId(R.id.pin_password_input_pin_edit_text)).perform(
        editTextInputAction.appendText("123"),
        closeSoftKeyboard()
      )
      onView(withText(context.getString(R.string.pin_password_incorrect_pin)))
        .check(matches(isDisplayed()))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPinAndNewPin_opensHomeActivity() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("12345"), closeSoftKeyboard())

      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(
        allOf(
          withId(R.id.reset_pin_input_pin_edit_text),
          isDescendantOfA(withId(R.id.reset_pin_input_pin))
        )
      )
        .inRoot(isDialog())
        .perform(editTextInputAction.appendText("321"), closeSoftKeyboard())
      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())

      testCoroutineDispatchers.runCurrent()
      onView(withText(context.getString(R.string.pin_password_close)))
        .inRoot(isDialog())
        .perform(click())
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText("321"))
      testCoroutineDispatchers.runCurrent()
      intended(hasComponent(HomeActivity::class.java.name))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPin_configChange_inputPinIsPresent() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("1234"), closeSoftKeyboard())
      onView(isRoot()).perform(orientationLandscape())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .check(matches(withText("1234")))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPin_submit_configChange_resetPinDisplayed() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("12345"), closeSoftKeyboard())
      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(isRoot()).perform(orientationLandscape())
      onView(withText(context.getString(R.string.reset_pin_enter)))
        .inRoot(isDialog())
        .check(matches(isDisplayed()))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPin_submit_inputNewPin_pinChanged() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("12345"), closeSoftKeyboard())

      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(
        allOf(
          withId(R.id.reset_pin_input_pin_edit_text),
          isDescendantOfA(withId(R.id.reset_pin_input_pin))
        )
      )
        .inRoot(isDialog())
        .perform(editTextInputAction.appendText("123"), closeSoftKeyboard())
      onView(isRoot()).perform(orientationLandscape())
      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      testCoroutineDispatchers.runCurrent()
      onView(
        withText(context.getString(R.string.pin_password_success))
      ).inRoot(isDialog()).check(matches(isDisplayed()))
    }
  }

  @Test
  fun testPinPassword_withAdmin_forgot_configChange_opensAdminForgotDialog() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      closeSoftKeyboard()
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(isRoot()).perform(orientationLandscape())
      onView(withText(getPinPasswordForgotMessage()))
        .inRoot(isDialog())
        .check(matches(isDisplayed()))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputWrongAdminPin_configChange_wrongAdminPinError() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("1234"), closeSoftKeyboard())
      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasErrorText(R.string.admin_settings_incorrect)))
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("5"), closeSoftKeyboard())
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPinAndIncorrectPin_errorIsDisplayed() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("1234"), closeSoftKeyboard())
      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasErrorText(R.string.admin_settings_incorrect)))
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasErrorText(R.string.admin_settings_incorrect)))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPinAndNullPin_errorIsDisplayed() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasErrorText(R.string.admin_auth_null)))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPinAndNullPin_configChange_errorIsDisplayed() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasErrorText(R.string.admin_auth_null)))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPinAndNullPin_imeAction_errorIsDisplayed() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText(""), pressImeActionButton())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasErrorText(R.string.admin_auth_null)))
    }
  }

  @Test
  fun testPinPassword_user_forgot_adminPinAndNullPin_configChange_imeAction_errorIsDisplayed() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText(""), pressImeActionButton())
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasErrorText(R.string.admin_auth_null)))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputNullAdminPin_configChange_wrongAdminPinError() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasErrorText(R.string.admin_auth_null)))
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("1"), closeSoftKeyboard())
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.admin_settings_input_pin))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testPinPassword_withUser_forgot_inputAdminPinAndInvalidPin_errorIsDisplayed() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = userId
      )
    ).use {
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .perform(editTextInputAction.appendText(""), closeSoftKeyboard())
      onView(withId(R.id.forgot_pin)).perform(click())
      onView(
        allOf(
          withId(R.id.admin_settings_input_pin_edit_text),
          isDescendantOfA(withId(R.id.admin_settings_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("12345"), closeSoftKeyboard())

      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(
        allOf(
          withId(R.id.reset_pin_input_pin_edit_text),
          isDescendantOfA(withId(R.id.reset_pin_input_pin))
        )
      ).inRoot(isDialog())
        .perform(editTextInputAction.appendText("11"), closeSoftKeyboard())
      onView(withText(context.getString(R.string.admin_settings_submit)))
        .inRoot(isDialog())
        .perform(click())
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.reset_pin_input_pin))
        .check(matches(hasErrorText(R.string.add_profile_error_pin_length)))
    }
  }

  @Test
  fun testPinPassword_withAdmin_inputWrongPin_configChange_incorrectPinIsDisplayed() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      closeSoftKeyboard()
      onView(withId(R.id.pin_password_input_pin_edit_text)).perform(
        editTextInputAction.appendText("54321"),
        closeSoftKeyboard()
      )
      onView(isRoot()).perform(orientationLandscape())
      onView(withText(context.getString(R.string.pin_password_incorrect_pin))).check(
        matches(
          isDisplayed()
        )
      )
    }
  }

  @Test
  fun testPinPassword_withAdmin_checkShowHidePassword_defaultText() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      closeSoftKeyboard()
      onView(withText(context.getString(R.string.pin_password_show))).check(matches(isDisplayed()))
    }
  }

  @Test
  fun testPinPassword_withAdmin_checkShowHidePassword_defaultImage() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      onView(withId(R.id.show_hide_password_image_view))
        .check(
          matches(
            withDrawable(
              R.drawable.ic_hide_eye_icon
            )
          )
        )
    }
  }

  @Test
  fun testPinPassword_withAdmin_showHideIcon_hasPasswordHiddenContentDescription() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      onView(withId(R.id.show_hide_password_image_view))
        .check(
          matches(
            withContentDescription(
              R.string.password_hidden_icon
            )
          )
        )
    }
  }

  @Test
  fun testPinPassword_withAdmin_showHidePassword_textChangesToHide() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.show_pin)).perform(click())
      onView(withText(context.getString(R.string.pin_password_hide))).check(matches(isDisplayed()))
    }
  }

  @Test
  fun testPinPassword_withAdmin_clickShowHideIcon_hasPasswordShownContentDescription() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      closeSoftKeyboard()
      onView(withId(R.id.show_pin)).perform(click())
      onView(withId(R.id.show_hide_password_image_view))
        .check(
          matches(
            withContentDescription(
              R.string.password_shown_icon
            )
          )
        )
    }
  }

  @Test
  fun testPinPassword_withAdmin_showHidePassword_imageChangesToShow() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      closeSoftKeyboard()
      onView(withId(R.id.show_pin)).perform(click())
      onView(withId(R.id.show_hide_password_image_view))
        .check(
          matches(
            withDrawable(
              R.drawable.ic_show_eye_icon
            )
          )
        )
    }
  }

  @Test
  fun testPinPassword_withAdmin_showHidePassword_configChange_showViewIsShown() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()
      closeSoftKeyboard()
      onView(withId(R.id.show_pin)).perform(click())
      onView(isRoot()).perform(orientationLandscape())
      onView(withText(context.getString(R.string.pin_password_hide))).check(matches(isDisplayed()))
      onView(withId(R.id.show_hide_password_image_view))
        .check(
          matches(
            withDrawable(
              R.drawable.ic_show_eye_icon
            )
          )
        )
    }
  }

  @Test
  fun testPinPassword_checkInputType_showHidePassword_inputTypeIsSame() {
    ActivityScenario.launch<PinPasswordActivity>(
      PinPasswordActivity.createPinPasswordActivityIntent(
        context = context,
        adminPin = adminPin,
        profileId = adminId
      )
    ).use {
      testCoroutineDispatchers.runCurrent()

      var inputType: Int = 0
      it.onActivity {
        inputType =
          it.findViewById<TextInputEditText>(R.id.pin_password_input_pin_edit_text).inputType
      }
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .check(matches(withInputType(inputType)))
      onView(withId(R.id.show_pin)).perform(click())
      onView(withId(R.id.pin_password_input_pin_edit_text))
        .check(matches(withInputType(inputType)))
    }
  }

  private fun getAppName(): String = context.resources.getString(R.string.app_name)

  private fun getPinPasswordForgotMessage(): String =
    context.resources.getString(R.string.pin_password_forgot_message, getAppName())

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

    fun inject(pinPasswordActivityTest: PinPasswordActivityTest)
  }

  class TestApplication : Application(), ActivityComponentFactory, ApplicationInjectorProvider {
    private val component: TestApplicationComponent by lazy {
      DaggerPinPasswordActivityTest_TestApplicationComponent.builder()
        .setApplication(this)
        .build() as TestApplicationComponent
    }

    fun inject(pinPasswordActivityTest: PinPasswordActivityTest) {
      component.inject(pinPasswordActivityTest)
    }

    override fun createActivityComponent(activity: AppCompatActivity): ActivityComponent {
      return component.getActivityComponentBuilderProvider().get().setActivity(activity).build()
    }

    override fun getApplicationInjector(): ApplicationInjector = component
  }
}
