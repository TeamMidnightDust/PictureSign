package eu.midnightdust.picturesign.media.platform;

import com.google.gson.Gson;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.platform.IPlatform;
import org.watermedia.tools.NetTool;

import java.io.InputStreamReader;
import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class PeerTubePlatform implements IPlatform {
    public static final String NAME = "PeerTube";
    private static final String API_PATH = "/api/v1/videos/";
    private static final Gson GSON = new Gson();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean validate(final URI uri) {
        if (uri.getPath().startsWith("/w/")) {
            try {
                final var connection = NetTool.connectToHTTP(new URI(uri.getScheme() + "://" + uri.getHost() + "/nodeinfo/2.0.json"), "GET", "application/json");
                NetTool.validateHTTP200(connection.getResponseCode(), uri);

                try (final var is = new InputStreamReader(connection.getInputStream())) {
                    final NodeInfo nodeInfo = GSON.fromJson(is, NodeInfo.class);
                    if ("peertube".equals(nodeInfo.software().name())) return true;
                } finally {
                    connection.disconnect();
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    public Result getSources(final URI uri) {
        final String videoId = uri.getPath().substring(3);

        try {
            final var connection = NetTool.connectToHTTP(new URI(uri.getScheme() + "://" + uri.getHost() + API_PATH + videoId), "GET", "application/json");
            NetTool.validateHTTP200(connection.getResponseCode(), uri);

            try (final var is = new InputStreamReader(connection.getInputStream())) {
                final PeerTubePlatform.VideoData video = GSON.fromJson(is, PeerTubePlatform.VideoData.class);

                Set<MRL.Source> builtSources = new HashSet<>();
                for (StreamingPlaylist playlist : video.streamingPlaylists) {
                    final var sourceBuilder = new MRL.SourceBuilder(MRL.MediaType.VIDEO);

                    var meta = new MRL.Metadata(video.name, videoId, new URI(uri.getHost()+video.thumbnailPath), Instant.parse(video.publishedAt), video.duration, "@%s@%s".formatted(video.account.name, video.account.host));
                    //System.out.println("Metadata: "+meta);
                    sourceBuilder.metadata(meta);

                    for (VideoFile videoFile : playlist.files) {
                        if (videoFile.width > 0)
                            sourceBuilder.quality(MRL.Quality.of(videoFile.width, videoFile.height), new URI(videoFile.playlistUrl));
                        else
                            sourceBuilder.audioSlave(new URI(videoFile.playlistUrl));
                    }
                    builtSources.add(sourceBuilder.build());
                }

                return new Result(null, builtSources.toArray(MRL.Source[]::new));
            } finally {
                connection.disconnect();
            }
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to get PeerTube video sources from " + uri, e);
        }
    }

    private record NodeInfo(Software software) {
    }

    private record Software(String name) {
    }

    private record VideoData(String name, long duration, String thumbnailPath, String publishedAt, Account account, StreamingPlaylist[] streamingPlaylists) {
    }

    private record Account(String name, String host) {
    }

    private record StreamingPlaylist(String id, int type, VideoFile[] files) {
    }

    public record VideoFile(String id, int width, int height, float fps, String fileUrl, String playlistUrl, int size) {
    }
}
