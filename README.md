# Quaero JPA

> **Two ways to kill JPA boilerplate: let the frontend drive the query as JSON, or build it fluently in Java. Either way, Quaero executes it.**

🌐 **Language / Idioma:** [English](README.md) · [Español](README.es.md)

[![Java](https://img.shields.io/badge/Java-8%2B-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Hibernate](https://img.shields.io/badge/Hibernate-5.x-yellow)](https://hibernate.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

---

## Table of contents

1. [Why Quaero](#1-why-quaero)
2. [How it compares](#2-how-it-compares)
3. [Three-way comparison — JPA · QueryBuilder · JSON](#3-three-way-comparison--jpa--querybuilder--json)
4. [Installation](#4-installation)
5. [Quick start](#5-quick-start)
6. [Core architecture](#6-core-architecture)
7. [Query object reference](#7-query-object-reference)
8. [Type coercion](#8-type-coercion)
9. [Portable SQL functions and GROUP BY fix](#9-portable-sql-functions-and-group-by-fix)
10. [Filter examples](#10-filter-examples)
11. [Select examples](#11-select-examples)
12. [Available operators](#12-available-operators)
13. [Tuple → nested Map](#13-tuple--nested-map)
14. [Demo app](#14-demo-app)
15. [Compatibility](#15-compatibility)
16. [License](#16-license)

---

## 1. Why Quaero?

Every data-driven application has a search screen — a grid with filterable columns,
sortable headers, pagination, maybe a date range or a status dropdown. The user clicks.
The backend has to respond.

The traditional path looks like this:

```
Sprint 1:  findByBrand(brand)
Sprint 2:  findByBrandAndFuelType(brand, fuelType)
Sprint 3:  findByBrandAndFuelTypeAndPriceBetween(brand, fuelType, min, max)
Sprint 4:  findByBrandAndFuelTypeAndPriceBetweenAndStatus(...)
           // every new filter = a new method, a new test, a new deploy
```

The slightly more sophisticated path uses the JPA Criteria API directly — composable,
typesafe, but notoriously verbose. A single filtered, paginated, multi-join query with
a GROUP BY on a formatted date takes **hundreds of lines** that all look the same,
all test the same way, and all need to be changed every time product adds a column.

**Quaero replaces all of that with a `Query` object — and gives you two ways to build it:**

**① JSON mode** — the frontend sends a JSON object describing exactly what it wants.
One universal backend endpoint handles every combination, forever. No redeploy when
requirements change. No new repository method. The product team ships UI changes
independently of the engineering team.

```
Filter form → JSON → one endpoint → QueryExecutor → Criteria API → Database
```

**② `QueryBuilder` mode** — when the query lives in Java (server-side reports, scheduled
jobs, batch exports, internal microservices), a fluent builder lets you construct the
same `Query` object programmatically with a clean chainable API — no Criteria API
boilerplate, no `if (param != null)` chains, no manual join wiring.

```java
Query q = QueryBuilder.builder("Sale")
    .select("id").as("saleId")
    .select("finalPrice").as("revenue").sum()
    .select("vehicle.trim.model.brand.name", "Inner","Inner","Inner","Inner").as("brand")
    .filterEqual("vehicle.powertrain.fuelType", "Electric")
    .filterBetween("finalPrice", 20000, 80000)
    .orderDesc("finalPrice")
    .page(0, 20)
    .build();
```

Both modes produce the same `Query` object, run through the same execution pipeline,
and benefit from every Quaero feature: portable functions, GROUP BY fix, type coercion,
nested map conversion. Pick the one that fits the context. Use both in the same project.

### What you get that pure JPA cannot give you

- A dynamic `GROUP BY` on `to_char(saleDate, 'YYYY-MM')` **that actually works** in Hibernate 5 — Hibernate has a known bug where literal arguments are stripped from the GROUP BY clause. Quaero fixes this transparently in both modes.
- Portable SQL functions (`quaero_format_date`, `quaero_trunc_date`, `quaero_lpad`…) that emit the correct native SQL on PostgreSQL, Oracle, SQL Server, H2 and SQLite — from a single query definition, JSON or Java.
- Auto-detection of your SQL engine at startup — no dialect configuration required.
- Field-to-field comparisons, arithmetic expressions, CASE/WHEN, COALESCE, correlated subqueries — composable in JSON or via the builder.
- Per-request type coercion: `"2023-01-15"` is automatically a `LocalDate` when the field needs it. No custom converters.

---

## 2. How it compares

| Capability | Pure JPA Criteria | Spring Data Specs | QueryDSL | jOOQ | **Quaero** |
|---|:---:|:---:|:---:|:---:|:---:|
| Frontend drives query at runtime — no backend changes | ❌ | ❌ | ❌ | ❌ | ✅ JSON mode |
| Dynamic filters from JSON — zero new code per combo | ❌ | ❌ | ❌ | ❌ | ✅ |
| Fluent Java builder — no Criteria API boilerplate | ❌ | ❌ | ⚠️ verbose | ⚠️ verbose | ✅ QueryBuilder |
| One API for both frontend-driven and server-side queries | ❌ | ❌ | ❌ | ❌ | ✅ |
| Recursive AND / OR nesting | ✅ verbose | ⚠️ limited | ✅ verbose | ✅ verbose | ✅ declarative |
| Field-to-field comparisons in filters | ✅ verbose | ❌ | ✅ | ✅ | ✅ |
| Correlated subqueries in filters | ✅ verbose | ❌ | ⚠️ | ✅ | ✅ |
| GROUP BY on SQL functions with literals | ⚠️ **bug H5** | ⚠️ **bug H5** | ⚠️ **bug H5** | ✅ | ✅ **fixed** |
| Portable functions across DB engines | ❌ | ❌ | ❌ | ✅ | ✅ |
| Auto-detect SQL engine at startup | ❌ | ❌ | ❌ | ✅ | ✅ |
| Per-request type coercion (STRICT / LENIENT) | ❌ | ❌ | ❌ | ❌ | ✅ |
| Tuple → nested JSON automatically | ❌ | ❌ | ❌ | ⚠️ | ✅ |
| Works with existing JPA entities as-is | ✅ | ✅ | ✅ | ⚠️ codegen | ✅ |
| No code generation step | ✅ | ✅ | ❌ | ❌ | ✅ |
| Java 8 / Spring Boot 2.x compatible | ✅ | ✅ | ✅ | ✅ | ✅ |

**The honest comparison:**

jOOQ and QueryDSL are excellent tools when a developer sits down to write a specific,
handcrafted query. They give you type-safety, IDE completion, and full SQL control —
and QueryDSL's builder is genuinely good. But both require you to know the query shape
at development time. Quaero's `QueryBuilder` covers the same server-side Java use case
with far less ceremony and zero codegen, while also solving the problem they cannot:
**when the user — not the developer — decides what to query, at runtime, through a UI.**

Spring Data Specifications are the closest built-in alternative, but they still require
a Java class per filter combination, don't handle JSON deserialization of arbitrary
filter trees, can't do GROUP BY on functions, and have no type coercion layer.

---

## 3. Three-way comparison — JPA · QueryBuilder · JSON

Each scenario shows the same query three ways:
**① Pure JPA Criteria** (what you'd write today) ·
**② Quaero `QueryBuilder`** (fluent Java, no Criteria API) ·
**③ Quaero JSON** (frontend-driven, no backend code ever).

---

### Scenario A — Filtered, paginated list with a deep join

*"Electric vehicles sold by Toyota, sorted by price descending, page 2"*

**① Pure JPA Criteria API — ~40 lines, one combination hardcoded**

```java
public List<Tuple> findSales(String brand, String fuelType, int page, int size) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<Sale> sale = cq.from(Sale.class);

    Join<?,?> vehicle    = sale.join("vehicle",       JoinType.INNER);
    Join<?,?> trim       = vehicle.join("trim",       JoinType.INNER);
    Join<?,?> model      = trim.join("model",         JoinType.INNER);
    Join<?,?> brandJoin  = model.join("brand",        JoinType.INNER);
    Join<?,?> powertrain = vehicle.join("powertrain", JoinType.INNER);

    List<Predicate> predicates = new ArrayList<>();
    if (brand != null)
        predicates.add(cb.equal(brandJoin.get("name"), brand));
    if (fuelType != null)
        predicates.add(cb.equal(powertrain.get("fuelType"), fuelType));

    cq.multiselect(sale.get("id"), sale.get("finalPrice"), brandJoin.get("name"))
      .where(predicates.toArray(new Predicate[0]))
      .orderBy(cb.desc(sale.get("finalPrice")));

    return em.createQuery(cq)
             .setFirstResult(page * size)
             .setMaxResults(size)
             .getResultList();
}
```

Every new filter means a new parameter, a new `if` block, a new test, a deploy.

**② Quaero `QueryBuilder` — 8 lines, any filter is one more chain call**

```java
Query q = QueryBuilder.builder("Sale")
    .select("id").as("saleId")
    .select("finalPrice").as("finalPrice")
    .select("vehicle.trim.model.brand.name", "Inner","Inner","Inner","Inner").as("brand")
    .filterAnd(f -> f
        .filterEqual("vehicle.trim.model.brand.name", "Toyota")
        .filterEqual("vehicle.powertrain.fuelType", "Electric"))
    .orderDesc("finalPrice")
    .page(1, 20)
    .build();
```

Readable, chainable, no Criteria API. Ideal for scheduled jobs, internal services,
and server-side report generation. Adding a new filter is one more `.filterXxx()` call.

**③ Quaero JSON — 0 backend lines, frontend controls everything**

```json
{
  "tableName": "Sale",
  "selects": [
    { "field": { "@type": "SelectSimple", "field": "id" }, "alias": "saleId" },
    { "field": { "@type": "SelectSimple", "field": "finalPrice" }, "alias": "finalPrice" },
    { "field": { "@type": "SelectSimple", "field": "vehicle.trim.model.brand.name",
                 "joinTypes": ["Inner","Inner","Inner","Inner"] }, "alias": "brand" }
  ],
  "filter": {
    "@type": "FilterArray", "operation": "and",
    "filters": [
      { "@type": "FilterSimple",
        "field": { "@type": "SelectSimple", "field": "vehicle.trim.model.brand.name" },
        "operatorType": "Eq", "value": { "@type": "SelectValue", "value": "Toyota" } },
      { "@type": "FilterSimple",
        "field": { "@type": "SelectSimple", "field": "vehicle.powertrain.fuelType" },
        "operatorType": "Eq", "value": { "@type": "SelectValue", "value": "Electric" } }
    ]
  },
  "orders": [{ "field": { "@type": "SelectSimple", "field": "finalPrice" }, "ascending": false }],
  "pageIndex": 1,
  "pageSize": 20
}
```

The frontend adds a price range? Two more elements in `filters`. Removes sorting?
Delete `orders`. The backend endpoint never changes.

---

### Scenario B — Monthly sales report with GROUP BY on a formatted date

*"Total sales and revenue grouped by month — a standard analytics endpoint"*

**① Pure JPA Criteria API — compiles, looks right, broken at runtime in Hibernate 5**

```java
// Produces invalid SQL:
//   SELECT   to_char(sale_date, 'YYYY-MM'), count(id), sum(final_price)
//   GROUP BY sale_date   ← BUG: literal stripped, function lost
//                           → database rejects: "not a GROUP BY expression"

Expression<String> month = cb.function("to_char", String.class,
    root.get("saleDate"),
    cb.literal("YYYY-MM"));      // ← Hibernate 5 drops this from GROUP BY

cq.multiselect(month, cb.count(root), cb.sum(root.<BigDecimal>get("finalPrice")))
  .groupBy(month);               // ← broken at runtime on every database
```

No clean fix in pure Hibernate 5. Workarounds involve raw JPQL strings or registering
one custom dialect function per format string. None are portable across databases.

**② Quaero `QueryBuilder` — server-side analytics job, fix applied automatically**

```java
SelectFunctionObject monthFn = new SelectFunctionObject()
    .function(QuaeroFunctions.FORMAT_DATE)
    .returnType("String")
    .params(Arrays.asList(
        new SelectSimpleObject("saleDate"),
        new SelectValueObject("YYYY-MM")
    ));

Query q = QueryBuilder.builder("Sale")
    .selectExpression(monthFn).as("month").groupBy()
    .select("id").as("totalSales").count()
    .select("finalPrice").as("revenue").sum()
    .build();
```

`QuaeroFunctionExpression` ensures both SELECT and GROUP BY emit `to_char(sale_date,
'YYYY-MM')` on PostgreSQL, `format(sale_date, 'yyyy-MM')` on SQL Server, etc.
Zero extra configuration.

**③ Quaero JSON — dashboard endpoint, grouping granularity configurable from the UI**

```json
{
  "tableName": "Sale",
  "selects": [
    {
      "field": {
        "@type": "SelectFunction",
        "function": "quaero_format_date",
        "returnType": "String",
        "params": [
          { "@type": "SelectSimple", "field": "saleDate" },
          { "@type": "SelectValue",  "value": "YYYY-MM" }
        ]
      },
      "alias": "month",
      "groupBy": true
    },
    { "field": { "@type": "SelectSimple", "field": "id" },
      "alias": "totalSales", "operatorType": "Cnt" },
    { "field": { "@type": "SelectSimple", "field": "finalPrice" },
      "alias": "revenue", "operatorType": "Sum" }
  ]
}
```

The UI can switch from `"YYYY-MM"` to `"YYYY"` (annual) or `"YYYY-Q"` (quarterly)
without touching the backend. Both modes fix the GROUP BY bug automatically.

---

### Scenario C — Field-to-field arithmetic filter

*"Sales where the final price is lower than list price minus discount"*

**① Pure JPA — technically possible, buried inside a growing method**

```java
predicates.add(
    cb.lessThan(
        sale.<BigDecimal>get("finalPrice"),
        cb.diff(sale.<BigDecimal>get("listPrice"),
                sale.<BigDecimal>get("discountAmount"))
    )
);
```

Works once, but lives inside a method that already has ten other filters and will
grow again next sprint.

**② Quaero `QueryBuilder` — one readable call**

```java
Query q = QueryBuilder.builder("Sale")
    .select("id").as("saleId")
    .select("finalPrice").as("finalPrice")
    .filterLessThan("finalPrice",
        QueryBuilder.diff("listPrice", "discountAmount"))
    .build();
```

**③ Quaero JSON — composable with any other filter, frontend-configurable**

```json
{
  "@type": "FilterSimple",
  "field": { "@type": "SelectSimple", "field": "finalPrice" },
  "operatorType": "Ls",
  "value": {
    "@type": "SelectArithmetic",
    "operation": "Diff",
    "fields": [
      { "@type": "SelectSimple", "field": "listPrice" },
      { "@type": "SelectSimple", "field": "discountAmount" }
    ]
  }
}
```

Both `field` and `value` in any filter are `ISelect` — any expression can go anywhere.

---

### Scenario D — Correlated subquery filter

*"Sales where the price is above the average for that same brand"*

**① Pure JPA — a correlated subquery, ~20 lines**

```java
Subquery<Double> avg = cq.subquery(Double.class);
Root<Sale> sub = avg.from(Sale.class);
Join<?,?> subBrand = sub.join("vehicle").join("trim").join("model").join("brand");
avg.select(cb.avg(sub.<Double>get("finalPrice")))
   .where(cb.equal(subBrand.get("name"), brandJoin.get("name")));

predicates.add(cb.greaterThan(sale.<Double>get("finalPrice"), avg));
```

**② Quaero `QueryBuilder` — one call**

```java
Query q = QueryBuilder.builder("Sale")
    .select("id").as("saleId")
    .select("finalPrice").as("finalPrice")
    .filterGreaterThanSubquery("finalPrice",
        QueryBuilder.subquery("Sale", "finalPrice", AVERAGE))
    .build();
```

**③ Quaero JSON — one filter object**

```json
{
  "@type": "FilterQuery",
  "field":        { "@type": "SelectSimple", "field": "finalPrice" },
  "operatorType": "Gr",
  "queryEntity":  "Sale",
  "queryField":   { "@type": "SelectSimple", "field": "finalPrice", "operatorType": "Avg" }
}
```

---

### Scenario E — Dynamic CASE/WHEN label in SELECT

*"Tag each sale as Premium, Mid-range or Entry based on configurable thresholds"*

**① Pure JPA — nested `selectCase()`, thresholds hardcoded, one deploy per change**

```java
cq.multiselect(
    cb.selectCase()
      .when(cb.greaterThan(sale.<BigDecimal>get("finalPrice"), new BigDecimal("60000")), "Premium")
      .when(cb.greaterThan(sale.<BigDecimal>get("finalPrice"), new BigDecimal("30000")), "Mid-range")
      .otherwise("Entry")
      .as(String.class)
);
```

**② Quaero `QueryBuilder` — thresholds are method arguments, reusable across report types**

```java
Query q = QueryBuilder.builder("Sale")
    .select("id").as("saleId")
    .selectConditional("segment")
        .when(QueryBuilder.gt("finalPrice", 60000), "Premium")
        .when(QueryBuilder.gt("finalPrice", 30000), "Mid-range")
        .otherwise("Entry")
    .build();
```

The same method can serve multiple report types with different segmentation rules
passed as parameters — no new code, no new deploy.

**③ Quaero JSON — thresholds live in the frontend, zero backend involvement**

```json
{
  "field": {
    "@type": "SelectConditional",
    "conditions": [
      {
        "condition": {
          "@type": "FilterSimple",
          "field": { "@type": "SelectSimple", "field": "finalPrice" },
          "operatorType": "Gr",
          "value": { "@type": "SelectValue", "value": 60000 }
        },
        "then": { "@type": "SelectValue", "value": "Premium" }
      },
      {
        "condition": {
          "@type": "FilterSimple",
          "field": { "@type": "SelectSimple", "field": "finalPrice" },
          "operatorType": "Gr",
          "value": { "@type": "SelectValue", "value": 30000 }
        },
        "then": { "@type": "SelectValue", "value": "Mid-range" }
      }
    ],
    "otherwise": { "@type": "SelectValue", "value": "Entry" }
  },
  "alias": "segment"
}
```

A product manager adjusts thresholds from the UI without engineering involvement.

---

## 4. Installation

```xml
<dependency>
    <groupId>io.github.tuusuario</groupId>
    <artifactId>quaero-jpa</artifactId>
    <version>1.1.0</version>
</dependency>
```

SQLite support (optional — only if you use SQLite):

```xml
<dependency>
    <groupId>com.github.gwenn</groupId>
    <artifactId>sqlite-dialect</artifactId>
    <version>0.1.3</version>
</dependency>
```

That's the only configuration required. Quaero registers its dialect resolver and
autoconfiguration automatically via `META-INF/spring.factories`.

---

## 5. Quick start

### Step 1 — Register the beans

```java
@Configuration
public class QuaeroConfiguration {

    @Autowired
    private EntityManagerFactory factory;

    @Bean
    public Map<String, EntityType<?>> entities() {
        Map<String, EntityType<?>> map = new HashMap<>();
        factory.getMetamodel().getEntities()
               .forEach(e -> map.put(e.getName(), e));
        return map;
    }

    @Bean
    public Map<String, ManagedType<?>> managedTypes() {
        Map<String, ManagedType<?>> map = new HashMap<>();
        factory.getMetamodel().getManagedTypes().stream()
               .filter(t -> t.getJavaType() != null)
               .forEach(t -> map.put(t.getJavaType().getSimpleName(), t));
        return map;
    }
}
```

### Step 2 — One universal endpoint

```java
@RestController
@RequestMapping("/api/sales")
public class SaleController {

    @Autowired
    private QueryExecutorService queryExecutorService;

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody Query query) {
        return ResponseEntity.ok(queryExecutorService.executeQuery(query));
    }
}
```

This single endpoint handles every query combination your frontend can construct —
today and in every future sprint.

### Step 3 — Call it from the frontend (Angular example)

```typescript
const query = {
  tableName: 'Sale',
  selects: [
    { field: { '@type': 'SelectSimple', field: 'id' },         alias: 'saleId'     },
    { field: { '@type': 'SelectSimple', field: 'finalPrice' }, alias: 'finalPrice' }
  ],
  filter: {
    '@type': 'FilterSimple',
    field:        { '@type': 'SelectSimple', field: 'status' },
    operatorType: 'Eq',
    value:        { '@type': 'SelectValue',  value: 'Completed' }
  },
  pageIndex: 0,
  pageSize:  25
};

this.http.post<any>('/api/sales/search', query).subscribe(result => {
  this.rows  = result.data;
  this.total = result.total;
});
```

### Step 4 — Or build it server-side with `QueryBuilder`

```java
Query q = QueryBuilder.builder("Sale")
    .coercionMode(CoercionMode.LENIENT)
    .select("id").as("saleId")
    .select("finalPrice").as("finalPrice")
    .select("vehicle.trim.model.brand.name", "Inner","Inner","Inner","Inner").as("brand")
    .filterEqual("vehicle.powertrain.fuelType", "Electric")
    .orderDesc("finalPrice")
    .page(0, 20)
    .build();
```

---

## 6. Core architecture

Quaero is built on two recursive interfaces following the **Composite Pattern**.
Both `field` and `value` in any filter are `ISelect` — enabling field-to-field
comparisons and nested expressions at any point in the tree.

```
IFilter
├── FilterSimpleObject      — field  operator  value
├── FilterArrayObject       — AND / OR of IFilter[]
└── FilterQueryObject       — field  operator  (subquery)

ISelect
├── SelectSimpleObject      — entity field by dot path  ("vehicle.brand.name")
├── SelectValueObject       — literal value
├── SelectCoalesceObject    — COALESCE(a, b, ...)
├── SelectConcatObject      — CONCAT(a, b, ...)
├── SelectConditionalObject — CASE WHEN ... THEN ... ELSE ... END
├── SelectArithmeticObject  — a ± b × c ÷ d % e
├── SelectNumericOperation  — ABS / SQRT / NEG / AVG / SUM / MAX / MIN
├── SelectSubstringObject   — SUBSTRING(field, pos [, len])
├── SelectTrimObject        — TRIM([spec] [char FROM] field)
├── SelectFunctionObject    — quaero_* portable or native SQL function  ← GROUP BY safe
└── SelectInnerSubselect    — (SELECT field FROM entity WHERE ...)
```

---

## 7. `Query` object reference

| Field | Type | Description |
|-------|------|-------------|
| `tableName` | `String` | JPA entity name — `"Sale"`, `"Customer"`, etc. |
| `tableAlias` | `String` | Optional alias for the root entity |
| `coercionMode` | `CoercionMode` | `STRICT` (default) or `LENIENT` |
| `selects` | `List<QuerySelectObject>` | Fields and expressions to SELECT |
| `filter` | `IFilter` | WHERE clause — arbitrarily nestable |
| `orders` | `List<QueryOrderObject>` | ORDER BY clauses |
| `pageIndex` | `Integer` | Zero-based page number |
| `pageSize` | `Integer` | Results per page |
| `dynamicJoins` | `List<QueryJoinObject>` | Cartesian joins — fixed main table |
| `dynamicJoinsMultiple` | `List<QueryMultiJoinObject>` | Cartesian joins — each param declares its own root |
| `paramJoinTypes` | `List<QueryJoinTypesObject>` | Override join type per path segment (INNER/LEFT/RIGHT) |
| `distinctResults` | `Boolean` | Add DISTINCT |

---

## 8. Type coercion

JSON has no native `LocalDate`, `BigDecimal`, or `LocalDateTime`. When filter values
arrive from the frontend their Java type often doesn't match the JPA field exactly.
Quaero handles this with a **per-request coercion mode** — no custom converters needed.

### STRICT (default)

Only exact type matches and safe lossless widenings are accepted.
Anything else throws `IncorrectParameterTypeException` immediately.

Safe widenings: `Integer → Long / Double / Float / BigDecimal / BigInteger` ·
`Long → Double / BigDecimal / BigInteger` · `Double → Float / BigDecimal` · `LocalDate → LocalDateTime`

Use STRICT for production endpoints where the frontend sends typed values.

### LENIENT

Best-effort conversion before failing. Adds on top of STRICT:

| From | To | Examples |
|------|----|---------|
| `String` | `LocalDate` | `"2023-01-15"` · `"15/01/2023"` · `"01/15/2023"` |
| `String` | `LocalDateTime` | `"2023-01-15T10:30:00"` · `"2023-01-15 10:30:00"` |
| `String` | `Date` | same formats as LocalDate |
| `String` | numeric types | `"42"` · `"3.14"` |
| `String` | `Boolean` | `"true"/"false"` · `"1"/"0"` · `"yes"/"no"` |
| `LocalDateTime` | `LocalDate` | truncates time component |
| `Number / Boolean` | `String` | via `toString()` |

Use LENIENT for internal tools, admin panels, and reporting dashboards.

### Exception handling

```java
@ExceptionHandler(IncorrectParameterTypeException.class)
public ResponseEntity<?> handleTypeError(IncorrectParameterTypeException ex) {
    return ResponseEntity.badRequest().body(Map.of(
        "field",    ex.getFieldName(),
        "expected", ex.getExpectedType().getSimpleName(),
        "received", ex.getActualType().getSimpleName(),
        "message",  ex.getMessage()
    ));
}
```

---

## 9. Portable SQL functions and GROUP BY fix

### The Hibernate 5 GROUP BY bug

When a function expression contains a `CriteriaBuilder.literal()` argument,
Hibernate 5 strips it from the GROUP BY clause:

```sql
-- SELECT is correct
SELECT to_char(sale_date, 'YYYY-MM'), count(id)

-- GROUP BY is broken
GROUP BY sale_date    ← should be: to_char(sale_date, 'YYYY-MM')
-- → the database rejects this with "not a GROUP BY expression"
```

Quaero fixes this transparently via `QuaeroFunctionExpression`, which overrides
Hibernate's internal `render(RenderingContext)` to emit the identical SQL fragment
in both SELECT and GROUP BY. Zero API changes — `SelectFunctionObject` applies the
fix automatically for every function call, in both JSON and `QueryBuilder` mode.

### Auto-detection of the SQL engine

Quaero registers a Hibernate `DialectResolver` via `spring.factories`.
No `application.properties` entry needed.

| Database | Dialect |
|----------|---------|
| PostgreSQL / CockroachDB | `CustomPostgreSQLDialect` |
| Oracle | `CustomOracleDialect` |
| Microsoft SQL Server | `CustomSQLServerDialect` |
| H2 | `CustomH2Dialect` |
| SQLite | `CustomSQLiteDialect` |

To pin a specific dialect:
```properties
spring.jpa.properties.hibernate.dialect=quaero.dialect.impl.CustomPostgreSQLDialect
```

### Portable `quaero_*` function names

Use these in `SelectFunctionObject.function` for queries that work unchanged on
any supported engine.

#### Date / time

| Function | Arguments | Maps to |
|----------|-----------|---------|
| `quaero_format_date` | `(date, format)` | `to_char` · `format` · `formatdatetime` · `strftime` |
| `quaero_trunc_date` | `(date, unit)` | `date_trunc` · `trunc` · `datetrunc` · `date(...,'start of')` |
| `quaero_date_part` | `(unit, date)` | `date_part` · `extract` · `datepart` · `strftime` |
| `quaero_date_add` | `(date, n, unit)` | `+ interval` · `add_months` · `dateadd` · `datetime` |
| `quaero_date_diff` | `(unit, d1, d2)` | `age` · `months_between` · `datediff` · `julianday` |
| `quaero_now` | `()` | `now()` · `sysdate` · `getdate()` · `datetime('now')` |

**Date units:** `year` · `quarter` · `month` · `week` · `day` · `hour` · `minute` · `second`

**Format tokens** (translated automatically per engine):

| Token | Meaning | PG / Oracle | SQL Server | H2 | SQLite |
|-------|---------|-------------|------------|-----|--------|
| `YYYY` | 4-digit year | `YYYY` | `yyyy` | `yyyy` | `%Y` |
| `MM` | Month 01-12 | `MM` | `MM` | `MM` | `%m` |
| `DD` | Day 01-31 | `DD` | `dd` | `dd` | `%d` |
| `HH24` | Hour 24h | `HH24` | `HH` | `HH` | `%H` |
| `MI` | Minutes | `MI` | `mm` | `mm` | `%M` |
| `SS` | Seconds | `SS` | `ss` | `ss` | `%S` |
| `MONTH` | Full month name | `MONTH` | `MMMM` | `MMMM` | `%B` |
| `MON` | Abbrev. month | `MON` | `MMM` | `MMM` | `%b` |
| `DAY` / `DY` | Day name | `DAY` / `DY` | `dddd` / `ddd` | `EEEE` / `EEE` | `%A` / `%a` |

#### String

| Function | Arguments | Not available on |
|----------|-----------|-----------------|
| `quaero_lpad` | `(str, len, pad)` | — |
| `quaero_rpad` | `(str, len, pad)` | — |
| `quaero_initcap` | `(str)` | ⚠️ first char only on SS / H2 / SQLite |
| `quaero_replace` | `(str, from, to)` | — |
| `quaero_regexp_replace` | `(str, pattern, repl)` | SQL Server · SQLite |
| `quaero_instr` | `(haystack, needle)` | — |

#### Numeric

| Function | Arguments | Description |
|----------|-----------|-------------|
| `quaero_round` | `(value, precision)` | Round to N decimal places |
| `quaero_trunc_number` | `(value, precision)` | Truncate without rounding |
| `quaero_log` | `(value, base)` | Logarithm in base N |

### Compatibility matrix

| | PG | Oracle | SQL Server | H2 | SQLite |
|---|:---:|:---:|:---:|:---:|:---:|
| `quaero_format_date` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `quaero_trunc_date` | ✅ | ✅ | ✅ *(2022+)* | ✅ *(2.0+)* | ✅ |
| `quaero_date_part` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `quaero_date_add` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `quaero_date_diff` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `quaero_now` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `quaero_lpad` / `rpad` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `quaero_replace` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `quaero_regexp_replace` | ✅ | ✅ | ❌ | ✅ | ❌ |
| `quaero_instr` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `quaero_round` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `quaero_trunc_number` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `quaero_log` | ✅ | ✅ | ✅ | ✅ | ✅ |
| GROUP BY fix | ✅ | ✅ | ✅ | ✅ | ✅ |
| Auto-detection | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 10. Filter examples

### Simple equality

```json
{
  "@type": "FilterSimple",
  "field":        { "@type": "SelectSimple", "field": "status" },
  "operatorType": "Eq",
  "value":        { "@type": "SelectValue",  "value": "Active" }
}
```

### AND / OR nesting

```json
{
  "@type": "FilterArray", "operation": "or",
  "filters": [
    {
      "@type": "FilterArray", "operation": "and",
      "filters": [
        { "@type": "FilterSimple",
          "field": { "@type": "SelectSimple", "field": "brand.name" },
          "operatorType": "Eq", "value": { "@type": "SelectValue", "value": "Toyota" } },
        { "@type": "FilterSimple",
          "field": { "@type": "SelectSimple", "field": "powertrain.fuelType" },
          "operatorType": "Eq", "value": { "@type": "SelectValue", "value": "Hybrid" } }
      ]
    },
    {
      "@type": "FilterSimple",
      "field": { "@type": "SelectSimple", "field": "finalPrice" },
      "operatorType": "LsEq",
      "value": { "@type": "SelectValue", "value": 30000 }
    }
  ]
}
```

### IN with list

```json
{
  "@type": "FilterSimple",
  "field":        { "@type": "SelectSimple", "field": "customer.state" },
  "operatorType": "In",
  "value":        { "@type": "SelectValue",  "value": ["CA","NY","TX"] }
}
```

### BETWEEN (range)

```json
{
  "@type": "FilterArray", "operation": "and",
  "filters": [
    { "@type": "FilterSimple",
      "field": { "@type": "SelectSimple", "field": "finalPrice" },
      "operatorType": "GrEq", "value": { "@type": "SelectValue", "value": 20000 } },
    { "@type": "FilterSimple",
      "field": { "@type": "SelectSimple", "field": "finalPrice" },
      "operatorType": "LsEq",    "value": { "@type": "SelectValue", "value": 40000 } }
  ]
}
```

### Subquery — field > AVG

```json
{
  "@type": "FilterQuery",
  "field":        { "@type": "SelectSimple", "field": "finalPrice" },
  "operatorType": "Gr",
  "queryEntity":  "Sale",
  "queryField":   { "@type": "SelectSimple", "field": "finalPrice", "operatorType": "Avg" }
}
```

### Filter on a specific root in a multi-root query

```json
{
  "@type": "FilterSimple",
  "entityAlias":  "fin",
  "field":        { "@type": "SelectSimple", "field": "lender" },
  "operatorType": "Eq",
  "value":        { "@type": "SelectValue",  "value": "Tesla Financing" }
}
```

---

## 11. Select examples

### Simple field

```json
{ "field": { "@type": "SelectSimple", "field": "finalPrice" }, "alias": "price" }
```

### GROUP BY + aggregate

```json
{ "field": { "@type": "SelectSimple", "field": "brand.name" }, "alias": "brand", "groupBy": true },
{ "field": { "@type": "SelectSimple", "field": "id" }, "alias": "units", "operatorType": "Cnt" },
{ "field": { "@type": "SelectSimple", "field": "finalPrice" }, "alias": "revenue", "operatorType": "SUMMATORY" }
```

### Portable function — GROUP BY safe, any engine

```json
{
  "field": {
    "@type": "SelectFunction",
    "function": "quaero_format_date",
    "returnType": "String",
    "params": [
      { "@type": "SelectSimple", "field": "saleDate" },
      { "@type": "SelectValue",  "value": "YYYY-MM" }
    ]
  },
  "alias": "month",
  "groupBy": true
}
```

### COALESCE

```json
{
  "field": {
    "@type": "SelectCoalesce",
    "values": [
      { "@type": "SelectSimple", "field": "tradeInValue" },
      { "@type": "SelectValue",  "value": 0 }
    ]
  },
  "alias": "tradeIn"
}
```

### CASE / WHEN

```json
{
  "field": {
    "@type": "SelectConditional",
    "conditions": [
      { "condition": { "@type": "FilterSimple",
          "field": { "@type": "SelectSimple", "field": "finalPrice" },
          "operatorType": "Gr", "value": { "@type": "SelectValue", "value": 50000 } },
        "then": { "@type": "SelectValue", "value": "Premium" }
      }
    ],
    "otherwise": { "@type": "SelectValue", "value": "Standard" }
  },
  "alias": "segment"
}
```

### Arithmetic

```json
{
  "field": {
    "@type": "SelectArithmetic",
    "operation": "Diff",
    "fields": [
      { "@type": "SelectSimple", "field": "finalPrice" },
      { "@type": "SelectSimple", "field": "discountAmount" }
    ]
  },
  "alias": "netPrice"
}
```

### CONCAT

```json
{
  "field": {
    "@type": "SelectConcat",
    "values": [
      { "@type": "SelectSimple", "field": "customer.firstName", "joinTypes": ["Inner"] },
      { "@type": "SelectValue",  "value": " " },
      { "@type": "SelectSimple", "field": "customer.lastName" }
    ]
  },
  "alias": "fullName"
}
```

---

## 12. Available operators

### Filter operators (`operatorType` in `FilterSimple`)

| Operator | JSON Value | SQL equivalent |
|----------|------------|----------------|
| `EQUAL` | `Eq` | `= ?` |
| `NOT_EQUAL` | `NtEq` | `<> ?` |
| `GREATER_THAN` | `Gr` | `> ?` |
| `GREATER_THAN_OR_EQUAL` | `GrEq` | `>= ?` |
| `LESS_THAN` | `Ls` | `< ?` |
| `LESS_THAN_OR_EQUAL` | `LsEq` | `<= ?` |
| `IS_NULL` | `Nl` | `field IS NULL` |
| `IS_NOT_NULL` | `NtNl` | `field IS NOT NULL` |
| `IS_EMPTY` | `Emp` | `field = ''` |
| `IS_NOT_EMPTY` | `NtEmp` | `field <> ''` |
| `LIKE` | `Lk` | `UPPER(field) LIKE UPPER(?)` |
| `NOT_LIKE` | `NtLk` | `UPPER(field) NOT LIKE UPPER(?)` |
| `LIKE_TRIM` | `LkTr` | LIKE with trim on value |
| `NOT_LIKE_TRIM` | `NtLkTr` | NOT LIKE with trim on value |
| `IN` | `In` | `field IN (?)` |
| `NOT_IN` | `NtIn` | `field NOT IN (?)` |
| `IN_TRIM` | `InTr` | IN with trim on values |
| `NOT_IN_TRIM` | `NtInTr` | NOT IN with trim on values |

### Select / aggregate operators (`operatorType` in `QuerySelectObject` / `QueryOrderObject`)

| Operator | JSON Value | SQL equivalent |
|----------|------------|---------------|
| `COUNT` | `Cnt` | `COUNT(field)` |
| `COUNT_DISTINCT` | `CntDst` | `COUNT(DISTINCT field)` |
| `SUMMATORY`  | `Sum` | `SUM(field)` |
| `AVERAGE`  | `Avg` | `AVG(field)` |
| `MAX` | `Max` | `MAX(field)` |
| `MIN` | `Min` | `MIN(field)` |

### Arithmetic operations (`operation` in `SelectArithmetic`)

| Operator | JSON Value | SQL equivalent |
|----------|------------|----------------|
| `SUMMATION` | `Sum` | `? + ?` |
| `DIFFERENCE` | `Diff` | `? - ?` |
| `MULTIPLY` | `Prod` | `? x ?` |
| `DIVISION` | `Div` | `? / ?` |
| `MOD` | `Mod` | `? % ?` |


### Numeric unary operations (`operation` in `SelectNumericOperation`)

| Operator | JSON Value | SQL equivalent |
|----------|------------|----------------|
| `AVERAGE` | `Avg` | `AVG(?) |
| `ABSOLUTE` | `Abs` | `ABS(?)` |
| `SQUARE_ROOT` | `Sqrt` | `SQRT(?)` |
| `MAX` | `Max` | `MAX(?)` |
| `MIN` | `Min` | `MIN(?)` |
| `NEGATION` | `Not` | `NOT(?)` |
| `SUM` | `Sum` | `SUM(?)` |

### Type converters (`converterType` in `SelectSimple`)

| Operator | JSON Value |
|----------|------------|
| `BIG_INTEGER` | `BInt` |
| `BIG_DECIMAL` | `BDec` |
| `DOUBLE` | `Dbl` |
| `FLOAT` | `Fl` |
| `LONG` | `Lng` |
| `INTEGER` | `Int` |
| `STRING` | `Str` |

---

## 13. Tuple → nested Map

Aliases with dots (`"brand.name"`, `"brand.country.isoCode"`) are automatically
converted to nested Maps, ready to send directly to the frontend:

```java
List<Map<String, Object>> result = QueryUtils.tupleToMapList(typedQuery.getResultList());
```

```json
{
  "brand": {
    "name": "Toyota",
    "country": { "isoCode": "JP" }
  },
  "finalPrice": 34500
}
```

---

## 14. Demo app

A fully working Spring Boot demo — car dealership data, H2 in-memory, 750 sales,
825 vehicles, 220 customers — is available at
[`quaero-demo`](https://github.com/dementhius/quaero-demo).

**15 pre-built scenarios** covering every Quaero feature, callable directly from Postman:

```bash
cd quaero-demo && mvn spring-boot:run

curl http://localhost:8080/api/demo/list
curl -X POST http://localhost:8080/api/demo/03-filter-and-or
curl http://localhost:8080/api/demo/10-subquery/query
```

H2 console: `http://localhost:8080/h2-console`

---

## 15. Compatibility

| Dependency | Version |
|-----------|---------|
| Java | 8+ |
| Spring Boot | 2.x |
| Spring Data JPA | 2.1+ |
| Hibernate | 5.x |
| javax.persistence-api | 2.2 |
| Jackson | 2.13+ |
| Apache Commons Lang3 | 3.x |

> **Spring Boot 3.x / Hibernate 6:** replace `javax.persistence` imports with
> `jakarta.persistence`. The Hibernate 5 GROUP BY fix is not needed in Hibernate 6 —
> `SelectFunctionObject` detects this automatically and falls back to the standard Criteria API.

---

## 16. License

Distributed under the **Apache License 2.0**.
See [LICENSE](LICENSE) or https://www.apache.org/licenses/LICENSE-2.0
