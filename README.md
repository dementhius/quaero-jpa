# Quaero JPA

**Dynamic query builder for Hibernate/JPA — from JSON to Criteria API**

🌐 **Language / Idioma:** [English](README.md) · [Español](README.es.md)

[![Java](https://img.shields.io/badge/Java-8%2B-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-brightgreen)](https://spring.io/projects/spring-boot)
[![JPA](https://img.shields.io/badge/JPA-2.2-orange)](https://jakarta.ee/specifications/persistence/)
[![Hibernate](https://img.shields.io/badge/Hibernate-5.x-yellow)](https://hibernate.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

---

## The problem it solves

In any application with a data grid, filters arrive from the frontend as JSON. The traditional approach forces you to write a different repository method for each combination of filters, handle optional parameters manually, and duplicate SQL logic in multiple places.

**Quaero lets your frontend send a `Query` object describing what it wants — and the library builds and executes the JPA Criteria query automatically.**

```
Angular filter form → JSON → Jackson → Quaero Query → Criteria API → JPA → Database
```

No extra repositories. No conditional `if (filter != null)`. No duplicated SQL.

---

## Features

- **Recursive filter composition** — AND/OR groups nested to any depth
- **Field-to-field comparison** — not limited to field-vs-literal; any expression can be a filter value
- **Correlated subqueries** in filters (`FilterQuery`)
- **All standard SQL operators** — EQUAL, LIKE, IN, BETWEEN (GTE+LTE), IS NULL, GREATER_THAN, etc.
- **Aggregates** — COUNT, SUM, AVG, MAX, MIN in both SELECT and ORDER BY
- **String functions** — CONCAT, TRIM, SUBSTRING
- **Arithmetic expressions** — SUM, DIFF, MULTIPLY, DIVISION, MOD across multiple fields
- **COALESCE** and **CASE/WHEN** expressions in SELECT
- **Native SQL function calls** via `SelectFunction`
- **Scalar subselects** in SELECT (`SelectInnerSubselect`)
- **Manual JPA joins** with custom ON conditions (`QueryJoinObject`)
- **Multi-root Cartesian joins** with per-param main table (`QueryMultiJoinObject`)
- **Configurable join types** per path segment (INNER, LEFT, RIGHT)
- **Pagination** — pageIndex + pageSize
- **GROUP BY** — mark any select field as `groupBy: true`
- **DISTINCT** results
- **Tuple → nested Map** conversion — dot aliases become nested JSON automatically
- **Per-request type coercion** — `STRICT` (default) or `LENIENT` mode per query
- **Compatible with Java 8** and Spring Boot 2.x / `javax.persistence`

---

## Installation

> **Note:** Quaero is not yet published to Maven Central. Install it locally first:
>
> ```bash
> git clone https://github.com/dementhius/quaero-jpa.git
> cd quaero-jpa
> mvn clean install -DskipTests
> ```

Then add it to your project's `pom.xml`:

```xml
<dependency>
    <groupId>io.github.dementhius</groupId>
    <artifactId>quaero-jpa</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Quick start

### 1. Register the required beans

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

### 2. Inject `QueryExecutor` and execute

```java
@Autowired
private QueryExecutor queryExecutor;

public List<Map<String, Object>> search(Query query) {
    TypedQuery<Tuple> typedQuery = queryExecutor.generateCustomQuery(query);
    return QueryUtils.tupleToMapList(typedQuery.getResultList());
}
```

### 3. Send a `Query` from the frontend

```json
{
  "tableName": "Sale",
  "coercionMode": "STRICT",
  "selects": [
    { "field": { "@type": "SelectSimple", "field": "id" },        "alias": "saleId"     },
    { "field": { "@type": "SelectSimple", "field": "finalPrice" }, "alias": "finalPrice" },
    { "field": { "@type": "SelectSimple",
                 "field": "vehicle.trim.model.brand.name",
                 "joinTypes": ["Inner","Inner","Inner","Inner"] }, "alias": "brand"      }
  ],
  "filter": {
    "@type": "FilterSimple",
    "field":        { "@type": "SelectSimple", "field": "vehicle.powertrain.fuelType" },
    "operatorType": "Eq",
    "value":        { "@type": "SelectValue",  "value": "Electric" }
  },
  "orders": [
    { "field": { "@type": "SelectSimple", "field": "finalPrice" }, "ascending": false }
  ],
  "pageIndex": 0,
  "pageSize": 20
}
```

### 4. Or build it programmatically with `QueryBuilder`

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

## Core architecture

Quaero is built on two recursive interfaces that follow the **Composite Pattern**:

```
IFilter
├── FilterSimpleObject     — single condition (field operator value)
├── FilterArrayObject      — AND / OR group of IFilter[]
└── FilterQueryObject      — field compared against a subquery

ISelect
├── SelectSimpleObject     — entity field by path ("brand.name")
├── SelectValueObject      — literal value
├── SelectCoalesceObject   — COALESCE(expr1, expr2, ...)
├── SelectConcatObject     — CONCAT(expr1, expr2, ...)
├── SelectConditionalObject — CASE WHEN ... THEN ... ELSE ... END
├── SelectArithmeticObject — expr1 ±×÷% expr2 ... exprN
├── SelectNumericOperation — ABS, SQRT, NEG, AVG, SUM, MAX, MIN (single field)
├── SelectSubstringObject  — SUBSTRING(field, pos[, len])
├── SelectTrimObject       — TRIM([spec] [char FROM] field)
├── SelectFunctionObject   — any native SQL function
└── SelectInnerSubselect   — (SELECT field FROM entity WHERE ...)
```

Both `field` and `value` in a filter are of type `ISelect`, which means you can do **field-to-field comparisons** like `price > discountedPrice`.

---

## `Query` object reference

| Field | Type | Description |
|-------|------|-------------|
| `tableName` | `String` | JPA entity name (e.g. `"Sale"`) |
| `tableAlias` | `String` | Optional alias for the root entity |
| `coercionMode` | `CoercionMode` | `STRICT` (default) or `LENIENT` — see section below |
| `selects` | `List<QuerySelectObject>` | Fields to SELECT |
| `filter` | `IFilter` | WHERE clause (can be deeply nested) |
| `orders` | `List<QueryOrderObject>` | ORDER BY |
| `pageIndex` | `Integer` | Zero-based page number |
| `pageSize` | `Integer` | Results per page |
| `dynamicJoins` | `List<QueryJoinObject>` | Cartesian joins with fixed main table |
| `dynamicJoinsMultiple` | `List<QueryMultiJoinObject>` | Cartesian joins where each param declares its own main table |
| `paramJoinTypes` | `List<QueryJoinTypesObject>` | Override join type (INNER/LEFT/RIGHT) per path |
| `distinctResults` | `Boolean` | Add DISTINCT |

---

## Type coercion

When filter values arrive from the frontend as JSON, their Java types often don't match the JPA field type exactly — for example, a date might come as a `String` like `"2023-01-15"` instead of a `LocalDate`, or a price as `Integer` instead of `BigDecimal`.

Quaero handles this through a **per-request coercion mode** set on the `Query` object.

### STRICT (default)

Only exact type matches and safe lossless widening promotions are allowed. Any other mismatch throws `IncorrectParameterTypeException` immediately.

Safe widenings allowed in STRICT mode: `Integer → Long / Double / Float / BigDecimal / BigInteger`, `Long → Double / BigDecimal / BigInteger`, `Double → Float / BigDecimal`, `LocalDate → LocalDateTime`.

```json
{ "tableName": "Sale", "coercionMode": "STRICT", ... }
```

Use STRICT for production endpoints where the frontend is expected to send correctly typed values.

### LENIENT

Best-effort automatic conversion is attempted before failing. Covers all STRICT widenings plus:

| From | To | Example |
|------|----|---------|
| `String` | `LocalDate` | `"2023-01-15"`, `"15/01/2023"`, `"01/15/2023"` |
| `String` | `LocalDateTime` | `"2023-01-15T10:30:00"`, `"2023-01-15 10:30:00"` |
| `String` | `Date` | same date formats as LocalDate |
| `String` | `Integer / Long / Double / Float / BigDecimal / BigInteger` | `"42"`, `"3.14"` |
| `String` | `Boolean` | `"true"/"false"`, `"1"/"0"`, `"yes"/"no"` |
| `LocalDateTime` | `LocalDate` | truncates time |
| `Number / Boolean` | `String` | via `toString()` (e.g. for LIKE on stored numbers) |

If the conversion is not possible even in LENIENT mode, `IncorrectParameterTypeException` is still thrown.

```json
{ "tableName": "Sale", "coercionMode": "LENIENT", ... }
```

Use LENIENT for internal dashboards, admin tools, or search endpoints where flexible input is desirable.

### Mixing modes per request

Both modes are set at the `Query` level, so different API endpoints can use different strategies:

```java
// Validated production form → enforce types
@PostMapping("/sales/search")
public ResponseEntity<?> search(@RequestBody Query query) {
    query.setCoercionMode(CoercionMode.STRICT); // force regardless of what frontend sent
    return ResponseEntity.ok(executor.search(query));
}

// Internal admin quick-filter → be tolerant
@PostMapping("/admin/search")
public ResponseEntity<?> adminSearch(@RequestBody Query query) {
    // let frontend decide, defaulting to STRICT if not set
    return ResponseEntity.ok(executor.search(query));
}
```

Or from the fluent builder:

```java
Query q = QueryBuilder.builder("Sale")
    .coercionMode(CoercionMode.LENIENT)
    .select("saleDate").as("date")
    .filterEqual("saleDate", "15/01/2023")   // String → LocalDate automatically
    .filterBetween("finalPrice", "30000", "55000") // String → BigDecimal automatically
    .build();
```

### Exception structure

`IncorrectParameterTypeException` carries the full context needed to return a useful error to the client:

```java
@ExceptionHandler(IncorrectParameterTypeException.class)
public ResponseEntity<?> handleTypeError(IncorrectParameterTypeException ex) {
    return ResponseEntity.badRequest().body(Map.of(
        "field",    ex.getFieldName(),    // e.g. "saleDate"
        "expected", ex.getExpectedType().getSimpleName(), // e.g. "LocalDate"
        "received", ex.getActualType().getSimpleName(),   // e.g. "String"
        "message",  ex.getMessage()
    ));
}
```

---

## Filter examples

### Simple equality

```json
{
  "@type": "FilterSimple",
  "field":        { "@type": "SelectSimple", "field": "status" },
  "operatorType": "EQUAL",
  "value":        { "@type": "SelectValue",  "value": "Active" }
}
```

### AND / OR nesting

```json
{
  "@type": "FilterArray",
  "operation": "or",
  "filters": [
    {
      "@type": "FilterArray", "operation": "and",
      "filters": [
        { "@type": "FilterSimple", "field": {"@type":"SelectSimple","field":"brand.name"},
          "operatorType": "EQUAL", "value": {"@type":"SelectValue","value":"Toyota"} },
        { "@type": "FilterSimple", "field": {"@type":"SelectSimple","field":"powertrain.fuelType"},
          "operatorType": "EQUAL", "value": {"@type":"SelectValue","value":"Hybrid"} }
      ]
    },
    {
      "@type": "FilterSimple",
      "field":        { "@type": "SelectSimple", "field": "finalPrice" },
      "operatorType": "LESS_THAN_OR_EQUAL",
      "value":        { "@type": "SelectValue",  "value": 30000 }
    }
  ]
}
```

### IN with list

```json
{
  "@type": "FilterSimple",
  "field":        { "@type": "SelectSimple", "field": "customer.state" },
  "operatorType": "IN",
  "value":        { "@type": "SelectValue",  "value": ["CA","NY","TX"] }
}
```

### Subquery — field > (SELECT AVG(...))

```json
{
  "@type": "FilterQuery",
  "field":        { "@type": "SelectSimple", "field": "finalPrice" },
  "operatorType": "GREATER_THAN",
  "queryEntity":  "Sale",
  "queryField":   { "@type": "SelectSimple", "field": "finalPrice", "operatorType": "AVERAGE" }
}
```

### Filter on a specific root in a multi-root query

When using `dynamicJoins` or `dynamicJoinsMultiple`, use `entityAlias` (or `entityName`) to target a specific root:

```json
{
  "@type": "FilterSimple",
  "entityAlias":  "fin",
  "field":        { "@type": "SelectSimple", "field": "lender" },
  "operatorType": "EQUAL",
  "value":        { "@type": "SelectValue",  "value": "Tesla Financing" }
}
```

---

## Select examples

### GROUP BY + aggregate

```json
{ "field": { "@type": "SelectSimple", "field": "brand.name" }, "alias": "brand", "groupBy": true },
{ "field": { "@type": "SelectSimple", "field": "id" }, "alias": "unitsSold", "operatorType": "COUNT" }
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
  "alias": "tradeInOrZero"
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

### Arithmetic

```json
{
  "field": {
    "@type": "SelectArithmetic",
    "operation": "DIFFERENCE",
    "fields": [
      { "@type": "SelectSimple", "field": "finalPrice" },
      { "@type": "SelectSimple", "field": "discountAmount" }
    ]
  },
  "alias": "netPrice"
}
```

### Native SQL function

```json
{
  "field": {
    "@type": "SelectFunction",
    "function": "to_char",
    "returnType": "java.lang.String",
    "params": [
      { "@type": "SelectSimple", "field": "saleDate" },
      { "@type": "SelectValue",  "value": "YYYY-MM" }
    ]
  },
  "alias": "saleMonth"
}
```

---

## Available operators

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

## Tuple → nested Map

Results where aliases contain dots (e.g. `"brand.name"`, `"brand.country.isoCode"`) are automatically converted into nested Maps:

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


## Compatibility

| Dependency | Version |
|-----------|---------|
| Java | 8+ |
| Spring Boot | 2.x |
| Spring Data JPA | 2.1+ |
| Hibernate | 5.x |
| javax.persistence-api | 2.2 |
| Jackson | 2.13+ |
| Apache Commons Lang3 | 3.x |

> For Spring Boot 3.x / Jakarta EE, change `javax.persistence` imports to `jakarta.persistence` in your consuming project.

---

## License

Distributed under the **Apache License, Version 2.0**.
See the [LICENSE](LICENSE) file for the full text, or visit https://www.apache.org/licenses/LICENSE-2.0