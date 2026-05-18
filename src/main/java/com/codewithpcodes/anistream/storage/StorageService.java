package com.codewithpcodes.anistream.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
@Slf4j

public class StorageService {

    private final S3Client s3Client;

    @Value("${application.backblaze.bucket.segments}")
    private String segmentsBucket;

    @Value("${application.backblaze.bucket.playlists}")
    private String playlistsBucket;

    @Value("${application.backblaze.bucket.thumbnails}")
    private String thumbnailsBucket;

    @Value("${application.backblaze.bucket.avatars}")
    private String avatarsBucket;

    @Value("${application.backblaze.bucket.raw}")
    private String rawBucket;

    public StorageService(
            @Value("${application.backblaze.endpoint}") String endpoint,
            @Value("${application.backblaze.access-key}") String accessKey,
            @Value("${application.backblaze.secret-key}") String secretKey
    ) {
        this.s3Client = s3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.of("auto"))
                .build();
    }
    // --- Upload Entire Transcoded Folder ------------
    // Uploads all .ts segments and .m3u8 playlists
    public void uploadTranscodedFolder(String localFolderPath, String s3Prefix) {
        try {
            Path folder = Paths.get(localFolderPath);
            try (Stream<Path> files = Files.walk(folder)) {
                files.filter(Files::isRegularFile)
                        .forEach(file -> {
                            String key = s3Prefix + "/" + folder.relativize(file).toString();
                            uploadFile(
                                    file,
                                    key.endsWith(".m3u8")
                                            ? playlistsBucket
                                            : segmentsBucket,
                                    key
                            );
                        });
            }
            log.info("Uploaded transcoded folder: {}", localFolderPath);
        } catch (Exception e) {
            log.error("Upload failed for folder {}: {}", localFolderPath, e.getMessage());
            throw new RuntimeException("Upload failed", e);
        }
    }

    public String uploadFile(Path filePath, String bucket, String key) {
        try {
            String contentType = resolveContentType(filePath.toString());
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromFile(filePath)
            );
            String url = buildPublicUrl(bucket, key);
            log.debug("Uploaded: {} -> {}", filePath, url);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload {}: {}",  filePath, e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    public void deleteFile(String bucket, String key) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
            log.info("Deleted: {}/{}",  bucket, key);
        } catch (Exception e) {
            log.error("Failed to delete {}/{}: {}",  bucket, key, e.getMessage());
        }
    }

    public void deleteRawFile(String key) {
        deleteFile(rawBucket, key);
    }

    public String buildPublicUrl(String bucket, String key) {
        return "https://f004.backblazeb2.com/file/" + bucket + "/" + key;
    }

    private String resolveContentType(String fileName) {
        if (fileName.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (fileName.endsWith(".ts")) return "video/mp2t";
        if (fileName.endsWith(".mp4")) return "video/mp4";
        if (fileName.endsWith(".jpg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
