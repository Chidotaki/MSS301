package com.fudn.movie_service.dto;

import com.fudn.movie_service.model.Genre;

public record GenreResponse(
        String genreId,
        String genreName,
        String description
) {
    public static GenreResponse from(Genre g) {
        return new GenreResponse(
                g.getGenreId(),
                g.getGenreName(),
                g.getDescription()
        );
    }
}