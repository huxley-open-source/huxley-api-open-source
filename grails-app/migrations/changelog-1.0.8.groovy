databaseChangeLog = {

    changeSet(author: "Diogo Cabral de Almeida", id: "121220161342") {
        createTable(tableName: "problem_i18n") {
            column(name: "problem_id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "problem_i18n_pkey")
            }
            column(name: "locale", type: "character varying(5)") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "problem_i18n_pkey")
            }
            column(name: "name", type: "character varying(255)")
            column(name: "description", type: "text")
        }

        createIndex(indexName: "problem_i18n_problem_id_locale_idx", unique: true, tableName: "problem_i18n") {
            column(name: "problem_id")
            column(name: "locale")
        }

        sql("INSERT INTO problem_i18n SELECT id, 'pt_BR', name, description FROM problem")

        dropColumn(tableName: "problem", columnName: "name")
        dropColumn(tableName: "problem", columnName: "description")

        createTable(tableName: "test_case_i18n") {
            column(name: "test_case_id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "test_case_i18n_pkey")
            }
            column(name: "locale", type: "character varying(5)") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "test_case_i18n_pkey")
            }
            column(name: "tip", type: "text")
        }

        createIndex(indexName: "test_case_i18n_test_case_id_locale_idx", unique: true, tableName: "test_case_i18n") {
            column(name: "test_case_id")
            column(name: "locale")
        }

        sql("INSERT INTO test_case_i18n SELECT id, 'pt_BR', tip FROM test_case")

        dropColumn(tableName: "test_case", columnName: "tip")

        createTable(tableName: "topic_i18n") {
            column(name: "topic_id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "topic_i18n_pkey")
            }
            column(name: "locale", type: "character varying(5)") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "topic_i18n_pkey")
            }
            column(name: "name", type: "character varying(255)")
        }

        createIndex(indexName: "topic_i18n_topic_id_locale_idx", unique: true, tableName: "topic_i18n") {
            column(name: "topic_id")
            column(name: "locale")
        }

        sql("INSERT INTO topic_i18n SELECT id, 'pt_BR', name FROM topic")

        dropColumn(tableName: "topic", columnName: "name")

        addForeignKeyConstraint(constraintName: "problem_i18n_problem_id_fkey", baseColumnNames: "problem_id", baseTableName: "problem_i18n", referencedColumnNames: "id", referencedTableName: "problem")
        addForeignKeyConstraint(constraintName: "test_case_i18n_test_case_id_fkey", baseColumnNames: "test_case_id", baseTableName: "test_case_i18n", referencedColumnNames: "id", referencedTableName: "test_case")
        addForeignKeyConstraint(constraintName: "topic_i18n_topic_id_fkey", baseColumnNames: "topic_id", baseTableName: "topic_i18n", referencedColumnNames: "id", referencedTableName: "topic")
    }

    changeSet(author: "Diogo Cabral de Almeida", id: "161220161347") {
        addColumn(tableName: "problem_i18n") {
            column(name: "input_format", type: "text")
            column(name: "output_format", type: "text")
        }

        sql("""UPDATE problem_i18n p SET
                input_format = (SELECT input_format FROM problem where id = p.problem_id),
                output_format = (SELECT output_format FROM problem where id = p.problem_id)""")

        dropColumn(tableName: "problem", columnName: "input_format")
        dropColumn(tableName: "problem", columnName: "output_format")
    }

    changeSet(author: "Marcio Aguiar", id: "201220161609") {

        createTable(tableName: "problem_choice") {
            column(name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "problem_choice_pkey")
            }

            column(name: "problem_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "correct", type: "boolean")
            column(name: "choice_order", type: "integer")
        }

        addForeignKeyConstraint(constraintName: "problem_choice_problem_fkey", baseColumnNames: "problem_id",
                baseTableName: "problem_choice", referencedColumnNames: "id", referencedTableName: "problem")

        createTable(tableName: "problem_choice_i18n") {
            column(name: "problem_choice_id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "problem_choice_i18n_pkey")
            }
            column(name: "locale", type: "character varying(5)") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "problem_choice_i18n_pkey")
            }

            column(name: "description", type: "character varying(500)")
        }

        addColumn(tableName: "problem") {
            column(name: "problem_type", type: "integer", defaultValue: "0")
        }

        addColumn(tableName: "problem") {
            column(name: "base_code", type: "varchar(10000)")
        }

        addColumn(tableName: "problem") {
            column(name: "blank_lines", type: "integer[]")
        }

        createTable(tableName: "code_part_submission") {
            column(name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "code_part_submission_pkey")
            }

            column(name: "submission_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "problem_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "line_number", type: "integer")

            column(name: "code", type: "varchar(1000)")

        }

        addForeignKeyConstraint(
                constraintName: "code_part_submission_submission_fkey",
                baseColumnNames: "submission_id",
                baseTableName: "code_part_submission",
                referencedColumnNames: "id",
                referencedTableName: "submission")

    }

    changeSet(author: "Marcio Aguiar", id: "261220161837") {
        addColumn(tableName: "problem") {
            column(name: "base_language_id", type: "bigint")
        }

        addForeignKeyConstraint(
                constraintName: "base_language_language_fkey",
                baseColumnNames: "base_language_id",
                baseTableName: "problem",
                referencedColumnNames: "id",
                referencedTableName: "language")


        addColumn(tableName: "submission") {
            column(name: "choices", type: "bigint[]")
        }
    }


    changeSet(author: "Diogo Cabral de Almeida", id: "281220161421") {
        createTable(tableName: "test_case_evaluation") {
            column(name: "submission_id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "submission_result_pkey")
            }
            column(name: "test_case_id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "submission_result_pkey")
            }
            column(name: "evaluation", type: "bigint")
            column(name: "time", type: "double precision DEFAULT '-1'::bigint")
            column(name: "error_msg", type: "text")
            column(name: "diff", type: "text")
        }

        addForeignKeyConstraint(constraintName: "submission_result_submission_id_fkey", baseColumnNames: "submission_id", baseTableName: "test_case_evaluation", referencedColumnNames: "id", referencedTableName: "submission")
        addForeignKeyConstraint(constraintName: "submission_result_test_case_id_fkey", baseColumnNames: "test_case_id", baseTableName: "test_case_evaluation", referencedColumnNames: "id", referencedTableName: "test_case")

        dropColumn(tableName: "submission", columnName: "error_msg")
        dropColumn(tableName: "submission", columnName: "diff")
        dropColumn(tableName: "submission", columnName: "test_case_id")
    }

    changeSet(author: "Marcio Aguiar", id: "291220160822") {

        addColumn(tableName: "problem") {
            column(name: "quiz_only", type: "boolean", defaultValue: false)
        }
    }

    changeSet(author: "Marcio Aguiar", id: "030120171358") {

        addColumn(tableName: "message_group") {
            column(name: "locale", type: "varchar(5)", defaultValue: 'pt')
        }
    }

    changeSet(author: "Luiz Paulo Barroca", id: "110120171345") {

        addColumn(tableName: "user") {
            column(name: "locale", type: "varchar(5)", defaultValue: 'pt_BR')
        }
    }

}