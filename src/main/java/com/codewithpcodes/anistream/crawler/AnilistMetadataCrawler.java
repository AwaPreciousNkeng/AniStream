package com.codewithpcodes.anistream.crawler;

import com.codewithpcodes.anistream.episode.EpisodeRepository;
import com.codewithpcodes.anistream.media.MediaRepository;
import com.codewithpcodes.anistream.transcode.TranscodeJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnilistMetadataCrawler {

    private final MediaRepository mediaRepository;
    private final EpisodeRepository episodeRepository;
    private final TranscodeJobRepository transcodeJobRepository;
    private final VideoDownloadService videoDownloadService;
}
