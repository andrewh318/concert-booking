package se325.assignment01.concert.service.services.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.config.Config;
import se325.assignment01.concert.common.dto.BookingDTO;
import se325.assignment01.concert.common.dto.BookingRequestDTO;
import se325.assignment01.concert.common.dto.ConcertInfoNotificationDTO;
import se325.assignment01.concert.common.dto.ConcertInfoSubscriptionDTO;
import se325.assignment01.concert.service.domain.Booking;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.domain.Seat;
import se325.assignment01.concert.service.domain.User;
import se325.assignment01.concert.service.mapper.BookingMapper;
import se325.assignment01.concert.service.services.PersistenceManager;

import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Path("/concert-service")
public class BookingResource {
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    public static final int THEATRE_CAPACITY = 120;
    private PersistenceManager persistenceManager;

    // initialize map here rather than in constructor otherwise the collection will be overwritten
    // each time a resource is created
    private static Map<Long, List<Subscription>> activeConcertSubscriptions = new ConcurrentHashMap<>();

    public BookingResource() {
         this.persistenceManager = PersistenceManager.instance();
    }

    /**
     * Retrieves all the bookings associated with a given user
     * @param cookie Cookie identifying a specific user in the system
     * @return JSON serialized array of BookingDTO objects
     */
    @GET
    @Path("/bookings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllBookings(@CookieParam(Config.AUTH_COOKIE) Cookie cookie) {
        if (cookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        LOGGER.info("Getting all bookings");

        GenericEntity<List<BookingDTO>> entity;

        EntityManager em =  persistenceManager.createEntityManager();
        // search this auth token to see if there is an associated user
        try {
            em.getTransaction().begin();
            
            // authenticate user
            User user = this.getUserByAuthTokenIfExists(cookie);

            if (user == null) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            // get all bookings associated with user
            TypedQuery<Booking> bookingQuery = em.createQuery("select b from Booking b where b.user.id = :userId", Booking.class)
                .setParameter("userId", user.getId());

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

    /**
     * Retrieves a booking by id. Only returns the booking if it is owned by the retrieving user.
     * @param cookie Cookie identifying a specific user in the system
     * @param id Id of booking that is being retrieved 
     * @return
     */
    @GET
    @Path("/bookings/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBooking(@CookieParam(Config.AUTH_COOKIE) Cookie cookie, @PathParam("id") long id) {
        if (cookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        LOGGER.info("Retrieving booking with id " + id);
        BookingDTO dtoBooking;
        EntityManager em =  persistenceManager.createEntityManager();

        try {
            em.getTransaction().begin();

            // get user making request
            User user = this.getUserByAuthTokenIfExists(cookie);

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

    /**
     * Creates a new booking for a user in the system given a valid concert and if all seats are available.
     * @param cookie Cookie identifying a specific user in the system
     * @param bookingRequestDTO Object representing information associated with the booking
     * @return URI with the path of the new booking created if successful
     */
    @POST
    @Path("/bookings")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBooking(@CookieParam(Config.AUTH_COOKIE) Cookie cookie, BookingRequestDTO bookingRequestDTO) {
        Booking booking;
        EntityManager em =  persistenceManager.createEntityManager();

        LOGGER.info("Creating a booking for concert " + bookingRequestDTO.getConcertId());

        try {
            em.getTransaction().begin();

            if (cookie == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            // find user making the booking
            User user = this.getUserByAuthTokenIfExists(cookie);

            if (user == null) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            Long targetConcertId = bookingRequestDTO.getConcertId();
            LocalDateTime targetDate = bookingRequestDTO.getDate();

            // check if concert exists
            Concert concert = em.find(Concert.class, targetConcertId);

            if (concert == null || !concert.getDates().contains(targetDate)) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            booking = this.attemptToBookSeats(bookingRequestDTO, user);

            if (booking == null) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        } finally {
            em.close();
        }

        this.processSubscriptionHook(bookingRequestDTO.getConcertId(), bookingRequestDTO.getDate());

        Response response = Response.created(URI.create("/concert-service/bookings/" + booking.getId())).build();
        return response;
    }

    /**
     * Subscribes a user to a concert on a specific date- notifying them when the number of available seats falls below
     * a specified threshold. The response is asynchronous so all active subscriptions are temporarily stored in map until
     * the notification requirements are satisfied.
     * @param response
     * @param cookie Cookie identifying a specific user in the system
     * @param concertInfoSubscriptionDTO Object representing information associated with the subscription
     */
    @POST
    @Path("/subscribe/concertInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void subscribe(@Suspended AsyncResponse response, @CookieParam(Config.AUTH_COOKIE) Cookie cookie, ConcertInfoSubscriptionDTO concertInfoSubscriptionDTO) {
        EntityManager em =  persistenceManager.createEntityManager();

        try {
            em.getTransaction().begin();

            if (cookie == null) {
                response.resume(Response.status(Response.Status.UNAUTHORIZED).build());
            }

            User user = this.getUserByAuthTokenIfExists(cookie);

            if (user == null) {
                response.resume(Response.status(Response.Status.FORBIDDEN).build());
            }

            // check for valid concert
            long targetConcertId = concertInfoSubscriptionDTO.getConcertId();
            LocalDateTime targetDate = concertInfoSubscriptionDTO.getDate();

            Concert concert = em.find(Concert.class, targetConcertId);

            if (concert == null || !concert.getDates().contains(targetDate)) {
                response.resume(Response.status(Response.Status.BAD_REQUEST).build());
            }

            // subscribe the user
            List<Subscription> subscriptions = BookingResource.activeConcertSubscriptions.getOrDefault(targetConcertId, new ArrayList<>());
            subscriptions.add(new Subscription(concertInfoSubscriptionDTO, response));

            BookingResource.activeConcertSubscriptions.put(targetConcertId, subscriptions);
            
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /**
     * Hook method that executes every time a new booking is created. It checks all the subscribers for a given concert
     * and sends a response if the available seats for the concert falls below the given threshold.
     * @param concertId
     * @param targetDate
     */
    private void processSubscriptionHook(long concertId, LocalDateTime targetDate) {
        EntityManager em =  persistenceManager.createEntityManager();

        try {
            em.getTransaction().begin();
            List<Subscription> subscriptions = BookingResource.activeConcertSubscriptions.get(concertId);

            if (subscriptions == null) {
                return;
            }

            TypedQuery<Seat> seatQuery = em.createQuery("select s from Seat s where s.date=:targetDate and s.isBooked=:status", Seat.class)
                    .setParameter("targetDate", targetDate)
                    .setParameter("status", false);

            int seatsAvailable = seatQuery.getResultList().size();

            List<Subscription> updatedSubscriptions = new ArrayList<>();

            for (Subscription subscription : subscriptions) {
                LocalDateTime date = subscription.getInfo().getDate();
                int threshold = subscription.getInfo().getPercentageBooked();

                // make sure that updates are only sent to users subscribed to the specified concert date
                if (date.isEqual(targetDate)) {
                    if (isThresholdExceeded(threshold, seatsAvailable, THEATRE_CAPACITY)) {
                        AsyncResponse response = subscription.getResponse();
                        response.resume(new ConcertInfoNotificationDTO(seatsAvailable));
                    } else {
                        updatedSubscriptions.add(subscription);
                    }

                } else {
                    updatedSubscriptions.add(subscription);
                }
            }

            BookingResource.activeConcertSubscriptions.put(concertId, updatedSubscriptions);

            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    /**
     * Checks if the number of available seats has fallen below a given threshold.
     * @param threshold Represented as an integer between 0-100. E.g a threshold of 90 means that a notification should
     * be sent if the number of booked seats exceeds 90% (only 10% available)
     * @param seatsAvailable The number of available seats for the given concert
     * @param totalCapacity The total number of seats for the given theatre of the concert
     * @return
     */
    private boolean isThresholdExceeded(int threshold, int seatsAvailable, int totalCapacity) {
        boolean isExceeded = threshold < ((double) (totalCapacity - seatsAvailable) / totalCapacity) * 100;
        return isExceeded;
    }

    /**
     * Attempts to book a number of seats for a given concert for a given user. Booking is only valid if all seats are available
     * @param bookingRequestDTO
     * @param user
     * @return
     */
    private Booking attemptToBookSeats(BookingRequestDTO bookingRequestDTO, User user) {
        EntityManager em = this.persistenceManager.createEntityManager();
        Booking booking = null;

        try {
            em.getTransaction().begin();;
            List<String> seatLabels = bookingRequestDTO.getSeatLabels();

            // rather than making one query per seat label, we can take advantage of the 'in' command
            TypedQuery<Seat> seatQuery = em.createQuery("select s from Seat s where s.label in :seats and s.date = :targetDate and s.isBooked = :targetStatus", Seat.class)
                    .setParameter("seats", bookingRequestDTO.getSeatLabels())
                    .setParameter("targetDate", bookingRequestDTO.getDate())
                    .setParameter("targetStatus", false);

            seatQuery.setLockMode(LockModeType.OPTIMISTIC);

            List<Seat> seatsToBook = seatQuery.getResultList();

            // return error message if not all seats are available
            if (!(seatsToBook.size() == seatLabels.size())) {
                return null;
            }

            // mark all seats as booked
            for (Seat seat : seatsToBook) {
                seat.setBooked(true);
                em.merge(seat);
            }

            // create new booking object
            booking = new Booking(
                    bookingRequestDTO.getConcertId(),
                    bookingRequestDTO.getDate(),
                    seatsToBook
            );

            booking.setUser(user);

            em.persist(booking);
            em.getTransaction().commit();
        } catch (OptimisticLockException e) {
            em.close();
            this.attemptToBookSeats(bookingRequestDTO, user);
        } finally{
            em.close();
        }

        return booking;
    }

    /**
     * Helper method to extract the UUID from a cookie and checks if a valid user is associated with it
     * @param cookie
     * @return User if one exists, otherwise null
     */
    private User getUserByAuthTokenIfExists(Cookie cookie) {
        EntityManager em = this.persistenceManager.createEntityManager();
        User user;
        try {
            em.getTransaction().begin();

            String authToken = cookie.getValue();

            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.authToken = :authToken", User.class)
                    .setParameter("authToken", authToken);

            user = userQuery.getResultList().stream().findFirst().orElse(null);
        } finally {
            em.close();
        }

        return user;
    }
    
}

class Subscription {
    private final AsyncResponse response;
    private final ConcertInfoSubscriptionDTO info;

    public Subscription(ConcertInfoSubscriptionDTO info, AsyncResponse response) {
        this.info = info;
        this.response = response;
    }

    public AsyncResponse getResponse() {
        return this.response;
    }

    public ConcertInfoSubscriptionDTO getInfo() {
        return this.info;
    }

}


