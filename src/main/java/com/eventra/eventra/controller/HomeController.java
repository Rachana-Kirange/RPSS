package com.eventra.eventra.controller;

import com.eventra.eventra.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final EventService eventService;

    public HomeController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Home page
     */
    @GetMapping("/")
    public String home(Model model) {
        var upcomingEvents = eventService.getUpcomingApprovedEvents();
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("eventCount", upcomingEvents.size());

        return "home/index";
    }

    /**
     * About page
     */
    @GetMapping("/about")
    public String about() {
        return "home/about";
    }

    /**
     * Contact page
     */
    @GetMapping("/contact")
    public String contact() {
        return "home/contact";
    }

    /**
     * FAQs page
     */
    @GetMapping("/faq")
    public String faq() {
        return "home/faq";
    }
}
