package se325.assignment01.concert.service.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@Table(name="CONCERTS")
public class Concert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "TITLE")
    private String title;
    @Column(name = "IMAGE_NAME")
    private String imageName;
    @Column(length = 1024, name = "BLURB")
    private String blurb;
    @ElementCollection
    @CollectionTable(
            name="CONCERT_DATES",
            joinColumns = @JoinColumn(name = "CONCERT_ID")
    )
    @Column (name = "DATE")
    private Set<LocalDateTime> dates;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(
            name="CONCERT_PERFORMER",
            joinColumns = @JoinColumn(name = "CONCERT_ID"),
            inverseJoinColumns = {@JoinColumn(name = "PERFORMER_ID")}
    )
    private Set<Performer> performers;

    public Concert() {}

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

    @Override
    public String toString() {
        return "Concert{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", imageName='" + imageName + '\'' +
                ", blurb='" + blurb + '\'' +
                ", dates=" + dates +
                ", performers=" + performers +
                '}';
    }
}
