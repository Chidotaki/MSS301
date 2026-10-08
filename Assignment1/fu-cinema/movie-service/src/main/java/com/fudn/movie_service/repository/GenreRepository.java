package com.fudn.movie_service.repository;

import com.fudn.movie_service.model.Genre;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GenreRepository extends MongoRepository<Genre, String> {

    boolean existsByGenreNameIgnoreCase(String genreName);

    boolean existsByGenreNameIgnoreCaseAndGenreIdNot(
            String genreName,
            String genreId
    );
}