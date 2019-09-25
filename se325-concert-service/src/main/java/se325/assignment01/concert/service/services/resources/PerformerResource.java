package se325.assignment01.concert.service.services.resources;

import se325.assignment01.concert.common.dto.PerformerDTO;
import se325.assignment01.concert.service.domain.Performer;
import se325.assignment01.concert.service.mapper.PerformerMapper;
import se325.assignment01.concert.service.services.PersistenceManager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/concert-service/performers")
public class PerformerResource {
    private PersistenceManager persistenceManager;

    public PerformerResource () {
        this.persistenceManager = PersistenceManager.instance();
    }

    /**
     * Retrieves a performer given an ID
     * @param id
     * @return
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPerformer(@PathParam("id") long id) {
        PerformerDTO dtoPerformer;

        EntityManager em = persistenceManager.createEntityManager();

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

    /**
     * Retrieves a list of all the performers in the database
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPerformers() {
        GenericEntity<List<PerformerDTO>> entity;
        EntityManager em =  persistenceManager.createEntityManager();

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

}
