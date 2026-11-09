package com.craftora.craftora.Controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LogoutBackFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        boolean privatePage =
                uri.equals("/admin-dashboard")
                        || uri.startsWith("/admin/")
                        || uri.equals("/artisan-dashboard")
                        || uri.startsWith("/artisan/")
                        || uri.equals("/customer-dashboard")
                        || uri.startsWith("/customer/");

        HttpSession session = request.getSession(false);

        boolean loggedIn = false;

        if (session != null) {

            Object loggedInUserId =
                    session.getAttribute("loggedInUserId");

            Object loggedInUser =
                    session.getAttribute("loggedInUser");

            loggedIn =
                    loggedInUserId != null
                            || loggedInUser != null;
        }

        if (privatePage && !loggedIn) {

            response.sendRedirect(
                    request.getContextPath() + "/"
            );

            return;
        }

        if (privatePage && loggedIn) {

            response.setHeader(
                    "Cache-Control",
                    "no-cache, no-store, must-revalidate"
            );

            response.setHeader(
                    "Pragma",
                    "no-cache"
            );

            response.setDateHeader(
                    "Expires",
                    0
            );
        }

        filterChain.doFilter(request, response);
    }
}