-- System roles: ADMIN, USER, and MEMBER (membership: the account belongs to an organization; what it
-- may do there is its organization role). The role now follows membership in code; this brings
-- existing accounts in line. Some had no system role at all, others kept USER after joining.

-- People in an organization are members...
INSERT INTO user_roles (user_id, role_id)
SELECT DISTINCT om.user_id, r.role_id
FROM organization_members om
JOIN roles r ON r.code = 'MEMBER'
WHERE om.status = 'ACTIVE'
ON CONFLICT DO NOTHING;

-- ...and no longer ordinary users.
DELETE FROM user_roles ur
USING roles r
WHERE ur.role_id = r.role_id
  AND r.code = 'USER'
  AND ur.user_id IN (SELECT user_id FROM organization_members WHERE status = 'ACTIVE');

-- People who left every organization are not members any more.
DELETE FROM user_roles ur
USING roles r
WHERE ur.role_id = r.role_id
  AND r.code = 'MEMBER'
  AND ur.user_id NOT IN (SELECT user_id FROM organization_members WHERE status = 'ACTIVE');

-- Every account has at least one system role; one with none is an ordinary user.
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.code = 'USER'
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.user_id);
