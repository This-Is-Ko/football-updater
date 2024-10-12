package com.ko.footballupdater.controllers;

import com.ko.footballupdater.services.FacebookApiService;
import com.ko.footballupdater.services.TiktokApiService;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping(path="/tiktok")
public class TiktokApiController {

    @Autowired
    private TiktokApiService tiktokApiService;

    /**
     * Login using tiktok api
     * @return redirect to main page
     */
    @GetMapping("/auth")
    public String handleLogin(@RequestParam @Nullable String state,
                              @RequestParam @Nullable String code) {
        try {
            tiktokApiService.handleLogin(state, code);
            return "redirect:/posts";
        } catch (Exception ex) {
            log.atError().setMessage(ex.getMessage()).setCause(ex).log();
            return "redirect:/posts";
        }
    }
}
