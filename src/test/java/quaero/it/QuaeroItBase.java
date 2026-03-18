/*
 * Copyright 2026 Dementhius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package quaero.it;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import quaero.QueryExecutor;
import quaero.it.entity.BrandEntity;
import quaero.it.entity.ProductEntity;
import quaero.query.Query;
import quaero.utils.QueryUtils;

/**
 * Base class for all integration tests.
 *
 * <p>Provides a shared Spring Boot context (H2 + Hibernate + Quaero beans),
 * inserts three products across two brands before each test, and rolls back
 * the transaction afterwards so every test starts with a clean state.
 *
 * <pre>
 * Brands  : Toyota, Honda
 * Products: Corolla (200, active, Toyota)
 *           Prius   (350, active, Toyota)
 *           Civic   (250, inactive, Honda)
 * </pre>
 */
@SpringBootTest(classes = QuaeroTestApplication.class)
@Transactional
public abstract class QuaeroItBase {

    @Autowired
    protected QueryExecutor queryExecutor;

    @PersistenceContext
    protected EntityManager em;

    protected BrandEntity toyota;
    protected BrandEntity honda;
    protected ProductEntity corolla;
    protected ProductEntity prius;
    protected ProductEntity civic;

    @BeforeEach
    void setupData() {
        toyota  = new BrandEntity("Toyota");
        honda   = new BrandEntity("Honda");
        em.persist(toyota);
        em.persist(honda);

        corolla = new ProductEntity("Corolla", new BigDecimal("200.00"), Boolean.TRUE,  toyota);
        prius   = new ProductEntity("Prius",   new BigDecimal("350.00"), Boolean.TRUE,  toyota);
        civic   = new ProductEntity("Civic",   new BigDecimal("250.00"), Boolean.FALSE, honda);
        em.persist(corolla);
        em.persist(prius);
        em.persist(civic);

        em.flush(); // make rows visible within this transaction
    }

    // ─── Execution helpers ────────────────────────────────────────────────────

    protected List<Tuple> execute(final Query q) {
        return queryExecutor.doQuery(q).getResultList();
    }

    protected List<Map<String, Object>> executeAsMap(final Query q) {
        return QueryUtils.tupleToMapList(execute(q));
    }
}
