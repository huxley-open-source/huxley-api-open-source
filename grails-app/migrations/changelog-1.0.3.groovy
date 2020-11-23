databaseChangeLog = {

  changeSet(author: "Diogo Cabral de Almeida", id: "071120161251") {

    createIndex(indexName: "submission_on_submission_date_idx", unique: false, tableName: "submission") {
      column(name: "submission_date", descending: true) //descending not working
    }

    createIndex(indexName: "user_on_username_uk", unique: true, tableName: "user") {
      column(name: "username")
    }

    createIndex(indexName: "user_on_email_uk", unique: true, tableName: "user") {
      column(name: "email")
    }

    createIndex(indexName: "role_on_authority_uk", unique: true, tableName: "role") {
      column(name: "authority")
    }

    createIndex(indexName: "pendency_key_on_hash_key_uk", unique: true, tableName: "pendency_key") {
      column(name: "hash_key")
    }
  }


  changeSet(author: "Marcio Aguiar", id: "081120161715") {
    dropForeignKeyConstraint(baseTableName: "questionnaire_user_penalty", constraintName: "questionnaire_user_penalty_questionnaire_user_id_fkey")

    sql("""UPDATE questionnaire_user_penalty
            SET questionnaire_user_id = subquery.user_id
            FROM (SELECT * from questionnaire_user) as subquery
            WHERE subquery.id = questionnaire_user_id""")

    addForeignKeyConstraint(
            constraintName: "questionnaire_user_penalty_user_id_fkey",
            baseColumnNames: "questionnaire_user_id", baseTableName: "questionnaire_user_penalty",
            referencedColumnNames: "id", referencedTableName: "user")

    dropTable(tableName: "questionnaire_user")
  }

  changeSet(author: "Diogo Cabral de Almeida", id: "091120161620") {
    renameColumn(tableName: "questionnaire_user_penalty", oldColumnName: "questionnaire_user_id", newColumnName: "user_id")
  }

}