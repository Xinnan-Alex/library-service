---
sessionId: session-260821-220149-l032
---

# Requirements

### Goal
Make the purpose of every explicit index in `src/main/resources/db/migration/V1__create_library_schema.sql` understandable directly from the migration.

### Scope
- Add concise `--` comments immediately above the four explicit index declarations.
- Explain that `book_copies_book_record_id_idx` supports locating physical copies for a bibliographic record.
- Explain that `loans_borrower_id_idx` supports borrower loan-history lookups.
- Explain that `loans_book_copy_id_idx` supports complete per-copy loan-history lookups.
- Explain that `loans_one_active_per_copy_idx` is a partial unique index that enforces at most one loan with `returned_at IS NULL` for each copy.
- Do not alter tables, constraints, index definitions, application behavior, or `README.md`.

# Technical Design

### Proposed Change
- Keep the comments next to the corresponding `CREATE INDEX` statements so future schema readers do not need external documentation.
- Distinguish performance-oriented lookup indexes from `loans_one_active_per_copy_idx`, which also enforces a lending invariant.
- Mention that PostgreSQL does not automatically create indexes for referencing foreign-key columns, clarifying why the explicit lookup indexes exist.
- Preserve all executable SQL exactly; only SQL comments change.

### Migration Compatibility
Flyway includes migration contents in its checksum, so changing comments in an already-applied `V1__create_library_schema.sql` may cause validation failures in persistent environments. Before implementation, determine whether existing databases need checksum repair/recreation; the repository's documented clean-development workflow can recreate the local volume, while shared or production databases must follow their migration governance rather than silently repairing checksums.

# Testing

### Validation
- Review the diff to confirm only comments were added and all four index statements are byte-for-byte unchanged.
- Run the existing migration/context test where PostgreSQL/Testcontainers is available to confirm a clean database still applies `V1` successfully.
- Run the normal Maven test suite; Docker-dependent tests may skip on the current host as already documented.

# Delivery Steps

### ✓ Step 1: Document the lookup indexes
The migration explains which access paths are accelerated by its three non-unique indexes.

- Add a comment above `book_copies_book_record_id_idx` describing physical-copy lookup by `book_record_id`.
- Add comments above `loans_borrower_id_idx` and `loans_book_copy_id_idx` describing borrower and physical-copy history queries.
- Note the foreign-key indexing rationale without implying that these indexes enforce referential integrity.

### ✓ Step 2: Document the active-loan invariant and validate the migration
The migration explains how the partial unique index protects borrowing correctness without changing schema behavior.

- Add a comment above `loans_one_active_per_copy_idx` explaining the `returned_at IS NULL` predicate and one-active-loan guarantee.
- Confirm the SQL definitions are unchanged and review the Flyway checksum impact for previously migrated databases.
- Execute the existing migration and regression checks as the environment permits.