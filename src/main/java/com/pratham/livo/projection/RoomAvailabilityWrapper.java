package com.pratham.livo.projection;

import com.pratham.livo.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomAvailabilityWrapper {
    private Room room;
    private Boolean isAvailable;
}
