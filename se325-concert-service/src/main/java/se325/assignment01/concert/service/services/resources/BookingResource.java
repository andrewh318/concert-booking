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
//TODO see if theres a better way to handle getting all the seat objects associated with seat labels 
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

    @POST
    @Path("/subscribe/concertInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    // produces annotation necessary here even though 'processSubscriptionHook' is calling resume
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

    //TODO make this actually be wrapped inside a new transaction
    // run this code after transaction closed for createBooking
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

            int numAvailableSeats = seatQuery.getResultList().size();

            List<Subscription> updatedSubscriptions = new ArrayList<>();

            for (Subscription subscription : subscriptions) {
                LocalDateTime date = subscription.getInfo().getDate();

                if (date.isEqual(targetDate)) {
                    if (isThresholdExceeded(subscription.getInfo().getPercentageBooked(), numAvailableSeats, THEATRE_CAPACITY)) {
                        AsyncResponse response = subscription.getResponse();
                        response.resume(new ConcertInfoNotificationDTO(numAvailableSeats));
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

    private boolean isThresholdExceeded(int threshold, int numAvailable, int totalCapacity) {
        boolean isExceeded = threshold < ((double) (totalCapacity - numAvailable) / totalCapacity) * 100;
        return isExceeded;
    }

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


