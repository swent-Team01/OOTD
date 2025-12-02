package com.android.ootd.ui.feed

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.OnSurfaceVariant
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Typography

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
        colors = CardDefaults.cardColors(containerColor = Secondary)) {
          Column(
              modifier =
                  Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
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

                DetailTextRow(
                    label = "Category",
                    value = item.category,
                    tag = SeeFitScreenTestTags.ITEM_CATEGORY)

                item.type
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      DetailTextRow(
                          label = "Type", value = it, tag = SeeFitScreenTestTags.ITEM_TYPE)
                    }

                item.brand
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      DetailTextRow(
                          label = "Brand", value = it, tag = SeeFitScreenTestTags.ITEM_BRAND)
                    }

                item.price?.let {
                  val c = item.currency ?: "CHF"
                  DetailTextRow(
                      label = "Price",
                      value = "${String.format("%.2f", it)} $c",
                      tag = SeeFitScreenTestTags.ITEM_PRICE)
                }

                if (item.material.isNotEmpty()) {
                  val materialText =
                      item.material
                          .mapNotNull { material ->
                            material?.let { entry ->
                              val percentage = entry.percentage.let { String.format("%.0f %%", it) }
                              listOfNotNull(entry.name, percentage).joinToString(" ")
                            }
                          }
                          .joinToString(separator = " - ")

                  DetailTextRow(
                      label = "Material",
                      value = materialText,
                      tag = SeeFitScreenTestTags.ITEM_MATERIAL)
                }

                item.condition
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      DetailTextRow(
                          label = "Condition",
                          value = it,
                          tag = SeeFitScreenTestTags.ITEM_CONDITION)
                    }

                item.size
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      DetailTextRow(
                          label = "Size", value = it, tag = SeeFitScreenTestTags.ITEM_SIZE)
                    }

                item.fitType
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      DetailTextRow(
                          label = "Fit", value = it, tag = SeeFitScreenTestTags.ITEM_FIT_TYPE)
                    }

                item.style
                    ?.takeUnless { it.isBlank() }
                    ?.let {
                      DetailTextRow(
                          label = "Style", value = it, tag = SeeFitScreenTestTags.ITEM_STYLE)
                    }

                item.notes
                    ?.takeUnless { it.isBlank() }
                    ?.let { notes ->
                      CopyableDetailRow(
                          label = "Notes",
                          value = notes,
                          textTag = SeeFitScreenTestTags.ITEM_NOTES,
                          copyTag = SeeFitScreenTestTags.ITEM_NOTES_COPY,
                          onCopy = { clipboardManager.setText(AnnotatedString(notes)) })
                    }

                item.link
                    ?.takeUnless { it.isBlank() }
                    ?.let { link ->
                      val context = LocalContext.current
                      CopyableDetailRow(
                          label = "Link",
                          value = link,
                          textTag = SeeFitScreenTestTags.ITEM_LINK,
                          copyTag = SeeFitScreenTestTags.ITEM_LINK_COPY,
                          isLink = true,
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
    label: String,
    value: String,
    textTag: String,
    copyTag: String,
    onCopy: () -> Unit,
    onTextClick: (() -> Unit)? = null,
    isLink: Boolean = false
) {
  val valueColor = if (isLink) Primary else OnSurfaceVariant
  val linkFontSize =
      when {
        !isLink -> Typography.bodyMedium.fontSize
        value.length > 80 -> 10.sp
        value.length > 60 -> 12.sp
        value.length > 40 -> 13.sp
        else -> Typography.bodyMedium.fontSize
      }

  Column(
      modifier = Modifier.fillMaxWidth().testTag(textTag),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
              Text(
                  text = label,
                  style =
                      Typography.bodyMedium.copy(
                          fontWeight = FontWeight.Bold, color = OnSurfaceVariant))
              IconButton(onClick = onCopy, modifier = Modifier.size(32.dp).testTag(copyTag)) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp))
              }
            }
        val clickableModifier =
            if (onTextClick != null) Modifier.padding(top = 2.dp).clickable { onTextClick() }
            else Modifier.padding(top = 2.dp)
        Text(
            text = value,
            style = Typography.bodyMedium.copy(color = valueColor, fontSize = linkFontSize),
            textAlign = TextAlign.Center,
            modifier = clickableModifier.heightIn(min = 32.dp))
      }
}

@Composable
private fun DetailTextRow(label: String, value: String, tag: String) {
  Column(
      modifier = Modifier.fillMaxWidth().testTag(tag),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style =
                Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = OnSurfaceVariant))
        Text(
            text = value,
            style = Typography.bodyMedium.copy(color = OnSurfaceVariant),
            textAlign = TextAlign.Center)
      }
}

@Preview(showBackground = true)
@Composable
fun SeeItemDetailsDialogPreview() {
  OOTDTheme {
    SeeItemDetailsDialog(
        item =
            Item(
                itemUuid = "preview-item",
                postUuids = emptyList(),
                image = ImageData(imageId = "preview", imageUrl = ""),
                category = "Outerwear",
                type = "Leather Jacket",
                brand = "Retro Co.",
                price = 199.99,
                currency = "USD",
                material = emptyList(),
                link = "https://example.com/item/very-long-link-to-show-scaling",
                ownerId = "demo",
                condition = "Like new",
                size = "M",
                fitType = "Regular",
                style = "Casual",
                notes = "Worn twice, stored properly."))
  }
}
