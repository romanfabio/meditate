package io.github.romanfabio.meditate.ui.about

import android.content.Context
import io.github.romanfabio.meditate.R
import org.json.JSONObject

data class LibraryInfo(
    val name: String,
    val version: String,
    val licenseIds: List<String>,
    val website: String?,
    val vendor: String?,
)

data class LicenseText(val id: String, val name: String, val content: String)

data class LicenseData(val libraries: List<LibraryInfo>, val licenses: List<LicenseText>)

/** The app's own license text (copied from the repository's LICENSE file at build time). */
fun readAppLicense(context: Context): String =
    context.assets.open("LICENSE.txt").bufferedReader().use { it.readText() }

/**
 * Third-party dependency licenses, generated at build time by the AboutLibraries Gradle plugin
 * from the resolved dependency metadata (res/raw/aboutlibraries.json). Only the plain JSON is
 * used; no AboutLibraries code ships in the app.
 */
fun readLicenseData(context: Context): LicenseData {
    val json = context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
    val root = JSONObject(json)

    val libs = root.optJSONArray("libraries")
    val libraries = buildList {
        if (libs != null) {
            for (i in 0 until libs.length()) {
                val o = libs.getJSONObject(i)
                val ids = o.optJSONArray("licenses")
                add(
                    LibraryInfo(
                        name = o.optString("name").ifBlank { o.optString("uniqueId") },
                        version = o.optString("artifactVersion"),
                        licenseIds = buildList {
                            if (ids != null) for (j in 0 until ids.length()) add(ids.getString(j))
                        },
                        website = o.optString("website").ifBlank { null },
                        vendor = o.optJSONObject("organization")?.optString("name")?.ifBlank { null },
                    ),
                )
            }
        }
    }.sortedBy { it.name.lowercase() }

    val licenseObj = root.optJSONObject("licenses")
    val licenses = buildList {
        if (licenseObj != null) {
            for (key in licenseObj.keys()) {
                val o = licenseObj.getJSONObject(key)
                add(LicenseText(key, o.optString("name", key), o.optString("content")))
            }
        }
    }.sortedBy { it.name }

    return LicenseData(libraries, licenses)
}
