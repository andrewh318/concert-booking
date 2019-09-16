package se325.assignment01.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.config.Config;
import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.domain.Performer;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// TODO Create separate resource class for concerts and performers. Separate services so each one can be scaled individually
@Path("/concert-service")
public class ConcertResource {
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private EntityManager em = PersistenceManager.instance().createEntityManager();
    
    @GET
    @Path("/concerts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ConcertDTO getConcert(@PathParam("id") long id) {
        LOGGER.info("Retrieving concert with id: " + id);
        ConcertDTO dtoConcert;
        try {
            em.getTransaction().begin();
            Concert concert = em.find(Concert.class, id);
            em.getTransaction().commit();

            if (concert == null) {
                throw new WebApplicationException(javax.ws.rs.core.Response.Status.NOT_FOUND);
            }

            dtoConcert = ConcertMapper.toDto(concert);
        } finally {
            em.close();
        }


        return dtoConcert;
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
    public PerformerDTO getPerformer(@PathParam("id") long id) {
        PerformerDTO dtoPerformer;

        try {
            em.getTransaction().begin();
            Performer performer = em.find(Performer.class, id);

            if (performer == null) {
                throw new WebApplicationException(javax.ws.rs.core.Response.Status.NOT_FOUND);
            }

            dtoPerformer = PerformerMapper.toDto(performer);
        } finally {
            em.close();
        }
        return dtoPerformer;
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
        if (cookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return null;
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
            response = appendCookie(Response.ok(), makeCookie(null)).build();
        }

        return response;
    }

    private NewCookie makeCookie(Cookie cookie) {
        NewCookie newCookie = null;

        if (cookie == null) {
            newCookie = new NewCookie(Config.AUTH_COOKIE, UUID.randomUUID().toString());
            LOGGER.info("Generated cookie " + newCookie.getValue());
        }

        return newCookie;
    }

    private Response.ResponseBuilder appendCookie(Response.ResponseBuilder rb, Cookie cookie) {
        if (cookie == null) {
            rb.cookie(makeCookie(cookie));
        }

        return rb;
    }

}
