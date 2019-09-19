package se325.assignment01.concert.service.services.resources;

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
import se325.assignment01.concert.service.services.PersistenceManager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/concert-service/concerts")
public class ConcertResource {
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private EntityManager em = PersistenceManager.instance().createEntityManager();
    
    @GET
    @Path("{id}")
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

        return Response.ok(dtoConcert).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllConcerts() {
        GenericEntity<List<ConcertDTO>> entity;
        try {
            em.getTransaction().begin();
            TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c", Concert.class);
            List<ConcertDTO> concerts = concertQuery.getResultList().stream().map(c -> {
                ConcertDTO dtoConcert = ConcertMapper.toDto(c);
                dtoConcert.setPerformers(c.getPerformers().stream().map(p -> PerformerMapper.toDto(p)).collect(Collectors.toList()));
                dtoConcert.setDates(new ArrayList<>(c.getDates()));
                return dtoConcert;
            }).collect(Collectors.toList());

            entity = new GenericEntity<List<ConcertDTO>>(concerts){};

            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }

    @GET
    @Path("/summaries")
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

}
