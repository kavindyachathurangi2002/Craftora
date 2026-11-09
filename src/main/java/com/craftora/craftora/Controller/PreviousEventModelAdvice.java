
package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.PreviousEvent;
import com.craftora.craftora.Repository.PreviousEventRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class PreviousEventModelAdvice {

    private final PreviousEventRepository previousEventRepository;

    public PreviousEventModelAdvice(
            PreviousEventRepository previousEventRepository) {

        this.previousEventRepository = previousEventRepository;
    }

    @ModelAttribute
    public void addPreviousEventsToPages(
            Model model,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String eventType = null;

        if ("/workshops".equals(path)) {
            eventType = "WORKSHOP";
        } else if ("/exhibitions".equalsIgnoreCase(path)) {
            eventType = "EXHIBITION";
        } else if ("/competitions".equalsIgnoreCase(path)) {
            eventType = "COMPETITION";
        }

        if (eventType != null) {

            List<PreviousEvent> previousEvents =
                    previousEventRepository
                            .findByEventTypeIgnoreCaseOrderByEventDateDesc(
                                    eventType);

            model.addAttribute("previousEvents", previousEvents);
        }
    }
}
