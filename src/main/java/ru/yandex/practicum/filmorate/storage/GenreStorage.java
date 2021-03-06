package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Set;

public interface GenreStorage extends CommonStorage<Genre> {
    Set<Genre> getGenresByFilm(Film film);
}