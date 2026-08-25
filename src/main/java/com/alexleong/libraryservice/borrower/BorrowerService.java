package com.alexleong.libraryservice.borrower;

import com.alexleong.libraryservice.loan.LoanRepository;
import com.alexleong.libraryservice.loan.LoanResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BorrowerService {
    private final BorrowerRepository repository;
    private final LoanRepository loanRepository;

    public BorrowerService(BorrowerRepository repository, LoanRepository loanRepository) {
        this.repository = repository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public BorrowerResponse create(CreateBorrowerRequest request) {
        Borrower borrower = repository.save(new Borrower(request.name().trim(), request.email().trim()));
        return BorrowerResponse.from(borrower);
    }

    @Transactional(readOnly = true)
    public List<BorrowerHistoryResponse> list() {
        Map<UUID, List<LoanResponse>> historyByBorrowerId = new HashMap<>();
        loanRepository.findAllWithBorrowerAndBookCopyOrderByBorrowedAt().forEach(loan ->
                historyByBorrowerId.computeIfAbsent(loan.getBorrower().getId(), ignored -> new ArrayList<>())
                        .add(LoanResponse.from(loan)));
        return repository.findAll(Sort.by("name", "id")).stream()
                .map(borrower -> BorrowerHistoryResponse.from(borrower,
                        historyByBorrowerId.getOrDefault(borrower.getId(), List.of())))
                .toList();
    }
}