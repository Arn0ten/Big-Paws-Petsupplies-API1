package john.api1.application.services.media;

import john.api1.application.adapters.repositories.media.MinioCreateUpdateRepositoryMongo;
import john.api1.application.adapters.services.MinioAdapter;
import john.api1.application.components.DomainResponse;
import john.api1.application.components.enums.BucketType;
import john.api1.application.domain.models.MediaDomain;
import john.api1.application.ports.repositories.wrapper.MediaPreview;
import john.api1.application.ports.repositories.wrapper.PreSignedUrlResponse;
import john.api1.application.ports.services.media.IMediaManagement;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class MediaManagementAS implements IMediaManagement {
    private final MinioAdapter minioAdapter;
    private final MinioCreateUpdateRepositoryMongo mediaRepository;

    @Autowired
    public MediaManagementAS(MinioAdapter minioAdapter, MinioCreateUpdateRepositoryMongo mediaRepository) {
        this.minioAdapter = minioAdapter;
        this.mediaRepository = mediaRepository;
    }


    public String generateMediaObjectName(String name, String id) {
        return name + ":" + id;
    }

    public DomainResponse<PreSignedUrlResponse> generateMediaFile(String ownerId, String fileName, BucketType bucketType) {
        if (!ObjectId.isValid(ownerId)) return DomainResponse.error("Invalid ownerId");

        try {
            String newUrl = minioAdapter.getUploadUrl(bucketType, fileName);
            Instant expirationTime = Instant.now().plus(bucketType.getMinuteExpire(), ChronoUnit.MINUTES);
            return DomainResponse.success(new PreSignedUrlResponse(newUrl, expirationTime));
        } catch (RuntimeException e) {
            return DomainResponse.error(e.getMessage());
        }
    }

    public DomainResponse<MediaPreview> saveMediaFile(MediaDomain media) {
        if (!ObjectId.isValid(media.ownerId()) ||
                media.typeId() != null && !ObjectId.isValid(media.typeId())) {    // if not null, check if valid
            return DomainResponse.error("Invalid ownerId or typeId.");
        }

        try {
            var savedMedia = mediaRepository.save(media);
            if (savedMedia.isPresent()) {
                MediaDomain mediaDomain = savedMedia.get();
                return DomainResponse.success(
                        new MediaPreview(
                                mediaDomain.id(),
                                mediaDomain.description(),
                                mediaDomain.bucketType(),
                                mediaDomain.fileName(),
                                mediaDomain.preSignedUrlExpire()
                        )
                );
            }
            return DomainResponse.error("Failed to save media to database.");
        } catch (RuntimeException e) {
            return DomainResponse.error("Database error: " + e.getMessage());
        }
    }

    public DomainResponse<MediaPreview> updatePreSignedUrlExpire(String mediaId, String ownerId) {
        return DomainResponse.error("");
    }


    public DomainResponse<MediaPreview> updateTypeId(String mediaId, String ownerId, ObjectId newTypeId) {
        return DomainResponse.error("");
    }

    public DomainResponse<String> archiveMedia(String mediaId, String ownerId) {
        return DomainResponse.error("");
    }

    public DomainResponse<String> deleteMedia(String mediaId, String ownerId) {
        return DomainResponse.error("");
    }
}
