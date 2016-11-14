/*
 * Copyright (c) 2012 Google Inc.
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

package com.yousearchj.cmdline.data;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Joiner;
import com.yousearchj.cmdline.Auth;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoCategory;
import com.google.api.services.youtube.model.VideoCategoryListResponse;
import com.google.api.services.youtube.model.VideoCategorySnippet;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoFileDetails;
import com.google.api.services.youtube.model.VideoFileDetailsVideoStream;
import com.google.api.services.youtube.model.VideoGetRatingResponse;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoPlayer;
import com.google.api.services.youtube.model.VideoRating;
import com.google.api.services.youtube.model.VideoRecordingDetails;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;
import com.google.api.services.youtube.model.VideoStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Print a list of videos matching a search term.
 *
 * @author Jeremy Walker
 */
public class Search {

    /**
     * Define a global variable that identifies the name of a file that
     * contains the developer's API key.
     */
    private static final String PROPERTIES_FILENAME = "youtube.properties";

    private static final long NUMBER_OF_VIDEOS_RETURNED = 5;

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;

    /**
     * Initialize a YouTube object to search for videos on YouTube. Then
     * display the name and thumbnail image of each video in the result set.
     *
     * @param args command line args.
     */
    public static void main(String[] args) {
        // Read the developer key from the properties file.
        Properties properties = new Properties();
        try {
            InputStream in = Search.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
            properties.load(in);

        } catch (IOException e) {
            System.err.println("There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause()
                    + " : " + e.getMessage());
            System.exit(1);
        }

        try {
            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.
            youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
                public void initialize(HttpRequest request) throws IOException {
                }
            }).setApplicationName("youtube-cmdline-search-sample").build();

            // Prompt the user to enter a query term.
            String queryTerm = getInputQuery();

            String apiKey1 = properties.getProperty("youtube.apikey1");
            
            YouTube.VideoCategories.List categories = youtube.videoCategories().list("snippet");
            categories.setId("10");
            //categories.setRegionCode("CA");
            categories.setKey(apiKey1);
            VideoCategoryListResponse categoryResponse = categories.execute();
            List<VideoCategory> categoryList = categoryResponse.getItems();
            System.out.println("Categories count: " + categoryList.size());
            VideoCategory category = categoryList.get(0);
            VideoCategorySnippet categorySnippet = category.getSnippet();
            System.out.println("Category : " + categorySnippet.getTitle());
            
            // Define the API request for retrieving search results.
            YouTube.Search.List search = youtube.search().list("id,snippet");

            // Set your developer key from the {{ Google Cloud Console }} for
            // non-authenticated requests. See:
            // {{ https://cloud.google.com/console }}
            search.setKey(apiKey1);
            search.setQ(queryTerm);

            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("video");

            // To increase efficiency, only retrieve the fields that the
            // application uses.
            //search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

            // Call the API and print results.
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            if (searchResultList != null) {
                prettyPrint(searchResultList.iterator(), queryTerm);
            }
            
            // get 
            System.out.println(" >>>>>>>>>>>>>> ======== <<<<<<<<<<<<<<< \n");
            List<String> videoIds = new ArrayList<String>();

