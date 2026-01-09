package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.CertificationStandard;
import com.heronix.scheduler.model.domain.CertificationStandard.RegulatorySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Certification Standards
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 18 - Compliance & Certification Standards
 */
@Repository
public interface CertificationStandardRepository extends JpaRepository<CertificationStandard, Long> {

    /**
     * Find all active standards for a state
     */
    List<CertificationStandard> findByStateCodeAndActiveTrue(String stateCode);

    /**
     * Find standards by subject area
     */
    List<CertificationStandard> findBySubjectAreaAndActiveTrue(String subjectArea);

    /**
     * Find standards by subject and state
     */
    List<CertificationStandard> findByStateCodeAndSubjectAreaAndActiveTrue(String stateCode, String subjectArea);

    /**
     * Find HQT (Highly Qualified Teacher) requirements
     */
    List<CertificationStandard> findByIsHQTRequirementTrueAndActiveTrue();

    /**
     * Find standards by regulatory source
     */
    List<CertificationStandard> findByRegulatorySourceAndActiveTrue(RegulatorySource source);

    /**
     * Find all federal standards
     */
    @Query("SELECT cs FROM CertificationStandard cs WHERE cs.stateCode = 'FEDERAL' AND cs.active = true")
    List<CertificationStandard> findAllFederalStandards();

    /**
     * Search standards by certification name
     */
    @Query("SELECT cs FROM CertificationStandard cs WHERE LOWER(cs.certificationName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND cs.active = true")
    List<CertificationStandard> searchByCertificationName(@Param("searchTerm") String searchTerm);
}
