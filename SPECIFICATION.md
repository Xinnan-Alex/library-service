# Library Service Specification

## Objective

Create a RESTful API for a simple library system.

## Required API actions

The API must allow a user to:

1. Register a new borrower.
2. Register a new book.
3. List all books in the library.
4. Borrow a book for a borrower, identified by book ID.
5. Return a borrowed book.

## Data models

### Borrower

- Unique ID
- Name
- Email address

### Book

- Unique ID
- ISBN number
- Title
- Author

## ISBN and copy rules

- Books with the same title and author but different ISBN numbers are different books.
- Books with the same ISBN number must have the same title and author.
- Multiple copies with the same ISBN number are allowed and must be registered with different book IDs.

## Mandatory requirements

1. Do not mention the prohibited organization named in the source specification anywhere in the assessment.
2. Use Java 17 and Spring Boot.
3. Use a package manager for project dependencies.
4. Implement proper data validation and error handling.
5. Store borrower and book data in a database, and justify the database choice.
6. Implement REST endpoints for every required API action.
7. Ensure that only one borrower can borrow a particular book ID at a time.
8. Provide clear API usage documentation.
9. Document assumptions for requirements not explicitly stated in the task.

## Nice to have

1. Demonstrate clean-code practices.
2. Include unit tests that verify the implementation.
3. Demonstrate declarative containerization and CI/CD tooling.
4. Demonstrate conformance, to an appropriate extent, with the [Twelve-Factor App](https://12factor.net/) principles.
