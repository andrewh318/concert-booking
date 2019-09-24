package se325.assignment01.concert.service.services.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.config.Config;
import se325.assignment01.concert.common.dto.UserDTO;
import se325.assignment01.concert.service.domain.User;
import se325.assignment01.concert.service.services.PersistenceManager;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/concert-service/login")
public class LoginResource {
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private PersistenceManager persistenceManager;

    public LoginResource() {
        this.persistenceManager = PersistenceManager.instance();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(UserDTO userDTO) {
        // check if the user is in the database
        User user;
        Response response;

        EntityManager em =  persistenceManager.createEntityManager();
        try {
            em.getTransaction().begin();
            TypedQuery<User> userQuery = em.createQuery(
                    "select u from User u where u.username = :targetUserName and u.password = :targetPassword",
                    User.class
            )
                .setParameter("targetUserName", userDTO.getUsername())
                .setParameter("targetPassword", userDTO.getPassword());

            userQuery.setLockMode(LockModeType.OPTIMISTIC);

            // calling getSingleResult throws an exception when no entry found which causes problems
            user = userQuery.getResultList().stream().findFirst().orElse(null);

            if (user == null) {
                response = Response.status(Response.Status.UNAUTHORIZED).build();
            } else {
                String uuid = UUID.randomUUID().toString();

                // set user auth token
                user.setAuthToken(uuid);
                em.merge(user);

                NewCookie cookie = new NewCookie(Config.AUTH_COOKIE, uuid);
                response = Response.ok().cookie(cookie).build();
            }

            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return response;
    }
}
