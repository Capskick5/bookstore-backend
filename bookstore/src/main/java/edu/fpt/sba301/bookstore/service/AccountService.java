package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.entity.Account;

public interface AccountService {
    Account login(String username, String password);
}
