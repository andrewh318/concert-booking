package se325.assignment01.concert.service.services.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.SeatDTO;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.jaxrs.LocalDateTimeParam;
import se325.assignment01.concert.service.services.PersistenceManager;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.List;

@Path("/concert-service/seats")
public class SeatResource {
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private EntityManager em = PersistenceManager.instance().createEntityManager();

    @GET
    @Path("{time}")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<SeatDTO> getSeats(@PathParam("time") LocalDateTimeParam dateParam, @QueryParam("status") BookingStatus status) {
        LocalDateTime localDateTime = dateParam.getLocalDateTime();
        return null;
    }

}
