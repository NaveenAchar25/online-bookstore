-- Demo data only. Wired to the 'dev' profile exclusively via
-- spring.flyway.locations in application.yml — this location is never on
-- the migration path when spring.profiles.active=prod.
INSERT INTO books (title, author, price, stock_quantity, isbn, description) VALUES
('Solid Principles', 'Robert C. Martin', 35.99, 25, '9780132350884', 'A handbook of agile software craftsmanship'),
('Effective Java', 'Joshua Bloch', 42.50, 15, '9780134685991', 'Best practices for the Java platform'),
('Design Patterns', 'Erich Gamma et al.', 39.99, 10, '9780201633610', 'Elements of reusable object-oriented software'),
('The Pragmatic Programmer', 'Andrew Hunt, David Thomas', 33.75, 20, '9780135957059', 'Your journey to mastery'),
('Data Structures and Algorithms', 'Eric Evans', 45.00, 8, '9780321125217', 'Tackling complexity in the heart of software'),
('Refactoring', 'Martin Fowler', 40.25, 12, '9780134757599', 'Improving the design of existing code');
