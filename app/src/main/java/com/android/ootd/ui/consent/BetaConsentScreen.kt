package com.android.ootd.ui.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Secondary

object BetaConsentScreenTestTags {
  const val SCREEN = "betaConsentScreen"
  const val TITLE = "betaConsentTitle"
  const val SCROLL_CONTENT = "betaConsentScrollContent"
  const val CHECKBOX = "betaConsentCheckbox"
  const val AGREE_BUTTON = "betaConsentAgreeButton"
  const val DECLINE_BUTTON = "betaConsentDeclineButton"
  const val SECTION_DATA_COLLECTION = "betaConsentDataCollectionSection"
  const val SECTION_PHOTOS = "betaConsentPhotosSection"
  const val SECTION_LOCATION = "betaConsentLocationSection"
}

/**
 * Beta consent screen that asks users to agree to data collection terms for the beta version of the
 * OOTD app.
 *
 * @param onAgree Callback when user agrees to the beta terms
 * @param onDecline Callback when user declines the beta terms
 * @param isLoading Whether the consent is being saved (shows loading indicator)
 * @param errorMessage Error message to display, if any
 * @param onErrorDismiss Callback when error message is dismissed
 */
@Composable
fun BetaConsentScreen(
    modifier: Modifier = Modifier,
    onAgree: () -> Unit = {},
    onDecline: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onErrorDismiss: () -> Unit = {}
) {
  var hasAgreed by remember { mutableStateOf(false) }
  val colors = MaterialTheme.colorScheme
  val typography = MaterialTheme.typography

  Box(
      modifier =
          modifier
              .fillMaxSize()
              .background(colors.background)
              .testTag(BetaConsentScreenTestTags.SCREEN)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Header with icon
              Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = "Beta Program Info",
                  tint = colors.primary,
                  modifier = Modifier.size(48.dp).padding(top = 16.dp))

              Spacer(modifier = Modifier.height(12.dp))

              // Title
              Text(
                  text = "OOTD Beta Program",
                  style = typography.headlineMedium,
                  fontWeight = FontWeight.Bold,
                  color = colors.primary,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(BetaConsentScreenTestTags.TITLE))

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                  text = "Data Collection & Usage Agreement",
                  style = typography.titleSmall,
                  color = colors.onSurfaceVariant,
                  textAlign = TextAlign.Center)

              Spacer(modifier = Modifier.height(16.dp))

              // Scrollable content
              Card(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  colors = CardDefaults.cardColors(containerColor = Secondary)) {
                    Column(
                        modifier =
                            Modifier.verticalScroll(rememberScrollState())
                                .padding(12.dp)
                                .testTag(BetaConsentScreenTestTags.SCROLL_CONTENT)) {
                          // Introduction
                          Text(
                              text = "Welcome to the OOTD Beta!",
                              style = typography.titleMedium,
                              fontWeight = FontWeight.Bold,
                              color = colors.onSurfaceVariant)

                          Spacer(modifier = Modifier.height(8.dp))

                          Text(
                              text =
                                  "Thank you for participating in our beta program. To help us improve OOTD and create the best outfit-sharing experience, we collect and analyze certain data during this testing phase.",
                              style = typography.bodySmall,
                              color = colors.onSurfaceVariant)

                          Spacer(modifier = Modifier.height(8.dp))

                          Text(
                              text =
                                  "This app is created for the course CS-311 at EPFL. It's still in active development, so if you encounter any bugs or just want to share feedback with us, feel free to reach out to us!",
                              style = typography.bodySmall,
                              color = colors.onSurfaceVariant)

                          Spacer(modifier = Modifier.height(16.dp))

                          HorizontalDivider(
                              Modifier,
                              DividerDefaults.Thickness,
                              color = colors.outline.copy(alpha = 0.3f))

                          Spacer(modifier = Modifier.height(12.dp))

                          // Data Collection Section
                          SectionHeader(
                              title = "Data We Collect",
                              modifier =
                                  Modifier.testTag(
                                      BetaConsentScreenTestTags.SECTION_DATA_COLLECTION))

                          Spacer(modifier = Modifier.height(8.dp))

                          // Photos Section
                          DataCollectionItem(
                              icon = "📸",
                              title = "Photos & Images",
                              description =
                                  "All photos you take or upload through the app, including outfit photos, clothing item images, and profile pictures. These images are stored on Firebase Storage and used to:",
                              bulletPoints =
                                  listOf(
                                      "Improve our compression algorithm",
                                      "Better understand the items users wear"),
                              modifier = Modifier.testTag(BetaConsentScreenTestTags.SECTION_PHOTOS))

                          Spacer(modifier = Modifier.height(12.dp))

                          // Location Section
                          DataCollectionItem(
                              icon = "📍",
                              title = "Location Data",
                              description =
                                  "Your location is taken when you create your profile. You can also choose to add the location to posts you make. This data helps us:",
                              bulletPoints =
                                  listOf(
                                      "Suggest friends who post around you with our algorithm",
                                      "Understand where our users post from and when"),
                              note =
                                  "Location is only collected when you choose to share it, not continuously in the background.",
                              modifier =
                                  Modifier.testTag(BetaConsentScreenTestTags.SECTION_LOCATION))

                          Spacer(modifier = Modifier.height(12.dp))

                          HorizontalDivider(
                              Modifier,
                              DividerDefaults.Thickness,
                              color = colors.outline.copy(alpha = 0.3f))

                          Spacer(modifier = Modifier.height(12.dp))

                          // How We Use Data
                          SectionHeader(title = "How We Use Your Data")

                          Spacer(modifier = Modifier.height(8.dp))

                          Text(
                              text = "During the beta phase, your data is used exclusively to:",
                              style = typography.bodySmall,
                              color = colors.onSurfaceVariant)

                          Spacer(modifier = Modifier.height(4.dp))

                          BulletPoint("Improve app functionality, performance, and user experience")
                          BulletPoint("Identify and fix bugs and technical issues")
                          BulletPoint("Develop and test new features")
                          BulletPoint(
                              "Conduct internal analytics to understand user behavior and preferences")
                          BulletPoint("Enhance our AI and machine learning models")

                          Spacer(modifier = Modifier.height(12.dp))

                          HorizontalDivider(
                              Modifier,
                              DividerDefaults.Thickness,
                              color = colors.outline.copy(alpha = 0.3f))

                          Spacer(modifier = Modifier.height(12.dp))

                          // Your Rights
                          SectionHeader(title = "Your Rights & Privacy")

                          Spacer(modifier = Modifier.height(8.dp))

                          InfoBox(
                              text =
                                  "• Your data is never sold to third parties\n" +
                                      "• We implement security measures to protect your information on the local device and in the databases\n" +
                                      "• You can request deletion of your data at any time by contacting us\n" +
                                      "• You can withdraw from the beta program at any time\n" +
                                      "• All data collection is used to improve the app in the context of this course")

                          Spacer(modifier = Modifier.height(12.dp))

                          // Beta Specific Notice
                          Card(
                              colors =
                                  CardDefaults.cardColors(
                                      containerColor = colors.primaryContainer)) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                      Icon(
                                          imageVector = Icons.Default.Info,
                                          contentDescription = null,
                                          tint = colors.onPrimaryContainer,
                                          modifier = Modifier.size(20.dp))
                                      Spacer(modifier = Modifier.width(8.dp))
                                      Text(
                                          text =
                                              "This is a beta version. Data collection practices may change as we prepare for the official launch. You'll be notified of any significant changes. You will need to download a new APK for updates.",
                                          style = typography.bodySmall,
                                          color = colors.onPrimaryContainer)
                                    }
                              }

                          Spacer(modifier = Modifier.height(12.dp))

                          Text(
                              text =
                                  "By agreeing below, you acknowledge that you have read and understood this agreement and consent to the collection and use of your data as described during the beta testing period.",
                              style = typography.bodySmall,
                              color = colors.onSurfaceVariant,
                              fontWeight = FontWeight.Medium)

                          Spacer(modifier = Modifier.height(4.dp))

                          Text(
                              text = "Last updated: November 5, 2025",
                              style = typography.bodySmall,
                              color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // Checkbox agreement
              Row(
                  modifier = Modifier.fillMaxWidth().testTag(BetaConsentScreenTestTags.CHECKBOX),
                  verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasAgreed,
                        onCheckedChange = { hasAgreed = it },
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor = colors.primary,
                                uncheckedColor = colors.onSurfaceVariant))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I agree to the data collection and usage terms described above",
                        style = typography.bodySmall,
                        color = colors.onSurface)
                  }

              Spacer(modifier = Modifier.height(12.dp))

              // Action buttons
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier =
                            Modifier.weight(1f).testTag(BetaConsentScreenTestTags.DECLINE_BUTTON),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.onSurfaceVariant)) {
                          Text("Decline", style = typography.titleSmall)
                        }

                    Button(
                        onClick = onAgree,
                        enabled = hasAgreed && !isLoading,
                        modifier =
                            Modifier.weight(1f).testTag(BetaConsentScreenTestTags.AGREE_BUTTON),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) {
                          if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.onPrimary,
                                strokeWidth = 2.dp)
                          } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp))
                          }
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(
                              if (isLoading) "Saving..." else "Agree & Continue",
                              style = typography.titleSmall)
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
                  Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                }
              },
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                Text(errorMessage, style = MaterialTheme.typography.bodySmall)
              }
        }
      }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
  Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier)
}

@Composable
private fun DataCollectionItem(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    description: String,
    bulletPoints: List<String>,
    note: String? = null
) {
  Column(modifier = modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = icon, style = MaterialTheme.typography.titleLarge)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)

    Spacer(modifier = Modifier.height(6.dp))

    bulletPoints.forEach { point ->
      BulletPoint(point)
      Spacer(modifier = Modifier.height(2.dp))
    }

    if (note != null) {
      Spacer(modifier = Modifier.height(6.dp))
      Card(
          colors =
              CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Text(
                text = "Note: $note",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(6.dp))
          }
    }
  }
}

@Composable
private fun BulletPoint(text: String) {
  Row(modifier = Modifier.padding(start = 8.dp)) {
    Text(
        text = "• ",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun InfoBox(text: String) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
      shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(12.dp))
      }
}

@Preview(showBackground = true)
@Composable
fun BetaConsentScreenPreview() {
  OOTDTheme { BetaConsentScreen() }
}
