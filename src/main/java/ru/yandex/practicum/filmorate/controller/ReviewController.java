package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;

@RestController
@RequestMapping("/reviews")
public class ReviewController extends AbstractController<Review, ReviewService> {

    @Autowired
    public ReviewController(ReviewService service) {
        super(service);
    }
}