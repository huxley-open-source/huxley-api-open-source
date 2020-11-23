package com.thehuxley.excel;

import com.thehuxley.QuizReportExcelBeanV2;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by marcio on 21/12/17.
 */
public class QuizesToExcel {

    private Workbook wb;
    private OutputStream out;

    public QuizesToExcel(OutputStream out) {
        wb = new HSSFWorkbook();
        this.out = out;
    }

    public OutputStream build(List<QuizReportExcelBeanV2> scores) throws IOException {

        Map<Long, List<QuizReportExcelBeanV2>> quizById = scores.stream().
                collect(Collectors.groupingBy(s -> s.quizId));

        Sheet sheet1 = wb.createSheet("Resumo");

        List<UserQuizScores> userQuizes = new ArrayList<>();

        List<QuizReportExcelBeanV2> quizScores = new ArrayList<>();
        QuizReportExcelBeanV2 last = null;
        for (QuizReportExcelBeanV2 score : scores) {

            if (last == null || score.quizId == last.quizId) {
                quizScores.add(score);
            } else {
                List<UserQuizScores> q = createSheet(quizScores);

                if (q != null) {
                    userQuizes.addAll(q);
                }
                quizScores.clear();
            }

            last = score;
        }

        createSheet(quizScores);

        Map<Long, List<UserQuizScores>> byUser = userQuizes.stream().collect(Collectors.groupingBy(q -> q.userId));

        int i = 1;
        for(Long userId : byUser.keySet()) {
            List<UserQuizScores> scs = byUser.get(userId);

            if (i == 1) {
                Row row = sheet1.createRow(0);
                row.createCell(0).setCellValue("Aluno");

                int j = 1;
                for (UserQuizScores qs : scs) {
                    row.createCell(j++).setCellValue(j + ": (" + qs.quizScore + ") " + qs.quizName);
                }
            }

            UserQuizScores sc = scs.get(0);
            Row row = sheet1.createRow(i++);
            row.createCell(0).setCellValue(sc.userName);

            int j = 1;
            for (UserQuizScores qs : scs) {
                Cell cell = row.createCell(j++);
                cell.setCellValue(qs.userScore);
            }
        }

        sheet1.autoSizeColumn(0);
        sheet1.createFreezePane(0, 1);

        wb.write(out);
        out.flush();
        wb.write(new FileOutputStream("test.xls"));
        return out;
    }

    public List<UserQuizScores> createSheet(List<QuizReportExcelBeanV2> scores) {

        if (scores.isEmpty()) return null;

        Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(scores.get(0).quizName));

        Map<Long, List<QuizReportExcelBeanV2>> byUser = scores.stream()
                .collect(Collectors.groupingBy(s -> s.userId));


        int i = 1;

        CellStyle success = successStyle();
        CellStyle fail = failStyle();

        List<UserQuizScores> quizScores = new ArrayList<>();
        for (Long userId : byUser.keySet()) {


            List<QuizReportExcelBeanV2> problems = byUser.get(userId);

            if (problems.isEmpty()) continue;

            QuizReportExcelBeanV2 first = problems.get(0);
            if (i == 1) {
                createHeaderRow(sheet, problems.size());
            }


            Row row = sheet.createRow(i++);
            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(first.userName);

            int col = 1;
            float total = 0;
            for (QuizReportExcelBeanV2 score : problems) {
                Cell cell = row.createCell(col++);
                float finalScore = getScore(score);
                total += finalScore;
                cell.setCellValue(finalScore);
            }
            Cell cell = row.createCell(col);
            cell.setCellValue(total);

            if (total < problems.get(0).quizScore * 0.7) {
                cell.setCellStyle(fail);
            } else {
                cell.setCellStyle(success);
            }

            quizScores.add(new UserQuizScores(userId, first.userName, first.quizName, first.quizId, first.quizScore, total));
        }

        sheet.autoSizeColumn(0);
        sheet.createFreezePane(0, 1);
        return quizScores;
    }

    private void createHeaderRow(Sheet sheet, int problemSize) {

        Row header = sheet.createRow(0);

        Font font = wb.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);

        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        header.setRowStyle(style);

        Cell cell = header.createCell(0);
        cell.setCellValue("Aluno");
        cell.setCellStyle(style);
        for (int j = 1; j < problemSize + 2; j++) {
            cell = header.createCell(j);
            cell.setCellValue(j);
            cell.setCellStyle(style);
        }

        cell = header.createCell(problemSize + 2);
        cell.setCellValue("TOTAL");
        cell.setCellStyle(style);
    }

    private CellStyle successStyle() {

        Font font = wb.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);

        CellStyle success = wb.createCellStyle();
        success.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        success.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        success.setFont(font);
        return success;
    }

    private CellStyle failStyle() {

        Font font = wb.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);

        CellStyle fail = wb.createCellStyle();
        fail.setFillForegroundColor(IndexedColors.RED.getIndex());
        fail.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        fail.setFont(font);

        return fail;
    }
    public float getScore(QuizReportExcelBeanV2 score) {
        float result = score.partialQuiz ? score.partialScore : score.score;

        if (score.restrictionPenalty != null && score.restrictionPenalty > 0 && (score.restrictionErrorCount != null && score.restrictionErrorCount > 0)) {
            result = result * (1 - (score.restrictionPenalty / 100));
        }

        if (score.penalty > -1) {
            result = score.penalty;
        }

        return result;
    }


    public class UserQuizScores {
        long userId;
        String userName;
        String quizName;
        long quizId;
        float quizScore;
        float userScore;

        public UserQuizScores(long userId, String userName, String quizName, long quizId, float quizScore, float userScore) {
            this.userId = userId;
            this.userName = userName;
            this.quizName = quizName;
            this.quizId = quizId;
            this.quizScore = quizScore;
            this.userScore = userScore;
        }
    }

}
