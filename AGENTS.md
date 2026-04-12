

## Coding Standards
- **Java 21 strict mode**
- **Null-safety**: Use `Optional` for nullable values.
- **For complex object creation**: Use a builder pattern leveraging in the lombok library.
- **DTOs**: Use record classes for data transfer objects.
- **FP over OOP**: Prefer the functional programming paradigm over object-oriented programming.


## Architecture Style
Follow the structure design of Hexagonal Architecture.
- **Domain**: The core business logic of the application. `src/main/java/com/nova/domain`
- **Infrastructure**: The components that interact with the outside world `src/main/java/com/nova/infrastructure`
- **Application**: Orchestrates the interaction between the domain and infrastructure layers. `src/main/java/com/nova/application`

## Design Pattern Decision
- **Use Strategy Pattern**: For complex branching logic. Create a strategy interface and concrete implementations rather than using `if-else` blocks.
- **Use Result Pattern**: For operations that may fail, encapsulate the result in a `Result` object with success and error states.
- **Use Builder Pattern**: For complex object creation. Use a builder class to construct objects step by step.
- **Use Dependency Injection**: To avoid the high-level modules (business rules,logic) depends on low-level modules like the database, external services,

## Configuration
- Avoid using the property files for configuration.

## Security Considerations
- **Always mask** in the log the sensitive information like passwords, tokens, emails, phone numbers.

## Logging
- Use the SLF4J logging framework.
- Log the time taken for each request.