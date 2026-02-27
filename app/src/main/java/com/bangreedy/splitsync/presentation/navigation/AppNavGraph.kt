package com.bangreedy.splitsync.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bangreedy.splitsync.presentation.addexpense.AddExpenseScreen
import com.bangreedy.splitsync.presentation.groups.GroupsScreen
import com.bangreedy.splitsync.presentation.groupdetails.GroupDetailsScreen
import com.bangreedy.splitsync.presentation.settleup.SettleUpScreen
import com.bangreedy.splitsync.presentation.friends.FriendsScreen
import com.bangreedy.splitsync.presentation.activity.ActivityScreen
import com.bangreedy.splitsync.presentation.account.AccountScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val GROUPS = "groups"
    const val GROUP_DETAILS = "group/{groupId}"
    fun groupDetails(groupId: String) = "group/$groupId"
    const val ADD_EXPENSE = "group/{groupId}/add-expense"
    fun addExpense(groupId: String) = "group/$groupId/add-expense"
    const val SETTLE_UP = "group/{groupId}/settle?fromId={fromId}&toId={toId}&amountMinor={amountMinor}"

    fun settleUp(groupId: String) = "group/$groupId/settle"

    fun settleUpPrefill(groupId: String, fromId: String, toId: String, amountMinor: Long) =
        "group/$groupId/settle?fromId=$fromId&toId=$toId&amountMinor=$amountMinor"
    const val INVITES = "invites"
    const val NOTIFICATIONS = "notifications"
    const val FRIENDS = "friends"
    const val FRIEND_DETAILS = "friend/{friendUid}"
    fun friendDetails(friendUid: String) = "friend/$friendUid"
    const val ACTIVITY = "activity"
    const val ACCOUNT = "account"
    const val NFC_FRIEND = "nfc-friend"
}

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Groups : BottomNavItem(Routes.GROUPS, "Groups", Icons.Default.Home)
    data object Friends : BottomNavItem(Routes.FRIENDS, "Friends", Icons.Default.Person)
    data object Activity : BottomNavItem(Routes.ACTIVITY, "Activity", Icons.AutoMirrored.Filled.List)
    data object Account : BottomNavItem(Routes.ACCOUNT, "Account", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Groups,
        BottomNavItem.Friends,
        BottomNavItem.Activity,
        BottomNavItem.Account
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = currentRoute in items.map { it.route }

    Scaffold(
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = { Text("SplitSync") }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.GROUPS,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.GROUPS) {
                GroupsScreen(
                    onGroupClick = { groupId ->
                        navController.navigate(Routes.groupDetails(groupId))
                    },
                    onInvitesClick = {
                        navController.navigate(Routes.INVITES)
                    },
                    onNotificationsClick = {
                        navController.navigate(Routes.NOTIFICATIONS)
                    }
                )
            }

            composable(
                route = Routes.GROUP_DETAILS,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                GroupDetailsScreen(
                    groupId = groupId,
                    onAddExpense = { navController.navigate(Routes.addExpense(groupId)) }
                )

            }

            composable(
                route = Routes.ADD_EXPENSE,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                AddExpenseScreen(groupId = groupId, onBack = { navController.popBackStack() })
            }


            composable(
                route = Routes.SETTLE_UP,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("fromId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("toId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("amountMinor") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                val fromId = backStackEntry.arguments?.getString("fromId").orEmpty()
                val toId = backStackEntry.arguments?.getString("toId").orEmpty()
                val amountMinor = backStackEntry.arguments?.getLong("amountMinor") ?: 0L

                SettleUpScreen(
                    groupId = groupId,
                    initialFromId = fromId,
                    initialToId = toId,
                    initialAmountMinor = amountMinor,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.INVITES) {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    com.bangreedy.splitsync.presentation.invites.InvitesScreen(myUid = uid)
                }
            }
            composable(Routes.NOTIFICATIONS) {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) com.bangreedy.splitsync.presentation.notifications.NotificationsScreen(uid)
            }

            composable(Routes.FRIENDS) {
                FriendsScreen(
                    onFriendClick = { friendUid ->
                        navController.navigate(Routes.friendDetails(friendUid))
                    },
                    onNfcClick = {
                        navController.navigate(Routes.NFC_FRIEND)
                    }
                )
            }

            composable(
                route = Routes.FRIEND_DETAILS,
                arguments = listOf(navArgument("friendUid") { type = NavType.StringType })
            ) { backStackEntry ->
                val friendUid = backStackEntry.arguments?.getString("friendUid") ?: return@composable
                com.bangreedy.splitsync.presentation.friends.FriendDetailsScreen(
                    friendUid = friendUid,
                    onBack = { navController.popBackStack() },
                    onNavigateToGroup = { groupId ->
                        navController.navigate(Routes.groupDetails(groupId))
                    }
                )
            }

            composable(Routes.NFC_FRIEND) {
                com.bangreedy.splitsync.presentation.friends.NfcFriendScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ACTIVITY) { ActivityScreen() }
            composable(Routes.ACCOUNT) { AccountScreen() }

        }
    }
}
