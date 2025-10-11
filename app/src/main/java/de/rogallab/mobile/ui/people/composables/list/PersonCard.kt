package de.rogallab.mobile.ui.people.composables.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.let

@Composable
fun PersonCard(
   firstName: String,
   lastName: String,
   email: String?,
   phone: String?,
   imagePath: String?,
   modifier: Modifier = Modifier
) {
   Card(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(percent = 10),
   ) {
      Row(
         verticalAlignment = Alignment.Companion.CenterVertically,
      ) {
         imagePath?.let { path: String ->
            Column(modifier = Modifier.weight(0.15f)) {
               AsyncImage(
                  model = path,
                  contentDescription = "Bild der Person",
                  modifier = Modifier
                     .size(width = 60.dp, height = 75.dp)
                     .clip(RoundedCornerShape(percent = 15))
                     .padding(start = 8.dp)
                     .padding(vertical = 4.dp),
                  alignment = Alignment.Companion.Center,
                  contentScale = ContentScale.Companion.Crop
               )
            }
         }

         Column(
            modifier = Modifier
               .weight(0.85f)
               .padding(vertical = 4.dp)
               .padding(horizontal = 8.dp)
         ) {
            Text(
               text = "$firstName $lastName",
               style = MaterialTheme.typography.bodyLarge,
            )
            email?.let {
               Text(
                  text = it,
                  style = MaterialTheme.typography.bodyMedium
               )
            }
            phone?.let {
               Text(
                  text = phone,
                  style = MaterialTheme.typography.bodyMedium,
               )
            }
         }


      }
   }
}