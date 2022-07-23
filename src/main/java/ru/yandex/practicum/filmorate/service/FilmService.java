package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.InvalidFilmException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Rating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class FilmService extends AbstractService<Film, FilmStorage> {
    private final static String MSG_ERR_DATE = "Дата релиза не раньше 28 декабря 1895 года ";
    private final static String MSG_ERR_MPA = "Не заполнен рейтинг MPA";

    private final LocalDate MIN_DATE = LocalDate.of(1895, 12, 28);
    private final UserService userService;
    private final GenreStorage genreStorage;
    private final JdbcTemplate jdbcTemplate;


    @Autowired
    public FilmService(FilmStorage storage, UserService userService, GenreStorage genreStorage, JdbcTemplate jdbcTemplate) {
        super(storage);
        this.userService = userService;
        this.genreStorage = genreStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Film create(Film film) {
        film = super.create(film);
        storage.createGenresByFilm(film);
        log.info("Добавлен фильма {}", film);
        return film;
    }

    public List<Film> getByDirector(Long directorId, String sortBy) {
        String sortedByLikes =
                "SELECT df.film_id FROM films_directors df " +
                        "LEFT JOIN films_likes l ON df.film_id=l.film_id " +
                        "WHERE df.director_id=? " +
                        "GROUP BY df.film_id, l.user_id ORDER BY COUNT(L.user_id) DESC";
        String sortedByYear =
                "SELECT df.film_id FROM films_directors df " +
                        "JOIN films f ON df.film_id=f.film_id " +
                        "WHERE df.director_id=? ORDER BY f.release_date";
        if (sortBy.equals("year"))
            return jdbcTemplate.query(sortedByYear, this::mapToFilm, directorId);
        else
            return jdbcTemplate.query(sortedByLikes, this::mapToFilm, directorId);
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
    public Film update(Film film) {
        film = super.update(film);
        storage.updateGenresByFilm(film);
        log.info("Обновлён фильм {}", film);
        return film;
    }

    @Override
    public List<Film> findAll() {
        List<Film> films = super.findAll();
        films.forEach(this::loadData);
        return films;
    }

    @Override
    public Film findById(Long id) {
        Film film = super.findById(id);
        loadData(film);
        return film;
    }

    @Override
    public void delete(Long id) {
        super.delete(id);
    }

    private void loadData(Film film) {
        film.setGenres(genreStorage.getGenresByFilm(film));
        storage.loadLikes(film);
    }

    //Шаблонный метод
    @Override
    public void validationBeforeCreate(Film film) {
        validateReleaseDate(film.getReleaseDate());
        validateMpa(film.getMpa());
    }

    //Шаблонный метод
    @Override
    public void validationBeforeUpdate(Film film) {
        super.validationBeforeUpdate(film);
        validateReleaseDate(film.getReleaseDate());
        validateMpa(film.getMpa());
    }

    private void validateReleaseDate(LocalDate date) {
        if (date.isBefore(MIN_DATE)) {
            log.warn(MSG_ERR_DATE + date);
            throw new InvalidFilmException(MSG_ERR_DATE);
        }
    }

    private void validateMpa(Rating mpa) {
        if (mpa == null) {
            log.warn(MSG_ERR_MPA);
            throw new InvalidFilmException(MSG_ERR_MPA);
        }
    }

    private void validateLike(Film film, User user) {
        if (film == null) {
            String message = ("Фильм не найден");
            log.warn(message);
            throw new NotFoundException(message);
        }
        if (user == null) {
            String message = ("Пользователь не найден");
            log.warn(message);
            throw new NotFoundException(message);
        }
    }

    public void addLike(Long id, Long userId) {
        Film film = this.findById(id);
        User user = userService.findById(userId);
        validateLike(film, user);
        film.addLike(userId);
        storage.saveLikes(film);
    }

    public void removeLike(Long id, Long userId) {
        Film film = this.findById(id);
        User user = userService.findById(userId);
        validateLike(film, user);
        film.removeLike(userId);
        storage.saveLikes(film);
    }

    public List<Film> findPopularMovies(int count) {
        List<Film> films = this.findAll();
        films.sort(Comparator.comparing(Film::getLikesCount).reversed());
        if (count > films.size()) {
            count = films.size();
        }

        return new ArrayList<>(films.subList(0, count));
    }
}