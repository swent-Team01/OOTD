package com.android.ootd.ui.feed

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
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
    val clipboardManager = LocalClipboardManager.current
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

                item.type
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      Text(
                          text = it,
                          style =
                              MaterialTheme.typography.bodyLarge.copy(
                                  color = MaterialTheme.colorScheme.onSurfaceVariant),
                          textAlign = TextAlign.Center,
                          modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_TYPE))
                    }

                item.brand
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      Text(
                          text = it,
                          style =
                              MaterialTheme.typography.bodyLarge.copy(
                                  color = MaterialTheme.colorScheme.onSurfaceVariant),
                          textAlign = TextAlign.Center,
                          modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_BRAND))
                    }

                item.price?.let {
                  val c = item.currency ?: "CHF"
                  Text(
                      text = "$c ${String.format("%.2f", it)}",
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

                item.condition
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      Text(
                          text = "Condition: $it",
                          style =
                              MaterialTheme.typography.bodyLarge.copy(
                                  color = MaterialTheme.colorScheme.onSurfaceVariant),
                          textAlign = TextAlign.Center,
                          modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_CONDITION))
                    }

                item.size
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      Text(
                          text = "Size: $it",
                          style =
                              MaterialTheme.typography.bodyLarge.copy(
                                  color = MaterialTheme.colorScheme.onSurfaceVariant),
                          textAlign = TextAlign.Center,
                          modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_SIZE))
                    }

                item.fitType
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      Text(
                          text = "Fit: $it",
                          style =
                              MaterialTheme.typography.bodyLarge.copy(
                                  color = MaterialTheme.colorScheme.onSurfaceVariant),
                          textAlign = TextAlign.Center,
                          modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_FIT_TYPE))
                    }

                item.style
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      Text(
                          text = "Style: $it",
                          style =
                              MaterialTheme.typography.bodyLarge.copy(
                                  color = MaterialTheme.colorScheme.onSurfaceVariant),
                          textAlign = TextAlign.Center,
                          modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_STYLE))
                    }

                item.notes
                    ?.takeUnless { it.isBlank() }
                    ?.let { notes ->
                      CopyableDetailRow(
                          text = notes,
                          textTag = SeeFitScreenTestTags.ITEM_NOTES,
                          copyTag = SeeFitScreenTestTags.ITEM_NOTES_COPY,
                          textStyle =
                              MaterialTheme.typography.bodyLarge.copy(
                                  color = MaterialTheme.colorScheme.onSurfaceVariant),
                          onCopy = { clipboardManager.setText(AnnotatedString(notes)) })
                    }

                item.link
                    ?.takeUnless { it.isBlank() }
                    ?.let { link ->
                      val context = LocalContext.current
                      CopyableDetailRow(
                          text = link,
                          textTag = SeeFitScreenTestTags.ITEM_LINK,
                          copyTag = SeeFitScreenTestTags.ITEM_LINK_COPY,
                          textStyle =
                              MaterialTheme.typography.bodyLarge.copy(
                                  color = Primary, textAlign = TextAlign.Center),
                          onTextClick = {
                            val intent =
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW, link.toUri())
                            context.startActivity(intent)
                          },
                          onCopy = { clipboardManager.setText(AnnotatedString(link)) })
                    }
              }
        }
  }
}

@Composable
private fun CopyableDetailRow(
    text: String,
    textTag: String,
    copyTag: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    onCopy: () -> Unit,
    onTextClick: (() -> Unit)? = null
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically) {
        var textModifier: Modifier = Modifier.weight(1f)
        if (onTextClick != null) {
          textModifier = textModifier.clickable { onTextClick() }
        }
        textModifier = textModifier.testTag(textTag)
        Text(text = text, style = textStyle, textAlign = TextAlign.Center, modifier = textModifier)
        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp).testTag(copyTag)) {
          Icon(
              imageVector = Icons.Filled.ContentCopy,
              contentDescription = "Copy",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp))
        }
      }
}
