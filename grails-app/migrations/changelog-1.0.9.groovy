databaseChangeLog = {

    changeSet(author: "Marcio Aguiar", id: "240120170808") {

        addColumn(tableName: "problem_i18n") {
            column(name: "user_suggest", type: "bigint")
        }

        createTable(tableName: "problem_i18n_aud") {
            column(name: "operation", type: "char(1)")
            column(name: "tstamp", type: "timestamp")

            column(name: "problem_id", type: "bigint") {
                constraints(nullable: "false")
            }
            column(name: "locale", type: "character varying(5)") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "character varying(255)")
            column(name: "description", type: "text")
            column(name: "input_format", type: "text")
            column(name: "output_format", type: "text")

            column(name: "user_suggest", type: "bigint")
        }

        createProcedure("""CREATE OR REPLACE FUNCTION process_problem_audit() RETURNS TRIGGER AS \$\$
                    BEGIN
                        IF(TG_OP = 'DELETE') THEN
                            INSERT INTO problem_i18n_aud SELECT 'D', now(), OLD.*;
                            RETURN OLD;
                        ELSIF(TG_OP = 'UPDATE') THEN
                            INSERT INTO problem_i18n_aud SELECT 'U', now(), NEW.*;
                            RETURN NEW;
                        ELSIF(TG_OP = 'INSERT') THEN
                            INSERT INTO problem_i18n_aud SELECT 'I', now(), NEW.*;
                            RETURN NEW;
                        END IF;
                        RETURN NULL;
                    END;
                \$\$ LANGUAGE plpgsql""")

        sql(/CREATE TRIGGER process_problem_audit
            AFTER INSERT OR UPDATE OR DELETE ON problem_i18n
                FOR EACH ROW EXECUTE PROCEDURE process_problem_audit()/)

    }

    changeSet(author: "Marcio Aguiar", id: "260120171547") {
        addColumn(tableName: "questionnaire") {
            column(name: "partial_score", type: "bool", defaultValue: "false")
        }

        addColumn(tableName: "submission") {
            column(name: "correct_test_cases", type: "integer", defaultValue: "-1")
            column(name: "total_test_cases", type: "integer", defaultValue: "-1")
        }
    }
}