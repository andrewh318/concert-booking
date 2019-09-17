package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.BookingRequestDTO;
import se325.assignment01.concert.service.domain.BookingRequest;

public class BookingRequestMapper {
    public static BookingRequest toDomainModel(BookingRequestDTO dtoBookingRequest) {
        BookingRequest fullBookingRequest = new BookingRequest(
                dtoBookingRequest.getConcertId(),
                dtoBookingRequest.getDate(),
                dtoBookingRequest.getSeatLabels()
        );

        return fullBookingRequest;
    }

    public static BookingRequestDTO toDto(BookingRequest bookingRequest) {
        BookingRequestDTO dtoBookingRequest = new BookingRequestDTO(
                bookingRequest.getConcertId(),
                bookingRequest.getDate(),
                bookingRequest.getSeatLabels()
        );

        return dtoBookingRequest;   
    }
}
