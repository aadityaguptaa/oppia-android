package org.oppia.android.app.settings.profile

import android.app.Application
import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
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

/** Test for [ProfileRenameActivity]. */
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
  application = ProfileRenameActivityTest.TestApplication::class,
  qualifiers = "port-xxhdpi"
)
class ProfileRenameActivityTest {
  @get:Rule
  val initializeDefaultLocaleRule = InitializeDefaultLocaleRule()

  @get:Rule
  val accessibilityTestRule = AccessibilityTestRule()

  @Inject
  lateinit var context: Context

  @Inject
  lateinit var profileTestHelper: ProfileTestHelper

  @Inject
  lateinit var testCoroutineDispatchers: TestCoroutineDispatchers

  @Inject
  lateinit var editTextInputAction: EditTextInputAction

  @get:Rule
  val activityTestRule: ActivityTestRule<ProfileRenameActivity> = ActivityTestRule(
    ProfileRenameActivity::class.java,
    /* initialTouchMode= */ true,
    /* launchActivity= */ false
  )

  @Before
  fun setUp() {
    Intents.init()
    setUpTestApplicationComponent()
    profileTestHelper.initializeProfiles()
  }

  @After
  fun tearDown() {
    Intents.release()
  }

  private fun setUpTestApplicationComponent() {
    ApplicationProvider.getApplicationContext<TestApplication>().inject(this)
  }

  @Test
  fun testProfileRenameActivity_hasCorrectActivityLabel() {
    activityTestRule.launchActivity(
      ProfileRenameActivity.createProfileRenameActivity(
        context = this.context.applicationContext,
        profileId = 1
      )
    )
    val title = activityTestRule.activity.title

    // Verify that the activity label is correct as a proxy to verify TalkBack will announce the
    // correct string when it's read out.
    assertThat(title).isEqualTo(context.getString(R.string.profile_rename_activity_title))
  }

  @Test
  fun testProfileRenameActivity_inputNewName_clickSave_checkProfileEditActivityIsOpen() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(editTextInputAction.appendText("James"))
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_rename_save_button)).perform(click())
      testCoroutineDispatchers.runCurrent()
      intended(hasComponent(ProfileEditActivity::class.java.name))
    }
  }

  @Test
  fun testProfileRenameActivity_inputNewName_clickImeActionButton_checkProfileEditActivityIsOpen() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(
        editTextInputAction.appendText("James"),
        pressImeActionButton()
      )
      testCoroutineDispatchers.runCurrent()
      intended(hasComponent(ProfileEditActivity::class.java.name))
    }
  }

  @Test
  fun testProfileRenameActivity_inputNewName_configurationChange_checkSaveIsEnabled() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(editTextInputAction.appendText("James"))
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_rename_save_button)).check(matches(isEnabled()))
    }
  }

  @Test
  fun testProfileRenameActivity_inputNewName_configurationChange_inputTextExists() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(editTextInputAction.appendText("James"))
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).check(
        matches(
          withText("James")
        )
      )
    }
  }

  @Test
  fun testProfileRenameActivity_inputOldName_clickSave_checkNameNotUniqueError() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(editTextInputAction.appendText("Admin"))
      onView(withId(R.id.profile_rename_save_button)).perform(click())
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_rename_input))
        .check(matches(hasErrorText(R.string.add_profile_error_name_not_unique)))
    }
  }

  @Test
  fun testProfileRenameActivity_inputOldName_clickSave_inputName_checkErrorIsCleared() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(editTextInputAction.appendText("Admin"))
      onView(withId(R.id.profile_rename_save_button)).perform(click())
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(editTextInputAction.appendText(" "))
      onView(withId(R.id.profile_rename_input))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testProfileRenameActivity_inputNameWithNumbers_clickCreate_checkNameOnlyLettersError() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(editTextInputAction.appendText("123"))
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_rename_save_button)).perform(click())
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_rename_input))
        .check(matches(hasErrorText(R.string.add_profile_error_name_only_letters)))
    }
  }

  @Test
  fun testProfileRenameActivity_inputNameWithNumbers_clickCreate_inputName_checkErrorIsCleared() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(editTextInputAction.appendText("123"))
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_rename_save_button)).perform(click())
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(editTextInputAction.appendText(" "))
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_rename_input))
        .check(matches(hasNoErrorText()))
    }
  }

  @Test
  fun testProfileRenameActivity_inputName_changeConfiguration_checkNameIsDisplayed() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(
        editTextInputAction.appendText("test"),
        closeSoftKeyboard()
      )
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      testCoroutineDispatchers.runCurrent()
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).check(
        matches(
          withText("test")
        )
      )
    }
  }

  @Test
  fun testProfileRenameActivity_inputOldName_clickSave_changeConfiguration_errorIsVisible() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(
        allOf(
          withId(R.id.profile_rename_input_edit_text),
          isDescendantOfA(withId(R.id.profile_rename_input))
        )
      ).perform(
        editTextInputAction.appendText("Admin"),
        closeSoftKeyboard()
      )
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_rename_save_button)).perform(click())
      testCoroutineDispatchers.runCurrent()
      onView(isRoot()).perform(orientationLandscape())
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_rename_input))
        .check(matches(hasErrorText(R.string.add_profile_error_name_not_unique)))
    }
  }

  @Test
  fun testProfileRenameActivity_clickSave_changeConfiguration_saveButtonIsNotClickable() {
    launch<ProfileRenameActivity>(
      ProfileRenameActivity.createProfileRenameActivity(
        context = context,
        profileId = 1
      )
    ).use {
      onView(withId(R.id.profile_rename_save_button)).check(matches(not(isClickable())))
      onView(isRoot()).perform(orientationLandscape())
      testCoroutineDispatchers.runCurrent()
      onView(withId(R.id.profile_rename_save_button)).check(matches(not(isClickable())))
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

    fun inject(profileRenameActivityTest: ProfileRenameActivityTest)
  }

  class TestApplication : Application(), ActivityComponentFactory, ApplicationInjectorProvider {
    private val component: TestApplicationComponent by lazy {
      DaggerProfileRenameActivityTest_TestApplicationComponent.builder()
        .setApplication(this)
        .build() as TestApplicationComponent
    }

    fun inject(profileRenameActivityTest: ProfileRenameActivityTest) {
      component.inject(profileRenameActivityTest)
    }

    override fun createActivityComponent(activity: AppCompatActivity): ActivityComponent {
      return component.getActivityComponentBuilderProvider().get().setActivity(activity).build()
    }

    override fun getApplicationInjector(): ApplicationInjector = component
  }
}