            if (searchResultList != null) {

                // Merge video IDs
                for (SearchResult searchResult : searchResultList) {
                    videoIds.add(searchResult.getId().getVideoId());
                }
                Joiner stringJoiner = Joiner.on(',');
                String videoId = stringJoiner.join(videoIds);

                YouTube.Videos.List listVideosRequest = youtube.videos().
                    list("snippet, recordingDetails, statistics, player, contentDetails, status").
                    setId(videoId);
                String apiKey2 = properties.getProperty("youtube.apikey1");
                listVideosRequest.setKey(apiKey2);
                VideoListResponse listResponse = listVideosRequest.execute();

                List<Video> ratingList = listResponse.getItems();
            
                Iterator<Video> videoItr = ratingList.iterator();
                while (videoItr.hasNext())
                {
                    Video singleVideo = videoItr.next();
                    String id = singleVideo.getId();
                    VideoStatistics videoStats = singleVideo.getStatistics();
                    VideoSnippet videoSnippet = singleVideo.getSnippet();
                    //VideoFileDetails videoFileDetails = singleVideo.getFileDetails();
                    VideoRecordingDetails recordingDetails = singleVideo.getRecordingDetails();

                    VideoContentDetails contentDetails = singleVideo.getContentDetails();
                    System.out.println("ContentDetails available: " + (contentDetails != null));

                    VideoStatus status = singleVideo.getStatus();
                    System.out.println("Status available: " + (status != null));

                    VideoPlayer player = singleVideo.getPlayer();
                    System.out.println("Player available: " + (player != null));

                    System.out.println(" Video Id: " + id);
                    System.out.println(" - Title        : " + videoSnippet.getTitle());
                    System.out.println(" - Chennel Id   : " + videoSnippet.getChannelId());
                    System.out.println(" - Chennel Title: " + videoSnippet.getChannelTitle());
                    System.out.println(" - Category Id  : " + videoSnippet.getCategoryId());
                    System.out.println(" - Tags         : " + videoSnippet.getTags());
                    
                    if (videoStats != null)
                    {
                        System.out.println(" ... comments count: " + videoStats.getCommentCount());
                        System.out.println(" ... dislike count : " + videoStats.getDislikeCount());
                        System.out.println(" ... like cout     : " + videoStats.getLikeCount());
                        System.out.println(" ... favorite count: " + videoStats.getFavoriteCount());
                        System.out.println(" ... view count    : " + videoStats.getViewCount());
                    }else {
                        System.out.println(" ... null stats");
                    }
                    if (recordingDetails != null) {
                        System.out.println(" >>> Recording Date: " + recordingDetails.getRecordingDate());
                        if (recordingDetails.getLocation() != null)
                            System.out.println(" >>> Location      : " + recordingDetails.getLocation().toPrettyString());
                    }else {
                        System.out.println(" ... null recoding details");
                    }
                    if (contentDetails != null) {
                        if (contentDetails.getContentRating() != null)
                            System.out.println(" >>> Rating     : " + contentDetails.getContentRating().toPrettyString());
                        //System.out.println(" >>> Dimension  : " + contentDetails.getDimension());
                        System.out.println(" >>> Duration   : " + contentDetails.getDuration());
                        //System.out.println(" >>> Projection : " + contentDetails.getProjection());
                    }else {
                        System.out.println(" ... null content details");
                    }
                    if (status != null) {
                        if (status.getPublishAt() != null)
                            System.out.println(" >>> PublishAt : " + status.getPublishAt().toString());
                    }else {
                        System.out.println(" ... null status");
                    }
                    if (player != null) {
                        System.out.println(" >>> EmbedHtml : " + player.getEmbedHtml());
                    }else {
                        System.out.println(" ... null player");
                    }
                    System.out.println(" .. Description: " + videoSnippet.getDescription());

                    System.out.println("\n-------------------------------------------------------------\n");
                }
            }

        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /*
     * Prompt the user to enter a query term and return the user-specified term.
     */
    private static String getInputQuery() throws IOException {

        String inputQuery = "";

        System.out.print("Please enter a search term: ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        inputQuery = bReader.readLine();

        if (inputQuery.length() < 1) {
            // Use the string "YouTube Developers Live" as a default.
            inputQuery = "YouTube Developers Live";
        }
        return inputQuery;
    }

    /*
     * Prints out all results in the Iterator. For each result, print the
     * title, video ID, and thumbnail.
     *
     * @param iteratorSearchResults Iterator of SearchResults to print
     *
     * @param query Search query (String)
     */
    private static void prettyPrint(Iterator<SearchResult> iteratorSearchResults, String query) {

        System.out.println("\n=============================================================");
        System.out.println(
                "   First " + NUMBER_OF_VIDEOS_RETURNED + " videos for search on \"" + query + "\".");
        System.out.println("=============================================================\n");

        if (!iteratorSearchResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        while (iteratorSearchResults.hasNext()) {

            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();
            
            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();

                System.out.println(" Video Id: " + rId.getVideoId());
                System.out.println(" Title: " + singleVideo.getSnippet().getTitle());
                System.out.println(" Thumbnail: " + thumbnail.getUrl());
                System.out.println("\n-------------------------------------------------------------\n");
            }
        }
    }
}
