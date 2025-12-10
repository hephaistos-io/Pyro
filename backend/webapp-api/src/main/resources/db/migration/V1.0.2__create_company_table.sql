-- Create user table
-- This matches the UserEntity JPA entity definition

CREATE TABLE company
(
    id   UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- create link to users, but without making it mandatory
ALTER TABLE user
    ADD CONSTRAINT cs_company_id FOREIGN KEY (company_id)
        REFERENCES company (id);
