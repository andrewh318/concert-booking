package se325.assignment01.concert.service.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Concert {
    private long id;
    private String title;
    private String imageName;
    private String blurb;
    private Set<LocalDateTime> dates;
    private Set<Performer> performers;

    public Concert(
            long id,
            String title,
            String imageName,
            String blurb ) {
        this.id = id;
        this.title = title;
        this.imageName = imageName;
        this.blurb = blurb;
        this.dates = new HashSet<>();
        this.performers = new HashSet<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getBlurb() {
        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    // TODO Implement this class.
    public Set<LocalDateTime> getDates() {
        return this.dates;
    }

    public void addDate(LocalDateTime date) {
        this.dates.add(date);
    }

    public Set<Performer> getPerformers() {
        return this.performers;
    }

    public void addPerformer(Performer performer) {
        this.performers.add(performer);
    }
}
