package com.cobrother.web.Repository;

import com.cobrother.web.model.becobrother.BeCobrother;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeCoBrotherRepository extends JpaRepository<BeCobrother, Long> {
}
