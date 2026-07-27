# 商品评价管理链路收口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve C-side comment pagination metadata and make admin comment search input-safe.

**Architecture:** Keep the existing controller and mapper contracts. Normalize input at the two service entry points, then use the existing query and VO enrichment flows. C-side empty pages reuse the existing empty-list-safe enrichment path while preserving MyBatis-Plus metadata.

**Tech Stack:** Java 8, Spring Boot 2.7, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Maven.

---

## Files

- Modify: `gj-mall-server/gj-mall-product/src/main/java/com/gj/mall/product/service/impl/ProductCommentServiceImpl.java`
- Modify: `gj-mall-server/gj-mall-product/src/test/java/com/gj/mall/product/service/impl/ProductCommentServiceImplTest.java`
- Modify: `gj-mall-server/gj-mall-admin-api/src/main/java/com/gj/mall/admin/service/impl/AdminCommentServiceImpl.java`
- Modify: `gj-mall-server/gj-mall-admin-api/src/test/java/com/gj/mall/admin/service/impl/AdminCommentServiceImplTest.java`

### Task 1: Preserve C-side empty-page metadata

- [ ] Write a failing `ProductCommentServiceImplTest` that mocks a page with `total=27`, no records, and requests page 3 with size 10. Assert `PageResult.total` remains 27 and `list` is empty.
- [ ] Run `mvn -pl gj-mall-product -am -Dtest=ProductCommentServiceImplTest -DfailIfNoTests=false test` and confirm the test fails because the service returns `PageResult.empty` with total 0.
- [ ] Replace the early empty-page return in `ProductCommentServiceImpl.page` with a `PageResult` built from `result.getTotal()`, `result.getCurrent()`, `result.getSize()`, and `enrich(result.getRecords())`.
- [ ] Re-run the same command and confirm all `ProductCommentServiceImplTest` tests pass.

### Task 2: Normalize backend comment-page queries

- [ ] Write failing `AdminCommentServiceImplTest` cases for `page(null)` and a query with `pageNum=-1`, `pageSize=500`, and keyword `  iPhone  `. Capture `IPage` and assert page 1, size 100, and a non-null result.
- [ ] Run `mvn -pl gj-mall-admin-api -am -Dtest=AdminCommentServiceImplTest -DfailIfNoTests=false test` and confirm `page(null)` fails with `NullPointerException`.
- [ ] Add `normalizeQuery`, `normalizePageNum`, and `normalizePageSize` helpers to `AdminCommentServiceImpl`; use the normalized query throughout `page`, with maximum page size 100 and trimmed keyword.
- [ ] Re-run the same command and confirm all `AdminCommentServiceImplTest` tests pass.

### Task 3: Verify integration scope

- [ ] Run `mvn -pl gj-mall-product,gj-mall-admin-api -am test` and confirm all selected module tests pass.
- [ ] Run `mvn -DskipTests package` from `gj-mall-server` and confirm all backend modules compile and package successfully.
- [ ] Review `git diff --check` and `git status --short`; commit only the four service and test files with the Chinese message `完善商品评价分页与后台查询边界`.
