package com.brigadka.app.previews

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.brigadka.app.presentation.search.SearchScreenPreview

@Preview
@Composable
fun SearchScreenPreviewWithFilters() {
    Surface {
        SearchScreenPreview(true)
    }
}

@Preview
@Composable
fun SearchScreenPreviewWithoutFilters() {
    Surface {
        SearchScreenPreview(false)
    }
}