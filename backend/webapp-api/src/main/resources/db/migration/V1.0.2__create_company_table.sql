-- Create company table
-- This matches the CompanyEntity JPA entity definition

CREATE TABLE company
(
    id UUID DEFAULT uuidv7() PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- add company_id column to customer table and create link to users (optional)
ALTER TABLE customer
    ADD COLUMN company_id UUID;

ALTER TABLE customer
    ADD CONSTRAINT cs_company_id FOREIGN KEY (company_id)
        REFERENCES company (id);
