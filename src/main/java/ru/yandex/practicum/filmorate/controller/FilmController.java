package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;


import java.util.List;

@RestController
@RequestMapping("/films")
public class FilmController extends AbstractController<Film, FilmService> {

    @Autowired
    public FilmController(FilmService service) {
        super(service);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        service.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable Long id, @PathVariable Long userId) {
        service.removeLike(id, userId);
    }

    @GetMapping("/popular")
    public List<Film> findPopularMovies(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "0") int genreId,
            @RequestParam(defaultValue = "0") int year) {
        return service.findPopularMovies(count, genreId, year);
    }

    @GetMapping("/common")
    public List<Film> getCommonMovies(@RequestParam Long userId, @RequestParam Long friendId) {
        return service.commonMovies(userId, friendId);
    }

    @GetMapping("/director/{directorId}")
    public List<Film> findFilmsByDirector(@PathVariable Long directorId, @RequestParam String sortBy) {
        return service.findFilmsByDirector(directorId, sortBy);
    }

    @GetMapping("search")
    public List<Film> search(@RequestParam(required = false) String query,
                             @RequestParam(required = false) String by) {
        return service.searchBy(query, by);
    }

}