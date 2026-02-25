# Quaero JPA

**Constructor de consultas dinámicas para Hibernate/JPA — de JSON a Criteria API**

🌐 **Language / Idioma:** [English](README.md) · [Español](README.es.md)

[![Java](https://img.shields.io/badge/Java-8%2B-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-brightgreen)](https://spring.io/projects/spring-boot)
[![JPA](https://img.shields.io/badge/JPA-2.2-orange)](https://jakarta.ee/specifications/persistence/)
[![Hibernate](https://img.shields.io/badge/Hibernate-5.x-yellow)](https://hibernate.org/)
[![License](https://img.shields.io/badge/Licencia-Apache%202.0-blue)](LICENSE)

---

## El problema que resuelve

En cualquier aplicación con una cuadrícula de datos, los filtros llegan del frontend como JSON. El enfoque tradicional obliga a escribir un método de repositorio diferente para cada combinación de filtros, gestionar parámetros opcionales manualmente y duplicar la lógica SQL en múltiples sitios.

**Quaero permite que el frontend envíe un objeto `Query` describiendo lo que necesita — y la librería construye y ejecuta la consulta JPA Criteria automáticamente.**

```
Formulario Angular → JSON → Jackson → Quaero Query → Criteria API → JPA → Base de datos
```

Sin repositorios extra. Sin condicionales `if (filtro != null)`. Sin SQL duplicado.

---

## Funcionalidades

- **Composición recursiva de filtros** — grupos AND/OR anidables a cualquier profundidad
- **Comparación campo a campo** — no limitado a campo-vs-literal; cualquier expresión puede ser el valor de un filtro
- **Subconsultas correlacionadas** en filtros (`FilterQuery`)
- **Todos los operadores SQL estándar** — EQUAL, LIKE, IN, BETWEEN (GTE+LTE), IS NULL, GREATER_THAN, etc.
- **Funciones de agregación** — COUNT, SUM, AVG, MAX, MIN tanto en SELECT como en ORDER BY
- **Funciones de cadena** — CONCAT, TRIM, SUBSTRING
- **Expresiones aritméticas** — SUMA, DIFERENCIA, MULTIPLICACIÓN, DIVISIÓN, MOD entre múltiples campos
- **COALESCE** y **CASE/WHEN** en SELECT
- **Llamadas a funciones SQL nativas** mediante `SelectFunction`
- **Subconsultas escalares** en SELECT (`SelectInnerSubselect`)
- **Joins manuales JPA** con condiciones ON personalizadas (`QueryJoinObject`)
- **Joins cartesianos multi-raíz** con tabla principal por parámetro (`QueryMultiJoinObject`)
- **Tipo de join configurable** por segmento de ruta (INNER, LEFT, RIGHT)
- **Paginación** — pageIndex + pageSize
- **GROUP BY** — marcar cualquier campo del select como `groupBy: true`
- **DISTINCT**
- **Conversión Tuple → Map anidado** — los alias con puntos se convierten en JSON anidado automáticamente
- **Coerción de tipos por petición** — modo `STRICT` (por defecto) o `LENIENT` configurable en cada query
- **Compatible con Java 8** y Spring Boot 2.x / `javax.persistence`

---

## Instalación

> **Nota:** Quaero todavía no está publicado en Maven Central. Instálalo localmente primero:
>
> ```bash
> git clone https://github.com/dementhius/quaero-jpa.git
> cd quaero-jpa
> mvn clean install -DskipTests
> ```

Luego añádelo al `pom.xml` de tu proyecto:

```xml
<dependency>
    <groupId>io.github.dementhius</groupId>
    <artifactId>quaero-jpa</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Inicio rápido

### 1. Registrar los beans necesarios

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

### 2. Inyectar `QueryExecutor` y ejecutar

```java
@Autowired
private QueryExecutor queryExecutor;

public List<Map<String, Object>> buscar(Query query) {
    TypedQuery<Tuple> typedQuery = queryExecutor.generateCustomQuery(query);
    return QueryUtils.tupleToMapList(typedQuery.getResultList());
}
```

### 3. Enviar una `Query` desde el frontend

```json
{
  "tableName": "Sale",
  "coercionMode": "STRICT",
  "selects": [
    { "field": { "@type": "SelectSimple", "field": "id" },        "alias": "saleId"     },
    { "field": { "@type": "SelectSimple", "field": "finalPrice" }, "alias": "finalPrice" },
    { "field": { "@type": "SelectSimple",
                 "field": "vehicle.trim.model.brand.name",
                 "joinTypes": ["Inner","Inner","Inner","Inner"] }, "alias": "marca"      }
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

### 4. O construirla por código con `QueryBuilder`

```java
Query q = QueryBuilder.builder("Sale")
    .coercionMode(CoercionMode.LENIENT)
    .select("id").as("saleId")
    .select("finalPrice").as("finalPrice")
    .select("vehicle.trim.model.brand.name", "Inner","Inner","Inner","Inner").as("marca")
    .filterEqual("vehicle.powertrain.fuelType", "Electric")
    .orderDesc("finalPrice")
    .page(0, 20)
    .build();
```

---

## Arquitectura

Quaero se basa en dos interfaces recursivas que siguen el **Patrón Composite**:

```
IFilter
├── FilterSimpleObject     — condición única (campo operador valor)
├── FilterArrayObject      — grupo AND / OR de IFilter[]
└── FilterQueryObject      — campo comparado contra una subconsulta

ISelect
├── SelectSimpleObject     — campo de entidad por ruta ("brand.name")
├── SelectValueObject      — valor literal
├── SelectCoalesceObject   — COALESCE(expr1, expr2, ...)
├── SelectConcatObject     — CONCAT(expr1, expr2, ...)
├── SelectConditionalObject — CASE WHEN ... THEN ... ELSE ... END
├── SelectArithmeticObject — expr1 ±×÷% expr2 ... exprN
├── SelectNumericOperation — ABS, SQRT, NEG, AVG, SUM, MAX, MIN (campo único)
├── SelectSubstringObject  — SUBSTRING(campo, pos[, longitud])
├── SelectTrimObject       — TRIM([spec] [char FROM] campo)
├── SelectFunctionObject   — cualquier función SQL nativa
└── SelectInnerSubselect   — (SELECT campo FROM entidad WHERE ...)
```

Tanto `field` como `value` en un filtro son de tipo `ISelect`, lo que permite **comparaciones campo a campo** como `precio > precioConDescuento`.

---

## Referencia del objeto `Query`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `tableName` | `String` | Nombre de la entidad JPA (p.ej. `"Sale"`) |
| `tableAlias` | `String` | Alias opcional para la entidad raíz |
| `coercionMode` | `CoercionMode` | `STRICT` (por defecto) o `LENIENT` — ver sección siguiente |
| `selects` | `List<QuerySelectObject>` | Campos del SELECT |
| `filter` | `IFilter` | Cláusula WHERE (puede estar profundamente anidada) |
| `orders` | `List<QueryOrderObject>` | ORDER BY |
| `pageIndex` | `Integer` | Número de página (base cero) |
| `pageSize` | `Integer` | Resultados por página |
| `dynamicJoins` | `List<QueryJoinObject>` | Joins cartesianos con tabla principal fija |
| `dynamicJoinsMultiple` | `List<QueryMultiJoinObject>` | Joins cartesianos donde cada parámetro declara su propia tabla principal |
| `paramJoinTypes` | `List<QueryJoinTypesObject>` | Sobreescribir tipo de join (INNER/LEFT/RIGHT) por ruta |
| `distinctResults` | `Boolean` | Añadir DISTINCT |

---

## Coerción de tipos

Cuando los valores de los filtros llegan del frontend como JSON, su tipo Java no siempre coincide con el tipo del campo JPA — por ejemplo, una fecha puede llegar como `String` (`"2023-01-15"`) en lugar de `LocalDate`, o un precio como `Integer` en lugar de `BigDecimal`.

Quaero gestiona esto mediante un **modo de coerción por petición** configurado en el objeto `Query`.

### STRICT (por defecto)

Solo se permiten coincidencias exactas de tipo y promociones numéricas sin pérdida. Cualquier otro desajuste lanza `IncorrectParameterTypeException` de inmediato.

Promociones permitidas en STRICT: `Integer → Long / Double / Float / BigDecimal / BigInteger`, `Long → Double / BigDecimal / BigInteger`, `Double → Float / BigDecimal`, `LocalDate → LocalDateTime`.

```json
{ "tableName": "Sale", "coercionMode": "STRICT", ... }
```

Usa STRICT en endpoints de producción donde el frontend debe enviar valores con el tipo correcto.

### LENIENT

Se intenta una conversión automática de máximo esfuerzo antes de fallar. Cubre todas las promociones de STRICT más:

| Desde | Hasta | Ejemplo |
|-------|-------|---------|
| `String` | `LocalDate` | `"2023-01-15"`, `"15/01/2023"`, `"01/15/2023"` |
| `String` | `LocalDateTime` | `"2023-01-15T10:30:00"`, `"2023-01-15 10:30:00"` |
| `String` | `Date` | mismos formatos de fecha que LocalDate |
| `String` | `Integer / Long / Double / Float / BigDecimal / BigInteger` | `"42"`, `"3.14"` |
| `String` | `Boolean` | `"true"/"false"`, `"1"/"0"`, `"yes"/"no"`, `"si"/"sí"` |
| `LocalDateTime` | `LocalDate` | trunca la hora |
| `Number / Boolean` | `String` | vía `toString()` (p.ej. LIKE sobre números almacenados) |

Si la conversión no es posible ni en modo LENIENT, se lanza igualmente `IncorrectParameterTypeException`.

```json
{ "tableName": "Sale", "coercionMode": "LENIENT", ... }
```

Usa LENIENT para herramientas internas, dashboards de administración o endpoints de búsqueda donde se quiere mayor tolerancia con los tipos.

### Diferentes modos por endpoint

El modo se configura a nivel de `Query`, por lo que distintos endpoints pueden tener estrategias distintas:

```java
// Formulario de producción validado → tipos obligatorios
@PostMapping("/sales/search")
public ResponseEntity<?> search(@RequestBody Query query) {
    query.setCoercionMode(CoercionMode.STRICT); // forzar sin importar lo que envíe el frontend
    return ResponseEntity.ok(executor.search(query));
}

// Filtro rápido de administración → tolerante con los tipos
@PostMapping("/admin/search")
public ResponseEntity<?> adminSearch(@RequestBody Query query) {
    // el frontend decide; si no lo envía, por defecto es STRICT
    return ResponseEntity.ok(executor.search(query));
}
```

O desde el builder:

```java
Query q = QueryBuilder.builder("Sale")
    .coercionMode(CoercionMode.LENIENT)
    .select("saleDate").as("fecha")
    .filterEqual("saleDate", "15/01/2023")         // String → LocalDate automáticamente
    .filterBetween("finalPrice", "30000", "55000") // String → BigDecimal automáticamente
    .build();
```

### Estructura de la excepción

`IncorrectParameterTypeException` incluye el contexto completo necesario para devolver un error útil al cliente:

```java
@ExceptionHandler(IncorrectParameterTypeException.class)
public ResponseEntity<?> handleTypeError(IncorrectParameterTypeException ex) {
    return ResponseEntity.badRequest().body(Map.of(
        "campo",    ex.getFieldName(),                          // p.ej. "saleDate"
        "esperado", ex.getExpectedType().getSimpleName(),       // p.ej. "LocalDate"
        "recibido", ex.getActualType().getSimpleName(),         // p.ej. "String"
        "mensaje",  ex.getMessage()
    ));
}
```

---

## Ejemplos de filtros

### Igualdad simple

```json
{
  "@type": "FilterSimple",
  "field":        { "@type": "SelectSimple", "field": "status" },
  "operatorType": "EQUAL",
  "value":        { "@type": "SelectValue",  "value": "Active" }
}
```

### Anidamiento AND / OR

```json
{
  "@type": "FilterArray",
  "operation": "or",
  "filters": [
    {
      "@type": "FilterArray", "operation": "and",
      "filters": [
        { "@type": "FilterSimple",
          "field": {"@type":"SelectSimple","field":"brand.name"},
          "operatorType": "EQUAL",
          "value": {"@type":"SelectValue","value":"Toyota"} },
        { "@type": "FilterSimple",
          "field": {"@type":"SelectSimple","field":"powertrain.fuelType"},
          "operatorType": "EQUAL",
          "value": {"@type":"SelectValue","value":"Hybrid"} }
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

### IN con lista

```json
{
  "@type": "FilterSimple",
  "field":        { "@type": "SelectSimple", "field": "customer.state" },
  "operatorType": "IN",
  "value":        { "@type": "SelectValue",  "value": ["CA","NY","TX"] }
}
```

### Subconsulta — campo > (SELECT AVG(...))

```json
{
  "@type": "FilterQuery",
  "field":        { "@type": "SelectSimple", "field": "finalPrice" },
  "operatorType": "GREATER_THAN",
  "queryEntity":  "Sale",
  "queryField":   { "@type": "SelectSimple", "field": "finalPrice", "operatorType": "AVERAGE" }
}
```

### Filtro sobre un root específico en consultas multi-raíz

Cuando se usan `dynamicJoins` o `dynamicJoinsMultiple`, usa `entityAlias` (o `entityName`) para apuntar al root correcto:

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

## Ejemplos de selects

### GROUP BY + agregación

```json
{ "field": { "@type": "SelectSimple", "field": "brand.name" }, "alias": "marca", "groupBy": true },
{ "field": { "@type": "SelectSimple", "field": "id" }, "alias": "unidadesVendidas", "operatorType": "COUNT" }
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
  "alias": "valorTradeInOCero"
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
  "alias": "nombreCompleto"
}
```

### Aritmética

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
  "alias": "precioNeto"
}
```

### Función SQL nativa

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
  "alias": "mesDeLaVenta"
}
```

---

## Operadores disponibles

### Operadores de filtro (`operatorType` en `FilterSimple`)

| Operador | Valor JSON | Equivalencia SQL |
|----------|------------|------------------|
| `EQUAL` | `Eq` | `= ?` |
| `NOT_EQUAL` | `NtEq` | `<> ?` |
| `GREATER_THAN` | `Gr` | `> ?` |
| `GREATER_THAN_OR_EQUAL` | `GrEq` | `>= ?` |
| `LESS_THAN` | `Ls` | `< ?` |
| `LESS_THAN_OR_EQUAL` | `LsEq` | `<= ?` |
| `IS_NULL` | `Nl` | `campo IS NULL` |
| `IS_NOT_NULL` | `NtNl` | `campo IS NOT NULL` |
| `IS_EMPTY` | `Emp` | `campo = ''` |
| `IS_NOT_EMPTY` | `NtEmp` | `campo <> ''` |
| `LIKE` | `Lk` | `UPPER(campo) LIKE UPPER(?)` |
| `NOT_LIKE` | `NtLk` | `UPPER(campo) NOT LIKE UPPER(?)` |
| `LIKE_TRIM` | `LkTr` | LIKE con trim sobre el valor |
| `NOT_LIKE_TRIM` | `NtLkTr` | NOT LIKE con trim sobre el valor |
| `IN` | `In` | `campo IN (?)` |
| `NOT_IN` | `NtIn` | `campo NOT IN (?)` |
| `IN_TRIM` | `InTr` | IN con trim sobre los valores |
| `NOT_IN_TRIM` | `NtInTr` | NOT IN con trim sobre los valores |

### Operadores de agregación (`operatorType` en `QuerySelectObject` / `QueryOrderObject`)

| Operador | Valor JSON | Equivalencia SQL |
|----------|------------|----------------|
| `COUNT` | `Cnt` | `COUNT(campo)` |
| `COUNT_DISTINCT` | `CntDst` | `COUNT(DISTINCT campo)` |
| `SUMMATORY` | `Sum` | `SUM(campo)` |
| `AVERAGE` | `Avg` | `AVG(campo)` |
| `MAX` | `Max` | `MAX(campo)` |
| `MIN` | `Min` | `MIN(campo)` |

### Operaciones aritméticas (`operation` en `SelectArithmetic`)

| Operador | Valor JSON | Equivalencia SQL |
|----------|------------|----------------|
| `SUMMATION` | `Sum` | `? + ?` |
| `DIFFERENCE` | `Diff` | `? - ?` |
| `MULTIPLY` | `Prod` | `? x ?` |
| `DIVISION` | `Div` | `? / ?` |
| `MOD` | `Mod` | `? % ?` |

### Operaciones numéricas unitarias (`operation` en `SelectNumericOperation`)

| Operador | Valor JSON | Equivalencia SQL 
|----------|------------|----------------|
| `AVERAGE` | `Avg` | `AVG(?) |
| `ABSOLUTE` | `Abs` | `ABS(?)` |
| `SQUARE_ROOT` | `Sqrt` | `SQRT(?)` |
| `MAX` | `Max` | `MAX(?)` |
| `MIN` | `Min` | `MIN(?)` |
| `NEGATION` | `Not` | `NOT(?)` |
| `SUM` | `Sum` | `SUM(?)` |

### Conversores de tipo (`converterType` en `SelectSimple`)

| Operador | Valor JSON | 
|----------|------------|
| `BIG_INTEGER` | `BInt` |
| `BIG_DECIMAL` | `BDec` |
| `DOUBLE` | `Dbl` |
| `FLOAT` | `Fl` |
| `LONG` | `Lng` |
| `INTEGER` | `Int` |
| `STRING` | `Str` |

---

## Tuple → Map anidado

Los resultados cuyos alias contienen puntos (p.ej. `"brand.name"`, `"brand.country.isoCode"`) se convierten automáticamente en Maps anidados:

```java
List<Map<String, Object>> resultado = QueryUtils.tupleToMapList(typedQuery.getResultList());
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


## Compatibilidad

| Dependencia | Versión |
|------------|---------|
| Java | 8+ |
| Spring Boot | 2.x |
| Spring Data JPA | 2.1+ |
| Hibernate | 5.x |
| javax.persistence-api | 2.2 |
| Jackson | 2.13+ |
| Apache Commons Lang3 | 3.x |

> Para Spring Boot 3.x / Jakarta EE, cambia los imports `javax.persistence` por `jakarta.persistence` en tu proyecto.

---

## Licencia

Distribuido bajo la **Apache License, Version 2.0**.
Consulta el fichero [LICENSE](LICENSE) para el texto completo, o visita https://www.apache.org/licenses/LICENSE-2.0