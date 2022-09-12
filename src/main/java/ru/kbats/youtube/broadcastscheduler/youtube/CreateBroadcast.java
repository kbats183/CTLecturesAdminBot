package ru.kbats.youtube.broadcastscheduler.youtube;

/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import ru.kbats.youtube.broadcastscheduler.youtube.Auth;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Use the YouTube Live Streaming API to insert a broadcast and a stream
 * and then bind them together. Use OAuth 2.0 to authorize the API requests.
 *
 * @author Ibrahim Ulukaya
 */
public class CreateBroadcast {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;

    /**
     * Create and insert a liveBroadcast resource.
     */
    public static void main(String[] args) {

        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account.
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");

        try {
            // Authorize the request.
            Credential credential = Auth.authorize(scopes, "createbroadcast");

            // This object is used to make YouTube Data API requests.
            youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-createbroadcast-sample").build();

            LiveBroadcast returnedBroadcast = createBroadcast();
            ThumbnailSetResponse result = setBroadcastThumbnail("C:\\Users\\bats_\\Downloads\\y2020. 3439. mathlog.png", returnedBroadcast);

            String streamId = "";
            LiveStreamListResponse streamsList = youtube.liveStreams().list("id,snippet").setMine(true).setMaxResults(50L).execute();
            System.out.println(streamsList);
            for (LiveStream stream : streamsList.getItems()) {
                System.out.println(stream.getId() + " " + stream.getSnippet().getTitle());
                if (stream.getSnippet().getTitle().equals("kbats_laptop")) {
                    System.out.println(stream.getId());
                    streamId = stream.getId();
                }
            }

//            LiveStream returnedStream = createLiveStram("New Stream");
//
//            // Construct and execute a request to bind the new broadcast
//            // and stream.
            bindBroadcast(returnedBroadcast, streamId);
        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
            e.printStackTrace();

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static LiveBroadcast bindBroadcast(LiveBroadcast broadcast, String streamId) throws IOException {
        YouTube.LiveBroadcasts.Bind liveBroadcastBind =
                youtube.liveBroadcasts().bind(broadcast.getId(), "id,contentDetails");
        liveBroadcastBind.setStreamId(streamId);
        broadcast = liveBroadcastBind.execute();

        // Print information from the API response.
        System.out.println("\n================== Returned Bound Broadcast ==================\n");
        System.out.println("  - Broadcast Id: " + broadcast.getId());
        System.out.println(
                "  - Bound Stream Id: " + broadcast.getContentDetails().getBoundStreamId());
        return broadcast;
    }

    private static ThumbnailSetResponse setBroadcastThumbnail(String filepath, LiveBroadcast returnedBroadcast) throws IOException {
        final File thFile = new File(filepath);
        return youtube.thumbnails().set(returnedBroadcast.getId(), new FileContent("image/png", thFile)).execute();
    }

    private static LiveBroadcast createBroadcast() throws IOException {
        // Prompt the user to enter a title for the broadcast.
        String title = getBroadcastTitle();

        // Create a snippet with the title and scheduled start and end
        // times for the broadcast. Currently, those times are hard-coded.
        LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
        broadcastSnippet.setTitle(title);
        broadcastSnippet.setScheduledStartTime(new DateTime("2024-01-30T00:00:00.000Z"));
        broadcastSnippet.setScheduledEndTime(new DateTime("2024-01-31T00:00:00.000Z"));

        // Set the broadcast's privacy status to "private". See:
        // https://developers.google.com/youtube/v3/live/docs/liveBroadcasts#status.privacyStatus
        LiveBroadcastStatus status = new LiveBroadcastStatus();
        status.setPrivacyStatus("private");

        LiveBroadcast broadcast = new LiveBroadcast();
        broadcast.setKind("youtube#liveBroadcast");
        broadcast.setSnippet(broadcastSnippet);
        broadcast.setStatus(status);

        // Construct and execute the API request to insert the broadcast.
        YouTube.LiveBroadcasts.Insert liveBroadcastInsert =
                youtube.liveBroadcasts().insert("snippet,status", broadcast);
        LiveBroadcast returnedBroadcast = liveBroadcastInsert.execute();

        // Print information from the API response.
        System.out.println("\n================== Returned Broadcast ==================\n");
        System.out.println("  - Id: " + returnedBroadcast.getId());
        System.out.println("  - Title: " + returnedBroadcast.getSnippet().getTitle());
        System.out.println("  - Description: " + returnedBroadcast.getSnippet().getDescription());
        System.out.println("  - Published At: " + returnedBroadcast.getSnippet().getPublishedAt());
        System.out.println(
                "  - Scheduled Start Time: " + returnedBroadcast.getSnippet().getScheduledStartTime());
        System.out.println(
                "  - Scheduled End Time: " + returnedBroadcast.getSnippet().getScheduledEndTime());

        return returnedBroadcast;
    }

    @NotNull
    private static LiveStream createLiveStram(String title) throws IOException {
        // Create a snippet with the video stream's title.
        LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
        streamSnippet.setTitle(title);

        // Define the content distribution network settings for the
        // video stream. The settings specify the stream's format and
        // ingestion type. See:
        // https://developers.google.com/youtube/v3/live/docs/liveStreams#cdn
        CdnSettings cdnSettings = new CdnSettings();
        cdnSettings.setFormat("1080p");
        cdnSettings.setIngestionType("rtmp");
        cdnSettings.setResolution("variable");
        cdnSettings.setFrameRate("variable");

        LiveStream stream = new LiveStream();
        stream.setKind("youtube#liveStream");
        stream.setSnippet(streamSnippet);
        stream.setCdn(cdnSettings);

        // Construct and execute the API request to insert the stream.
        YouTube.LiveStreams.Insert liveStreamInsert =
                youtube.liveStreams().insert("snippet,cdn", stream);
        LiveStream returnedStream = liveStreamInsert.execute();

        // Print information from the API response.
        System.out.println("\n================== Returned Stream ==================\n");
        System.out.println("  - Id: " + returnedStream.getId());
        System.out.println("  - Title: " + returnedStream.getSnippet().getTitle());
        System.out.println("  - Description: " + returnedStream.getSnippet().getDescription());
        System.out.println("  - Published At: " + returnedStream.getSnippet().getPublishedAt());
        return returnedStream;
    }

    /*
     * Prompt the user to enter a title for a broadcast.
     */
    private static String getBroadcastTitle() throws IOException {

        String title = "";

        System.out.print("Please enter a broadcast title: ");
//        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
//        title = bReader.readLine();

        if (title.length() < 1) {
            // Use "New Broadcast" as the default title.
            title = "New Broadcast";
        }
        return title;
    }

}
