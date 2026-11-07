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
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    AABrowserTheme {
        Box(modifier = modifier.fillMaxSize()) {
            DynamicWallpaperBackground(customBackgroundBitmap = customBackgroundBitmap)
            if (showAppChooser) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(Color(0xFF071521), Color(0xFF122B3B)))
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showAppChooser) {
                    Spacer(Modifier.height(if (isLandscape) 8.dp else 24.dp))
                    Text(
                        text = "Youtube On Auto",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFFF4FBFF),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Bir seçenek seç",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF95ADBA),
                        modifier = Modifier.padding(top = 6.dp, bottom = if (isLandscape) 14.dp else 22.dp)
                    )
                    val choices = listOf(
                        LauncherItem("youtube", "YouTube", "Videoları web üzerinden aç", "https://m.youtube.com"),
                        LauncherItem("google", "Google", "Google aramasını aç", "https://www.google.com"),
                        LauncherItem("kantan", "Kantan News", "Teknoloji haberlerini aç", "https://kantan.news/"),
                        LauncherItem("browser", "Browser", "Adres yaz veya gezin", null)
                    )
                    if (isLandscape) {
                        choices.chunked(2).forEachIndexed { rowIndex, rowItems ->
                            if (rowIndex > 0) Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowItems.forEach { item ->
                                    LauncherChoice(item, Modifier.weight(1f)) {
                                        if (item.url == null) showAppChooser = false else callbacks.onNavigate(item.url)
                                    }
                                }
                            }
                        }
                    } else {
                        choices.forEachIndexed { index, item ->
                            if (index > 0) Spacer(Modifier.height(12.dp))
                            LauncherChoice(item, Modifier.fillMaxWidth()) {
                                if (item.url == null) showAppChooser = false else callbacks.onNavigate(item.url)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
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
    item: LauncherItem,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE6213A49)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF355668))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(92.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LauncherLogo(item.kind)
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF3F9FC))
                Text(text = item.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFFA8BAC4), maxLines = 1)
            }
            Text("›", color = Color(0xFF51D5E4), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private data class LauncherItem(val kind: String, val title: String, val description: String, val url: String?)

@Composable
private fun LauncherLogo(kind: String) {
    val tile = when (kind) {
        "kantan" -> Color(0xFFF6F2EB)
        else -> Color.White
    }
    Box(
        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(tile),
        contentAlignment = Alignment.Center
    ) {
        when (kind) {
            "youtube" -> Box(Modifier.size(width = 38.dp, height = 27.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFF0033)), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(16.dp)) {
                    val p = Path().apply { moveTo(size.width * .2f, 0f); lineTo(size.width, size.height * .5f); lineTo(size.width * .2f, size.height); close() }
                    drawPath(p, Color.White)
                }
            }
            "google" -> Image(
                painter = painterResource(com.kododake.aabrowser.R.drawable.google_g_logo),
                contentDescription = "Google",
                modifier = Modifier.size(38.dp)
            )
            "kantan" -> Text("K", color = Color(0xFF172A39), fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineLarge)
            else -> {
                // A compact browser mark inspired by AABrowser's window and navigation controls.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("A", color = Color(0xFF168BA3), fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                    Box(Modifier.width(30.dp).height(3.dp).clip(CircleShape).background(Color(0xFF168BA3)))
                }
            }
        }
    }
}
