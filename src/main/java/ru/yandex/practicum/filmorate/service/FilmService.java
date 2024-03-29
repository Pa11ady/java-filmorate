package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.InvalidFilmException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Rating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;

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
    private final EventService eventService;
    private final UserService userService;
    private final GenreStorage genreStorage;
    private final DirectorStorage directorStorage;

    @Autowired
    public FilmService(FilmStorage storage, EventService eventService, UserService userService,
                       GenreStorage genreStorage, DirectorStorage directorStorage) {
        super(storage);
        this.eventService = eventService;
        this.userService = userService;
        this.genreStorage = genreStorage;
        this.directorStorage = directorStorage;
    }

    @Override
    public Film create(Film film) {
        film = super.create(film);
        storage.createGenresByFilm(film);
        storage.createDirectorsByFilm(film);
        log.info("Добавлен фильма {}", film);
        return film;
    }

    @Override
    public Film update(Film film) {
        film = super.update(film);
        storage.updateGenresByFilm(film);
        storage.updateDirectorsByFilm(film);
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

    private void loadData(Film film) {
        film.setGenres(genreStorage.getGenresByFilm(film));
        film.setDirectors(directorStorage.getDirectorsByFilm(film));
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
        eventService.createAddLikeEvent(userId, id);
    }

    public void removeLike(Long id, Long userId) {
        Film film = this.findById(id);
        User user = userService.findById(userId);
        validateLike(film, user);
        film.removeLike(userId);
        storage.saveLikes(film);
        eventService.createRemoveLikeEvent(userId, id);
    }

    public List<Film> findPopularMovies(int count, int genreId, int year) {
        List<Film> films;
        if (genreId == 0 && year == 0) {
            films = this.findAll();
        } else if (genreId == 0 && year != 0) {
            //селект по всем жанрам и по конкретному году
            films = storage.findAllByYear(year);
            films.forEach(this::loadData);
        } else if (genreId != 0 && year == 0) {
            //селект по конкретному жанру и по всем годам
            films = storage.findAllByGenre(genreId);
            films.forEach(this::loadData);
        } else {
            //селект по конкретному жанру и по конкрутному году
            films = storage.findAllByGenreAndYear(genreId, year);
            films.forEach(this::loadData);
        }
        films.sort(Comparator.comparing(Film::getLikesCount).reversed());
        if (count > films.size()) {
            count = films.size();
        }
        return new ArrayList<>(films.subList(0, count));
    }

    public List<Film> commonMovies(Long userId, Long friendId) {
         List <Film> commonMovies = storage.commonMovies(userId, friendId);
        for (var film : commonMovies) {
            storage.loadLikes(film);
        }
        commonMovies.sort(Comparator.comparing(Film::getLikesCount).reversed());
        return commonMovies;
    }

    public List<Film> findFilmsByDirector(Long directorId, String sortBy) {
        List<Film> films = storage.findFilmsByDirector(directorId, sortBy);
        if (films.isEmpty()) throw  new NotFoundException("");
        films.forEach(this::loadData);
        return films;
    }

    public List<Film> searchBy(String queryString, String searchBy) {
        List<Film> films = storage.searchBy(queryString, searchBy);
        films.forEach(this::loadData);
        return films;
    }

}