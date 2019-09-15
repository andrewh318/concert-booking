package se325.assignment01.concert.service.services;

import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.jaxrs.LocalDateTimeParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.ws.Response;
import java.time.LocalDateTime;
import java.util.List;

// TODO Create separate resource class for concerts and performers. Separate services so each one can be scaled individually
@Path("/concert-service")
public class ConcertResource {
    @GET
    @Path("/concerts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ConcertDTO getConcert(@PathParam("id") long id) {
        return null;
    }

    @GET
    @Path("/concerts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ConcertDTO> getAllConcerts() {
        return null;
    }

    @GET
    @Path("/concerts/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ConcertSummaryDTO> getSummaries() {
        return null;
    }

    @GET
    @Path("/performers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public PerformerDTO getPerformer(@PathParam("id") long id) {
        return null;
    }

    @GET
    @Path("/performers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PerformerDTO> getAllPerformers() {
        return null;
    }

    @POST
    @Path("/bookings")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBooking(BookingRequestDTO bookingRequestDTO) {
        return null;
    }

    @GET
    @Path("/seats/{time}")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<SeatDTO> getSeats(@PathParam("time") LocalDateTimeParam dateParam, @QueryParam("status") BookingStatus status) {
        LocalDateTime localDateTime = dateParam.getLocalDateTime();
        return null;
    }

}
