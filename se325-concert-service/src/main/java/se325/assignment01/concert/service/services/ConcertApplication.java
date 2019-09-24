package se325.assignment01.concert.service.services;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import se325.assignment01.concert.service.services.resources.*;
import se325.assignment01.concert.service.util.ConcertUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * This is the Application class for the concert service. This class is complete - you should not need to modify it
 * (but will not be deducted marks if you decide you need to do so).
 */
@ApplicationPath("/services")
public class ConcertApplication extends Application {

    private Set<Object> singletons = new HashSet<>();
    private Set<Class<?>> classes = new HashSet<>();

    public ConcertApplication() {
        // REST API is split across multiple resource classes for readability and maintainability
        classes.add(TestResource.class);
        classes.add(ConcertResource.class);
        classes.add(PerformerResource.class);
        classes.add(BookingResource.class);
        classes.add(LoginResource.class);
        classes.add(SeatResource.class);
        
        singletons.add(PersistenceManager.instance());

        ConcertUtils.initConcerts();
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

}
