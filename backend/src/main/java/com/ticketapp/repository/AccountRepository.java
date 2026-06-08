package com.ticketapp.repository;

import com.ticketapp.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByEmail(String email);

    Optional<Account> findByPhoneNumber(String phoneNumber);

    Optional<Account> findByPersonalId(String personalId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPersonalId(String personalId);

    @Query("SELECT a FROM Account a WHERE a.email = :email AND a.isActive = true")
    Optional<Account> findActiveByEmail(@Param("email") String email);

    @Query("SELECT a FROM Account a WHERE a.isEmailVerified = true AND a.isActive = true")
    java.util.List<Account> findAllVerifiedAndActive();

    @Query("SELECT COUNT(a) FROM Account a WHERE a.isActive = true")
    long countActiveAccounts();

    @Query("SELECT COUNT(a) FROM Account a WHERE a.isEmailVerified = false")
    long countUnverifiedAccounts();

    @Query("SELECT a FROM Account a WHERE a.mustChangePassword = true")
    java.util.List<Account> findAccountsRequiringPasswordChange();
}
