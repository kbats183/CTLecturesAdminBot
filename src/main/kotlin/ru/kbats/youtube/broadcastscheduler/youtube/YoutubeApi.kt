package ru.kbats.youtube.broadcastscheduler.youtube

import com.google.api.client.auth.oauth2.Credential
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.CdnSettings
import com.google.api.services.youtube.model.LiveBroadcast
import com.google.api.services.youtube.model.LiveStream
import com.google.api.services.youtube.model.LiveStreamSnippet
import java.io.IOException

class YoutubeApi(private val credential: Credential) {
    // This object is used to make YouTube Data API requests.
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
}
