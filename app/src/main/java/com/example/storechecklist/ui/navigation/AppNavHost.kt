package com.example.storechecklist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.storechecklist.ui.screens.RoleSelectionScreen
import com.example.storechecklist.ui.screens.admin.AdminChecklistScreen
import com.example.storechecklist.ui.screens.admin.AdminListsScreen
import com.example.storechecklist.ui.screens.user.UserChecklistScreen
import com.example.storechecklist.ui.screens.user.UserListsScreen
import com.example.storechecklist.ui.viewmodel.AdminChecklistViewModel
import com.example.storechecklist.ui.viewmodel.UserChecklistViewModel

private object Routes {
    const val ROLE = "role"
    const val ADMIN_LISTS = "admin_lists"
    const val USER_LISTS = "user_lists"
    const val CHECKLIST_ID = "checklistId"
    const val ADMIN_CHECKLIST_TEMPLATE = "admin_checklist/{checklistId}"
    const val USER_CHECKLIST_TEMPLATE = "user_checklist/{checklistId}"

    fun adminChecklist(checklistId: Long): String = "admin_checklist/$checklistId"
    fun userChecklist(checklistId: Long): String = "user_checklist/$checklistId"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.ROLE,
    ) {
        composable(Routes.ROLE) {
            RoleSelectionScreen(
                onOpenAdmin = { navController.navigate(Routes.ADMIN_LISTS) },
                onOpenUser = { navController.navigate(Routes.USER_LISTS) },
            )
        }

        composable(Routes.ADMIN_LISTS) {
            AdminListsScreen(
                onBack = { navController.popBackStack() },
                onOpenChecklist = { checklistId ->
                    navController.navigate(Routes.adminChecklist(checklistId))
                },
            )
        }

        composable(
            route = Routes.ADMIN_CHECKLIST_TEMPLATE,
            arguments = listOf(navArgument(Routes.CHECKLIST_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val checklistId = backStackEntry.arguments?.getLong(Routes.CHECKLIST_ID) ?: return@composable
            val checklistViewModel = viewModel<AdminChecklistViewModel>(
                factory = AdminChecklistViewModel.factory(checklistId),
            )
            AdminChecklistScreen(
                viewModel = checklistViewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.USER_LISTS) {
            UserListsScreen(
                onBack = { navController.popBackStack() },
                onOpenChecklist = { checklistId ->
                    navController.navigate(Routes.userChecklist(checklistId))
                },
            )
        }

        composable(
            route = Routes.USER_CHECKLIST_TEMPLATE,
            arguments = listOf(navArgument(Routes.CHECKLIST_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val checklistId = backStackEntry.arguments?.getLong(Routes.CHECKLIST_ID) ?: return@composable
            val checklistViewModel = viewModel<UserChecklistViewModel>(
                factory = UserChecklistViewModel.factory(checklistId),
            )
            UserChecklistScreen(
                viewModel = checklistViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
