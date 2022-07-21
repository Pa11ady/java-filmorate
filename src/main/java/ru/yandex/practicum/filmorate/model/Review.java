package ru.yandex.practicum.filmorate.model;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class Review extends AbstractEntity{
    private String content;
    private boolean isPositive;
    private Long userId;
    private Long filmId;
    private int useful;
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final Map<Long, Boolean> grades = new HashMap<>();
}
