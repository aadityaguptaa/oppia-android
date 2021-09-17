package org.oppia.android.domain.locale

import org.oppia.android.util.locale.OppiaLocale
import java.util.Locale

/**
 * A profile to represent an Android [Locale] object which can be used to easily compare different
 * locales (based on the properties the app cares about), or reconstruct a [Locale] object.
 *
 * @property languageCode the IETF BCP 47 or ISO 639-2 language code
 * @property regionCode the IETF BCP 47 or ISO 3166 alpha-2 region code
 */
data class AndroidLocaleProfile(val languageCode: String, val regionCode: String) {
  /** Returns whether this profile matches the specified [otherProfile] for the given locale. */
  fun matches(
    machineLocale: OppiaLocale.MachineLocale,
    otherProfile: AndroidLocaleProfile,
  ): Boolean {
    return machineLocale.run {
      languageCode.equalsIgnoreCase(otherProfile.languageCode)
    } && machineLocale.run {
      val regionsAreEqual = regionCode.equalsIgnoreCase(otherProfile.regionCode)
      val eitherRegionIsWildcard =
        regionCode == REGION_WILDCARD || otherProfile.regionCode == REGION_WILDCARD
      return@run regionsAreEqual || eitherRegionIsWildcard
    }
  }

  companion object {
    /** A wildcard that will match against any region when provided. */
    const val REGION_WILDCARD = "*"

    /** Returns a new [AndroidLocaleProfile] that represents the specified Android [Locale]. */
    fun createFrom(androidLocale: Locale): AndroidLocaleProfile =
      AndroidLocaleProfile(androidLocale.language, androidLocale.country)
  }
}
