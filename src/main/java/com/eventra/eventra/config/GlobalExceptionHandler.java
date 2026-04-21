package com.eventra.eventra.config;

import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.ui.Model;
import java.util.logging.Logger;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = Logger.getLogger(GlobalExceptionHandler.class.getName());
    
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(NoHandlerFoundException e, Model model) {
        log.warning("404 ERROR: " + e.getRequestURL());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleGlobalException(Exception e, Model model) {
        log.severe("500 ERROR: " + e.getClass().getName());
        log.severe("MESSAGE: " + getMostSpecificMessage(e));
        e.printStackTrace();

        model.addAttribute("error", getMostSpecificMessage(e));
        model.addAttribute("errorClass", e.getClass().getSimpleName());
        
        return "error/500";
    }

    private String getMostSpecificMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unexpected server error";
        }

        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }

        if (message == null || message.isBlank()) {
            return root.getClass().getSimpleName();
        }

        return message;
    }
}
