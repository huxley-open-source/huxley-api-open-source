databaseChangeLog = {

    changeSet(author: "Diogo Cabral de Almeida", id: "051220161357") {
        dropTable(tableName: "old_message")
        dropTable(tableName: "old_message_message")
    }

    changeSet(author: "Luiz Paulo Barroca", id: "071220160925") {
        createTable(tableName: "user_tip_vote") {
            column(name: "version", type: "bigint")
            column(name: "user_id", type: "bigint") {
                constraints(nullable: "false")
            }
            column(name: "test_case_id", type: "bigint") {
                constraints(nullable: "false")
            }
            column(name: "useful", type: "boolean") {
                constraints(nullable: "false")
            }
        }
        addPrimaryKey(columnNames: "user_id, test_case_id", constraintName:"user_tip_vote_pkey", tableName: "user_tip_vote")
        addForeignKeyConstraint(constraintName: "user_tip_vote_test_case_fkey", baseColumnNames: "test_case_id",
                baseTableName: "user_tip_vote", referencedColumnNames: "id", referencedTableName: "test_case")

        addForeignKeyConstraint(constraintName: "user_tip_vote_user_fkey", baseColumnNames: "user_id",
                baseTableName: "user_tip_vote", referencedColumnNames: "id", referencedTableName: "user")
    }

    changeSet(author: "Luiz Paulo Barroca", id: "071220161408") {
        sql("UPDATE language SET compile_params='-lm -lcrypt -O2 -pipe -w' where id=1")
        sql("UPDATE language SET compile_params='-lm -lcrypt -std=c++11 -O2 -pipe -w' where id=4")
    }

    changeSet(author: "Diogo Cabral de Almeida", id: "071220161607") {
        sql("""DELETE FROM plagiarism WHERE status = 0 AND (submission1_id, submission2_id) IN (
                SELECT submission1_id, submission2_id FROM plagiarism 
                GROUP BY submission1_id, submission2_id HAVING count(*) > 1)""")

        createIndex(indexName: "plagiarism_on_submission1_id_submission2_id_idx", unique: true, tableName: "plagiarism") {
          column(name: "submission1_id")
          column(name: "submission2_id")
        }
    }

    changeSet(author: "Diogo Cabral de Almeida", id: "091220161030") {
        sql("""DELETE FROM questionnaire_submission""")
    }

    changeSet(author: "Diogo Cabral de Almeida", id: "101220160015") {
        createProcedure("""
            CREATE OR REPLACE FUNCTION fix_plagiarism_data() RETURNS integer AS \$\$
            DECLARE 
              plagiarism_todos record;
              wrong_records plagiarism%rowtype;
              row_count integer;
            BEGIN
              row_count := 0;
              FOR plagiarism_todos IN
                SELECT
                  CASE 
                    WHEN submission1_id < submission2_id THEN submission1_id
                    ELSE submission2_id
                  END AS source,
                  CASE 
                    WHEN submission2_id > submission1_id THEN submission2_id
                    ELSE submission1_id
                  END AS target,
                  count(*)
                FROM plagiarism
                GROUP BY source, target
                HAVING count(*) > 1
              LOOP    
                FOR wrong_records IN
                  SELECT * FROM plagiarism 
                  WHERE 
                  (submission1_id = plagiarism_todos.source AND submission2_id = plagiarism_todos.target)  
                  OR 
                  (submission1_id = plagiarism_todos.target AND submission2_id = plagiarism_todos.source)
                  ORDER BY id DESC
                LOOP
                  DELETE FROM plagiarism WHERE id = wrong_records.id;
                  EXIT;
                END LOOP;
              END LOOP;
              
              FOR wrong_records IN
                select p.* from plagiarism p
                inner join submission s1 on (s1.id = p.submission1_id)
                inner join submission s2 on (s2.id = p.submission2_id)
                where s1.submission_date > s2.submission_date
              LOOP
                UPDATE plagiarism SET submission1_id = wrong_records.submission2_id, submission2_id = wrong_records.submission1_id WHERE id = wrong_records.id;
                row_count = row_count + 1;
              END LOOP;
              RETURN row_count;
            END;
            \$\$ LANGUAGE plpgsql
        """)

        sql("SELECT fix_plagiarism_data()")

        sql("DROP FUNCTION fix_plagiarism_data()")
    }

    changeSet(author: "Luiz Paulo de Melo Barroca", id: "121220161416") {
        sql("UPDATE language SET compile_params='-Tlinux -v0 -l- 1>&2' where id=3")
        sql("UPDATE language SET compile_params='-qfH' where id=7")
    }

}