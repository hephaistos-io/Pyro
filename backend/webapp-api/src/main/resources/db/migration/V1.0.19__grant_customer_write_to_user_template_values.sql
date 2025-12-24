-- Grant INSERT and UPDATE permissions on user_template_values to customer-flagforge
-- This allows the customer-api to create and update user template values via the WRITE API key
GRANT INSERT, UPDATE ON user_template_values TO "customer-flagforge";
