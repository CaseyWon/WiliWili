package com.example.bilimini.data.recommendation

import com.example.bilimini.data.model.VideoSummary
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max

class HomeFeedRanker(
    private val profileStore: RecommendationProfileStore,
) {
    fun rerank(candidates: List<VideoSummary>): List<VideoSummary> {
        if (candidates.size < 3) {
            return candidates
        }

        val history = profileStore.recentHistory()
        val authorWeights = weightedAuthors(history)
        val tokenWeights = weightedTokens(history)
        val recentBvids = history.take(10).map { it.bvid }.toSet()
        val preferredDuration = preferredDuration(history)
        val candidateCount = max(candidates.size, 1)

        val scored = candidates.mapIndexed { index, video ->
            val titleTokens = tokenize(video.title)
            val authorAffinity = authorWeights[video.author].orEmptyWeight()
            val tokenAffinity = if (titleTokens.isEmpty()) {
                0.0
            } else {
                titleTokens.sumOf { tokenWeights[it].orEmptyWeight() } / titleTokens.size
            }
            val popularityScore = ln(1.0 + (video.playCount ?: 0L).toDouble()) / 16.0
            val baseRankScore = (candidateCount - index).toDouble() / candidateCount
            val durationScore = durationPreferenceScore(
                preferredDuration = preferredDuration,
                candidateDuration = video.durationSeconds,
            )
            val repetitionPenalty = if (video.bvid in recentBvids) 0.55 else 0.0

            RankedCandidate(
                video = video,
                topicTokens = titleTokens,
                baseScore = (baseRankScore * 0.38) +
                    (popularityScore * 0.18) +
                    (authorAffinity * 0.18) +
                    (tokenAffinity * 0.18) +
                    (durationScore * 0.08) -
                    repetitionPenalty,
            )
        }

        return diversify(scored)
    }

    private fun diversify(scored: List<RankedCandidate>): List<VideoSummary> {
        val remaining = scored.toMutableList()
        val selected = mutableListOf<RankedCandidate>()

        while (remaining.isNotEmpty()) {
            val next = remaining.maxByOrNull { candidate ->
                candidate.baseScore -
                    authorPenalty(selected, candidate.video.author) -
                    topicPenalty(selected, candidate.topicTokens)
            } ?: break
            selected += next
            remaining.remove(next)
        }

        return selected.map { it.video }
    }

    private fun weightedAuthors(history: List<RecommendationHistoryEntry>): Map<String, Double> {
        return buildMap {
            history.forEachIndexed { index, entry ->
                val weight = recencyWeight(index)
                put(entry.author, getOrDefault(entry.author, 0.0) + weight)
            }
        }
    }

    private fun weightedTokens(history: List<RecommendationHistoryEntry>): Map<String, Double> {
        return buildMap {
            history.forEachIndexed { index, entry ->
                val weight = recencyWeight(index)
                tokenize(entry.title).forEach { token ->
                    put(token, getOrDefault(token, 0.0) + weight)
                }
            }
        }
    }

    private fun preferredDuration(history: List<RecommendationHistoryEntry>): Double? {
        val values = history.mapNotNull { it.durationSeconds?.toDouble() }
        if (values.isEmpty()) {
            return null
        }
        val weightedSum = values.mapIndexed { index, value ->
            value * recencyWeight(index)
        }.sum()
        val totalWeight = values.indices.sumOf { recencyWeight(it) }
        return if (totalWeight == 0.0) null else weightedSum / totalWeight
    }

    private fun durationPreferenceScore(
        preferredDuration: Double?,
        candidateDuration: Long?,
    ): Double {
        if (preferredDuration == null || candidateDuration == null) {
            return 0.0
        }
        val ratio = abs(candidateDuration - preferredDuration) / max(preferredDuration, 1.0)
        return (1.0 - ratio.coerceIn(0.0, 1.0)) * 0.8
    }

    private fun authorPenalty(selected: List<RankedCandidate>, author: String): Double {
        val count = selected.count { it.video.author == author }
        return count * 0.28
    }

    private fun topicPenalty(
        selected: List<RankedCandidate>,
        topicTokens: Set<String>,
    ): Double {
        if (topicTokens.isEmpty()) {
            return 0.0
        }
        val overlapCount = selected.count { picked ->
            picked.topicTokens.intersect(topicTokens).isNotEmpty()
        }
        return overlapCount * 0.12
    }

    private fun tokenize(text: String): Set<String> {
        val normalized = text.lowercase()
        val regex = Regex("""[\u4e00-\u9fff]{2,}|[a-z0-9]{2,}""")
        return regex.findAll(normalized)
            .map { it.value }
            .take(12)
            .toSet()
    }

    private fun recencyWeight(index: Int): Double = 1.0 / (1.0 + (index * 0.55))

    private fun Double?.orEmptyWeight(): Double = this ?: 0.0

    private data class RankedCandidate(
        val video: VideoSummary,
        val topicTokens: Set<String>,
        val baseScore: Double,
    )
}
