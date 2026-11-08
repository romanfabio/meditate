package io.github.romanfabio.meditate.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val VISIBLE_ITEMS = 5

/** Vertical snapping number wheel. The item in the middle band is the selected value. */
@Composable
fun WheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 46.dp
    val count = range.last - range.first + 1
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, count - 1))
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val centered by remember {
        derivedStateOf {
            val extra = if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
            (state.firstVisibleItemIndex + extra).coerceIn(0, count - 1)
        }
    }
    LaunchedEffect(range) {
        snapshotFlow { centered }.collect { onValueChange(range.first + it) }
    }

    Box(
        modifier = modifier
            .width(76.dp)
            .height(itemHeight * VISIBLE_ITEMS)
            .semantics { contentDescription = "$description ${range.first + centered}" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
        )
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = itemHeight * (VISIBLE_ITEMS / 2)),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed((range.first..range.last).toList()) { index, number ->
                val selected = index == centered
                Box(Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "%02d".format(number),
                        style = TextStyle(
                            fontSize = if (selected) 32.sp else 22.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        },
                    )
                }
            }
        }
    }
}
