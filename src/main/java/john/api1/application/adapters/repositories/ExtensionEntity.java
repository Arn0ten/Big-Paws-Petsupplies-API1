package john.api1.application.adapters.repositories;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "boarding_extension")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ExtensionEntity {
    @Id
    private ObjectId id;
    private ObjectId requestId;
    private ObjectId boardingId;
    private double additionalPrice;
    private long extendedHours;
    @Nullable
    private String description;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    private boolean approved;

    public static ExtensionEntity create(ObjectId requestId, ObjectId boardingId, double additionalPrice, long extendedHours, String description, Instant createdAt, Instant updatedAt, boolean approved) {
        return new ExtensionEntity(null, requestId, boardingId, additionalPrice, extendedHours, description, createdAt, updatedAt, approved);
    }
}
