package se325.assignment01.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.config.Config;
import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.domain.Performer;
import se325.assignment01.concert.service.domain.Seat;
import se325.assignment01.concert.service.domain.User;
import se325.assignment01.concert.service.jaxrs.LocalDateTimeParam;
import se325.assignment01.concert.service.mapper.ConcertMapper;
import se325.assignment01.concert.service.mapper.PerformerMapper;
import sun.net.www.content.text.Generic;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// TODO Create separate resource class for concerts and performers. Separate services so each one can be scaled individually

// TODO Create better authentication. Should be saving the token to the user domain object. Verify upon receiving token that the user is who they say they are
@Path("/concert-service")
public class ConcertResource {
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private EntityManager em = PersistenceManager.instance().createEntityManager();
    
    @GET
    @Path("/concerts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConcert(@PathParam("id") long id) {
        LOGGER.info("Retrieving concert with id: " + id);
        Concert concert;
        ConcertDTO dtoConcert;
        try {
            em.getTransaction().begin();
            TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c where c.id = :targetId", Concert.class);
            concertQuery.setParameter("targetId", id);

            concert = concertQuery.getResultList().stream().findFirst().orElse(null);

            if (concert == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().commit();

            dtoConcert = ConcertMapper.toDto(concert);

            // get list of performer dtos
            List<PerformerDTO> dtoPerformers = concert.getPerformers().stream().map(
                    p -> PerformerMapper.toDto(p))
                    .collect(Collectors.toList());
            List<LocalDateTime> dtoDates = new ArrayList<>(concert.getDates());
            
            dtoConcert.setPerformers(dtoPerformers);
            dtoConcert.setDates(dtoDates);
            
            LOGGER.info("the dto concert is is: " + dtoConcert.getId());
        } finally {
            em.close();
        }

        LOGGER.info("the dto concert size is: " + dtoConcert.getPerformers().size());
        return Response.ok(dtoConcert).build();
    }

    @GET
    @Path("/concerts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllConcerts() {
        GenericEntity<List<ConcertDTO>> entity;
        try {
            em.getTransaction().begin();
            TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c", Concert.class);
            List<ConcertDTO> concerts = concertQuery.getResultList().stream().map(c -> ConcertMapper.toDto(c)).collect(Collectors.toList());

            entity = new GenericEntity<List<ConcertDTO>>(concerts){};
        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }

    @GET
    @Path("/concerts/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSummaries() {
        GenericEntity<List<ConcertSummaryDTO>> entity;
        try {
            em.getTransaction().begin();
            TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c", Concert.class);
            List<ConcertSummaryDTO> concertSummaries = concertQuery.getResultList().stream().map(c -> {
                ConcertSummaryDTO concertSummary = new ConcertSummaryDTO(c.getId(), c.getTitle(), c.getImageName());
                return concertSummary;
            }).collect(Collectors.toList());

            entity = new GenericEntity<List<ConcertSummaryDTO>>(concertSummaries){};
        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }

    @GET
    @Path("/performers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPerformer(@PathParam("id") long id) {
        PerformerDTO dtoPerformer;

        try {
            em.getTransaction().begin();
            Performer performer = em.find(Performer.class, id);

            if (performer == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            dtoPerformer = PerformerMapper.toDto(performer);
        } finally {
            em.close();
        }
        return Response.ok(dtoPerformer).build();
    }

    @GET
    @Path("/performers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPerformers() {
        GenericEntity<List<PerformerDTO>> entity;

        try {
            em.getTransaction().begin();
            TypedQuery<Performer> performerQuery = em.createQuery("select p from Performer p", Performer.class);
            List<PerformerDTO> performers = performerQuery.getResultList().stream().map(p -> PerformerMapper.toDto(p)).collect(Collectors.toList());

            entity = new GenericEntity<List<PerformerDTO>>(performers){};
        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }

    @POST
    @Path("/bookings")
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

    @GET
    @Path("/seats/{time}")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<SeatDTO> getSeats(@PathParam("time") LocalDateTimeParam dateParam, @QueryParam("status") BookingStatus status) {
        LocalDateTime localDateTime = dateParam.getLocalDateTime();
        return null;
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(UserDTO userDTO) {
        // check if the user is in the database
        User user;
        try {
            em.getTransaction().begin();
            TypedQuery<User> userQuery = em.createQuery(
                    "select u from User u where u.username = :targetUserName and u.password = :targetPassword",
                    User.class
            );
            userQuery.setParameter("targetUserName", userDTO.getUsername());
            userQuery.setParameter("targetPassword", userDTO.getPassword());

            // calling getSingleResult throws an exception when no entry found which causes problems
            user = userQuery.getResultList().stream().findFirst().orElse(null);
        } finally {
            em.close();
        }

        Response response;
        if (user == null) {
            response = Response.status(Response.Status.UNAUTHORIZED).build();
        } else {
            NewCookie cookie = new NewCookie(Config.AUTH_COOKIE, UUID.randomUUID().toString());
            LOGGER.info("Heres my cookie: ");
            LOGGER.info(cookie.toString());
            response = Response.ok().cookie(cookie).build();
        }

        return response;
    }

}
