package john.api1.application.components.enums.boarding;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum BoardingType {
    DAYCARE("DAYCARE"),
    LONG_STAY("LONG_STAY");

    private final String boardingType;

    public static BoardingType fromStringOrDefault(String dbValue) {
        return Arrays.stream(BoardingType.values())
                .filter(bt -> bt.boardingType.equalsIgnoreCase(dbValue))
                .findFirst()
                .orElse(BoardingType.DAYCARE); // Default to DAYCARE if unknown
    }

    // High usage in Controller?
    public static BoardingType fromStringOrError(String dbValue) {
        return Arrays.stream(BoardingType.values())
                .filter(bt -> bt.boardingType.equalsIgnoreCase(dbValue))
                .findFirst()
                .orElseThrow(()-> new IllegalArgumentException ("Boarding type request value '" + dbValue + "' cannot be converted to enum"));
    }
}
