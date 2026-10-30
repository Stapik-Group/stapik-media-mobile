package pl.stapik.media.ui.media

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

interface SortPreferenceStorage {
    suspend fun load(): EntrySort
    suspend fun save(sort: EntrySort)
}

private val Context.sortDataStore by preferencesDataStore(name = "sort_preference")

class DataStoreSortPreferenceStorage(private val context: Context) : SortPreferenceStorage {

    private val fieldKey = stringPreferencesKey("field")
    private val directionKey = stringPreferencesKey("direction")

    override suspend fun load(): EntrySort {
        val preferences = context.sortDataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .first()
        return EntrySort(
            field = SortField.fromKey(preferences[fieldKey]) ?: SortField.CONSUMED,
            direction = SortDirection.fromKey(preferences[directionKey]) ?: SortDirection.DESCENDING,
        )
    }

    override suspend fun save(sort: EntrySort) {
        context.sortDataStore.edit { preferences ->
            preferences[fieldKey] = sort.field.key
            preferences[directionKey] = sort.direction.key
        }
    }
}
