package john.api1.application.services.pet;

import john.api1.application.components.DomainResponse;
import john.api1.application.components.enums.BucketType;
import john.api1.application.domain.models.MediaDomain;
import john.api1.application.dto.mapper.ProfileDTO;
import john.api1.application.ports.services.media.IMediaManagement;
import john.api1.application.ports.services.pet.IPetProfilePhoto;
import john.api1.application.ports.services.pet.IPetUpdate;
import org.springframework.stereotype.Service;

@Service
public class PetProfilePhotoAS implements IPetProfilePhoto {
    private final IPetUpdate petUpdate;
    private final IMediaManagement mediaManagement;

    public PetProfilePhotoAS(IPetUpdate petUpdate, IMediaManagement mediaManagement) {
        this.petUpdate = petUpdate;
        this.mediaManagement = mediaManagement;
    }

    @Override
    public DomainResponse<ProfileDTO> processProfilePhoto(String id, String petName) {

        // Step 1: Create pre-signed url
        String objectName = mediaManagement.generateMediaObjectName(petName, id);
        var generateMedia = mediaManagement.generateMediaFile(id, objectName, BucketType.PROFILE_PHOTO);
        if (!generateMedia.isSuccess()) {
            return DomainResponse.error(generateMedia.getMessage(), DomainResponse.ErrorType.SERVER_ERROR);
        }

        // Step 2: Create media domain object and parse pre-signed url
        MediaDomain mediaDomain = MediaDomain.create(
                id,
                objectName,
                BucketType.PROFILE_PHOTO,
                generateMedia.getData().expiresAt()
        );

        // Step 3: Save domain object, update pet db. Check if successful or not
        var saveMediaResponse = mediaManagement.saveMediaFile(mediaDomain);
        if (!saveMediaResponse.isSuccess()) {
            return DomainResponse.error(saveMediaResponse.getMessage(), DomainResponse.ErrorType.SERVER_ERROR);
        }

        var updatePetResponse = petUpdate.updatePetProfilePicture(id, objectName);
        if (!updatePetResponse.isSuccess()) {
            return DomainResponse.error(updatePetResponse.getMessage(), DomainResponse.ErrorType.SERVER_ERROR);
        }

        // Step 4: Return Response
        return DomainResponse.success(
                new ProfileDTO(
                        saveMediaResponse.getData().id(),
                        generateMedia.getData().preSignedUrl(),
                        saveMediaResponse.getData().expiredAt()
                )
        );
    }

}
