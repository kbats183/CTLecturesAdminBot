package ru.kbats.youtube.broadcastscheduler.youtube

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.http.FileContent
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.*
import kotlinx.datetime.Instant
import ru.kbats.youtube.broadcastscheduler.data.LectureBroadcastPrivacy
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.time.Duration.Companion.hours

class YoutubeApi(credential: Credential) {
    private val youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
        .setApplicationName("youtube-cmdline-createbroadcast-sample").build()

    fun getStreams(): MutableList<LiveStream> {
        return youtube.liveStreams().list("id,snippet,cdn,status").setMine(true).setMaxResults(50L).execute().items!!
    }

    fun getStream(id: String): LiveStream? =
        youtube.liveStreams().list("id,snippet,cdn,status").setId(id).setMaxResults(50L).execute().items.firstOrNull()

    fun createStream(title: String): LiveStream {
        // Create a snippet with the video stream's title.
        val streamSnippet = LiveStreamSnippet()
        streamSnippet.title = title

        // Define the content distribution network settings for the
        // video stream. The settings specify the stream's format and
        // ingestion type. See:
        // https://developers.google.com/youtube/v3/live/docs/liveStreams#cdn
        val cdnSettings = CdnSettings()
        cdnSettings.format = "1080p"
        cdnSettings.ingestionType = "rtmp"
        cdnSettings.resolution = "variable"
        cdnSettings.frameRate = "variable"
        val stream = LiveStream()
        stream.kind = "youtube#liveStream"
        stream.snippet = streamSnippet
        stream.cdn = cdnSettings

        try {
            // Construct and execute the API request to insert the stream.
            val liveStreamInsert = youtube.liveStreams().insert("snippet,cdn", stream)
            return liveStreamInsert.execute()!!
        } catch (e: IOException) {
            throw FailedApiRequestException(e)
        }
    }

    fun getBroadcasts(broadcastStatus: String? = null): MutableList<LiveBroadcast> {
        return youtube.liveBroadcasts().list("id,snippet,contentDetails,status")
            .setBroadcastStatus(broadcastStatus ?: "all").setMaxResults(50L).execute().items
    }

    fun getBroadcast(id: String): LiveBroadcast? {
        val response =
            youtube.liveBroadcasts().list("id,snippet,contentDetails,status").setId(id).setMaxResults(50L).execute()
        return response.items.firstOrNull()
    }

    /*
    Changes status of broadcast by id. From ready to testing, from testing to live, from live to complete
     */
    fun transitionBroadcast(id: String, newStatus: String): LiveBroadcast? {
        return youtube.liveBroadcasts().transition(newStatus, id, "id,snippet,contentDetails,status").execute()
    }

    fun createBroadcast(
        title: String,
        description: String,
        startTime: Instant,
        endTime: Instant = startTime + 2.hours,
        privacy: LectureBroadcastPrivacy,
    ): LiveBroadcast? {
        val broadcastSnippet = LiveBroadcastSnippet()
        broadcastSnippet.title = title
        broadcastSnippet.description = description
        broadcastSnippet.scheduledStartTime = DateTime(startTime.toEpochMilliseconds())
        broadcastSnippet.scheduledEndTime = DateTime(endTime.toEpochMilliseconds())

        val status = LiveBroadcastStatus()
        status.privacyStatus = privacy.name.lowercase(Locale.getDefault())

        val broadcast = LiveBroadcast()
        broadcast.kind = "youtube#liveBroadcast"
        broadcast.snippet = broadcastSnippet
        broadcast.status = status

        return youtube.liveBroadcasts().insert("snippet,status", broadcast).execute()
    }

    fun uploadBroadcastThumbnail(broadcastId: String, thumbnailPngFile: File): ThumbnailSetResponse? {
        return youtube.thumbnails().set(broadcastId, FileContent("image/png", thumbnailPngFile)).execute()
    }

    fun bindBroadcastStream(broadcastId: String, stramId: String): LiveBroadcast? {
        return youtube.liveBroadcasts().bind(broadcastId, "id,snippet,contentDetails,status")
            .setStreamId(stramId).execute()
    }
}
