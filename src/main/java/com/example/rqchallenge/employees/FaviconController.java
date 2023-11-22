package com.example.rqchallenge.employees;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Ignore requests for favicon.ico
 * (prevents false errors in log when testing from a browser).
 */
@Controller
public class FaviconController {

    @GetMapping("favicon.ico")
    @ResponseBody
    void returnNoFavicon() {
        // intentionally does nothing
    }
}