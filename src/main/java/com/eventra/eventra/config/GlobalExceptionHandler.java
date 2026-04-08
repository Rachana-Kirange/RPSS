package com.eventra.eventra.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.ui.Model;
import java.util.logging.Logger;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = Logger.getLogger(GlobalExceptionHandler.class.getName());
    
    @ExceptionHandler(Exception.class)
    public String handleGlobalException(Exception e, Model model) {
        log.severe("GLOBAL EXCEPTION: " + e.getClass().getName());
        log.severe("MESSAGE: " + e.getMessage());
        e.printStackTrace();
        
        model.addAttribute("error", "An unexpected error occurred: " + e.getMessage());
        model.addAttribute("errorClass", e.getClass().getSimpleName());
        
        return "error/not-found";
    }
}
