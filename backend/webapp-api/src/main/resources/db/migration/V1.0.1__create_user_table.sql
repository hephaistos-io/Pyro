-- Create customer table
-- This matches the UserEntity JPA entity definition

CREATE TABLE customer
(
    id UUID DEFAULT uuidv7() PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name  VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL
);

-- Create index on email for faster lookups
CREATE INDEX idx_customer_email ON customer (email);
