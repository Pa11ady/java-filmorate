package ru.yandex.practicum.filmorate.storage.db_impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Rating;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
@Primary
@Slf4j
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override

    public Film findById(Long id) {
        String sql =
                "SELECT f.FILM_ID, f.NAME, f.DESCRIPTION, f.RELEASE_DATE, f.DURATION, f.RATING_ID, r.NAME R_NAME " +
                        "FROM FILMS f JOIN RATINGS r ON f.RATING_ID = r.RATING_ID " +
                        "WHERE f.FILM_ID = ?";
        List<Film> result = jdbcTemplate.query(sql, this::mapToFilm, id);
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    private Film mapToFilm(ResultSet resultSet, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(resultSet.getLong("FILM_ID"));
        film.setName(resultSet.getString("NAME"));
        film.setDescription(resultSet.getString("DESCRIPTION"));
        film.setReleaseDate(resultSet.getDate("RELEASE_DATE").toLocalDate());
        film.setDuration(resultSet.getInt("DURATION"));
        film.setMpa(new Rating(resultSet.getLong("RATING_ID"), resultSet.getString("R_NAME")));
        return film;
    }

    @Override
    public List<Film> findAll() {
        String sql =
                "SELECT f.FILM_ID, f.NAME, f.DESCRIPTION, f.RELEASE_DATE, f.DURATION, f.RATING_ID, r.NAME R_NAME " +
                        "FROM FILMS f JOIN RATINGS r ON f.RATING_ID = r.RATING_ID ORDER BY f.FILM_ID";
        return jdbcTemplate.query(sql, this::mapToFilm);
    }

    public List<Film> findAllByYear(int year) {
        String sql =
                "SELECT f.FILM_ID, f.NAME, f.DESCRIPTION, f.RELEASE_DATE, f.DURATION, f.RATING_ID, r.NAME R_NAME " +
                        "FROM FILMS f JOIN RATINGS r ON f.RATING_ID = r.RATING_ID " +
                        "WHERE YEAR(f.RELEASE_DATE) = ? ORDER BY f.FILM_ID";
        return jdbcTemplate.query(sql, this::mapToFilm, year);
    }

    public List<Film> findAllByGenre(int genreId) {
        String sql =
                "SELECT f.FILM_ID, f.NAME, f.DESCRIPTION, f.RELEASE_DATE, f.DURATION, f.RATING_ID, r.NAME R_NAME " +
                        "FROM FILMS f JOIN RATINGS r ON f.RATING_ID = r.RATING_ID " +
                        "WHERE f.FILM_ID IN (SELECT FILMS_GENRES.FILM_ID FROM FILMS_GENRES WHERE GENRE_ID = ?) ORDER BY f.FILM_ID";
        return jdbcTemplate.query(sql, this::mapToFilm, genreId);
    }

    public List<Film> findAllByGenreAndYear(int genreId, int year) {
        String sql =
                "SELECT f.FILM_ID, f.NAME, f.DESCRIPTION, f.RELEASE_DATE, f.DURATION, f.RATING_ID, r.NAME R_NAME " +
                        "FROM FILMS f JOIN RATINGS r ON f.RATING_ID = r.RATING_ID " +
                        "WHERE f.FILM_ID IN (SELECT FILMS_GENRES.FILM_ID FROM FILMS_GENRES WHERE GENRE_ID = ?) " +
                        "AND YEAR(f.RELEASE_DATE) = ? ORDER BY f.FILM_ID";
        return jdbcTemplate.query(sql, this::mapToFilm, genreId, year);
    }

    @Override
    public Film create(Film film) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("FILMS")
                .usingGeneratedKeyColumns("FILM_ID");

        Map<String, Object> values = new HashMap<>();
        values.put("NAME", film.getName());
        values.put("DESCRIPTION", film.getDescription());
        values.put("RELEASE_DATE", film.getReleaseDate());
        values.put("DURATION", film.getDuration());
        values.put("RATING_ID", film.getMpa().getId());

        film.setId(simpleJdbcInsert.executeAndReturnKey(values).longValue());
        return film;
    }

    @Override
    public Film update(Film film) {
        String sql =
                "UPDATE FILMS SET NAME = ?, DESCRIPTION = ?, RELEASE_DATE = ?, DURATION = ?, RATING_ID = ? " +
                        "WHERE FILM_ID = ?";
        jdbcTemplate.update(sql, film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(),
                film.getMpa().getId(), film.getId());

        return film;
    }

    @Override
    public void delete(Long id) {
        final String sql = "DELETE FROM FILMS WHERE FILM_ID = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void saveLikes(Film film) {
        jdbcTemplate.update("DELETE FROM FILMS_LIKES WHERE FILM_ID = ?", film.getId());

        String sql = "INSERT INTO FILMS_LIKES (FILM_ID, USER_ID) VALUES(?, ?)";
        Set<Long> likes = film.getLikes();
        for (var like : likes) {
            jdbcTemplate.update(sql, film.getId(), like);
        }
    }

    @Override
    public void loadLikes(Film film) {
        String sql = "SELECT USER_ID FROM FILMS_LIKES WHERE FILM_ID = ?";
        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(sql, film.getId());
        while (sqlRowSet.next()) {
            film.addLike(sqlRowSet.getLong("USER_ID"));
        }
    }


    @Override
    public void createGenresByFilm(Film film) {
        String sql = "INSERT INTO FILMS_GENRES (FILM_ID, GENRE_ID) VALUES(?, ?)";
        Set<Genre> genres = film.getGenres();
        if (genres == null) {
            return;
        }
        for (var genre : genres) {
            jdbcTemplate.update(sql, film.getId(), genre.getId());
        }
    }

    @Override
    public void updateGenresByFilm(Film film) {
        String sql = "DELETE FROM FILMS_GENRES WHERE FILM_ID = ?";
        jdbcTemplate.update(sql, film.getId());
        createGenresByFilm(film);
    }

    @Override
    public List<Film> commonMovies(Long userId, Long friendId) {
        String sql =
                "SELECT f.FILM_ID, f.NAME, f.DESCRIPTION, f.RELEASE_DATE, f.DURATION, f.RATING_ID, r.NAME R_NAME " +
                        "FROM FILMS_LIKES fl1 JOIN FILMS_LIKES fl2 ON " +
                        "fl1.FILM_ID  = fl2.FILM_ID " +
                        "AND fl1.USER_ID  != fl2.USER_ID " +
                        "AND fl1.USER_ID = ? " +
                        "AND fl2.USER_ID = ? " +
                        "JOIN FILMS f ON fl1.FILM_ID = f.FILM_ID " +
                        "JOIN RATINGS r ON f.RATING_ID = r.RATING_ID ORDER BY f.FILM_ID";
        List<Film> films = jdbcTemplate.query(sql, this::mapToFilm, userId, friendId);
        return films;
    }

    @Override
    public void updateDirectorsByFilm(Film film) {
        String sql = "DELETE FROM films_directors WHERE film_id = ?";
        jdbcTemplate.update(sql, film.getId());
        createDirectorsByFilm(film);
    }

    @Override
    public List<Film> findFilmsByDirector(Long directorId, String sortBy) {
        String sqlByYear = "SELECT fd.film_id " +
                "FROM films_directors AS fd " +
                "LEFT OUTER JOIN films ON fd.film_id = films.film_id " +
                "WHERE director_id = ? " +
                "ORDER BY films.release_date";

        String sqlByLikes = "SELECT fd.film_id, COUNT(fl.user_id) AS p " +
                "FROM films_directors AS fd " +
                "LEFT OUTER JOIN films_likes AS fl ON fd.film_id = fl.film_id " +
                "WHERE director_id = ? " +
                "GROUP BY fd.film_id " +
                "ORDER BY p DESC ";

        List<Film> films = new ArrayList<>();
        if (sortBy.equals("year")) {
            List<Long> filmsId = jdbcTemplate.query(sqlByYear, (rs, rowNum) -> rs.getLong("film_id"), directorId);
            for (Long id : filmsId) {
                films.add(findById(id));
            }
            return films;
        } else {
            List<Long> filmsId = jdbcTemplate.query(sqlByLikes, (rs, rowNum) -> rs.getLong("film_id"), directorId);
            for (Long id : filmsId) {
                films.add(findById(id));
            }
            return films;
        }
    }

    @Override
    public void createDirectorsByFilm(Film film) {
        String sql = "INSERT INTO films_directors (film_id, director_id) VALUES(?, ?)";
        Set<Director> directors = film.getDirectors();
        if (directors == null) {
            return;
        }
        for (Director director : directors) {
            jdbcTemplate.update(sql, film.getId(), director.getId());
        }
    }

    @Override
    public List<Film> searchBy(String queryString, String searchBy) {
        String searchByTitle = "SELECT f.FILM_ID, f.NAME, f.DESCRIPTION, f.RELEASE_DATE, f.DURATION, f.RATING_ID, r.NAME R_NAME  FROM films AS f LEFT OUTER JOIN " +
                "(SELECT film_id, COUNT (*) likes_count FROM FILMS_LIKES GROUP BY film_id) " +
                "AS l ON f.film_id = l.film_id " +
                "LEFT OUTER JOIN ratings AS r ON f.rating_id = r.rating_id " +
                "WHERE f.name ILIKE CONCAT('%', ?, '%')";
        String searchByDir = "SELECT f.FILM_ID, f.NAME, f.DESCRIPTION, f.RELEASE_DATE, f.DURATION, f.RATING_ID, r.NAME R_NAME  FROM films AS f LEFT OUTER JOIN " +
                "(SELECT film_id, COUNT (*) likes_count FROM FILMS_LIKES GROUP BY film_id) " +
                "AS l ON f.film_id = l.film_id " +
                "LEFT OUTER JOIN ratings AS r ON f.rating_id = r.rating_id " +
                "LEFT OUTER JOIN FILMS_DIRECTORS AS fd ON f.FILM_ID = fd.FILM_ID " +
                "LEFT OUTER JOIN DIRECTORS AS d ON fd.DIRECTOR_ID = d.DIRECTOR_ID " +
                "WHERE d.name ILIKE CONCAT('%', ?, '%')";
        final String searchDirectorFilm = searchByDir + " UNION ALL " + searchByTitle + " ORDER BY FILM_ID desc";
        final String searchFilmDirector = searchByTitle + " UNION ALL " + searchByDir + " ORDER BY FILM_ID desc";

        List<Film> result = null;
        switch (searchBy) {
            case "director":
                result = jdbcTemplate.query(searchByDir, this::mapToFilm, queryString);
                break;
            case "title":
                result = jdbcTemplate.query(searchByTitle, this::mapToFilm, queryString);
                break;
            case "director,title":
                result = jdbcTemplate.query(searchDirectorFilm, this::mapToFilm, queryString, queryString);
                break;
            case "title,director":
                result = jdbcTemplate.query(searchFilmDirector, this::mapToFilm, queryString, queryString);
                break;
        }
        return result;
    }
}