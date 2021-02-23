package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class GroupItemLinkResolver {

    private static final Logger logger = LogManager.getLogger();

    private static final int THREADS_PER_TYPE = 3;
    private static final GroupItemResolver resolver = new GroupItemResolver();
    private static final Map<Pattern, LinkType> linkPatterns = new HashMap<>();
    static {
        final Pattern video1 = Pattern.compile("(?:http[s]?://)?(?:\\w+\\.)?youtube.com/watch\\?v=([\\w_-]+)(?:&.*)?");
        final Pattern video2 = Pattern.compile("(?:http[s]?://)?youtu.be/([\\w_-]+)(?:\\?.*)?");
        final Pattern playlist = Pattern.compile("(?:http[s]?://)?(?:\\w+\\.)?youtube.com/playlist\\?list=([\\w_-]+)(?:&.*)?");
        final Pattern channelUser = Pattern.compile("(?:http[s]?://)?(?:\\w+\\.)?youtube.com/user/([\\w_-]+)(?:\\?.*)?");
        final Pattern channelId = Pattern.compile("(?:http[s]?://)?(?:\\w+\\.)?youtube.com/channel/([\\w_-]+)(?:\\?.*)?");
        final Pattern channelCustom1 = Pattern.compile("(?:http[s]?://)?(?:\\w+\\.)?youtube.com/c/([\\w_-]+)(?:\\?.*)?");
        final Pattern channelCustom2 = Pattern.compile("(?:http[s]?://)?(?:\\w+\\.)?youtube.com/([\\w_-]+)(?:\\?.*)?");

        linkPatterns.put(video1, LinkType.VIDEO);
        linkPatterns.put(video2, LinkType.VIDEO);
        linkPatterns.put(playlist, LinkType.PLAYLIST);
        linkPatterns.put(channelUser, LinkType.CHANNEL_USER);
        linkPatterns.put(channelId, LinkType.CHANNEL_ID);
        linkPatterns.put(channelCustom1, LinkType.CHANNEL_CUSTOM);
        linkPatterns.put(channelCustom2, LinkType.CHANNEL_CUSTOM);
    }

    private final CommentDatabase database;
    private final YouTube youTube;

    public GroupItemLinkResolver() {
        this.database = CommentSuite.getDatabase();
        this.youTube = CommentSuite.getYouTube();
    }

    public Optional<GroupItem> from(final String link) {
        return parseAndAwait(Collections.singleton(link)).stream().findFirst();
    }

    public List<GroupItem> from(final Collection<String> links) {
        return parseAndAwait(links);
    }

    private List<GroupItem> parseAndAwait(final Collection<String> links) {
        if (links == null || links.isEmpty()) {
            return Collections.emptyList();
        }

        final Map<String, LinkType> matches = new HashMap<>();
        for (final String link : links.stream().distinct().collect(Collectors.toList())) {
            if (isBlank(link)) {
                continue;
            }

            for (final Pattern pattern : linkPatterns.keySet()) {
                final Matcher matcher = pattern.matcher(link);
                if (matcher.matches()) {
                    matches.put(matcher.group(1), linkPatterns.get(pattern));
                }
            }
        }

        if (matches.isEmpty()) {
            return Collections.emptyList();
        }

        final List<GroupItem> results = new ArrayList<>();
        final ExecutorGroup videoExecutor = parseVideos(matches, results);
        final ExecutorGroup playlistsExecutor = parsePlaylists(matches, results);
        final ExecutorGroup channelIdsExecutor = parseChannelIds(matches, results);
        final ExecutorGroup channelUsersExecutor = parseChannelUsers(matches, results);
        final ExecutorGroup channelCustomsExecutor = parseChannelCustoms(matches, results);

        try {
            awaitAll(videoExecutor, playlistsExecutor, channelIdsExecutor, channelUsersExecutor, channelCustomsExecutor);
        } catch (InterruptedException e) {
            logger.error(e);
        }

        logger.debug(results);

        return results;
    }

    private void awaitAll(final ExecutorGroup... executors) throws InterruptedException {
        for (ExecutorGroup executor : executors) {
            if (executor.isStillWorking()) {
                executor.await();
            }
        }
    }

    private LinkedBlockingQueue<String> toQueue(final Map<String, LinkType> matches, final LinkType type) {
        return matches.entrySet().stream()
                .filter(entry -> entry.getValue() == type)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedBlockingQueue::new));
    }

    private ExecutorGroup parseVideos(final Map<String, LinkType> matches, final List<GroupItem> results) {
        final LinkedBlockingQueue<String> ids = toQueue(matches, LinkType.VIDEO);
        final ExecutorGroup executors = new ExecutorGroup(THREADS_PER_TYPE);
        executors.submitAndShutdown(() -> {
            final List<String> toCheck = new ArrayList<>();
            while (!ids.isEmpty()) {
                final String id = ids.poll();
                if (id == null) {
                    continue;
                } else {
                    toCheck.add(id);
                }

                if (toCheck.size() == 50) {
                    checkVideoIds(toCheck, results);
                    toCheck.clear();
                }
            }

            if (!toCheck.isEmpty()) {
                checkVideoIds(toCheck, results);
                toCheck.clear();
            }
        });
        return executors;
    }

    private void checkVideoIds(final List<String> ids, final List<GroupItem> results) {
        try {
            logger.debug("checkVideoIds({})", ids);

            final VideoListResponse videos = youTube.videos()
                    .list("snippet")
                    .setKey(CommentSuite.getYouTubeApiKey())
                    .setId(String.join(",", ids))
                    .setMaxResults(50L)
                    .execute();

            Optional.ofNullable(videos)
                    .map(response -> response.getItems().stream())
                    .orElseGet(Stream::empty)
                    .map(resolver::from)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(results::add);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private ExecutorGroup parsePlaylists(final Map<String, LinkType> matches, final List<GroupItem> results) {
        final LinkedBlockingQueue<String> ids = toQueue(matches, LinkType.PLAYLIST);
        final ExecutorGroup executors = new ExecutorGroup(THREADS_PER_TYPE);
        executors.submitAndShutdown(() -> {
            final List<String> toCheck = new ArrayList<>();
            while (!ids.isEmpty()) {
                final String id = ids.poll();
                if (id == null) {
                    continue;
                } else {
                    toCheck.add(id);
                }

                if (toCheck.size() == 50) {
                    checkPlaylistIds(toCheck, results);
                    toCheck.clear();
                }
            }

            if (!toCheck.isEmpty()) {
                checkPlaylistIds(toCheck, results);
                toCheck.clear();
            }
        });
        return executors;
    }

    private void checkPlaylistIds(final List<String> ids, final List<GroupItem> results) {
        try {
            logger.debug("checkPlaylistIds({})", ids);

            final PlaylistListResponse playlists = youTube.playlists()
                    .list("snippet")
                    .setKey(CommentSuite.getYouTubeApiKey())
                    .setId(String.join(",", ids))
                    .setMaxResults(50L)
                    .execute();

            Optional.ofNullable(playlists)
                    .map(response -> response.getItems().stream())
                    .orElseGet(Stream::empty)
                    .map(resolver::from)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(results::add);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private ExecutorGroup parseChannelIds(final Map<String, LinkType> matches, final List<GroupItem> results) {
        final LinkedBlockingQueue<String> ids = toQueue(matches, LinkType.CHANNEL_ID);
        final ExecutorGroup executors = new ExecutorGroup(THREADS_PER_TYPE);
        executors.submitAndShutdown(() -> {
            final List<String> toCheck = new ArrayList<>();
            while (!ids.isEmpty()) {
                final String id = ids.poll();
                if (id == null) {
                    continue;
                } else {
                    toCheck.add(id);
                }

                if (toCheck.size() == 50) {
                    checkChannelIds(toCheck, results);
                    toCheck.clear();
                }
            }

            if (!toCheck.isEmpty()) {
                checkChannelIds(toCheck, results);
                toCheck.clear();
            }
        });
        return executors;
    }

    private void checkChannelIds(final List<String> ids, final List<GroupItem> results) {
        try {
            logger.debug("checkChannelIds({})", ids);

            final ChannelListResponse channels = youTube.channels()
                    .list("snippet")
                    .setKey(CommentSuite.getYouTubeApiKey())
                    .setId(String.join(",", ids))
                    .setMaxResults(50L)
                    .execute();

            checkChannelResponse(channels, results);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private ExecutorGroup parseChannelUsers(final Map<String, LinkType> matches, final List<GroupItem> results) {
        final LinkedBlockingQueue<String> ids = toQueue(matches, LinkType.CHANNEL_USER);
        final ExecutorGroup executors = new ExecutorGroup(THREADS_PER_TYPE);
        executors.submitAndShutdown(() -> {
            while (!ids.isEmpty()) {
                final String id = ids.poll();
                if (id == null) {
                    continue;
                }

                checkChannelUser(id, results);
            }
        });
        return executors;
    }

    private void checkChannelUser(final String id, final List<GroupItem> results) {
        try {
            logger.debug("checkChannelUser({})", id);

            final ChannelListResponse channels = youTube.channels()
                    .list("snippet")
                    .setKey(CommentSuite.getYouTubeApiKey())
                    .setForUsername(id)
                    .execute();

            checkChannelResponse(channels, results);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private ExecutorGroup parseChannelCustoms(final Map<String, LinkType> matches, final List<GroupItem> results) {
        final LinkedBlockingQueue<String> ids = toQueue(matches, LinkType.CHANNEL_CUSTOM);
        final ExecutorGroup executors = new ExecutorGroup(THREADS_PER_TYPE);
        executors.submitAndShutdown(() -> {
            while (!ids.isEmpty()) {
                final String id = ids.poll();
                if (id == null) {
                    continue;
                }

                checkChannelCustom(id, results);
            }
        });
        return executors;
    }

    /**
     * Cannot directly query by customUrl so have to workaround by checking channels returned from
     * search.list. However, that extra detail can only be found from channels.list not search.list.
     */
    private void checkChannelCustom(final String id, final List<GroupItem> results) {
        try {
            logger.debug("checkChannelCustom({})", id);

            final SearchListResponse search = youTube.search()
                    .list("snippet")
                    .setKey(CommentSuite.getYouTubeApiKey())
                    .setQ(id)
                    .setType("channel")
                    .setMaxResults(50L)
                    .execute();

            final List<String> channelIds = Optional.ofNullable(search)
                    .map(res -> res.getItems().stream())
                    .orElseGet(Stream::empty)
                    .map(SearchResult::getSnippet)
                    .map(SearchResultSnippet::getChannelId)
                    .distinct()
                    .collect(Collectors.toList());

            final ChannelListResponse channels = youTube.channels()
                    .list("snippet")
                    .setKey(CommentSuite.getYouTubeApiKey())
                    .setId(String.join(",", channelIds))
                    .setMaxResults(50L)
                    .execute();

            final Optional<GroupItem> match = Optional.ofNullable(channels)
                    .map(res -> res.getItems().stream())
                    .orElseGet(Stream::empty)
                    .filter(channel -> StringUtils.equalsIgnoreCase(id, channel.getSnippet().getCustomUrl()))
                    .findFirst()
                    .flatMap(resolver::from);

            match.ifPresent(results::add);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void checkChannelResponse(final ChannelListResponse response, final List<GroupItem> results) {
        Optional.ofNullable(response)
                .map(res -> res.getItems().stream())
                .orElseGet(Stream::empty)
                .map(resolver::from)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(results::add);
    }


}
