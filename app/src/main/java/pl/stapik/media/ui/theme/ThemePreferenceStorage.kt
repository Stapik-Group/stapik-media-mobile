package pl.stapik.media.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

interface ThemePreferenceStorage {
    suspend fun load(): AppTheme
    suspend fun save(theme: AppTheme)
}

private val Context.themeDataStore by preferencesDataStore(name = "theme_preference")

class DataStoreThemePreferenceStorage(private val context: Context) : ThemePreferenceStorage {

    private val key = stringPreferencesKey("selected_theme")

    override suspend fun load(): AppTheme {
        val stored = context.themeDataStore.data.first()[key]
        return AppTheme.entries.firstOrNull { it.name == stored } ?: AppTheme.CLASSIC
    }

    override suspend fun save(theme: AppTheme) {
        context.themeDataStore.edit { it[key] = theme.name }
    }
}