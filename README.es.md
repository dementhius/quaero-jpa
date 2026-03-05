# Quaero JPA

> **Dos formas de acabar con el boilerplate de JPA: deja que el frontend dirija la query como JSON, o constrúyela en Java de forma fluida. En ambos casos, Quaero la ejecuta.**

🌐 **Language / Idioma:** [English](README.md) · [Español](README.es.md)

[![Java](https://img.shields.io/badge/Java-8%2B-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Hibernate](https://img.shields.io/badge/Hibernate-5.x-yellow)](https://hibernate.org/)
[![License](https://img.shields.io/badge/Licencia-Apache%202.0-blue)](LICENSE)
[![npm](https://img.shields.io/npm/v/@quaero/query)](https://www.npmjs.com/package/@quaero/query)

---

## Ecosistema

| Paquete | Descripción |
|---------|-------------|
| **quaero-jpa** (este) | Backend Java / Spring Boot — ejecuta objetos `Query` mediante JPA Criteria API |
| **[@quaero/query](https://github.com/dementhius/quaero-query)** | Cliente TypeScript — construye objetos `Query` de forma fluida desde el navegador o Node.js (`npm install @quaero/query`) |

---

## Índice

1. [Por qué Quaero](#1-por-qué-quaero)
2. [Cómo se compara](#2-cómo-se-compara)
3. [Comparativa a tres bandas — JPA · QueryBuilder · JSON](#3-comparativa-a-tres-bandas--jpa--querybuilder--json)
4. [Instalación](#4-instalación)
5. [Inicio rápido](#5-inicio-rápido)
6. [Arquitectura](#6-arquitectura)
7. [Referencia del objeto Query](#7-referencia-del-objeto-query)
8. [Coerción de tipos](#8-coerción-de-tipos)
9. [Funciones SQL portables y corrección del GROUP BY](#9-funciones-sql-portables-y-corrección-del-group-by)
10. [Ejemplos de filtros](#10-ejemplos-de-filtros)
11. [Ejemplos de selects](#11-ejemplos-de-selects)
12. [Operadores disponibles](#12-operadores-disponibles)
13. [Tuple → Map anidado](#13-tuple--map-anidado)
14. [Aplicación demo](#14-aplicación-demo)
15. [Compatibilidad](#15-compatibilidad)
16. [Licencia](#16-licencia)

---

## 1. ¿Por qué Quaero?

Toda aplicación de datos tiene una pantalla de búsqueda — una tabla con columnas filtrables,
cabeceras ordenables, paginación, quizá un rango de fechas o un desplegable de estado.
El usuario hace clic. El backend tiene que responder.

El camino tradicional se parece a esto:

```
Sprint 1:  findByMarca(marca)
Sprint 2:  findByMarcaAndTipoCombustible(marca, tipo)
Sprint 3:  findByMarcaAndTipoCombustibleAndPrecioBetween(marca, tipo, min, max)
Sprint 4:  findByMarcaAndTipoCombustibleAndPrecioBetweenAndEstado(...)
           // cada filtro nuevo = nuevo método, nuevo test, nuevo deploy
```

El camino algo más sofisticado usa la Criteria API de JPA directamente — composable
y con tipos seguros, pero notoriamente verbosa. Una sola query filtrada, paginada, con
múltiples joins y un GROUP BY sobre una fecha formateada necesita **cientos de líneas**
que todas se parecen, se prueban igual y hay que cambiar cada vez que producto añade
una columna.

**Quaero reemplaza todo eso con un objeto `Query` — y te da dos formas de construirlo:**

**① Modo JSON** — el frontend envía un objeto JSON describiendo exactamente lo que quiere.
Un único endpoint backend gestiona cada combinación, para siempre. Sin redeploy cuando
cambian los requisitos. Sin nuevos métodos de repositorio. El equipo de producto publica
cambios de UI de forma independiente al equipo de ingeniería.

```
Formulario → JSON → un endpoint → QueryExecutor → Criteria API → Base de datos
```

**② Modo `QueryBuilder`** — cuando la query vive en Java (informes server-side, jobs
programados, exportaciones batch, microservicios internos), un builder fluido permite
construir el mismo objeto `Query` de forma programática con una API encadenable y
limpia — sin boilerplate de Criteria API, sin cadenas `if (param != null)`, sin
cableado manual de joins.

```java
Query q = QueryBuilder.builder("Sale")
    .select("id").as("saleId")
    .select("finalPrice").as("revenue").sum()
    .select("vehicle.trim.model.brand.name", "Inner","Inner","Inner","Inner").as("marca")
    .filterEqual("vehicle.powertrain.fuelType", "Electric")
    .filterBetween("finalPrice", 20000, 80000)
    .orderDesc("finalPrice")
    .page(0, 20)
    .build();
```

Ambos modos producen el mismo objeto `Query`, pasan por el mismo pipeline de ejecución
y se benefician de todas las funcionalidades de Quaero: funciones portables, corrección
del GROUP BY, coerción de tipos, conversión a mapa anidado. Elige el que encaje con el
contexto. Úsalos ambos en el mismo proyecto.

### Lo que consigues y JPA puro no puede darte

- Un `GROUP BY` dinámico sobre `to_char(saleDate, 'YYYY-MM')` **que realmente funciona** en Hibernate 5 — Hibernate tiene un bug conocido donde los literales se eliminan de la cláusula GROUP BY. Quaero lo corrige de forma transparente en ambos modos.
- Funciones SQL portables (`quaero_format_date`, `quaero_trunc_date`, `quaero_lpad`…) que emiten el SQL nativo correcto en PostgreSQL, Oracle, SQL Server, H2 y SQLite — desde una única definición de query, JSON o Java.
- Autodetección del motor SQL al arrancar — sin configuración de dialecto.
- Comparaciones campo a campo, expresiones aritméticas, CASE/WHEN, COALESCE, subconsultas correlacionadas — componibles en JSON o mediante el builder.
- Coerción de tipos por petición: `"2023-01-15"` se convierte automáticamente en `LocalDate` cuando el campo lo necesita. Sin conversores personalizados.

---

## 2. Cómo se compara

| Capacidad | JPA Criteria puro | Spring Data Specs | QueryDSL | jOOQ | **Quaero** |
|---|:---:|:---:|:---:|:---:|:---:|
| Frontend dirige la query en runtime — sin cambios backend | ❌ | ❌ | ❌ | ❌ | ✅ modo JSON |
| Filtros dinámicos desde JSON — cero código nuevo por combinación | ❌ | ❌ | ❌ | ❌ | ✅ |
| Builder Java fluido — sin boilerplate de Criteria API | ❌ | ❌ | ⚠️ verboso | ⚠️ verboso | ✅ QueryBuilder |
| Una sola API para queries frontend y server-side | ❌ | ❌ | ❌ | ❌ | ✅ |
| Anidamiento recursivo AND / OR | ✅ verboso | ⚠️ limitado | ✅ verboso | ✅ verboso | ✅ declarativo |
| Comparaciones campo a campo en filtros | ✅ verboso | ❌ | ✅ | ✅ | ✅ |
| Subconsultas correlacionadas en filtros | ✅ verboso | ❌ | ⚠️ | ✅ | ✅ |
| GROUP BY sobre funciones SQL con literales | ⚠️ **bug H5** | ⚠️ **bug H5** | ⚠️ **bug H5** | ✅ | ✅ **corregido** |
| Funciones portables entre motores de BD | ❌ | ❌ | ❌ | ✅ | ✅ |
| Autodetección del motor SQL al arrancar | ❌ | ❌ | ❌ | ✅ | ✅ |
| Coerción de tipos por petición (STRICT / LENIENT) | ❌ | ❌ | ❌ | ❌ | ✅ |
| Tuple → JSON anidado automáticamente | ❌ | ❌ | ❌ | ⚠️ | ✅ |
| Funciona con entidades JPA existentes tal cual | ✅ | ✅ | ✅ | ⚠️ codegen | ✅ |
| Sin paso de generación de código | ✅ | ✅ | ❌ | ❌ | ✅ |
| Compatible con Java 8 / Spring Boot 2.x | ✅ | ✅ | ✅ | ✅ | ✅ |

**La comparación honesta:**

jOOQ y QueryDSL son excelentes herramientas cuando un desarrollador se sienta a escribir
una consulta compleja específica. Dan seguridad de tipos, autocompletado en el IDE y
control total del SQL — y el builder de QueryDSL es genuinamente bueno. Pero ambos
requieren conocer la forma de la query en tiempo de desarrollo. El `QueryBuilder` de
Quaero cubre el mismo caso de uso server-side en Java con mucha menos ceremonia y sin
codegen, y además resuelve el problema que ellos no pueden: **cuando es el usuario —
no el desarrollador — quien decide qué consultar, en tiempo de ejecución, a través de
una interfaz, sin cambios en el backend.**

Las Spring Data Specifications son la alternativa built-in más cercana, pero siguen
requiriendo una clase Java por combinación de filtros, no gestionan la deserialización
JSON de árboles de filtros arbitrarios, no pueden hacer GROUP BY sobre funciones, y no
tienen capa de coerción de tipos.

---

## 3. Comparativa a tres bandas — JPA · QueryBuilder · JSON

Cada escenario muestra la misma query de tres formas:
**① JPA Criteria puro** (lo que escribirías hoy) ·
**② Quaero `QueryBuilder`** (Java fluido, sin Criteria API) ·
**③ Quaero JSON** (dirigido por el frontend, sin código backend nunca).

---

### Escenario A — Lista filtrada y paginada con join profundo

*"Vehículos eléctricos vendidos por Toyota, ordenados por precio descendente, página 2"*

**① JPA Criteria API puro — ~40 líneas, una combinación hardcodeada**

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

Cada nuevo filtro implica nuevo parámetro, nuevo `if`, nuevo test, deploy.

**② Quaero `QueryBuilder` — 8 líneas, cada filtro nuevo es una llamada más**

```java
Query q = QueryBuilder.builder("Sale")
    .select("id").as("saleId")
    .select("finalPrice").as("finalPrice")
    .select("vehicle.trim.model.brand.name", "Inner","Inner","Inner","Inner").as("marca")
    .filterAnd(f -> f
        .filterEqual("vehicle.trim.model.brand.name", "Toyota")
        .filterEqual("vehicle.powertrain.fuelType", "Electric"))
    .orderDesc("finalPrice")
    .page(1, 20)
    .build();
```

Legible, encadenable, sin Criteria API. Ideal para jobs programados, servicios internos
y generación de informes server-side. Añadir un filtro nuevo es una línea más.

**③ Quaero JSON — 0 líneas en el backend, el frontend controla todo**

```json
{
  "tableName": "Sale",
  "selects": [
    { "field": { "@type": "SelectSimple", "field": "id" }, "alias": "saleId" },
    { "field": { "@type": "SelectSimple", "field": "finalPrice" }, "alias": "finalPrice" },
    { "field": { "@type": "SelectSimple", "field": "vehicle.trim.model.brand.name",
                 "joinTypes": ["Inner","Inner","Inner","Inner"] }, "alias": "marca" }
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

¿El frontend añade un rango de precio? Dos elementos más en `filters`.
¿Elimina el orden? Borra `orders`. El endpoint nunca cambia.

---

### Escenario B — Informe mensual con GROUP BY sobre fecha formateada

*"Total de ventas e ingresos agrupados por mes — un endpoint de analítica estándar"*

**① JPA Criteria puro — compila, parece correcto, roto en Hibernate 5**

```java
// Produce SQL inválido:
//   SELECT   to_char(sale_date, 'YYYY-MM'), count(id), sum(final_price)
//   GROUP BY sale_date   ← BUG: literal eliminado, función perdida
//                           → la BD lo rechaza: "not a GROUP BY expression"

Expression<String> month = cb.function("to_char", String.class,
    root.get("saleDate"),
    cb.literal("YYYY-MM"));      // ← Hibernate 5 elimina esto del GROUP BY

cq.multiselect(month, cb.count(root), cb.sum(root.<BigDecimal>get("finalPrice")))
  .groupBy(month);               // ← roto en tiempo de ejecución en todos los motores
```

No hay solución limpia en Hibernate 5 puro. Los workarounds implican JPQL crudo o
registrar una función de dialecto por cada cadena de formato. Ninguno es portable.

**② Quaero `QueryBuilder` — job de analítica server-side, corrección aplicada automáticamente**

```java
SelectFunctionObject monthFn = new SelectFunctionObject()
    .function(QuaeroFunctions.FORMAT_DATE)
    .returnType("String")
    .params(Arrays.asList(
        new SelectSimpleObject("saleDate"),
        new SelectValueObject("YYYY-MM")
    ));

Query q = QueryBuilder.builder("Sale")
    .selectExpression(monthFn).as("mes").groupBy()
    .select("id").as("totalVentas").count()
    .select("finalPrice").as("ingresos").sum()
    .build();
```

`QuaeroFunctionExpression` garantiza que SELECT y GROUP BY emitan `to_char(sale_date,
'YYYY-MM')` en PostgreSQL, `format(sale_date, 'yyyy-MM')` en SQL Server, etc.
Cero configuración extra.

**③ Quaero JSON — endpoint de dashboard, granularidad configurable desde la UI**

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
      "alias": "mes",
      "groupBy": true
    },
    { "field": { "@type": "SelectSimple", "field": "id" },
      "alias": "totalVentas", "operatorType": "Cnt" },
    { "field": { "@type": "SelectSimple", "field": "finalPrice" },
      "alias": "ingresos", "operatorType": "Sum" }
  ]
}
```

La UI puede cambiar de `"YYYY-MM"` a `"YYYY"` (anual) o `"YYYY-Q"` (trimestral)
sin tocar el backend. Ambos modos corrigen el bug del GROUP BY automáticamente.

---

### Escenario C — Filtro aritmético campo a campo

*"Ventas donde el precio final es menor que el precio de lista menos el descuento"*

**① JPA puro — funciona, enterrado en un método que crece cada sprint**

```java
predicates.add(
    cb.lessThan(
        sale.<BigDecimal>get("finalPrice"),
        cb.diff(sale.<BigDecimal>get("listPrice"),
                sale.<BigDecimal>get("discountAmount"))
    )
);
```

**② Quaero `QueryBuilder` — una llamada legible**

```java
Query q = QueryBuilder.builder("Sale")
    .select("id").as("saleId")
    .select("finalPrice").as("finalPrice")
    .filterLessThan("finalPrice",
        QueryBuilder.diff("listPrice", "discountAmount"))
    .build();
```

**③ Quaero JSON — componible con cualquier otro filtro, configurable desde el frontend**

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

Tanto `field` como `value` en cualquier filtro son `ISelect` — cualquier expresión puede ir en cualquier lado.

---

### Escenario D — Filtro con subconsulta de agregación

*"Ventas donde el precio está por encima de la media de esa misma marca"*

**① JPA puro — subconsulta correlacionada, ~20 líneas**

```java
Subquery<Double> avg = cq.subquery(Double.class);
Root<Sale> sub = avg.from(Sale.class);
Join<?,?> subBrand = sub.join("vehicle").join("trim").join("model").join("brand");
avg.select(cb.avg(sub.<Double>get("finalPrice")))
   .where(cb.equal(subBrand.get("name"), brandJoin.get("name")));

predicates.add(cb.greaterThan(sale.<Double>get("finalPrice"), avg));
```

**② Quaero `QueryBuilder` — una llamada**

```java
Query q = QueryBuilder.builder("Sale")
    .select("id").as("saleId")
    .select("finalPrice").as("finalPrice")
    .filterGreaterThanSubquery("finalPrice",
        QueryBuilder.subquery("Sale", "finalPrice", AVERAGE))
    .build();
```

**③ Quaero JSON — un objeto de filtro**

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

### Escenario E — Etiqueta CASE/WHEN dinámica en el SELECT

*"Etiquetar cada venta como Premium, Mid-range o Entry según umbrales configurables"*

**① JPA puro — `selectCase()` anidado, umbrales hardcodeados, un deploy por cambio**

```java
cq.multiselect(
    cb.selectCase()
      .when(cb.greaterThan(sale.<BigDecimal>get("finalPrice"), new BigDecimal("60000")), "Premium")
      .when(cb.greaterThan(sale.<BigDecimal>get("finalPrice"), new BigDecimal("30000")), "Mid-range")
      .otherwise("Entry")
      .as(String.class)
);
```

**② Quaero `QueryBuilder` — umbrales como argumentos, reutilizable para distintos informes**

```java
Query q = QueryBuilder.builder("Sale")
    .select("id").as("saleId")
    .selectConditional("segmento")
        .when(QueryBuilder.gt("finalPrice", 60000), "Premium")
        .when(QueryBuilder.gt("finalPrice", 30000), "Mid-range")
        .otherwise("Entry")
    .build();
```

El mismo método sirve para múltiples tipos de informe con reglas de segmentación
distintas pasadas como parámetros — sin código nuevo, sin deploy.

**③ Quaero JSON — umbrales en el frontend, cero intervención del backend**

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
  "alias": "segmento"
}
```

Un product manager ajusta los umbrales desde la interfaz sin intervención del equipo de ingeniería.

---

## 4. Instalación

```xml
<dependency>
    <groupId>io.github.dementhius</groupId>
    <artifactId>quaero-jpa</artifactId>
    <version>1.1.0</version>
</dependency>
```

Soporte SQLite (opcional):

```xml
<dependency>
    <groupId>com.github.gwenn</groupId>
    <artifactId>sqlite-dialect</artifactId>
    <version>0.1.3</version>
</dependency>
```

Esa es la única configuración necesaria. Quaero registra su resolver de dialecto y su
autoconfiguración automáticamente vía `META-INF/spring.factories`.

---

## 5. Inicio rápido

### Paso 1 — Registrar los beans

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

### Paso 2 — Un endpoint universal

```java
@RestController
@RequestMapping("/api/ventas")
public class SaleController {

    @Autowired
    private QueryExecutorService queryExecutorService;

    @PostMapping("/buscar")
    public ResponseEntity<Map<String, Object>> buscar(@RequestBody Query query) {
        return ResponseEntity.ok(queryExecutorService.executeQuery(query));
    }
}
```

Este único endpoint gestiona cada combinación de query que el frontend pueda construir —
hoy y en cualquier sprint futuro.

### Paso 3 — Llamarlo desde el frontend con `@quaero/query`

Instala el cliente TypeScript oficial:

```bash
npm install @quaero/query
```

```typescript
import { QueryBuilder, Selects } from '@quaero/query';

const query = QueryBuilder.for('Sale')
  .select(Selects.field('id')).as('saleId')
  .select(Selects.field('finalPrice')).as('finalPrice')
  .filterEqual('status', 'Completed')
  .page(0, 25)
  .build();

this.http.post<any>('/api/ventas/buscar', query).subscribe(result => {
  this.rows  = result.data;
  this.total = result.total;
});
```

> El builder genera exactamente el JSON plano que Quaero JPA espera. También puedes enviar JSON directamente — consulta la [sección 7](#7-referencia-del-objeto-query) para la referencia completa del objeto `Query`.

### Paso 4 — O construirlo por código con `QueryBuilder`

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

## 6. Arquitectura

Quaero se basa en dos interfaces recursivas que siguen el **Patrón Composite**.
Tanto `field` como `value` en cualquier filtro son `ISelect` — permitiendo comparaciones
campo a campo y expresiones anidadas en cualquier punto del árbol.

```
IFilter
├── FilterSimpleObject      — campo  operador  valor
├── FilterArrayObject       — AND / OR de IFilter[]
└── FilterQueryObject       — campo  operador  (subconsulta)

ISelect
├── SelectSimpleObject      — campo por ruta de puntos ("vehicle.brand.name")
├── SelectValueObject       — valor literal
├── SelectCoalesceObject    — COALESCE(a, b, ...)
├── SelectConcatObject      — CONCAT(a, b, ...)
├── SelectConditionalObject — CASE WHEN ... THEN ... ELSE ... END
├── SelectArithmeticObject  — a ± b × c ÷ d % e
├── SelectNumericOperation  — ABS / SQRT / NEG / AVG / SUM / MAX / MIN
├── SelectSubstringObject   — SUBSTRING(campo, pos [, longitud])
├── SelectTrimObject        — TRIM([spec] [char FROM] campo)
├── SelectFunctionObject    — función quaero_* portable o SQL nativa  ← segura en GROUP BY
└── SelectInnerSubselect    — (SELECT campo FROM entidad WHERE ...)
```

---

## 7. Referencia del objeto `Query`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `tableName` | `String` | Nombre de entidad JPA — `"Sale"`, `"Customer"`, etc. |
| `tableAlias` | `String` | Alias opcional para la entidad raíz |
| `coercionMode` | `CoercionMode` | `STRICT` (por defecto) o `LENIENT` |
| `selects` | `List<QuerySelectObject>` | Campos y expresiones del SELECT |
| `filter` | `IFilter` | Cláusula WHERE — anidable arbitrariamente |
| `orders` | `List<QueryOrderObject>` | Cláusulas ORDER BY |
| `pageIndex` | `Integer` | Número de página (base cero) |
| `pageSize` | `Integer` | Resultados por página |
| `dynamicJoins` | `List<QueryJoinObject>` | Joins cartesianos — tabla principal fija |
| `dynamicJoinsMultiple` | `List<QueryMultiJoinObject>` | Joins cartesianos — cada parámetro declara su propia raíz |
| `paramJoinTypes` | `List<QueryJoinTypesObject>` | Sobreescribir tipo de join por segmento de ruta (INNER/LEFT/RIGHT) |
| `distinctResults` | `Boolean` | Añadir DISTINCT |

---

## 8. Coerción de tipos

JSON no tiene `LocalDate`, `BigDecimal` ni `LocalDateTime` nativos. Cuando los valores
de los filtros llegan del frontend, su tipo Java a menudo no coincide exactamente con
el campo JPA. Quaero lo resuelve con un **modo de coerción por petición** — sin
conversores personalizados.

### STRICT (por defecto)

Solo se aceptan coincidencias exactas de tipo y promociones sin pérdida.
Cualquier otra discrepancia lanza `IncorrectParameterTypeException` inmediatamente.

Promociones permitidas: `Integer → Long / Double / Float / BigDecimal / BigInteger` ·
`Long → Double / BigDecimal / BigInteger` · `Double → Float / BigDecimal` · `LocalDate → LocalDateTime`

Usa STRICT para endpoints de producción donde el frontend envía valores con tipos correctos.

### LENIENT

Conversión de máximo esfuerzo antes de fallar. Añade sobre STRICT:

| Desde | Hasta | Ejemplos |
|-------|-------|---------|
| `String` | `LocalDate` | `"2023-01-15"` · `"15/01/2023"` · `"01/15/2023"` |
| `String` | `LocalDateTime` | `"2023-01-15T10:30:00"` · `"2023-01-15 10:30:00"` |
| `String` | `Date` | mismos formatos que LocalDate |
| `String` | tipos numéricos | `"42"` · `"3.14"` |
| `String` | `Boolean` | `"true"/"false"` · `"1"/"0"` · `"yes"/"no"` · `"si"/"sí"` |
| `LocalDateTime` | `LocalDate` | trunca la hora |
| `Number / Boolean` | `String` | vía `toString()` |

Usa LENIENT para herramientas internas, paneles de administración e informes.

### Gestión de errores

```java
@ExceptionHandler(IncorrectParameterTypeException.class)
public ResponseEntity<?> handleTypeError(IncorrectParameterTypeException ex) {
    return ResponseEntity.badRequest().body(Map.of(
        "campo",    ex.getFieldName(),
        "esperado", ex.getExpectedType().getSimpleName(),
        "recibido", ex.getActualType().getSimpleName(),
        "mensaje",  ex.getMessage()
    ));
}
```

---

## 9. Funciones SQL portables y corrección del GROUP BY

### El bug del GROUP BY en Hibernate 5

Cuando una expresión de función contiene un argumento `CriteriaBuilder.literal()`,
Hibernate 5 lo elimina de la cláusula GROUP BY:

```sql
-- SELECT correcto
SELECT to_char(sale_date, 'YYYY-MM'), count(id)

-- GROUP BY roto
GROUP BY sale_date    ← debería ser: to_char(sale_date, 'YYYY-MM')
-- → la BD lo rechaza: "not a GROUP BY expression"
```

Quaero lo corrige de forma transparente mediante `QuaeroFunctionExpression`, que
sobreescribe el `render(RenderingContext)` interno de Hibernate para emitir el fragmento
SQL idéntico en SELECT y GROUP BY. Cero cambios de API — `SelectFunctionObject` aplica
la corrección automáticamente en ambos modos, JSON y `QueryBuilder`.

### Autodetección del motor SQL

Quaero registra un `DialectResolver` de Hibernate vía `spring.factories`.
Sin configuración en `application.properties`.

| Base de datos | Dialecto |
|--------------|---------|
| PostgreSQL / CockroachDB | `CustomPostgreSQLDialect` |
| Oracle | `CustomOracleDialect` |
| Microsoft SQL Server | `CustomSQLServerDialect` |
| H2 | `CustomH2Dialect` |
| SQLite | `CustomSQLiteDialect` |

Para fijar un dialecto específico:
```properties
spring.jpa.properties.hibernate.dialect=quaero.dialect.impl.CustomPostgreSQLDialect
```

### Nombres de función `quaero_*` portables

#### Fecha / hora

| Función | Argumentos | Se mapea a |
|---------|-----------|-----------|
| `quaero_format_date` | `(fecha, formato)` | `to_char` · `format` · `formatdatetime` · `strftime` |
| `quaero_trunc_date` | `(fecha, unidad)` | `date_trunc` · `trunc` · `datetrunc` · `date(...,'start of')` |
| `quaero_date_part` | `(unidad, fecha)` | `date_part` · `extract` · `datepart` · `strftime` |
| `quaero_date_add` | `(fecha, n, unidad)` | `+ interval` · `add_months` · `dateadd` · `datetime` |
| `quaero_date_diff` | `(unidad, f1, f2)` | `age` · `months_between` · `datediff` · `julianday` |
| `quaero_now` | `()` | `now()` · `sysdate` · `getdate()` · `datetime('now')` |

**Unidades:** `year` · `quarter` · `month` · `week` · `day` · `hour` · `minute` · `second`

**Tokens de formato** (traducidos automáticamente por motor):

| Token | Significado | PG / Oracle | SQL Server | H2 | SQLite |
|-------|-------------|-------------|------------|-----|--------|
| `YYYY` | Año 4 dígitos | `YYYY` | `yyyy` | `yyyy` | `%Y` |
| `MM` | Mes 01-12 | `MM` | `MM` | `MM` | `%m` |
| `DD` | Día 01-31 | `DD` | `dd` | `dd` | `%d` |
| `HH24` | Hora 24h | `HH24` | `HH` | `HH` | `%H` |
| `MI` | Minutos | `MI` | `mm` | `mm` | `%M` |
| `SS` | Segundos | `SS` | `ss` | `ss` | `%S` |
| `MONTH` | Nombre mes completo | `MONTH` | `MMMM` | `MMMM` | `%B` |
| `MON` | Mes abreviado | `MON` | `MMM` | `MMM` | `%b` |
| `DAY` / `DY` | Nombre del día | `DAY` / `DY` | `dddd` / `ddd` | `EEEE` / `EEE` | `%A` / `%a` |

#### Cadenas de texto

| Función | Argumentos | No disponible en |
|---------|-----------|-----------------|
| `quaero_lpad` | `(str, lon, relleno)` | — |
| `quaero_rpad` | `(str, lon, relleno)` | — |
| `quaero_initcap` | `(str)` | ⚠️ solo primer carácter en SS / H2 / SQLite |
| `quaero_replace` | `(str, desde, hasta)` | — |
| `quaero_regexp_replace` | `(str, patrón, reemplazo)` | SQL Server · SQLite |
| `quaero_instr` | `(cadena, subcadena)` | — |

#### Numérico

| Función | Argumentos | Descripción |
|---------|-----------|-------------|
| `quaero_round` | `(valor, precisión)` | Redondea a N decimales |
| `quaero_trunc_number` | `(valor, precisión)` | Trunca sin redondear |
| `quaero_log` | `(valor, base)` | Logaritmo en base N |

### Tabla de compatibilidad

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
| Corrección GROUP BY | ✅ | ✅ | ✅ | ✅ | ✅ |
| Autodetección | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 10. Ejemplos de filtros

### Igualdad simple

```json
{
  "@type": "FilterSimple",
  "field":        { "@type": "SelectSimple", "field": "status" },
  "operatorType": "Eq",
  "value":        { "@type": "SelectValue",  "value": "Active" }
}
```

### Anidamiento AND / OR

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

### IN con lista

```json
{
  "@type": "FilterSimple",
  "field":        { "@type": "SelectSimple", "field": "customer.state" },
  "operatorType": "In",
  "value":        { "@type": "SelectValue",  "value": ["CA","NY","TX"] }
}
```

### BETWEEN (rango)

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

### Subconsulta — campo > AVG

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

## 11. Ejemplos de selects

### Campo simple

```json
{ "field": { "@type": "SelectSimple", "field": "finalPrice" }, "alias": "precio" }
```

### GROUP BY + agregación

```json
{ "field": { "@type": "SelectSimple", "field": "brand.name" }, "alias": "marca", "groupBy": true },
{ "field": { "@type": "SelectSimple", "field": "id" }, "alias": "unidades", "operatorType": "Cnt" },
{ "field": { "@type": "SelectSimple", "field": "finalPrice" }, "alias": "ingresos", "operatorType": "SUMMATORY" }
```

### Función portable — segura en GROUP BY, cualquier motor

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
  "alias": "mes",
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
  "alias": "valorTradeIn"
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
    "otherwise": { "@type": "SelectValue", "value": "Estándar" }
  },
  "alias": "segmento"
}
```

### Aritmética

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
  "alias": "precioNeto"
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

---

## 12. Operadores disponibles

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

## 13. Tuple → Map anidado

Los alias con puntos (`"brand.name"`, `"brand.country.isoCode"`) se convierten
automáticamente en Maps anidados, listos para enviar directamente al frontend:

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

## 14. Aplicación demo

Una aplicación Spring Boot completamente funcional — datos de una red de concesionarios,
H2 en memoria, 750 ventas, 825 vehículos, 220 clientes — disponible en
[`quaero-demo`](https://github.com/dementhius/quaero-demo).

**15 escenarios preconfigurados** que cubren todas las funcionalidades de Quaero,
invocables directamente desde Postman:

```bash
cd quaero-demo && mvn spring-boot:run

curl http://localhost:8080/api/demo/list
curl -X POST http://localhost:8080/api/demo/03-filter-and-or
curl http://localhost:8080/api/demo/10-subquery/query
```

Consola H2: `http://localhost:8080/h2-console`

---

## 15. Compatibilidad

| Dependencia | Versión |
|------------|---------|
| Java | 8+ |
| Spring Boot | 2.x |
| Spring Data JPA | 2.1+ |
| Hibernate | 5.x |
| javax.persistence-api | 2.2 |
| Jackson | 2.13+ |
| Apache Commons Lang3 | 3.x |

> **Spring Boot 3.x / Hibernate 6:** cambia los imports `javax.persistence` por
> `jakarta.persistence`. La corrección del GROUP BY de Hibernate 5 no es necesaria
> en Hibernate 6 — `SelectFunctionObject` lo detecta automáticamente y usa la
> Criteria API estándar.

---

## 16. Licencia

Distribuido bajo la **Apache License 2.0**.
Ver [LICENSE](LICENSE) o https://www.apache.org/licenses/LICENSE-2.0
