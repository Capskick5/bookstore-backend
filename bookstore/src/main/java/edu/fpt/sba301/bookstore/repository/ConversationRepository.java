package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Conversation;
import edu.fpt.sba301.bookstore.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Page<Conversation> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
