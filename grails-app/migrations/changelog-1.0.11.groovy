databaseChangeLog = {

    changeSet(author: "Diogo Cabral de Almeida", id: "150220171349") {

        sql('''update submission s set 
                time = (
                    select max(time) from test_case_evaluation 
                    where submission_id = s.id 
                    group by submission_id
                )''')
    }

}