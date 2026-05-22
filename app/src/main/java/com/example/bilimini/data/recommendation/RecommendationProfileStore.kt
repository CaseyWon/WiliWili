package com.example.bilimini.data.recommendation

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.bilimini.data.model.VideoDetail
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RecommendationProfileStore(context: Context) {
    private val preferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun recentHistory(): List<RecommendationHistoryEntry> {
        val raw = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<RecommendationHistoryEntry>>(raw)
        }.getOrDefault(emptyList())
    }

    fun recordVideoOpen(detail: VideoDetail) {
        val nextHistory = buildList {
            add(
                RecommendationHistoryEntry(
                    bvid = detail.bvid,
                    title = detail.title,
                    author = detail.author,
                    durationSeconds = detail.durationSeconds,
                    openedAtEpochMs = System.currentTimeMillis(),
                )
            )
            recentHistory()
                .filterNot { it.bvid == detail.bvid }
                .take(MAX_HISTORY_SIZE - 1)
                .forEach(::add)
        }
        preferences.edit()
            .putString(KEY_HISTORY, json.encodeToString(nextHistory))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "bilimini.recommendation.profile"
        const val KEY_HISTORY = "history"
        const val MAX_HISTORY_SIZE = 40
    }
}
