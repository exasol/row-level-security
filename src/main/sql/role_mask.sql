---
-- Get the role mask for a single role ID.
--
-- @param role_id ID of the role for which the mask should be calculated
--
-- @return role mask or <code>NULL</code> if the role ID is out of bounds.
--
CREATE OR REPLACE FUNCTION ROLE_MASK (ROLE_ID IN DECIMAL(2,0))
RETURN DECIMAL(20,0)
BEGIN
    IF ROLE_ID < 1 OR ROLE_ID > 64 THEN
        RETURN NULL;
    END IF;
    RETURN BIT_SET(0, ROLE_ID - 1);
END
/