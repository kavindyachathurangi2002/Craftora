package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.PreviousEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PreviousEventRepository
        extends JpaRepository<PreviousEvent, Long> {

    List<PreviousEvent>
        findByEventTypeIgnoreCaseOrderByEventDateDesc(String eventType);
}
