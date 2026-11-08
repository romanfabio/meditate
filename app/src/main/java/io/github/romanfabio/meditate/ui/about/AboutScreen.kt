package io.github.romanfabio.meditate.ui.about

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.romanfabio.meditate.R

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appLicense = remember { runCatching { readAppLicense(context) }.getOrDefault("MIT License") }
    val data = remember { runCatching { readLicenseData(context) }.getOrDefault(LicenseData(emptyList(), emptyList())) }
    val version = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            "${info.versionName} (${info.longVersionCode})"
        }.getOrDefault("")
    }

    var appLicenseOpen by rememberSaveable { mutableStateOf(false) }
    var openLicenseId by rememberSaveable { mutableStateOf<String?>(null) }
    var openLibrary by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Column(Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                if (version.isNotEmpty()) {
                    Text(
                        stringResource(R.string.about_version, version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // --- This app's license ---
        item { SectionTitle(stringResource(R.string.about_app_license_title)) }
        item {
            ExpandableCard(
                title = stringResource(R.string.about_app_license_name),
                subtitle = stringResource(R.string.about_app_license_hint),
                expanded = appLicenseOpen,
                onToggle = { appLicenseOpen = !appLicenseOpen },
            ) { LicenseBody(appLicense) }
        }
        item {
            Text(
                stringResource(R.string.about_assets_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        // --- Third-party licenses ---
        item { SectionTitle(stringResource(R.string.about_third_party_title)) }
        item {
            Text(
                stringResource(R.string.about_third_party_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(data.licenses, key = { "license-${it.id}" }) { license ->
            val count = data.libraries.count { license.id in it.licenseIds }
            ExpandableCard(
                title = license.name,
                subtitle = pluralStringResource(R.plurals.about_libraries_count, count, count),
                expanded = openLicenseId == license.id,
                onToggle = { openLicenseId = if (openLicenseId == license.id) null else license.id },
            ) { LicenseBody(license.content) }
        }
        item { Spacer(Modifier.height(8.dp)) }
        items(data.libraries, key = { "lib-${it.name}-${it.version}-${it.website}" }) { lib ->
            val id = "${lib.name} ${lib.version}"
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { openLibrary = if (openLibrary == id) null else id }
                    .padding(vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lib.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        lib.version,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    lib.licenseIds.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (openLibrary == id) {
                    lib.vendor?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    lib.website?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ExpandableCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    body: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge)
        }
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            body()
        }
    }
}

@Composable
private fun LicenseBody(text: String) {
    Text(
        text.trim(),
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
