package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.BookingDTO;
import se325.assignment01.concert.common.dto.BookingRequestDTO;
import se325.assignment01.concert.common.dto.SeatDTO;
import se325.assignment01.concert.service.domain.Booking;
import se325.assignment01.concert.service.domain.Seat;

import java.util.List;
import java.util.stream.Collectors;

public class BookingMapper {
    public static Booking toDomainModel(BookingDTO dtoBooking) {
        List<Seat> seats = dtoBooking.getSeats().stream().map(s -> SeatMapper.toDomainModel(s)).collect(Collectors.toList());
        Booking fullBooking = new Booking(
                dtoBooking.getConcertId(),
                dtoBooking.getDate(),
                seats
        );

        return fullBooking;
    }

    public static BookingDTO toDto(Booking booking) {
        List<SeatDTO> seats = booking.getSeats().stream().map(s -> SeatMapper.toDto(s)).collect(Collectors.toList());
        BookingDTO dtoBooking = new BookingDTO(
                booking.getConcertId(),
                booking.getDate(),
                seats
        );

        return dtoBooking;
    }
}
