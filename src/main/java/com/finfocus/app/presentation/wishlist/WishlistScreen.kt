package com.finfocus.app.presentation.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.domain.model.WishStatus
import com.finfocus.app.presentation.components.EmptyState
import com.finfocus.app.presentation.plans.PlansViewModel
import com.finfocus.app.presentation.util.formatByr
import com.finfocus.app.ui.theme.FinFocusTheme

/**
 * Shows items already bought — the "history" counterpart to PlansScreen
 * which shows items being saved for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(viewModel: PlansViewModel = hiltViewModel()) {
    val boughtItems = viewModel.wishItems.collectAsStateWithLifecycle().value
        .filter { it.status == WishStatus.BOUGHT }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.wishlist_bought_title)) },
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            )
        },
    ) { padding ->
        if (boughtItems.isEmpty()) {
            EmptyState(stringResource(R.string.empty_wishlist_bought))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(boughtItems) { item ->
                    Card {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(item.category, style = MaterialTheme.typography.bodySmall)
                                if (item.comment.isNotBlank()) {
                                    Text(item.comment, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(item.price.formatByr(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WishlistPreview() {
    FinFocusTheme { WishlistScreen() }
}

