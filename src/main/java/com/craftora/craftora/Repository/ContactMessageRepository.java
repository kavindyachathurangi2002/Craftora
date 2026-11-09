package com.craftora.craftora.Repository;

import com.craftora.craftora.Entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository
        extends JpaRepository<ContactMessage, Long> {
}