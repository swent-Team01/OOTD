package com.android.ootd.ui.feed

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.items.Item
import com.android.ootd.ui.theme.Primary

/**
 * Dialog displaying detailed information about an item.
 *
 * @param item The item whose details are to be displayed when the dialog is clicked
 * @param onDismissRequest Callback when the dialog is dismissed
 */
@SuppressLint("DefaultLocale")
@Composable
fun SeeItemDetailsDialog(
    item: Item,
    onDismissRequest: () -> Unit = {},
) {
  Dialog(onDismissRequest = { onDismissRequest() }) {
    Card(
        modifier =
            Modifier.fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag(SeeFitScreenTestTags.ITEM_DETAILS_DIALOG),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)) {
          Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = item.image.imageUrl,
                    contentDescription = "Item Image",
                    modifier =
                        Modifier.fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag(SeeFitScreenTestTags.ITEM_IMAGE),
                    placeholder = painterResource(R.drawable.ic_photo_placeholder),
                    error = painterResource(R.drawable.ic_photo_placeholder),
                    contentScale = ContentScale.Crop)

                Text(
                    text = item.category,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_CATEGORY))

                Text(
                    text = item.type ?: "Item Type",
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_TYPE))

                Text(
                    text = item.brand ?: "Item Brand",
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_BRAND))

                item.price?.let {
                  Text(
                      text = "CHF ${String.format("%.2f", it)}",
                      style =
                          MaterialTheme.typography.bodyLarge.copy(
                              color = MaterialTheme.colorScheme.onSurfaceVariant),
                      textAlign = TextAlign.Center,
                      modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_PRICE))
                }

                if (item.material.isNotEmpty()) {
                  val materialText =
                      item.material
                          .mapNotNull { material ->
                            material?.let { material ->
                              val percentage =
                                  material.percentage.let { String.format("%.0f %%", it) }
                              listOfNotNull(material.name, percentage).joinToString(" ")
                            }
                          }
                          .joinToString(separator = " - ")

                  Text(
                      text = materialText,
                      style =
                          MaterialTheme.typography.bodyLarge.copy(
                              color = MaterialTheme.colorScheme.onSurfaceVariant),
                      textAlign = TextAlign.Center,
                      modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_MATERIAL))
                }

                item.link?.let {
                  val context = LocalContext.current
                  Text(
                      text = it,
                      style =
                          MaterialTheme.typography.bodyLarge.copy(
                              color = Primary, textAlign = TextAlign.Center),
                      modifier =
                          Modifier.padding(top = 8.dp)
                              .clickable {
                                val intent =
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW, it.toUri())
                                context.startActivity(intent)
                              }
                              .testTag(SeeFitScreenTestTags.ITEM_LINK))
                }
              }
        }
  }
}
