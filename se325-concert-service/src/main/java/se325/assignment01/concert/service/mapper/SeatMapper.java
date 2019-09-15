package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.SeatDTO;
import se325.assignment01.concert.service.domain.Seat;

import java.time.LocalDateTime;

public class SeatMapper {
    static Seat toDomainModel(SeatDTO dtoSeat) {
        Seat fullSeat = new Seat(
                dtoSeat.getLabel(),
                false,
                LocalDateTime.now(),
                dtoSeat.getPrice()
        );

        return fullSeat;
    }

    static SeatDTO toDto(Seat seat) {
        SeatDTO dtoSeat = new SeatDTO(
           seat.getLabel(),
           seat.getPrice()
        );

        return dtoSeat;
    }
}
