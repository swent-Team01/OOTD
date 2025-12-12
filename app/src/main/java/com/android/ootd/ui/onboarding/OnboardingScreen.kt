package com.android.ootd.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ootd.R
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.OnSurfaceVariant
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.TertiaryContainer
import com.android.ootd.ui.theme.Typography
import kotlinx.coroutines.launch

object OnboardingScreenTestTags {
  const val SCREEN = "onboardingScreen"
  const val TITLE = "onboardingTitle"
  const val SUBTITLE = "onboardingSubtitle"
  const val PAGER = "onboardingPager"
  const val SKIP_BUTTON = "onboardingSkipButton"
  const val NEXT_BUTTON = "onboardingNextButton"
  const val INDICATORS = "onboardingIndicators"
  const val PAGE_CARD_PREFIX = "onboardingPageCard_"
  const val PAGE_TITLE_PREFIX = "onboardingPageTitle_"
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val accent: androidx.compose.ui.graphics.Color,
    val emoji: String,
    val imageRes: Int? = null
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onAgree: () -> Unit = {},
    onSkip: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onErrorDismiss: () -> Unit = {}
) {
  val colors = MaterialTheme.colorScheme
  val pages = remember {
    listOf(
        OnboardingPage(
            title = "Capture your outfit with a FitCheck",
            description =
                "Take a photo of your outfit of the day to share with your friends and to view their posts.",
            accent = Primary,
            emoji = "📸"),
        OnboardingPage(
            title = "Add your items to your post",
            description =
                "Break outfits into pieces so you can tag, re-use, and build your closet over time.",
            accent = Secondary,
            emoji = "👕"),
        OnboardingPage(
            title = "See friends' posts and star their items so you can always have them !",
            description =
                "Browse the feed, open fits you love, and star the pieces you want to remember so you can store them in your account.",
            accent = TertiaryContainer,
            emoji = "⭐",
            imageRes = R.drawable.star_items),
        OnboardingPage(
            title = "Use the map or public feed to find new friends",
            description =
                "Hop to the map to spot nearby users or follow people from the public feed to grow your circle and enhance your experience.",
            accent = Secondary,
            emoji = "🗺️",
            imageRes = R.drawable.find_friends))
  }

  val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
  val scope = rememberCoroutineScope()

  Box(
      modifier =
          modifier.fillMaxSize().background(Background).testTag(OnboardingScreenTestTags.SCREEN)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.Start) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onSkip,
                    enabled = !isLoading,
                    modifier = Modifier.testTag(OnboardingScreenTestTags.SKIP_BUTTON),
                    shape = RoundedCornerShape(50),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Secondary, contentColor = OnSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                      Text("Skip", style = Typography.labelLarge)
                    }
              }

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                  text = "Get to know OOTD",
                  style = Typography.headlineMedium,
                  modifier = Modifier.testTag(OnboardingScreenTestTags.TITLE))
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                  text = "Quick tips to start sharing your outfits",
                  style = Typography.bodyMedium,
                  color = OnSurfaceVariant,
                  modifier = Modifier.testTag(OnboardingScreenTestTags.SUBTITLE))

              Spacer(modifier = Modifier.height(16.dp))

              HorizontalPager(
                  state = pagerState,
                  modifier =
                      Modifier.weight(1f).fillMaxWidth().testTag(OnboardingScreenTestTags.PAGER)) {
                      page ->
                    val onboardingPage = pages[page]
                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .testTag("${OnboardingScreenTestTags.PAGE_CARD_PREFIX}$page"),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = onboardingPage.accent.copy(alpha = 0.2f))) {
                          Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = onboardingPage.emoji,
                                style = Typography.headlineLarge,
                                modifier = Modifier.padding(bottom = 4.dp))
                            Text(
                                text = onboardingPage.title,
                                style = Typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier =
                                    Modifier.testTag(
                                        "${OnboardingScreenTestTags.PAGE_TITLE_PREFIX}$page"))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = onboardingPage.description,
                                style = Typography.bodyMedium,
                                color = OnSurfaceVariant)
                            onboardingPage.imageRes?.let { resId ->
                              val painter = painterResource(id = resId)
                              val ratio =
                                  painter.intrinsicSize.let { size ->
                                    val defaultRatio = 1.4f
                                    if (size.width > 0f && size.height > 0f) {
                                      val computed = size.width / size.height
                                      computed.coerceIn(1f, 1.5f)
                                    } else {
                                      defaultRatio
                                    }
                                  }
                              val (imageModifier, imageAlignment) =
                                  when {
                                    // Slide 3: favor top of the image
                                    page == 2 ->
                                        Modifier.fillMaxWidth()
                                            .height(240.dp)
                                            .offset(y = (-10).dp)
                                            .clip(RoundedCornerShape(16.dp)) to Alignment.TopCenter
                                    // Slide 4: keep previous sizing
                                    page == pages.lastIndex ->
                                        Modifier.fillMaxWidth()
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(16.dp)) to Alignment.Center
                                    else ->
                                        Modifier.fillMaxWidth()
                                            .aspectRatio(ratio)
                                            .clip(RoundedCornerShape(16.dp)) to Alignment.Center
                                  }
                              Spacer(modifier = Modifier.height(12.dp))
                              androidx.compose.foundation.Image(
                                  painter = painter,
                                  contentDescription = onboardingPage.title,
                                  modifier = imageModifier,
                                  contentScale = ContentScale.Crop,
                                  alignment = imageAlignment)
                            }
                          }
                        }
                  }

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                  modifier = Modifier.fillMaxWidth().testTag(OnboardingScreenTestTags.INDICATORS),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically) {
                    repeat(pages.size) { index ->
                      val isSelected = pagerState.currentPage == index
                      Box(
                          modifier =
                              Modifier.padding(horizontal = 4.dp)
                                  .size(if (isSelected) 12.dp else 8.dp)
                                  .background(
                                      color =
                                          if (isSelected) Primary
                                          else OnSurfaceVariant.copy(alpha = 0.4f),
                                      shape = CircleShape))
                    }
                  }

              Spacer(modifier = Modifier.height(20.dp))

              Button(
                  onClick = {
                    if (pagerState.currentPage == pages.lastIndex) {
                      onAgree()
                    } else {
                      scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                  },
                  enabled = !isLoading,
                  modifier = Modifier.fillMaxWidth().testTag(OnboardingScreenTestTags.NEXT_BUTTON),
                  colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    if (isLoading) {
                      CircularProgressIndicator(
                          modifier = Modifier.size(18.dp),
                          color = colors.onPrimary,
                          strokeWidth = 2.dp)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Saving...", style = Typography.titleSmall)
                    } else {
                      val isLast = pagerState.currentPage == pages.lastIndex
                      Icon(
                          imageVector =
                              if (isLast) Icons.Default.Check else Icons.Default.ArrowForward,
                          contentDescription = null,
                          modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                          if (isLast) "Start using OOTD" else "Next", style = Typography.titleSmall)
                    }
                  }

              Spacer(modifier = Modifier.height(8.dp))
            }

        // Error Snackbar
        if (errorMessage != null) {
          Snackbar(
              modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
              action = {
                TextButton(onClick = onErrorDismiss) {
                  Text("Dismiss", style = Typography.labelSmall)
                }
              },
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                Text(errorMessage, style = Typography.bodySmall)
              }
        }
      }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
  OOTDTheme { OnboardingScreen() }
}
