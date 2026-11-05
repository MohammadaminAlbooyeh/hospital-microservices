package com.hospital.prescription.infrastructure.persistence;

import com.hospital.prescription.domain.model.Prescription;
import com.hospital.prescription.domain.repository.PrescriptionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface JpaPrescriptionRepository extends JpaRepository<Prescription, UUID>,
        JpaSpecificationExecutor<Prescription>, PrescriptionRepository {

    @Override
    default List<Prescription> search(UUID patientId, UUID encounterId) {
        Specification<Prescription> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (patientId != null) {
                predicates.add(criteriaBuilder.equal(root.get("patientId"), patientId));
            }
            if (encounterId != null) {
                predicates.add(criteriaBuilder.equal(root.get("encounterId"), encounterId));
            }
            if (query != null) {
                query.orderBy(criteriaBuilder.desc(root.get("issuedAt")));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(specification);
    }
}
