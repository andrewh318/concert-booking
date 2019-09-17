package se325.assignment01.concert.service.services.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.config.Config;
import se325.assignment01.concert.common.dto.BookingRequestDTO;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.domain.Seat;
import se325.assignment01.concert.service.services.PersistenceManager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Path("/concert-service/bookings")
public class BookingResource {
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private EntityManager em = PersistenceManager.instance().createEntityManager();


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBooking(@CookieParam(Config.AUTH_COOKIE) Cookie cookie, BookingRequestDTO bookingRequestDTO) {
        // check authorization
        if (cookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Long targetConcertId = bookingRequestDTO.getConcertId();
        LocalDateTime targetDate = bookingRequestDTO.getDate();

        // check if concert exists
        Concert concert;

        try {
            em.getTransaction().begin();
            concert = em.find(Concert.class, targetConcertId);
            em.getTransaction().commit();

            if (concert == null) {
                LOGGER.info("concert is null");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            if (!concert.getDates().contains(targetDate)) {
                LOGGER.info("no valid concert");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        } finally {
            em.close();
        }

        List<String> seatLabels = bookingRequestDTO.getSeatLabels();

        // currently doing multiple database calls
        // alternative: https://thoughts-on-java.org/fetch-multiple-entities-id-hibernate/
        boolean allAvailable = true;
        for (String seatLabel : seatLabels) {
            // check if seat is booked
            Seat seat;
            try {
                em.getTransaction().begin();
                TypedQuery<Seat> seatQuery = em.createQuery(
                        "select s from Seat s " +
                                "where s.concertId = :targetConcertId " +
                                "and s.date = :targetDate " +
                                "and s.label = :seatLabel " +
                                "and s.isBooked = :targetIsBooked",
                        Seat.class
                );
                seatQuery.setParameter("targetConcertId", targetConcertId);
                seatQuery.setParameter("targetDate", targetDate);
                seatQuery.setParameter("seatLabel", seatLabel);
                seatQuery.setParameter("targetIsBooked", false);

                seat = seatQuery.getResultList().stream().findFirst().orElse(null);

                // temp error message- seat shouldn't ever be null as DB will be initiated properly
                if (seat == null) {
                    allAvailable = false;
                    break;
                }

                em.getTransaction().commit();
            }  finally {
                em.close();
            }
        }

        // return error message if not all seats are available
        if (!allAvailable) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // mark all seats as booked
        for (String seatLabel : seatLabels) {
            Seat seat;
            try {
                em.getTransaction().begin();
                TypedQuery<Seat> seatQuery = em.createQuery(
                        "select s from Seat s " +
                                "where s.concertId = :targetConcertId " +
                                "and s.date = :targetDate " +
                                "and s.label = :seatLabel " +
                                "and s.isBooked = :targetIsBooked",
                        Seat.class
                );
                seatQuery.setParameter("targetConcertId", targetConcertId);
                seatQuery.setParameter("targetDate", targetDate);
                seatQuery.setParameter("seatLabel", seatLabel);
                seatQuery.setParameter("targetIsBooked", false);

                seat = seatQuery.getResultList().stream().findFirst().orElse(null);

                seat.setBooked(true);
                em.merge(seat);

                em.getTransaction().commit();
            }  finally {
                em.close();
            }
        }

        Response response = Response.created(URI.create("/bookings/" + 1)).build();
        return response;
    }
}
