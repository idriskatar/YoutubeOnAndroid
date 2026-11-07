/*
 * Copyright (C) 2025 AABrowser Contributors (https://github.com/kododake/AABrowser)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package com.kododake.aabrowser.ui.compose.screens.startpage

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kododake.aabrowser.data.BrowserPreferences
import com.kododake.aabrowser.data.prefs.StartPagePreferences
import com.kododake.aabrowser.data.prefs.UIPreferences
import com.kododake.aabrowser.ui.compose.components.DynamicWallpaperBackground
import com.kododake.aabrowser.ui.compose.theme.AABrowserTheme

data class StartPageScreenCallbacks(
    val onNavigate: (String) -> Unit = {},
    val onSlotClick: (Int, String?) -> Unit = { _, _ -> },
    val onMoveSlot: (Int, Int) -> Unit = { _, _ -> },
    val onClearSlot: (Int) -> Unit = {},
    val onResumeClick: () -> Unit = {},
    val onPhotoOnlyToggle: () -> Unit = {},
    val onOpenSponsors: (String) -> Unit = {},
    val onOpenAAAnything: () -> Unit = {},
    val onDismissAAAnything: () -> Unit = {}
)

@Composable
fun StartPageScreen(
    context: Context,
    slots: List<StartPageSlotUi>,
    hasResumePage: Boolean,
    sponsorsQrBitmap: Bitmap?,
    customBackgroundBitmap: Bitmap? = null,
    isNavigating: Boolean = false,
    callbacks: StartPageScreenCallbacks = StartPageScreenCallbacks(),
    modifier: Modifier = Modifier
) {
    var showAppChooser by remember { mutableStateOf(true) }
    var shouldHideSponsors by remember {
        mutableStateOf(BrowserPreferences.shouldHideSponsors(context))
    }
    var showAAAnythingPromotion by remember {
        mutableStateOf(BrowserPreferences.shouldShowAAAnythingPromotion(context))
    }
    val prefsListener = remember(context) {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == StartPagePreferences.KEY_SHOW_AA_ANYTHING_PROMOTION) {
                showAAAnythingPromotion = BrowserPreferences.shouldShowAAAnythingPromotion(context)
            } else if (key == UIPreferences.KEY_HIDE_SPONSORS) {
                shouldHideSponsors = BrowserPreferences.shouldHideSponsors(context)
            }
        }
    }
    DisposableEffect(context, prefsListener) {
        val prefs = context.getSharedPreferences(StartPagePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        }
    }
    val scrollState = rememberScrollState()

    AABrowserTheme {
        Box(modifier = modifier.fillMaxSize()) {
            DynamicWallpaperBackground(customBackgroundBitmap = customBackgroundBitmap)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showAppChooser) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Youtube On Auto",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Bir seçenek seç",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
                    )
                    LauncherChoice("▶", "YouTube", "Videoları web üzerinden aç", MaterialTheme.colorScheme.errorContainer) {
                        callbacks.onNavigate("https://m.youtube.com")
                    }
                    Spacer(Modifier.height(14.dp))
                    LauncherChoice("G", "Google", "Google aramasını aç", MaterialTheme.colorScheme.surfaceContainerHigh) {
                        callbacks.onNavigate("https://www.google.com")
                    }
                    Spacer(Modifier.height(14.dp))
                    LauncherChoice("K", "Kantan News", "Teknoloji haberlerini aç", MaterialTheme.colorScheme.surfaceContainerHigh) {
                        callbacks.onNavigate("https://kantan.news/")
                    }
                    Spacer(Modifier.height(14.dp))
                    LauncherChoice("▥", "MyOBD", "Yerel OBD gösterge panelini aç", MaterialTheme.colorScheme.surfaceContainerHigh) {
                        callbacks.onNavigate("http://127.0.0.1:8765/")
                    }
                    Spacer(Modifier.height(14.dp))
                    LauncherChoice("◎", "Browser", "Adres yaz veya internette gezin", MaterialTheme.colorScheme.surfaceContainerHigh) {
                        showAppChooser = false
                    }
                    Spacer(Modifier.height(24.dp))
                } else {
                    StartPageHeader(
                        onNavigate = callbacks.onNavigate
                    )

                    if (showAAAnythingPromotion) {
                        Spacer(Modifier.height(16.dp))
                        AAAnythingPromotionCard(
                            onOpenUrl = callbacks.onOpenAAAnything,
                            onDismiss = {
                                showAAAnythingPromotion = false
                                callbacks.onDismissAAAnything()
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    StartPageQuickLinks(
                        slots = slots,
                        onSlotClick = callbacks.onSlotClick,
                        onMoveSlot = callbacks.onMoveSlot,
                        onClearSlot = callbacks.onClearSlot
                    )

                    Spacer(Modifier.height(16.dp))

                    if (!shouldHideSponsors) {
                        Spacer(Modifier.height(16.dp))
                        StartPageSponsorsSection(
                            githubQrBitmap = sponsorsQrBitmap,
                            onOpenUrl = callbacks.onOpenSponsors
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    StartPageActionRow(
                        hasResumePage = hasResumePage,
                        onResumeClick = callbacks.onResumeClick,
                        onPhotoOnlyClick = callbacks.onPhotoOnlyToggle
                    )

                    Spacer(Modifier.height(48.dp))
                }
            }
            }
        }
    }

@Composable
private fun LauncherChoice(
    symbol: String,
    title: String,
    description: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = symbol, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.padding(start = 18.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
