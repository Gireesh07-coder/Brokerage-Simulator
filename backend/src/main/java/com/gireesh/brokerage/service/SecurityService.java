package com.gireesh.brokerage.service;

import com.gireesh.brokerage.dto.PageResponse;
import com.gireesh.brokerage.dto.SecurityResponse;
import com.gireesh.brokerage.entity.Security;
import com.gireesh.brokerage.exception.ApiException;
import com.gireesh.brokerage.repository.SecurityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class SecurityService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "symbol", "companyName", "currentPrice", "exchange", "sector");

    private final SecurityRepository securityRepository;

    public SecurityService(SecurityRepository securityRepository) {
        this.securityRepository = securityRepository;
    }

    public PageResponse<SecurityResponse> search(
            String query,
            String sector,
            String sortField,
            String direction,
            int page,
            int size) {
        if (page < 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_PAGE", "Page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_PAGE_SIZE", "Page size must be between 1 and 100");
        }
        if (!SORT_FIELDS.contains(sortField)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_SORT", "Unsupported security sort field");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_SORT_DIRECTION", "Direction must be 'asc' or 'desc'");
        }

        Specification<Security> specification =
                (root, criteriaQuery, builder) -> builder.conjunction();
        if (query != null && !query.isBlank()) {
            String value = query.trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, criteriaQuery, builder) ->
                    builder.or(
                            builder.like(builder.lower(root.get("symbol")), "%" + value + "%"),
                            builder.like(builder.lower(root.get("companyName")), "%" + value + "%")));
        }
        if (sector != null && !sector.isBlank()) {
            String value = sector.trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, criteriaQuery, builder) ->
                    builder.equal(builder.lower(root.get("sector")), value));
        }

        Page<Security> results = securityRepository.findAll(
                specification,
                PageRequest.of(page, size, Sort.by(sortDirection, sortField)));

        return new PageResponse<>(
                results.getContent().stream().map(SecurityResponse::from).toList(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    public SecurityResponse getByIdOrSymbol(String identifier) {
        String value = identifier.trim();
        Security security;
        if (value.matches("\\d+")) {
            long id;
            try {
                id = Long.parseLong(value);
            } catch (NumberFormatException exception) {
                throw notFound(value);
            }
            security = securityRepository.findById(id).orElseThrow(() -> notFound(value));
        } else {
            security = securityRepository.findBySymbolIgnoreCase(value)
                    .orElseThrow(() -> notFound(value));
        }
        return SecurityResponse.from(security);
    }

    private ApiException notFound(String identifier) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "SECURITY_NOT_FOUND",
                "Security not found: " + identifier);
    }
}
