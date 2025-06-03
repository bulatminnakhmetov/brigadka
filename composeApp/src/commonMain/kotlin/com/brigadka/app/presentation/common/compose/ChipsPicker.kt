package com.brigadka.app.presentation.common.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brigadka.app.data.api.models.StringItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsPicker(items: List<StringItem>, selected: List<String>, onClick: (String) -> Unit, onlySelected: Boolean = false, modifier: Modifier = Modifier){
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { style ->
            if (onlySelected && style.code !in selected) return@forEach
            FilterChip(
                selected = style.code in selected,
                onClick = { onClick(style.code) },
                label = { Text(style.label) }
            )
        }
    }
}