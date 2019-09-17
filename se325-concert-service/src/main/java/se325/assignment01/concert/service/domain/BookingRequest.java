package se325.assignment01.concert.service.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import se325.assignment01.concert.common.jackson.LocalDateTimeDeserializer;
import se325.assignment01.concert.common.jackson.LocalDateTimeSerializer;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
//TODO come up with better annotations for table names of seat labels 
public class BookingRequest {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;
    private long concertId;
    private LocalDateTime date;
    @ElementCollection
    private List<String> seatLabels = new ArrayList<>();

    public BookingRequest(){}

    public BookingRequest(long concertId, LocalDateTime date, List<String> seatLabels) {
        this.concertId = concertId;
        this.date = date;
        this.seatLabels = seatLabels;
    }

    public long getConcertId() {
        return concertId;
    }

    public void setConcertId(long concertId) {
        this.concertId = concertId;
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public List<String> getSeatLabels() {
        return seatLabels;
    }

    public void setSeatLabels(List<String> seatLabels) {
        this.seatLabels = seatLabels;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
