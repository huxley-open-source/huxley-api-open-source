databaseChangeLog = {

    changeSet(author: "Marcio Aguiar", id: "110420171233") {

        sql("""UPDATE problem set nd = 5 WHERE nd > 8""")
        sql("""UPDATE problem set nd = 4 WHERE nd > 6 AND nd < 9""")
        sql("""UPDATE problem set nd = 3 WHERE nd > 4 AND nd < 7""")
        sql("""UPDATE problem set nd = 2 WHERE nd > 2 AND nd < 5""")
        sql("""UPDATE problem set nd = 1 WHERE nd < 3""")

    }

    changeSet(author: "Marcio Aguiar", id: "180420171553") {

        addColumn(tableName: "test_case") {
            column(name: "large", type: "bool", defaultValue: "false")
        }

    }
}