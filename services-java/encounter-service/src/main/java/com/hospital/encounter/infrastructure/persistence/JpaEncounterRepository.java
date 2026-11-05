package com.hospital.encounter.infrastructure.persistence;

import com.hospital.encounter.domain.model.Encounter;
import com.hospital.encounter.domain.model.EncounterState;
import com.hospital.encounter.domain.repository.EncounterRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface JpaEncounterRepository extends JpaRepository<Encounter, UUID>,
        JpaSpecificationExecutor<Encounter>, EncounterRepository {

    @Override
    default List<Encounter> search(UUID patientId, UUID doctorId, EncounterState state) {
        Specification<Encounter> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (patientId != null) {
                predicates.add(criteriaBuilder.equal(root.get("patientId"), patientId));
            }
            if (doctorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("doctorId"), doctorId));
            }
            if (state != null) {
                predicates.add(criteriaBuilder.equal(root.get("state"), state));
            }
            if (query != null) {
                query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(specification);
    }
}
