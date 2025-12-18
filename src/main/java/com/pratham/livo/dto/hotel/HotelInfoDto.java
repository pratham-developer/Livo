package com.pratham.livo.dto.hotel;

import com.pratham.livo.dto.room.RoomResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HotelInfoDto {
    HotelResponseDto hotel;
    List<RoomResponseDto> rooms;
}
