package se325.assignment01.concert.service.services.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.ConcertDTO;
import se325.assignment01.concert.common.dto.SeatDTO;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.domain.Seat;
import se325.assignment01.concert.service.jaxrs.LocalDateTimeParam;
import se325.assignment01.concert.service.mapper.SeatMapper;
import se325.assignment01.concert.service.services.PersistenceManager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Path("/concert-service/seats")
public class SeatResource {
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private EntityManager em = PersistenceManager.instance().createEntityManager();

    @GET
    @Path("{time}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSeats(@PathParam("time") LocalDateTimeParam dateParam, @QueryParam("status") BookingStatus status) {
        LocalDateTime localDateTime = dateParam.getLocalDateTime();
        GenericEntity<List<SeatDTO>> entity;
        try {
            em.getTransaction().begin();
            TypedQuery<Seat> seatQuery = em.createQuery(
                    "select s from Seat s where s.isBooked=:targetIsBooked and s.date=:targetDate",
                    Seat.class
            );

            boolean targetIsBooked = status == BookingStatus.Booked;

            seatQuery.setParameter("targetIsBooked", targetIsBooked);
            seatQuery.setParameter("targetDate", localDateTime);

            List<SeatDTO> seats = seatQuery.getResultList().stream().map(s -> {
                SeatDTO dtoSeat = SeatMapper.toDto(s);
                return dtoSeat;
            }).collect(Collectors.toList());

            LOGGER.info("number of seats is: " + seats.size());

            entity = new GenericEntity<List<SeatDTO>>(seats){};
            em.getTransaction().commit();
        } finally {
            em.close();
        }

        return Response.ok(entity).build();
    }

}
