        "backup" -> {
            BackupRestoreScreen(
                onBack = onBack,
            )
        }
        "advanced" -> {
            AdvancedSettings(
                onBack = onBack,
            )
        }
        "data_import_export" -> {
            DataImportExportScreen(
                onBack = onBack,
            )
        }
        "font_selection" -> {
            FontSelectionScreen(
                onBack = onBack,
            )
        }
        "font_import" -> {
            FontImportScreen(
                onBack = onBack,
            )
        }
        "download_audio" -> {
            DownloadAudioScreen(
                onBack = onBack,
            )
        }
        "support" -> {
            SupportScreen(
                onBack = onBack,
            )
        }
        "keyboard_help" -> {
            KeyboardHelpScreen(
                onBack = onBack,
                onLaunchTutorial = onLaunchTutorial,
                modifier = modifier,
            )
        }
        else -> {
            CategoryDetailScreen(
                categoryId = selectedCategory!!,
                onBack = onBack,
                onOpenCategory = { selectedCategory = it },
                onOpenThemeCustomization = onOpenThemeCustomization,
                onGoHome = onGoHome,
            )
        }