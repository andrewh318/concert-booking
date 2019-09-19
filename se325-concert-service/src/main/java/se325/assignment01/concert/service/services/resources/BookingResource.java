package se325.assignment01.concert.service.services.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.config.Config;
import se325.assignment01.concert.common.dto.BookingDTO;
import se325.assignment01.concert.common.dto.BookingRequestDTO;
import se325.assignment01.concert.service.domain.Booking;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.domain.Seat;
import se325.assignment01.concert.service.domain.User;
import se325.assignment01.concert.service.mapper.BookingMapper;
import se325.assignment01.concert.service.services.PersistenceManager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        // authenticate user
        String authToken = cookie.getValue();

        GenericEntity<List<BookingDTO>> entity;
        // search this auth token to see if there is an associated user
        try {
            em.getTransaction().begin();
            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.authToken = :authToken", User.class);
            userQuery.setParameter("authToken", authToken);

            User user = userQuery.getResultList().stream().findFirst().orElse(null);

            if (user == null) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            long userId = user.getId();

            // get all bookings associated with user
            TypedQuery<Booking> bookingQuery = em.createQuery("select b from Booking b where b.user.id = :userId", Booking.class);
            bookingQuery.setParameter("userId", userId);

            List<BookingDTO> bookingRequests = bookingQuery.getResultList().stream().map(b -> {
                return BookingMapper.toDto(b);
            }).collect(Collectors.toList());

            entity = new GenericEntity<List<BookingDTO>>(bookingRequests){};

            em.getTransaction().commit();
        } finally {
            em.close();
        }


        return Response.ok(entity).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBooking(@CookieParam(Config.AUTH_COOKIE) Cookie cookie, @PathParam("id") long id) {
        if (cookie == null) {
            LOGGER.info("no cookie for get booking");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        LOGGER.info("Retrieving booking with id " + id);
        BookingDTO dtoBooking;
        try {
            em.getTransaction().begin();

            String authToken = cookie.getValue();
            // get user making request
            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.authToken = :authToken", User.class);
            userQuery.setParameter("authToken", authToken);

            User user = userQuery.getResultList().stream().findFirst().orElse(null);

            if (user == null) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            Booking booking = em.find(Booking.class, id);

            if (booking == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // check if user owns the booking
            long expectedUserId = booking.getUser().getId();

            if (user.getId() != expectedUserId) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            
            em.getTransaction().commit();

            dtoBooking = BookingMapper.toDto(booking);
        } finally {
            em.close();
        }

        return Response.ok(dtoBooking).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBooking(@CookieParam(Config.AUTH_COOKIE) Cookie cookie, BookingRequestDTO bookingRequestDTO) {
        Booking booking;
        try {
            em.getTransaction().begin();

            if (cookie == null) {
                LOGGER.info("Unauthorized request");
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            String authToken = cookie.getValue();

            // find user making the booking
            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.authToken = :authToken", User.class);
            userQuery.setParameter("authToken", authToken);

            User user = userQuery.getResultList().stream().findFirst().orElse(null);

            if (user == null) {
                return Response.status(Response.Status.FORBIDDEN).build();
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

            List<Seat> bookedSeats = new ArrayList<>();
            
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
                bookedSeats.add(seat);
                
                em.merge(seat);
            }

            // create new booking object
            booking = new Booking(
                    concert.getId(),
                    targetDate,
                    bookedSeats
            );

            booking.setUser(user);

            em.persist(booking);
            em.getTransaction().commit();
        } finally {
            em.close();
        }

        LOGGER.debug("new booking id is " + booking.getId());

        Response response = Response.created(URI.create("/concert-service/bookings/" + booking.getId())).build();
        return response;
    }
}
