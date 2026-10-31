-- Default starter seed data for API Testing Playground

INSERT INTO collections (name, description) VALUES
('Starter API Collection', 'Sample collection containing common REST API requests to get started quickly.');

INSERT INTO environments (name, is_active) VALUES
('Local Echo Server', TRUE),
('Production Demo', FALSE);

-- Environment variables for 'Local Echo Server' (id 1)
INSERT INTO environment_variables (environment_id, var_key, var_value, is_secret, is_enabled) VALUES
(1, 'baseUrl', 'http://localhost:8080/api/v1/echo', FALSE, TRUE),
(1, 'apiKey', 'playground-secret-token-xyz123', TRUE, TRUE),
(1, 'demoUserId', 'usr_998877', FALSE, TRUE);

-- Environment variables for 'Production Demo' (id 2)
INSERT INTO environment_variables (environment_id, var_key, var_value, is_secret, is_enabled) VALUES
(2, 'baseUrl', 'https://httpbin.org', FALSE, TRUE),
(2, 'apiKey', 'prod-vault-key-abc987', TRUE, TRUE);

-- Sample saved requests in 'Starter API Collection' (id 1)
INSERT INTO saved_requests (collection_id, name, method, url, query_params, headers, auth_type, auth_config, body) VALUES
(1, 'Echo GET with Params', 'GET', '{{baseUrl}}/get?test=true&platform=playground', '[{"key":"test","value":"true","enabled":true},{"key":"platform","value":"playground","enabled":true}]', '[{"key":"Accept","value":"application/json","enabled":true}]', 'NONE', '{}', NULL),
(1, 'Echo Authenticated Endpoint', 'GET', '{{baseUrl}}/auth-check', '[]', '[]', 'API_KEY', '{"keyName":"X-API-Key","keyValue":"{{apiKey}}","placement":"HEADER"}', NULL),
(1, 'Echo POST JSON Body', 'POST', '{{baseUrl}}/post', '[]', '[{"key":"Content-Type","value":"application/json","enabled":true}]', 'NONE', '{}', '{\n  "message": "Hello from API Testing Playground!",\n  "userId": "{{demoUserId}}",\n  "timestamp": 1730000000000\n}');
