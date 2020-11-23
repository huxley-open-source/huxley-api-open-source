package com.thehuxley

import org.jxls.common.Context
import org.jxls.util.JxlsHelper
import org.springframework.context.i18n.LocaleContextHolder

import java.text.Collator
import java.text.SimpleDateFormat

class FileExportService {

    def questionnaireService
    def simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

    def exportToExcel(Questionnaire quiz) {
        def bean = new QuizReportExcelBean()

        bean.quizTitle = quiz.title
        bean.quizDescription = quiz.description
        bean.quizScore = quiz.score
        bean.groupName = quiz.group.name
        bean.startDate = simpleDateFormat.format(quiz.startDate)
        bean.endDate = simpleDateFormat.format(quiz.endDate)

        def userScore = [:]

        questionnaireService.findScores(quiz.id).each { questionnaireScore ->
            def user = User.get(questionnaireScore['userId'])

            userScore[user] = (userScore[user] ?: 0) + (quiz.partialScore ? questionnaireScore['partialScore'] : questionnaireScore['score'])
        }

        Collator collator = Collator.getInstance(LocaleContextHolder.locale);

        userScore = userScore.sort(new Comparator<User>() {
            @Override
            int compare(User o1, User o2) {
                return collator.compare(o1.name, o2.name)
            }
        })

        userScore.each { user, score ->
            bean.students.add(new QuizReportStudentBean(name: user.name, score: score))
        }

        def is = FileExportService.class.getResourceAsStream('/data/huxley-quiz-template.xlsx')
        def os = new ByteArrayOutputStream()

        Context context = new Context()

        context.putVar("bean", bean)

        JxlsHelper.getInstance().processTemplate(is, os, context)

        byte[] file = os.toByteArray()
        is.close()
        os.close()

        return file
    }
}

class QuizReportExcelBean {
    String quizTitle
    String quizDescription
    double quizScore
    String startDate
    String endDate
    String groupName

    List<QuizReportStudentBean> students = new ArrayList<QuizReportStudentBean>()

}

class QuizReportStudentBean {
    String name
    double score
}

class QuizReportExcelBeanV2 {
    public long quizId;
    public String quizName;
    public boolean partialQuiz;
    public float quizScore;
    public long quizProblemId;
    public long problemId;
    public long userId;
    public String userName;
    public float penalty;
    public float score;
    public float partialScore;
    public Float restrictionPenalty;
    public int submissionCount;
    public Integer restrictionErrorCount;
}