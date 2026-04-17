package com.eventra.eventra.interceptor;

import com.eventra.eventra.model.User;
import com.eventra.eventra.enums.RoleEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.stereotype.Component;

@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();

        // Only intercept admin routes
        if (!requestPath.startsWith("/admin")) {
            return true;
        }

        HttpSession session = request.getSession();
        User loggedInUser = (User) session.getAttribute("loggedInUser");

        // Check if user is logged in and is an ADMIN
        if (loggedInUser == null || loggedInUser.getRole() == null || 
            loggedInUser.getRole().getRoleName() != RoleEnum.ADMIN) {
            
            // Redirect non-admins to dashboard with error message
            response.sendRedirect("/dashboard?error=Access%20Denied%20-%20Admin%20privileges%20required");
            return false;
        }

        // Check if admin is approved (should already be approved, but extra safety check)
        if (!loggedInUser.isApproved()) {
            response.sendRedirect("/dashboard?error=Your%20admin%20account%20is%20not%20yet%20approved");
            return false;
        }

        return true;
    }
}
