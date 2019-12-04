-- This script creates roles which will be be available for assigning to users later.
--/
CREATE OR REPLACE SCRIPT row_level_security_test_schema.add_user_role(role_name) returns table AS
        log = {}
        table_name = 'EXA_ROLES_MAPPING'

        function add_role_into_new_table()
                log[#log+1] = {"Table "..table_name.." not found. Creating a new table"}
                pquery([[CREATE TABLE ::table_name (EXA_ROLE VARCHAR(120),
                                                    EXA_ROLE_ID DECIMAL(2, 0) NOT NULL)]], {table_name=table_name})
                insert_role(1)
        end

        function insert_role(role_id)
                local success = query([[INSERT INTO ::table_name VALUES (:role_name, :role_id)]], {table_name=table_name, role_name=role_name, role_id=role_id})
                if success then
                        log[#log+1] = {"New role "..role_name.." was successfully added. Role id: "..role_id}
                else
                        error("An error happend during adding a new role "..role_name)
                end
        end

        function check_if_role_already_exists()
                local result = query([[SELECT * FROM ::table_name WHERE EXA_ROLE = :role_name]], {table_name=table_name, role_name=role_name})
                if #result > 0 then
                         error("The role '"..role_name.."' already exists. Please choose another name or use an existing role.")
                end
        end

        function find_free_role_id()
                for id=1, 63, 1 do
                        local result3 = query([[SELECT * FROM ::table_name WHERE EXA_ROLE_ID = :id]], {table_name=table_name, id=id})
                        if #result3 == 0 then
                                return id
                        end
                end
        end

        function add_role_into_existing_table()
                log[#log+1] = {"Table "..table_name.." exists. Adding a new role "..role_name}
                check_if_role_already_exists()
                local result2 = query([[SELECT EXA_ROLE_ID FROM ::table_name WHERE EXA_ROLE = :role_name]], {table_name=table_name, role_name=role_name})
                if #result2 > 0 then
                         error("The role"..role_name.."already exists. Please choose another name or use an existing role.")
                else
                        id=find_free_role_id()
                        insert_role(id)
                end
        end

        local success = pquery([[SELECT * FROM ::table_name]], {table_name=table_name})
        if not success then
                add_role_into_new_table()
        else
                add_role_into_existing_table()
        end
        return log, "LOG_MSG VARCHAR(2000000)"
/