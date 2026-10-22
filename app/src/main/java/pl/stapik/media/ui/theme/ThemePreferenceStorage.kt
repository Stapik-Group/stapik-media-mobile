package pl.stapik.media.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import pl.stapik.common.theme.ThemeId

interface ThemePreferenceStorage {
    suspend fun load(): ThemeId
    suspend fun save(theme: ThemeId)
}

private val Context.themeDataStore by preferencesDataStore(name = "theme_preference")

class DataStoreThemePreferenceStorage(private val context: Context) : ThemePreferenceStorage {

    private val key = stringPreferencesKey("selected_theme")

    override suspend fun load(): ThemeId {
        val stored = context.themeDataStore.data.first()[key]
        // Earlier versions stored the enum name (CLASSIC_PINK); the shared id is "classic-pink".
        return ThemeId.fromId(stored?.lowercase()?.replace('_', '-'), default = ThemeId.CLASSIC)
    }

    override suspend fun save(theme: ThemeId) {
        context.themeDataStore.edit { it[key] = theme.id }
    }
}
