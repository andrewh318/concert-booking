package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.PerformerDTO;
import se325.assignment01.concert.service.domain.Performer;

public class PerformerMapper {
    static Performer toDomainModel(PerformerDTO dtoPerformer) {
        Performer fullPerformer = new Performer(
                dtoPerformer.getId(),
                dtoPerformer.getName(),
                dtoPerformer.getImageName(),
                dtoPerformer.getGenre(),
                dtoPerformer.getBlurb()
        );

        return fullPerformer;
    }

    static PerformerDTO toDto(Performer performer) {
        PerformerDTO dtoPerformer = new PerformerDTO(
                performer.getId(),
                performer.getName(),
                performer.getImageName(),
                performer.getGenre(),
                performer.getBlurb()
        );

        return dtoPerformer;
    }
}
