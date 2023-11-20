package com.ko.footballupdater.controllers;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping(path="")
public class HomeController {

    /**
     * Home
     * @return redirect to post
     */
    @GetMapping("")
    public String home() {
        return "redirect:/posts";
    }

}
