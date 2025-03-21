package john.api1.application.ports.repositories.media;

import john.api1.application.components.enums.BucketType;
import john.api1.application.ports.repositories.records.MediaPreview;
import john.api1.application.ports.repositories.records.MediaUrlAndId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;


public interface IMediaSearchRepository {

    // 📌 Basic Query: Find a single media file by its unique ID.
    Optional<MediaPreview> findById(String id);

    // 📌 Archive Dashboard: Fetch all media for a specific owner within a date range (e.g., by week).
    List<MediaPreview> findByOwnerIdAndUploadedBetween(String ownerId, Instant start, Instant end);

    // 📌 General Fetch: Retrieve all media associated with a specific owner.
    List<MediaPreview> findByOwnerId(String ownerId);

    // 📌 Filter by Bucket: Get media of a certain type (e.g., PROFILE_PHOTO, PET_PHOTO) for a user within a date range.
    List<MediaPreview> findByOwnerIdAndBucketTypeAndUploadedBetween(String ownerId, BucketType bucketType, Instant start, Instant end);

    // 📌 Request-Based Filtering: Find media linked to a specific request (e.g., service request, booking request).
    List<MediaPreview> findByTypeId(String typeId);
}
