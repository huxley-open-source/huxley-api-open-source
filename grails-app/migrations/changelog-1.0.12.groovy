databaseChangeLog = {

    changeSet(author: "Diogo Cabral de Almeida", id: "020320171431") {

        dropIndex(indexName: "user_on_username_uk", unique: true, tableName: "user")

        createIndex(indexName: "user_on_username_uk", unique: true, tableName: "user") {
            column(name: "lower(username)")
        }
    }

    changeSet(author: "Diogo Cabral de Almeida", id: "100320171359") {
        modifyDataType(tableName: "code_part_submission", columnName: "code", newDataType: "text")
    }

    changeSet(author: "Diogo Cabral de Almeida", id: "170320171413") {
        createProcedure("""
            CREATE OR REPLACE FUNCTION fix_user_group_data() RETURNS integer AS \$\$
            DECLARE 
              user_group_all record;
              wrong_records user_group%rowtype;
              row_count integer;
            BEGIN
              row_count := 0;
              FOR user_group_all IN
                SELECT 
                    user_id, 
                    group_id, 
                    role, 
                    count(*) 
                FROM user_group 
                GROUP BY user_id, group_id, role 
                HAVING count(*) > 1
              LOOP    
                FOR wrong_records IN
                  SELECT id FROM user_group 
                  WHERE 
                  user_id = user_group_all.user_id and group_id = user_group_all.group_id and role = user_group_all.role 
                  ORDER BY id DESC
                LOOP
                  DELETE FROM user_group WHERE id = wrong_records.id;
                  row_count = row_count + 1;
                END LOOP;
                INSERT INTO user_group (id, version, user_id, group_id, role, enabled) VALUES (nextval('user_group_id_seq'), 0, user_group_all.user_id, user_group_all.group_id, user_group_all.role, true);
              END LOOP;

              RETURN row_count;
            END;
            \$\$ LANGUAGE plpgsql
        """)

        sql("SELECT fix_user_group_data()")

        sql("DROP FUNCTION fix_user_group_data()")

        createIndex(indexName: "user_group_on_user_id_group_id_role_uk", unique: true, tableName: "user_group") {
            column(name: "user_id")
            column(name: "group_id")
            column(name: "role")
        }
    }

}