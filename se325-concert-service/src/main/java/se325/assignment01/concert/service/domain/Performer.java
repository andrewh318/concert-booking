package se325.assignment01.concert.service.domain;

import se325.assignment01.concert.common.types.Genre;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name="PERFORMERS")
public class Performer {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(name = "IMAGE_NAME")
    private String imageName;
    @Enumerated(EnumType.STRING)
    private Genre genre;
    @Column(length = 1024)
    private String blurb;
    @ManyToMany(mappedBy = "performers")
    private Set<Concert> concerts;

    public Performer() {}

    public Performer(Long id, String name, String imageName, Genre genre, String blurb) {
        this.id = id;
        this.name = name;
        this.imageName = imageName;
        this.genre = genre;
        this.blurb = blurb;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public String getBlurb() {
        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    public Set<Concert> getConcerts() {
        return concerts;
    }

    public void setConcerts(Set<Concert> concerts) {
        this.concerts = concerts;
    }
}
