package parser.data;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.LocalTime;

public interface PositionWithTime {

    double getLongitude();

    double getLatitude();

    double getAltitude();

    LocalTime getTime();

    @Nullable
    LocalDateTime getDateTime();
}
