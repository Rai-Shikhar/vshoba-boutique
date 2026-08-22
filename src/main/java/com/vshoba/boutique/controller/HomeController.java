package com.vshoba.boutique.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/admin")
    public String admin() {
        return "forward:/admin.html";
    }

    @GetMapping("/orders")
    public String orders() {
        return "forward:/orders.html";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "forward:/checkout.html";
    }
}