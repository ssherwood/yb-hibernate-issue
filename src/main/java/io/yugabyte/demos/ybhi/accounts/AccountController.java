package io.yugabyte.demos.ybhi.accounts;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountRepo accountRepo;

    public AccountController(AccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    @GetMapping
    public List<Account> findAll() {
        return accountRepo.findAll();
    }

    @PostMapping
    public Account createOne(@RequestBody Account account) {
        return accountRepo.save(account);
    }
}
