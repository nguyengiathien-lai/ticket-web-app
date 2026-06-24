package com.ticketapp.repository;

import com.ticketapp.entity.Account;
import com.ticketapp.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {

    Optional<OtpCode> findFirstByAccountAndTypeAndIsUsedFalseOrderByCreatedAtDesc(Account account, String type);

    List<OtpCode> findByAccountAndTypeAndIsUsedFalse(Account account, String type);

    void deleteByAccount(Account account);
}
