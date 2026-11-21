package com.example.pricelistapp.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pricelistapp.R
import com.example.pricelistapp.data.local.ProductWithVariants
import com.example.pricelistapp.data.local.User
import com.example.pricelistapp.ui.login.UserRole

enum class MainTab(
    val title: String,
    val icon: ImageVector
) {
    SEARCH("Search", Icons.Default.Search),
    BROWSE("Browse", Icons.Default.AccountTree),
    PRODUCT("Product", Icons.Default.Book),
    ADD("Add", Icons.Default.Add),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: User,
    currentRole: UserRole,
    onRoleChange: (UserRole) -> Unit,
    onLogout: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(MainTab.SEARCH) }
    var selectedProduct by remember { mutableStateOf<ProductWithVariants?>(null) }
    var isFromSearch by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .height(32.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        // Role Icon (left of username dropdown)
                        Icon(
                            imageVector = if (currentRole == UserRole.MASTER) Icons.Default.Star else Icons.Default.Person,
                            contentDescription = currentRole.displayName,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        // User Dropdown Button
                        Box {
                            TextButton(
                                onClick = { menuExpanded = true }
                            ) {
                                Text(
                                    text = user.userName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "User Menu",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Device Role",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Row(
                                                    modifier = Modifier
                                                        .selectable(
                                                            selected = currentRole == UserRole.CLIENT,
                                                            onClick = { onRoleChange(UserRole.CLIENT) },
                                                            role = Role.RadioButton
                                                        )
                                                        .padding(end = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = currentRole == UserRole.CLIENT,
                                                        onClick = null
                                                    )
                                                    Text("Client", modifier = Modifier.padding(start = 4.dp))
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .selectable(
                                                            selected = currentRole == UserRole.MASTER,
                                                            onClick = { onRoleChange(UserRole.MASTER) },
                                                            role = Role.RadioButton
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = currentRole == UserRole.MASTER,
                                                        onClick = null
                                                    )
                                                    Text("Master", modifier = Modifier.padding(start = 4.dp))
                                                }
                                            }
                                        }
                                    },
                                    onClick = {}
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Logout",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "Logout",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onLogout()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.35f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "iconScale"
                    )

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (tab == MainTab.PRODUCT) {
                                isFromSearch = false
                            }
                            selectedTab = tab
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(iconScale)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                // Tab transition animation (Right to Left / Left to Right)
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            // Slide in from right, slide out to left
                            (fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)) { width -> width }) togetherWith
                                    (fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(300)) { width -> -width })
                        } else {
                            // Slide in from left, slide out to right
                            (fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)) { width -> -width }) togetherWith
                                    (fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(300)) { width -> width })
                        }
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        MainTab.SEARCH -> SearchTabContent(
                            user = user,
                            currentRole = currentRole,
                            onProductClick = { product ->
                                selectedProduct = product
                                isFromSearch = true
                                selectedTab = MainTab.PRODUCT
                            }
                        )
                        MainTab.BROWSE -> BrowseTabContent(user = user, currentRole = currentRole)
                        MainTab.PRODUCT -> ProductTabContent(
                            user = user,
                            currentRole = currentRole,
                            productWithVariants = selectedProduct,
                            isFromSearch = isFromSearch,
                            onBackToSearch = { selectedTab = MainTab.SEARCH }
                        )
                        MainTab.ADD -> AddTabContent(user = user, currentRole = currentRole)
                        MainTab.SETTINGS -> SettingsTabContent(user = user, currentRole = currentRole)
                    }
                }
            }
        }
    }
}

@Composable
fun BrowseTabContent(user: User, currentRole: UserRole) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Browse Hierarchy",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hierarchical tree table will be displayed here.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun SettingsTabContent(user: User, currentRole: UserRole) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "App configuration and LAN sync options will be displayed here.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
