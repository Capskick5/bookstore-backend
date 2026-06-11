package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account,Integer> {

}
