package se325.assignment01.concert.service.services.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.config.Config;
import se325.assignment01.concert.common.dto.BookingRequestDTO;
import se325.assignment01.concert.service.domain.BookingRequest;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.domain.Seat;
import se325.assignment01.concert.service.services.PersistenceManager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.print.attribute.standard.Media;
import javax.ws.rs.*;
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllBookings(@CookieParam(Config.AUTH_COOKIE) Cookie cookie) {
        if (cookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return null;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBooking(@CookieParam(Config.AUTH_COOKIE) Cookie cookie, @PathParam("id") long id) {
        if (cookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBooking(@CookieParam(Config.AUTH_COOKIE) Cookie cookie, BookingRequestDTO bookingRequestDTO) {
        BookingRequest bookingRequest;
        try {
            em.getTransaction().begin();

            if (cookie == null) {
                LOGGER.info("Unauthorized request");
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Long targetConcertId = bookingRequestDTO.getConcertId();
            LocalDateTime targetDate = bookingRequestDTO.getDate();

            // check if concert exists
            Concert concert = em.find(Concert.class, targetConcertId);

            if (concert == null) {
                LOGGER.info("concert is null");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            if (!concert.getDates().contains(targetDate)) {
                LOGGER.info("no valid concert");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            List<String> seatLabels = bookingRequestDTO.getSeatLabels();

            // currently doing multiple database calls
            // alternative: https://thoughts-on-java.org/fetch-multiple-entities-id-hibernate/
            boolean allAvailable = true;

            for (String seatLabel : seatLabels) {
                TypedQuery<Seat> seatQuery = em.createQuery(
                        "select s from Seat s " +
                                "where s.date = :targetDate " +
                                "and s.label = :seatLabel ",
                        Seat.class
                );
                seatQuery.setParameter("targetDate", targetDate);
                seatQuery.setParameter("seatLabel", seatLabel);

                Seat seat = seatQuery.getResultList().stream().findFirst().orElse(null);

                // TODO merge into one method
                if (seat == null) {
                    LOGGER.info("Unavailable seat is " + seatLabel);
                    allAvailable = false;
                    break;
                }

                if (seat.isBooked() == true) {
                    LOGGER.info("Seat is booked " + seatLabel);
                    allAvailable = false;
                    break;
                }
            }

            // return error message if not all seats are available
            if (!allAvailable) {
                LOGGER.info("All seats not available");
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            // mark all seats as booked
            for (String seatLabel : seatLabels) {
                Seat seat;
                // TODO refactor this out into a function helper
                TypedQuery<Seat> seatQuery = em.createQuery(
                        "select s from Seat s " +
                                "where s.date = :targetDate " +
                                "and s.label = :seatLabel ",
                        Seat.class
                );
                seatQuery.setParameter("targetDate", targetDate);
                seatQuery.setParameter("seatLabel", seatLabel);

                seat = seatQuery.getResultList().stream().findFirst().orElse(null);
                seat.setBooked(true);
                em.merge(seat);
            }

            // create new booking object
            bookingRequest = new BookingRequest(
                    concert.getId(),
                    targetDate,
                    seatLabels
            );

            em.persist(bookingRequest);
            em.getTransaction().commit();
        } finally {
            em.close();
        }

        LOGGER.debug("new booking request id is " + bookingRequest.getId());

        Response response = Response.created(URI.create("/bookings/" + bookingRequest.getId())).build();
        return response;
    }
}
