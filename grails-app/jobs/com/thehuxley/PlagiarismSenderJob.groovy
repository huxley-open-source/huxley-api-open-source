package com.thehuxley

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.util.Environment
import groovy.json.JsonOutput
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClients
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.util.EntityUtils
import org.hibernate.FetchMode
import org.hibernate.transform.Transformers

import com.google.common.cache.CacheBuilder
import org.joda.time.LocalDate

import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class PlagiarismSenderJob {

    static triggers = {
        simple startDelay: 10000, repeatInterval: 5 * 60 * 1000
    }

    def final groupCache = CacheBuilder.newBuilder()
            .expireAfterAccess(12, TimeUnit.HOURS)
            .build()

    def final httpClient = HttpClients.createDefault()

    def final submissions_path = '/api/submissions'

    def final groups_path = '/api/groups'

    def concurrent = false

    GrailsApplication grailsApplication

    String uri

    def sessionFactory

    def localContext

    static executionsCount = 0

    def execute() {
        if (Environment.isDevelopmentMode()) {
            log.info 'Job de plágio desativado em dev'
            return
        }
        uri = grailsApplication.config.plagiarism.uri

        def credentialsProvider = new BasicCredentialsProvider()

        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                grailsApplication.config.plagiarism.username.toString(),
                grailsApplication.config.plagiarism.password.toString()))

        localContext = HttpClientContext.create()

        localContext.setCredentialsProvider(credentialsProvider)

        def minSubmissionDate = (executionsCount % 20 == 0 ?
                new LocalDate(2017, 1, 1) : new LocalDate().minusMonths(1)).toString("yyyy-MM-dd")
        executionsCount++

        def query = """
            select * from (
                select
                    i.id as institution_id,
                    i.name as institution_name,
                    g.id as group_id,
                    g.name as group_name,
                    q.id as questionnaire_id,
                    q.title as questionnaire_name,
                    p.id as problem_id,
                    pi18n.name as problem_name,
                    l.id as language_id,
                    l.name as language_name,
                    s.id as submission_id,
                    s.filename as filename,
                    (select array_to_string(array_agg(test_case_id order by test_case_id asc), '-') from test_case_evaluation where submission_id = s.id and evaluation = 0) as correct
                from questionnaire q
                inner join questionnaire_problem qp on (qp.questionnaire_id = q.id)
                inner join problem p on (p.id = qp.problem_id)
                inner join problem_i18n pi18n on (pi18n.problem_id = p.id)
                inner join user_group ug on (ug.group_id = q.group_id)
                inner join public.group g on (g.id = ug.group_id)
                inner join institution i on (i.id = g.institution_id)
                inner join submission s on (s.problem_id = qp.problem_id and s.user_id = ug.user_id)
                inner join language l on (l.id = s.language_id)
                where 1 = 1
                and s.submission_date <= q.end_date and s.submission_date > '$minSubmissionDate'
                and ug.role = 0
                and pi18n.locale = 'pt_BR'
                and not exists (select 1 from questionnaire_submission where questionnaire_id = q.id and submission_id = s.id)
                order by s.id asc) as a
            where length(a.correct) > 0 limit 1000
        """

        def submissions = sessionFactory.currentSession.createSQLQuery(query)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .list()

        submissions.each { submission ->
            def groupInformation = new LinkedHashMap<>()

            String correct = crypt(submission['correct'])

            def groupPath = [submission['institution_id'], submission['group_id'], submission['questionnaire_id'],
                             submission['problem_id'], correct, submission['language_id']]

            groupInformation.put('/' << groupPath.with { it[0..0] }.join('/'), submission['institution_name'])
            groupInformation.put('/' << groupPath.with { it[0..1] }.join('/'), submission['group_name'])
            groupInformation.put('/' << groupPath.with { it[0..2] }.join('/'), submission['questionnaire_name'])
            groupInformation.put('/' << groupPath.with { it[0..3] }.join('/'), submission['problem_name'])
            groupInformation.put('/' << groupPath.with { it[0..4] }.join('/'), correct)
            groupInformation.put('/' << groupPath.with { it[0..5] }.join('/'), submission['language_name'])

            def parentGroupId

            groupInformation.each { key, value ->
                parentGroupId = groupCache.asMap().get(key.toString()) ?: getGroup(key.toString(), value, parentGroupId)
                if (groupCache.asMap().containsKey(key.toString())) {
                    log.debug "Caching $key -> ${parentGroupId}"
                    groupCache.asMap().put(key.toString(), parentGroupId)
                }
            }

            log.info "Enviando a submissão para o detector de similaridade. submission.id = " + submission['submission_id']

            def statusCode = sendSubmission(parentGroupId, '/' << groupPath.join('/'), submission['submission_id'])

            if (statusCode == 201) {
                sessionFactory.currentSession.createSQLQuery(
                        "INSERT INTO questionnaire_submission VALUES (:questionnaire_id, :submission_id)")
                        .setParameter("questionnaire_id", submission['questionnaire_id'])
                        .setParameter("submission_id", submission['submission_id'])
                        .executeUpdate()

                log.info "Submissão enviada. submission.id = " + submission['submission_id']
            } else if (statusCode == 302) {
                log.info "Submissão já existe. submission.id = " + submission['submission_id']

                def result = sessionFactory.currentSession.createSQLQuery(
                        "SELECT 1 FROM questionnaire_submission WHERE questionnaire_id = :questionnaire_id and submission_id = :submission_id")
                        .setParameter("questionnaire_id", submission['questionnaire_id'])
                        .setParameter("submission_id", submission['submission_id'])
                        .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                        .list()

                if (result.size() > 0) {
                    sessionFactory.currentSession.createSQLQuery(
                            "INSERT INTO questionnaire_submission VALUES (:questionnaire_id, :submission_id)")
                            .setParameter("questionnaire_id", submission['questionnaire_id'])
                            .setParameter("submission_id", submission['submission_id'])
                            .executeUpdate()
                }
            } else {
                throw new Exception("Não foi possível enviar a submissão")
            }
        }
    }

    def sendSubmission(groupId, privateId, submissionId) {
        def submission = Submission.createCriteria().get {
            fetchMode("language", FetchMode.JOIN)
            eq("id", submissionId.longValue())
        }

        def entity = MultipartEntityBuilder.create()
            .addTextBody("group_id", groupId.toString())
            .addTextBody("private_id", [privateId, "/", submission.id].join(''))
            .addTextBody("language", submission.language.name)
            .addBinaryBody("file",
                new ByteArrayInputStream(submission.getSourceCode().bytes),
                ContentType.TEXT_PLAIN,
                submission.originalFilename)
            .build()

        def uploadSubmissionRequest = new HttpPost([uri, submissions_path].join(''))

        uploadSubmissionRequest.setEntity(entity)

        def response = httpClient.execute(uploadSubmissionRequest, localContext)

        log.debug EntityUtils.toString(response.getEntity())

        response.getStatusLine().getStatusCode()
    }

    def getGroup(privateId, groupName, parentGroupId) {
        def checkIfGroupRequest = new HttpGet([uri, groups_path, "?private_id=${privateId}"].join(''))

        def response = httpClient.execute(checkIfGroupRequest, localContext)

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new Exception("Não foi possível enviar a submissão para o avaliador de plágio.")
        }

        def groups = JSON.parse(EntityUtils.toString(response.getEntity()))

        if (groups.size() > 0) {
            groups.get(0).id
        } else {
            def data = [private_id: privateId, name: groupName]

            if (parentGroupId) {
                data << [parent_group: [id: parentGroupId]]
            }

            saveGroup(data)
        }
    }

    def saveGroup(groupData) {
        def saveGroupRequest = new HttpPost([uri, groups_path].join(''))

        saveGroupRequest.setEntity(new StringEntity(
                JsonOutput.toJson(groupData),
                ContentType.APPLICATION_JSON))

        def response = httpClient.execute(saveGroupRequest, localContext)

        if (response.getStatusLine().getStatusCode() == 201) {
            JSON.parse(EntityUtils.toString(response.getEntity())).id
        } else {
            throw new Exception("Não foi possível enviar a submissão para o avaliador de plágio.")
        }
    }

    private String crypt(String str) {
        try {

            MessageDigest md = MessageDigest.getInstance( "SHA1" );
            md.update(str.getBytes());

            return byteArrayToHexString(md.digest());

        } catch (Exception ex) {
            throw new RuntimeException("error-while-hashing", ex);
        }

    }

    private String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result +=
                    Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

}
