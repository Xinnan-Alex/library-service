CREATE TABLE borrowers (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT borrowers_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT borrowers_email_not_blank CHECK (btrim(email) <> '')
);

CREATE TABLE book_records (
    id UUID PRIMARY KEY,
    isbn VARCHAR(13) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(300) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT book_records_isbn_not_blank CHECK (btrim(isbn) <> ''),
    CONSTRAINT book_records_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT book_records_author_not_blank CHECK (btrim(author) <> '')
);

CREATE TABLE book_copies (
    id UUID PRIMARY KEY,
    book_record_id UUID NOT NULL REFERENCES book_records(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- this supports locating physical copies for a bibliographic record.
CREATE INDEX book_copies_book_record_id_idx ON book_copies(book_record_id);

CREATE TABLE loans (
    id UUID PRIMARY KEY,
    book_copy_id UUID NOT NULL REFERENCES book_copies(id),
    borrower_id UUID NOT NULL REFERENCES borrowers(id),
    borrowed_at TIMESTAMPTZ NOT NULL,
    returned_at TIMESTAMPTZ,
    CONSTRAINT loans_return_after_borrow CHECK (returned_at IS NULL OR returned_at >= borrowed_at)
);

-- this supports borrower loan-history lookups.
CREATE INDEX loans_borrower_id_idx ON loans(borrower_id);
-- this supports complete per-copy loan-history lookups.
CREATE INDEX loans_book_copy_id_idx ON loans(book_copy_id);
-- This partial unique index enforces at most one active loan (returned_at IS NULL) for each physical copy.
CREATE UNIQUE INDEX loans_one_active_per_copy_idx
    ON loans(book_copy_id)
    WHERE returned_at IS NULL;