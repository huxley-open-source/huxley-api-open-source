databaseChangeLog = {

  changeSet(author: "Marcio Aguiar", id: "161120161728") {
    sql("UPDATE questionnaire_user_penalty set penalty = 0 where penalty < 0")

    sql("""UPDATE questionnaire_user_penalty
            SET penalty = subquery.score - penalty
            FROM (SELECT * from questionnaire_problem) as subquery
            WHERE subquery.id = questionnaire_problem_id""")
  }

  changeSet(author: "Diogo Cabral de Almeida", id: "141120161418") {
    renameColumn(tableName: "submission", oldColumnName: "submission", newColumnName: "original_filename")

    dropColumn(tableName: "submission", columnName: "output")

    addColumn(tableName: "submission") {
      column(name: "filename", type: "character varying(255)")
    }
    
    dropTable(tableName: "license")
    dropTable(tableName: "license_pack")
    dropTable(tableName: "license_type")

    renameSequence(oldSequenceName: "cluster_id_seq", newSequenceName: "group_id_seq")
    renameSequence(oldSequenceName: "user_cluster_id_seq", newSequenceName: "user_group_id_seq")

    renameTable(oldTableName: "cluster", newTableName: "group")
    renameTable(oldTableName: "user_cluster", newTableName: "user_group")

    renameColumn(tableName: "user_group", oldColumnName: "cluster_id", newColumnName: "group_id")

    dropForeignKeyConstraint(baseTableName: "group", constraintName: "cluster_institution_id_fkey")
    dropForeignKeyConstraint(baseTableName: "user_group", constraintName: "user_cluster_cluster_id_fkey")
    dropForeignKeyConstraint(baseTableName: "user_group", constraintName: "user_cluster_user_id_fkey")

    addForeignKeyConstraint(constraintName: "group_institution_id_fkey", baseColumnNames: "institution_id", baseTableName: "group", referencedColumnNames: "id", referencedTableName: "institution")
    addForeignKeyConstraint(constraintName: "user_group_group_id_fkey", baseColumnNames: "group_id", baseTableName: "user_group", referencedColumnNames: "id", referencedTableName: "group")
    addForeignKeyConstraint(constraintName: "user_group_user_id_fkey", baseColumnNames: "user_id", baseTableName: "user_group", referencedColumnNames: "id", referencedTableName: "user")

    dropIndex(indexName: "cluster_on_institution_id_idx", tableName: "cluster")
    dropIndex(indexName: "user_cluster_on_cluster_id_idx", tableName: "user_cluster")
    dropIndex(indexName: "user_cluster_on_user_id_idx", tableName: "user_cluster")

    createIndex(indexName: "user_group_on_cluster_id_idx", tableName: "user_group") {
      column(name: "group_id")
    }

    createIndex(indexName: "user_group_on_user_id_idx", tableName: "user_group") {
      column(name: "user_id")
    }
  }

  changeSet(author: "Diogo Cabral de Almeida", id: "171120161320") {
    createTable(tableName: "questionnaire_submission") {
        column(name: "questionnaire_id", type: "bigint") {
            constraints(nullable: "false", primaryKey: "true", primaryKeyName: "questionnaire_submission_pkey")
        }
        column(name: "submission_id", type: "bigint") {
            constraints(nullable: "false", primaryKey: "true", primaryKeyName: "questionnaire_submission_pkey")
        }
    }

    addForeignKeyConstraint(constraintName: "questionnaire_submission_questionnaire_id_fkey", baseColumnNames: "questionnaire_id", baseTableName: "questionnaire_submission", referencedColumnNames: "id", referencedTableName: "questionnaire")
    addForeignKeyConstraint(constraintName: "questionnaire_submission_submission_id_fkey", baseColumnNames: "submission_id", baseTableName: "questionnaire_submission", referencedColumnNames: "id", referencedTableName: "submission")

    dropColumn(tableName: "submission", columnName: "plagiarism_status_id")

    dropTable(tableName: "plagiarism_status")

    renameColumn(tableName: "submission", oldColumnName: "diff_file", newColumnName: "diff")
  }

}