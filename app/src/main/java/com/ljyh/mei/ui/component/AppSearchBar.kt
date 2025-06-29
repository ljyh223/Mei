package com.ljyh.mei.ui.component

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import com.ljyh.mei.MainScreenState
import com.ljyh.mei.R
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain

@Composable
fun AppSearchBar(
    active: Boolean,
    state: MainScreenState,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit
) {
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        active = active,
        onActiveChange = onActiveChange,
        scrollBehavior = searchBarScrollBehavior,
        placeholder = {
            Text("搜索")
        },
        leadingIcon = {
            IconButton(
                onClick = {
                    when {
                        active -> onActiveChange(false)
                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                            navController.navigateUp()
                        }

                        else -> onActiveChange(true)
                    }
                },
                onLongClick = {
                    when {
                        active -> {}
                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                            navController.backToMain()
                        }

                        else -> {}
                    }
                }
            ) {
                Icon(
                    imageVector = if (active || !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        Icons.AutoMirrored.Rounded.ArrowBack
                    } else {
                        Icons.Rounded.Search
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = null
                )
            }
        },
        trailingIcon = {
            if (active) {
                if (query.text.isNotEmpty()) {
                    androidx.compose.material3.IconButton(
                        onClick = { onQueryChange(TextFieldValue("")) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                }
                androidx.compose.material3.IconButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "还未实现",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            R.drawable.cloud
                        ),
                        contentDescription = null
                    )
                }
            } else if (navBackStackEntry?.destination?.route in topLevelScreens) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            navController.navigate(Screen.Setting.route)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null
                    )
                }
            }
        },
        focusRequester = searchBarFocusRequester,
        modifier = Modifier.align(Alignment.TopCenter),
    ) {

    }
}