package se325.assignment01.concert.service.services.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.mapper.ConcertMapper;
import se325.assignment01.concert.service.mapper.PerformerMapper;
import se325.assignment01.concert.service.services.PersistenceManager;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Path("/concert-service/concerts")
public class ConcertResource {
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private PersistenceManager persistenceManager;

    public ConcertResource () {
        this.persistenceManager = PersistenceManager.instance();
    }

    /**
     * Retrieves the concert associated with a given id
     * @param id
     * @return
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConcert(@PathParam("id") long id) {
        LOGGER.info("Retrieving concert with id: " + id);
        
        ConcertDTO dtoConcert;
        EntityManager em = persistenceManager.createEntityManager();
        try {
            em.getTransaction().begin();

            Concert concert = em.find(Concert.class, id);

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
            
        } finally {
            em.close();
        }

        return Response.ok(dtoConcert).build();
    }

    /**
     * Retrieves a list of all the performers in the database
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllConcerts() {
        GenericEntity<List<ConcertDTO>> entity;
        EntityManager em = persistenceManager.createEntityManager();
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

    /**
     * Retrieves a list of summaries for all the concerts in the database
     * @return
     */
    @GET
    @Path("/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSummaries() {
        GenericEntity<List<ConcertSummaryDTO>> entity;
        EntityManager em = persistenceManager.createEntityManager();
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
