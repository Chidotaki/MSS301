package com.fudn.movie_service.repository;

import com.fudn.movie_service.model.Movie;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MovieRepository extends MongoRepository<Movie, String> {

    boolean existsByGenreId(String genreId);
}