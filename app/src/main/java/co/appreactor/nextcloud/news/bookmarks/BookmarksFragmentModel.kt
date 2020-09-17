package co.appreactor.nextcloud.news.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.appreactor.nextcloud.news.feeds.FeedsRepository
import co.appreactor.nextcloud.news.common.Preferences
import co.appreactor.nextcloud.news.common.cropPreviewImages
import co.appreactor.nextcloud.news.common.showPreviewImages
import co.appreactor.nextcloud.news.db.Feed
import co.appreactor.nextcloud.news.db.Entry
import co.appreactor.nextcloud.news.entries.*
import co.appreactor.nextcloud.news.opengraph.EntriesImagesRepository
import co.appreactor.nextcloud.news.podcasts.EntriesAudioRepository
import co.appreactor.nextcloud.news.podcasts.isPodcast
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.text.DateFormat
import java.util.*

class BookmarksFragmentModel(
    private val feedsRepository: FeedsRepository,
    private val entriesRepository: EntriesRepository,
    private val entriesSummariesRepository: EntriesSummariesRepository,
    private val entriesImagesRepository: EntriesImagesRepository,
    private val entriesAudioRepository: EntriesAudioRepository,
    private val prefs: Preferences,
) : ViewModel() {

    init {
        viewModelScope.launch {
            entriesImagesRepository.warmUpMemoryCache()
        }
    }

    suspend fun getBookmarks() = combine(
        feedsRepository.getAll(),
        entriesRepository.getStarred(),
        getShowPreviewImages(),
        getCropPreviewImages(),
    ) { feeds, entries, showPreviewImages, cropPreviewImages ->
        entries.map {
            val feed = feeds.singleOrNull { feed -> feed.id == it.feedId.toString() }
            it.toRow(feed, showPreviewImages, cropPreviewImages)
        }
    }

    suspend fun downloadPodcast(id: String) {
        entriesAudioRepository.downloadPodcast(id)
    }

    suspend fun getEntry(id: String) = entriesRepository.get(id).first()

    private suspend fun getShowPreviewImages() = prefs.showPreviewImages()

    private suspend fun getCropPreviewImages() = prefs.cropPreviewImages()

    private suspend fun Entry.toRow(
        feed: Feed?,
        showPreviewImages: Boolean,
        cropPreviewImages: Boolean,
    ): EntriesAdapterItem {
        val publishedDateTime = LocalDateTime.parse(published)
        val publishedDateString = DateFormat.getDateInstance()
            .format(Date(publishedDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()))

        return EntriesAdapterItem(
            id = id,
            title = title,
            (feed?.title ?: "Unknown feed") + " · " + publishedDateString,
            viewed = false,
            podcast = isPodcast(),
            podcastDownloadPercent = flow {
                entriesAudioRepository.getDownloadProgress(this@toRow.id).collect {
                    emit(it)
                }
            },
            image = flow {
                entriesImagesRepository.parse(this@toRow)

                entriesImagesRepository.getImage(this@toRow).collect {
                    emit(it)
                }
            },
            cachedImage = entriesImagesRepository.getImage(this).first(),
            showImage = showPreviewImages,
            cropImage = cropPreviewImages,
            summary = flow { emit(entriesSummariesRepository.getSummary(this@toRow)) },
            cachedSummary = entriesSummariesRepository.getCachedSummary(this.id)
        )
    }
}