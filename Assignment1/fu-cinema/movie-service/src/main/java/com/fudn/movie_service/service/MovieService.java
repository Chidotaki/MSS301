package com.fudn.movie_service.service;

import com.fudn.movie_service.dto.MovieRequest;
import com.fudn.movie_service.dto.MovieResponse;
import com.fudn.movie_service.exception.ApiException;
import com.fudn.movie_service.model.Genre;
import com.fudn.movie_service.model.Movie;
import com.fudn.movie_service.model.MovieStatus;
import com.fudn.movie_service.repository.GenreRepository;
import com.fudn.movie_service.repository.MovieRepository;
import com.fudn.movie_service.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final ShowtimeRepository showtimeRepository;
    private final GenreService genreService;
    private final MongoTemplate mongoTemplate;

    public List<MovieResponse> search(
            String keyword,
            String genreId,
            MovieStatus status
    ) {
        Query query = new Query();

        if (keyword != null && !keyword.isBlank()) {
            query.addCriteria(
                    Criteria.where("title")
                            .regex(
                                    Pattern.quote(keyword.trim()),
                                    "i"
                            )
            );
        }

        if (genreId != null && !genreId.isBlank()) {
            query.addCriteria(
                    Criteria.where("genreId").is(genreId)
            );
        }

        if (status != null) {
            query.addCriteria(
                    Criteria.where("movieStatus").is(status)
            );
        }

        query.with(Sort.by("title"));

        List<Movie> movies =
                mongoTemplate.find(query, Movie.class);

        return toResponses(movies);
    }

    public MovieResponse getById(String id) {
        Movie movie = find(id);

        Genre genre =
                genreService.find(movie.getGenreId());

        return MovieResponse.from(
                movie,
                genre.getGenreName()
        );
    }

    public MovieResponse create(MovieRequest request) {
        Movie movie = new Movie();

        Genre genre = apply(movie, request);

        Movie saved =
                movieRepository.save(movie);

        return MovieResponse.from(
                saved,
                genre.getGenreName()
        );
    }

    public MovieResponse update(
            String id,
            MovieRequest request
    ) {
        Movie movie = find(id);

        Genre genre = apply(movie, request);

        Movie saved =
                movieRepository.save(movie);

        return MovieResponse.from(
                saved,
                genre.getGenreName()
        );
    }

    public void delete(String id) {
        Movie movie = find(id);

        if (showtimeRepository.existsByMovieId(id)) {
            throw ApiException.conflict(
                    "Cannot delete movie that already has showtimes. "
                            + "Set status to ENDED instead."
            );
        }

        movieRepository.delete(movie);
    }

    Movie find(String id) {
        return movieRepository.findById(id)
                .orElseThrow(() ->
                        ApiException.notFound(
                                "Movie not found with id: " + id
                        )
                );
    }

    private List<MovieResponse> toResponses(
            List<Movie> movies
    ) {
        Map<String, String> genreNames =
                genreRepository.findAll()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Genre::getGenreId,
                                        Genre::getGenreName
                                )
                        );

        return movies.stream()
                .map(movie ->
                        MovieResponse.from(
                                movie,
                                genreNames.get(
                                        movie.getGenreId()
                                )
                        )
                )
                .toList();
    }

    private Genre apply(
            Movie movie,
            MovieRequest request
    ) {
        Genre genre =
                genreService.find(request.genreId());

        movie.setTitle(request.title());
        movie.setDescription(request.description());
        movie.setDirector(request.director());
        movie.setDurationMinutes(
                request.durationMinutes()
        );
        movie.setLanguage(request.language());
        movie.setAgeRating(request.ageRating());
        movie.setReleaseDate(request.releaseDate());
        movie.setGenreId(genre.getGenreId());
        movie.setMovieStatus(request.movieStatus());

        return genre;
    }
}