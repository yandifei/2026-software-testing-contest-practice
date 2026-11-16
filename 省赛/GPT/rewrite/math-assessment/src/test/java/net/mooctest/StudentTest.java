package net.mooctest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.lang.reflect.*;
import net.mooctest.pricing.domain.*;
import net.mooctest.pricing.service.*;
import net.mooctest.refund.domain.*;
import net.mooctest.refund.service.*;
import net.mooctest.enrollment.domain.*;
import net.mooctest.enrollment.service.*;
import net.mooctest.enrollment.port.*;
import net.mooctest.enrollment.adapter.*;
import net.mooctest.scheduling.domain.*;
import net.mooctest.scheduling.service.*;
import net.mooctest.grading.domain.*;
import net.mooctest.grading.service.*;
import net.mooctest.grading.expression.*;
import net.mooctest.progress.domain.*;
import net.mooctest.progress.service.*;
import java.time.LocalDate;
import java.util.Set;
import net.mooctest.expression.engine.AbnormalOperation;
import net.mooctest.expression.engine.BooleanCalculation;
import net.mooctest.expression.engine.BooleanCalculation2;
import net.mooctest.expression.engine.BracketsCalculation;
import net.mooctest.expression.engine.BracketsCalculation2;
import net.mooctest.expression.engine.BracketsExpression;
import net.mooctest.expression.engine.Calculation;
import net.mooctest.expression.engine.CalculationBooleanResults;
import net.mooctest.expression.engine.CalculationManagement;
import net.mooctest.expression.engine.CalculationNumberResults;
import net.mooctest.expression.engine.CalculationOptimized;
import net.mooctest.expression.engine.CalculationResults;
import net.mooctest.expression.engine.ConstantRegion;
import net.mooctest.expression.engine.CumulativeCalculation;
import net.mooctest.expression.engine.ExtractException;
import net.mooctest.expression.engine.LogResults;
import net.mooctest.expression.engine.Mathematical_Expression;
import net.mooctest.expression.engine.NameExpression;
import net.mooctest.expression.engine.NumberCalculation;
import net.mooctest.expression.engine.NumberUtils;
import net.mooctest.expression.engine.PackExpression;
import net.mooctest.expression.engine.PrefixExpression;
import net.mooctest.expression.engine.PrefixExpressionOperation;
import net.mooctest.expression.engine.StrUtils;
import net.mooctest.expression.engine.WrongFormat;
import net.mooctest.grading.domain.AnswerResult;
import net.mooctest.grading.domain.ExamSubmission;
import net.mooctest.grading.domain.GradeReport;
import net.mooctest.grading.domain.GradingPolicy;
import net.mooctest.grading.domain.QuestionType;
import net.mooctest.grading.expression.ExpressionAnswerEvaluation;
import net.mooctest.grading.expression.ExpressionAnswerGradingService;
import net.mooctest.grading.expression.ExpressionAnswerStatus;
import net.mooctest.grading.expression.ExpressionAnswerSubmission;
import net.mooctest.grading.expression.ExpressionEvaluationException;
import net.mooctest.grading.expression.ExpressionExamGrade;
import net.mooctest.grading.expression.ExpressionExamGradingService;
import net.mooctest.grading.expression.ExpressionExamSubmission;
import net.mooctest.grading.expression.ExpressionQuestion;
import net.mooctest.grading.expression.MathematicalExpressionEvaluator;
import net.mooctest.grading.service.ExamGradingService;
import net.mooctest.progress.domain.CourseProgressSnapshot;
import net.mooctest.progress.domain.LessonProgress;
import net.mooctest.progress.domain.LessonType;
import net.mooctest.progress.domain.ProgressEvaluation;
import net.mooctest.progress.service.LearningProgressService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import top.lingyuzhao.varFormatter.core.VarFormatter;
import top.lingyuzhao.varFormatter.utils.DataObj;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Province-round single-file test suite for the math-assessment task.
 *
 * <p>Two independently written suites were merged: the local rewrite suite and the
 * reference suite. Every expected value below is an independently hand-computed worked
 * example or boundary of the public API; no production method is modified.</p>
 */
class StudentTest {
    static final Instant T = Instant.parse("2026-06-01T06:00:00Z");
    static final Clock FIXED = Clock.fixed(T, ZoneOffset.UTC);
    static BigDecimal decimal(String number) { return new BigDecimal(number); }
    static void amount(String expected, BigDecimal actual) { assertEquals(new BigDecimal(expected).setScale(2), actual.setScale(2)); }

    /** Reconstructs ordinary records through their public constructor, without altering behavior. */
    @SuppressWarnings("unchecked")
    static <R> R changed(R original, Object... fields) {
        Map<String,Object> replacements = new HashMap<>();
        for(int k=0;k<fields.length;k+=2) replacements.put((String)fields[k],fields[k+1]);
        try {
            RecordComponent[] parts = original.getClass().getRecordComponents();
            Object[] args = new Object[parts.length]; Class<?>[] signature = new Class<?>[parts.length];
            for(int k=0;k<parts.length;k++) {
                String key=parts[k].getName(); signature[k]=parts[k].getType();
                args[k]=replacements.containsKey(key)?replacements.remove(key):parts[k].getAccessor().invoke(original);
            }
            assertTrue(replacements.isEmpty(), "Unknown fixture fields: "+replacements.keySet());
            return (R) original.getClass().getConstructor(signature).newInstance(args);
        } catch(InvocationTargetException ex) { throw new AssertionError(ex.getCause()); }
        catch(ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    static <R> void rejects(R original, Consumer<R> call, String expected, Object... fields) {
        IllegalArgumentException ex=assertThrows(IllegalArgumentException.class,()->call.accept(changed(original,fields)));
        assertEquals(expected,ex.getMessage());
    }
    static AnswerResult answer(Object... changes) {
        return changed(new AnswerResult("q",QuestionType.SINGLE_CHOICE,"core",decimal("80"),false,true,true,4,4,0,null,null,0,0,0,true,false,false,false),changes);
    }
    static GradingPolicy policy(Object... changes) {
        return changed(new GradingPolicy(60,true,true,decimal("0.25"),decimal("0.10"),10,50,0,true,true,3,true,true,true),changes);
    }
    static ExamSubmission exam(Object... changes) {
        return changed(new ExamSubmission("exam","learner",List.of(answer()),T,T.plusSeconds(600),null,600,1,true,true,false,false,false,0,0,BigDecimal.ZERO,null,null),changes);
    }
    static GradeReport grade(AnswerResult a, GradingPolicy p) { return new ExamGradingService().grade(exam("answers",List.of(a)),p); }

    @Test void gradingWorkedExamplesForEveryQuestionType() {
        for(QuestionType kind:QuestionType.values()) {
            AnswerResult a=answer("type",kind,"totalTests",4,"passedTests",4);
            GradeReport full=grade(a,policy()); amount("80",full.rawPoints()); amount("80",full.finalPoints()); amount("80",full.maximumPoints()); assertEquals(100,full.percentage()); assertEquals("A+",full.letterGrade()); assertTrue(full.passed()); assertEquals(Map.of("core",100),full.sectionPercentages());
            amount("64",grade(changed(a,"timedOut",true),policy()).rawPoints());
        }
        amount("30",grade(answer("type",QuestionType.MULTIPLE_CHOICE,"exactMatch",false,"selectedCorrectOptions",2,"selectedIncorrectOptions",2),policy()).rawPoints());
        amount("20",grade(answer("type",QuestionType.FILL_BLANK,"exactMatch",false,"rubricPercent",25),policy()).rawPoints());
        amount("40",grade(answer("type",QuestionType.ESSAY,"exactMatch",false,"rubricPercent",50,"manualReviewRequired",true),policy()).rawPoints());
        amount("40",grade(answer("type",QuestionType.PROGRAMMING,"totalTests",6,"passedTests",3),policy()).rawPoints());
        amount("24",grade(answer("type",QuestionType.PROGRAMMING,"totalTests",6,"passedTests",3,"timedOut",true),policy()).rawPoints());
        amount("0",grade(answer("type",QuestionType.PROGRAMMING,"totalTests",6,"passedTests",3,"plagiarismFlag",true),policy()).rawPoints());
        amount("8",grade(answer("type",QuestionType.PROGRAMMING,"compilationSucceeded",false),policy("programmingCompilationRequired",false)).rawPoints());
        amount("0",grade(answer("type",QuestionType.PROGRAMMING,"compilationSucceeded",false),policy()).rawPoints());
        amount("0",grade(answer("type",QuestionType.PROGRAMMING,"manualReviewRequired",true),policy()).rawPoints());
    }
    @Test void numericGradingToleranceAndPartialCredit() {
        String[] errors={"-1","0","1","5.5","10","10.01"}; String[] scores={"80","80","80","40","0","0"};
        for(int k=0;k<errors.length;k++) amount(scores[k],grade(answer("type",QuestionType.NUMERIC,"exactMatch",false,"numericError",decimal(errors[k]),"numericTolerance",BigDecimal.ONE),policy()).rawPoints());
        amount("0",grade(answer("type",QuestionType.NUMERIC,"exactMatch",false,"numericError",BigDecimal.ONE,"numericTolerance",BigDecimal.ZERO),policy()).rawPoints());
        amount("80",grade(answer("type",QuestionType.NUMERIC,"exactMatch",false,"numericError",BigDecimal.ZERO,"numericTolerance",BigDecimal.ZERO),policy()).rawPoints());
        for(String missing:List.of("numericError","numericTolerance")) {
            var a=answer("type",QuestionType.NUMERIC,"exactMatch",false,"numericError",BigDecimal.ONE,"numericTolerance",BigDecimal.ONE,missing,null);
            var r=grade(a,policy()); amount("0",r.rawPoints()); assertTrue(r.warnings().contains("NUMERIC_TOLERANCE_DATA_MISSING:q"));
        }
    }
    @Test void gradingPenaltiesAreObservableAcrossSeveralAnswers() {
        var correct=answer("questionId","credit");
        for(QuestionType kind:List.of(QuestionType.SINGLE_CHOICE,QuestionType.TRUE_FALSE,QuestionType.FILL_BLANK,QuestionType.NUMERIC)) {
            var incorrect=answer("type",kind,"exactMatch",false,"numericError",decimal("20"),"numericTolerance",BigDecimal.ONE);
            var r=new ExamGradingService().grade(exam("answers",List.of(correct,incorrect)),policy("partialCreditEnabled",false)); amount("60",r.rawPoints()); assertEquals(38,r.percentage());
            amount("80",new ExamGradingService().grade(exam("answers",List.of(correct,incorrect)),policy("negativeMarkingEnabled",false,"partialCreditEnabled",false)).rawPoints());
        }
        var unanswered=answer("answered",false,"mandatory",true);
        var r=new ExamGradingService().grade(exam("answers",List.of(correct,unanswered)),policy()); amount("72",r.rawPoints()); assertTrue(r.failures().contains("MANDATORY_QUESTION_UNANSWERED:q")); assertTrue(r.warnings().containsAll(List.of("UNANSWERED_PENALTY:q","MANDATORY_QUESTION_NO_CREDIT:q"))); assertFalse(r.passed());
        amount("80",new ExamGradingService().grade(exam("answers",List.of(correct,unanswered)),policy("unansweredPenaltyRate",BigDecimal.ZERO)).rawPoints());
        var multiple=answer("type",QuestionType.MULTIPLE_CHOICE,"exactMatch",false,"selectedCorrectOptions",0,"selectedIncorrectOptions",1);
        assertTrue(grade(multiple,policy()).warnings().contains("ONLY_INCORRECT_OPTIONS_SELECTED:q"));
        amount("60",new ExamGradingService().grade(exam("answers",List.of(correct,multiple)),policy("partialCreditEnabled",false)).rawPoints());
        amount("80",new ExamGradingService().grade(exam("answers",List.of(correct,changed(multiple,"selectedIncorrectOptions",0))),policy("partialCreditEnabled",false)).rawPoints());
        assertTrue(grade(changed(multiple,"correctOptionCount",0),policy()).warnings().contains("MULTIPLE_CHOICE_HAS_NO_CORRECT_OPTIONS:q"));
        amount("0",grade(answer("type",QuestionType.ESSAY,"exactMatch",false),policy()).rawPoints());
    }
    @Test void overallGradesLateRoundingAttemptsAndOverrides() {
        ExamGradingService service=new ExamGradingService();
        for(int seconds:new int[]{1,59,60,61,3599,3600,3601,7200,18000,18001}) {
            int penalty=seconds<=3659?10:seconds<=7259?20:50;
            var report=service.grade(exam("deadline",T.plusSeconds(600-seconds)),policy());
            amount(penalty==10?"72":penalty==20?"64":"40",report.finalPoints()); assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:"+penalty));
        }
        for(int extra:new int[]{0,1,60,61,300,301}) {
            var report=service.grade(exam("submittedAt",T.plusSeconds(600+extra)),policy()); amount(extra<=60?"80":extra<=300?"76":"64",report.finalPoints());
            assertEquals(extra>0&&extra<=60,report.warnings().contains("EXAM_GRACE_PERIOD_USED")); assertEquals(extra>300,report.failures().contains("EXAM_DURATION_EXCEEDED"));
        }
        var late=service.grade(exam("deadline",T),policy("latePenaltyPercentPerHour",100,"maximumLatePenaltyPercent",100)); assertTrue(late.failures().contains("SUBMISSION_TOO_LATE")); amount("0",late.finalPoints());
        for(int attempt:new int[]{2,3,4}) {var r=service.grade(exam("attemptNumber",attempt),policy()); amount(attempt>3?"0":"80",r.finalPoints()); assertEquals(attempt==3,r.warnings().contains("FINAL_ALLOWED_ATTEMPT"));}
        amount("80",service.grade(exam("bonusPoints",decimal("20")),policy()).finalPoints()); amount("100",service.grade(exam("bonusPoints",decimal("20")),policy("capScoreAtOneHundred",false)).finalPoints());
        int[] percentages={0,59,60,64,65,69,70,74,75,79,80,84,85,89,90,94,95,100}; String[] letters={"F","F","D","D","C","C","C+","C+","B","B","B+","B+","A-","A-","A","A","A+","A+"};
        for(int k=0;k<percentages.length;k++) {var r=service.grade(exam("instructorOverridePercent",percentages[k]),policy()); assertEquals(letters[k],r.letterGrade()); assertEquals(percentages[k]>=60,r.passed()); assertEquals(percentages[k],r.percentage()); assertTrue(r.warnings().contains("INSTRUCTOR_OVERRIDE_APPLIED"));}
        var empty=service.grade(exam("answers",null),policy()); amount("0",empty.maximumPoints()); assertEquals(0,empty.percentage());
    }
    @Test void integrityAndSectionRequirementsRemainIndependentOfScore() {
        ExamGradingService service=new ExamGradingService();
        for(Object[] vector:new Object[][]{{"suspendedAccount",true,"ACCOUNT_SUSPENDED"},{"identityVerified",false,"IDENTITY_NOT_VERIFIED"},{"fullscreenExitCount",3,"EXCESSIVE_FULLSCREEN_EXITS"},{"copyPasteEventCount",5,"EXCESSIVE_COPY_PASTE_EVENTS"}}) {
            var submitted=exam(vector[0],vector[1]); var report=service.grade(submitted,policy()); assertFalse(report.passed()); assertTrue(report.failures().contains(vector[2])); assertTrue(service.grade(submitted,policy("failOnIntegrityViolation",false)).passed());
        }
        for(int count:new int[]{0,1,2,3,4}) {var r=service.grade(exam("fullscreenExitCount",count),policy()); assertEquals(count>=3,r.manualReview());}
        for(int count:new int[]{0,1,4,5,6}) {var r=service.grade(exam("copyPasteEventCount",count),policy()); assertEquals(count>=5,r.manualReview()); assertEquals(count>0&&count<5,r.warnings().contains("COPY_PASTE_EVENTS_RECORDED"));}
        var disconnected=service.grade(exam("proctorDisconnected",true,"ipAddressChanged",true,"termsAccepted",false),policy()); assertTrue(disconnected.manualReview()); assertTrue(disconnected.warnings().containsAll(List.of("PROCTOR_CONNECTION_LOST","IP_ADDRESS_CHANGED","RESULT_PENDING_MANUAL_REVIEW"))); assertTrue(disconnected.failures().contains("EXAM_TERMS_NOT_ACCEPTED"));
        assertTrue(service.grade(exam("identityVerified",false),policy("identityVerificationRequired",false)).warnings().contains("IDENTITY_NOT_VERIFIED"));
        var missing=service.grade(exam("sectionPassPercentages",Map.of("core",100,"extra",1)),policy()); assertFalse(missing.passed()); assertEquals(Map.of("core",100,"extra",0),missing.sectionPercentages()); assertTrue(missing.failures().contains("REQUIRED_SECTION_MISSING:extra"));
        var zeroRequired=service.grade(exam("sectionPassPercentages",Map.of("extra",0)),policy()); assertTrue(zeroRequired.passed());
        var partial=answer("type",QuestionType.ESSAY,"exactMatch",false,"rubricPercent",60); assertFalse(service.grade(exam("answers",List.of(partial)),policy("minimumSectionPercentage",61)).passed()); assertTrue(service.grade(exam("answers",List.of(partial)),policy("minimumSectionPercentage",60)).passed());
        var integrity=grade(answer("plagiarismFlag",true),policy()); assertTrue(integrity.manualReview()); assertFalse(integrity.passed()); assertTrue(integrity.failures().contains("ANSWER_INTEGRITY_VIOLATION:q"));
    }

    @Test void gradingRejectsMalformedSubmissionPolicyAndAnswers() {
        ExamGradingService service=new ExamGradingService();
        assertThrows(NullPointerException.class,()->service.grade(null,policy())); assertThrows(NullPointerException.class,()->service.grade(exam(),null));
        for(String field:List.of("examId","studentId")) for(String value:new String[]{null,""}) rejects(exam(),e->service.grade(e,policy()),field+" must not be blank",field,value);
        for(String field:List.of("startedAt","submittedAt")) rejects(exam(),e->service.grade(e,policy()),"exam timestamps must not be null",field,null);
        rejects(exam(),e->service.grade(e,policy()),"submittedAt must not be before startedAt","submittedAt",T.minusSeconds(1));
        rejects(exam(),e->service.grade(e,policy()),"allowedDurationSeconds must not be negative","allowedDurationSeconds",-1);
        rejects(exam(),e->service.grade(e,policy()),"attemptNumber must be positive","attemptNumber",0);
        for(String field:List.of("fullscreenExitCount","copyPasteEventCount")) rejects(exam(),e->service.grade(e,policy()),"integrity event counts must not be negative",field,-1);
        for(BigDecimal b:new BigDecimal[]{null,decimal("-1")}) rejects(exam(),e->service.grade(e,policy()),"bonusPoints must not be negative","bonusPoints",b);
        for(int n:new int[]{-1,101}) {
            rejects(exam(),e->service.grade(e,policy()),"override percentage must be between 0 and 100","instructorOverridePercent",n);
            rejects(exam(),e->service.grade(e,policy()),"section percentage must be between 0 and 100","sectionPassPercentages",Map.of("s",n));
            for(String field:List.of("passPercentage","minimumSectionPercentage")) rejects(policy(),v->service.grade(exam(),v),field+" must be between 0 and 100",field,n);
        }
        rejects(exam(),e->service.grade(e,policy()),"section name must not be blank","sectionPassPercentages",Map.of("",0));
        for(String field:List.of("wrongAnswerPenaltyRate","unansweredPenaltyRate")) for(BigDecimal b:new BigDecimal[]{null,decimal("-0.01"),decimal("1.01")}) rejects(policy(),v->service.grade(exam(),v),field+" must be between 0 and 1",field,b);
        for(Object[] bad:new Object[][]{{"latePenaltyPercentPerHour",-1},{"maximumLatePenaltyPercent",-1},{"maximumLatePenaltyPercent",101}}) rejects(policy(),v->service.grade(exam(),v),"late penalties must be between 0 and 100",bad);
        rejects(policy(),v->service.grade(exam(),v),"maximumAttempts must be positive","maximumAttempts",0);
        for(String field:List.of("questionId","section")) for(String value:new String[]{null," "}) rejects(answer(),a->grade(a,policy()),field.equals("section")?"section must not be blank":"questionId must not be blank",field,value);
        rejects(answer(),a->grade(a,policy()),"question type must not be null","type",null);
        for(BigDecimal b:new BigDecimal[]{null,BigDecimal.ZERO,decimal("-1")}) rejects(answer(),a->grade(a,policy()),"maximumPoints must be positive","maximumPoints",b);
        for(String field:List.of("correctOptionCount","selectedCorrectOptions","selectedIncorrectOptions")) rejects(answer(),a->grade(a,policy()),"option counts must not be negative",field,-1);
        rejects(answer(),a->grade(a,policy()),"selected correct options exceed available options","selectedCorrectOptions",5);
        rejects(answer(),a->grade(a,policy()),"numericTolerance must not be negative","numericTolerance",decimal("-0.01"));
        for(int n:new int[]{-1,101}) rejects(answer(),a->grade(a,policy()),"rubricPercent must be between 0 and 100","rubricPercent",n);
        for(Object[] bad:new Object[][]{{"passedTests",-1},{"totalTests",-1},{"passedTests",1}}) rejects(answer(),a->grade(a,policy()),"program test counts are invalid",bad);
        rejects(answer(),a->grade(a,policy()),"compiled program requires tests or manual review","type",QuestionType.PROGRAMMING);
        rejects(exam(),e->service.grade(e,policy()),"questionId must be unique","answers",List.of(answer(),answer()));
    }
    static LessonProgress lesson(Object... fields) {return changed(new LessonProgress("lesson",LessonType.VIDEO,true,0,100,100,0,3,true,true,false,100,100,true),fields);}
    static CourseProgressSnapshot progress(Object... fields) {return changed(new CourseProgressSnapshot("student","course",List.of(lesson()),false,80,60,true,true,true,false,true,T.minusSeconds(1),null,true,"Student Name",false,false,false),fields);}
    static ProgressEvaluation learn(CourseProgressSnapshot snapshot) {return new LearningProgressService(FIXED).evaluate(snapshot);}
    @Test void progressWeightedCompletionForEachLessonType() {
        for(LessonType type:LessonType.values()) {var r=learn(progress("lessons",List.of(lesson("type",type)))); assertEquals(100,r.overallPercent()); assertEquals(1,r.completedLessons()); assertEquals(1,r.totalLessons()); assertTrue(r.courseCompleted()); assertTrue(r.certificateEligible()); assertNull(r.nextLessonId()); assertTrue(r.blockers().isEmpty());}
        Object[][] samples={{LessonType.VIDEO,69,66,false},{LessonType.VIDEO,70,66,false},{LessonType.VIDEO,89,86,false},{LessonType.VIDEO,90,86,true},{LessonType.ARTICLE,99,90,false},{LessonType.ARTICLE,100,100,true}};
        for(Object[] sample:samples) {var r=learn(progress("lessons",List.of(lesson("type",sample[0],"completionPercent",sample[1])))); assertEquals(sample[2],r.overallPercent()); assertEquals((boolean)sample[3]?1:0,r.completedLessons()); assertEquals(sample[0]==LessonType.VIDEO&&(int)sample[1]>=70&&(int)sample[1]<90,r.warnings().contains("VIDEO_NEAR_COMPLETION:lesson"));}
        for(boolean submitted:List.of(false,true)) for(boolean approved:List.of(false,true)) {
            var assignment=learn(progress("lessons",List.of(lesson("type",LessonType.ASSIGNMENT,"submitted",submitted,"approved",approved)))); assertEquals(!submitted?0:approved?100:48,assignment.overallPercent()); assertEquals(submitted&&approved?1:0,assignment.completedLessons());
            var project=learn(progress("lessons",List.of(lesson("type",LessonType.PROJECT,"submitted",submitted,"approved",approved)))); assertEquals(!submitted?0:approved?100:33,project.overallPercent());
        }
        for(int score:new int[]{0,59,60,100}) {var r=learn(progress("lessons",List.of(lesson("type",LessonType.QUIZ,"score",score)))); assertEquals(score>=60?1:0,r.completedLessons()); assertEquals(score<60,r.warnings().contains("QUIZ_SCORE_TOO_LOW:lesson"));}
        assertEquals(0,learn(progress("lessons",List.of(lesson("type",LessonType.QUIZ,"submitted",false)))).overallPercent());
        for(int attended:new int[]{0,1,79,80,100,101}) {var r=learn(progress("lessons",List.of(lesson("type",LessonType.LIVE_SESSION,"attendedMinutes",attended)))); assertEquals(attended>=80?1:0,r.completedLessons()); assertEquals(attended==0?0:attended==1?0:attended==79?75:attended==80?80:100,r.overallPercent());}
        for(int attended:new int[]{0,1}) assertEquals(attended==0?0:100,learn(progress("lessons",List.of(lesson("type",LessonType.LIVE_SESSION,"requiredMinutes",0,"attendedMinutes",attended)))).overallPercent());
        var weighted=learn(progress("lessons",List.of(lesson("completionPercent",0),lesson("lessonId","article","order",1,"type",LessonType.ARTICLE)))); assertEquals(40,weighted.overallPercent()); assertEquals(1,weighted.completedLessons());
    }
    @Test void progressAcademicBlocksNextLessonOrderAndCertificate() {
        var first=lesson("completionPercent",0); var second=lesson("lessonId","second","order",1,"completionPercent",0);
        var r=learn(progress("lessons",List.of(second,first),"sequentialLearning",true)); assertEquals("lesson",r.nextLessonId()); assertTrue(r.warnings().contains("SEQUENCE_RULE_BYPASSED:second")); assertFalse(r.courseCompleted());
        assertNull(learn(progress("lessons",List.of(changed(first,"unlocked",false),second),"sequentialLearning",true)).nextLessonId());
        for(int percent:new int[]{49,50}) {var optional=changed(first,"mandatory",false,"completionPercent",percent,"unlocked",false); assertEquals(percent==50?"second":null,learn(progress("lessons",List.of(optional,second),"sequentialLearning",true)).nextLessonId());}
        assertTrue(learn(progress("lessons",List.of(lesson("unlocked",false)))).warnings().contains("LOCKED_LESSON_HAS_PROGRESS:lesson"));
        var limit=learn(progress("lessons",List.of(changed(first,"attempts",3)))); assertTrue(limit.blockers().contains("ATTEMPT_LIMIT_REACHED:lesson"));
        var plagiarism=learn(progress("lessons",List.of(lesson("type",LessonType.PROJECT,"plagiarismFlag",true)))); assertTrue(plagiarism.blockers().contains("PLAGIARISM_REVIEW:lesson")); assertTrue(plagiarism.warnings().contains("PROJECT_HELD_FOR_INTEGRITY_REVIEW:lesson"));
        assertTrue(learn(progress("finalProjectRequired",true)).blockers().contains("FINAL_PROJECT_NOT_APPROVED"));
        assertTrue(learn(progress("finalProjectRequired",true,"lessons",List.of(lesson("type",LessonType.PROJECT)))).courseCompleted());
        assertTrue(learn(progress("instructorApprovalRequired",true,"lessons",List.of(lesson("type",LessonType.ASSIGNMENT,"approved",false)))).blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));
        var optional=learn(progress("requiredOverallPercent",0,"lessons",List.of(changed(first,"mandatory",false)))); assertTrue(optional.courseCompleted()); assertTrue(optional.warnings().contains("COMPLETION_STATE_CONFLICT"));
        var empty=learn(progress("lessons",null)); assertEquals(0,empty.totalLessons()); assertEquals(0,empty.overallPercent()); assertFalse(empty.courseCompleted()); assertTrue(empty.blockers().contains("COURSE_HAS_NO_LESSONS"));
        for(Object[] c:new Object[][]{{"identityVerified",false,"CERTIFICATE_REQUIRES_IDENTITY_VERIFICATION"},{"outstandingBalance",true,"CERTIFICATE_BLOCKED_BY_BALANCE"},{"certificateName",null,"CERTIFICATE_NAME_REQUIRED"},{"certificateName"," ","CERTIFICATE_NAME_REQUIRED"},{"certificateName","a","CERTIFICATE_NAME_LENGTH_INVALID"},{"certificateName","a".repeat(81),"CERTIFICATE_NAME_LENGTH_INVALID"}}) {var denied=learn(progress(c[0],c[1])); assertTrue(denied.courseCompleted()); assertFalse(denied.certificateEligible()); assertTrue(denied.blockers().contains(c[2]));}
        for(int length:new int[]{2,80}) assertTrue(learn(progress("certificateName","x".repeat(length))).certificateEligible());
        assertFalse(learn(progress("certificateEnabled",false)).certificateEligible());
    }
    @Test void progressAccessStateAndValidationMessages() {
        for(Object[] c:new Object[][]{{"coursePublished",false,"COURSE_NOT_PUBLISHED"},{"accountSuspended",true,"ACCOUNT_SUSPENDED"},{"subscriptionActive",false,"SUBSCRIPTION_INACTIVE"},{"codeOfConductAccepted",false,"CODE_OF_CONDUCT_NOT_ACCEPTED"},{"enrolledAt",T.plusSeconds(1),"ENROLLMENT_DATE_IN_FUTURE"},{"accessExpiresAt",T.minusSeconds(1),"COURSE_ACCESS_EXPIRED"}}) assertTrue(learn(progress(c[0],c[1])).blockers().contains(c[2]));
        for(int days:new int[]{0,7,8}) assertEquals(days<=7,learn(progress("accessExpiresAt",T.plusSeconds(days*86400L))).warnings().contains("COURSE_ACCESS_EXPIRES_SOON"));
        assertThrows(NullPointerException.class,()->learn(null));
        for(String name:List.of("studentId","courseId")) for(String value:new String[]{null," "}) rejects(progress(),StudentTest::learn,name+" must not be blank",name,value);
        for(String name:List.of("requiredOverallPercent","requiredQuizScore")) for(int value:new int[]{-1,101}) rejects(progress(),StudentTest::learn,name+" must be between 0 and 100",name,value);
        for(String value:new String[]{null,""}) rejects(lesson(),a->learn(progress("lessons",List.of(a))),"lessonId must not be blank","lessonId",value);
        rejects(lesson(),a->learn(progress("lessons",List.of(a))),"lesson type must not be null","type",null);
        rejects(lesson(),a->learn(progress("lessons",List.of(a))),"lesson order must not be negative","order",-1);
        for(String field:List.of("completionPercent","score")) for(int value:new int[]{-1,101}) rejects(lesson(),a->learn(progress("lessons",List.of(a))),field+" must be between 0 and 100",field,value);
        for(String field:List.of("attempts","maximumAttempts")) rejects(lesson(),a->learn(progress("lessons",List.of(a))),"attempt counts must not be negative",field,-1);
        rejects(lesson(),a->learn(progress("lessons",List.of(a))),"attempts must not exceed maximumAttempts","attempts",4);
        for(String field:List.of("attendedMinutes","requiredMinutes")) rejects(lesson(),a->learn(progress("lessons",List.of(a))),"attendance minutes must not be negative",field,-1);
        rejects(progress(),StudentTest::learn,"lessonId must be unique","lessons",List.of(lesson(),lesson("order",1)));
        rejects(progress(),StudentTest::learn,"lesson order must be unique","lessons",List.of(lesson(),lesson("lessonId","other")));
    }

    @Test void expressionAnswersFlowThroughRealEngineAndExamScoring() {
        var evaluator=new MathematicalExpressionEvaluator(); var answers=new ExpressionAnswerGradingService(evaluator);
        var question=new ExpressionQuestion("math","algebra","3*7",decimal("20"),decimal("0.5"),true);
        String[] responses={"7*3","21.5","20.5","21.51","word",null," "};
        ExpressionAnswerStatus[] states={ExpressionAnswerStatus.EXACT,ExpressionAnswerStatus.WITHIN_TOLERANCE,ExpressionAnswerStatus.WITHIN_TOLERANCE,ExpressionAnswerStatus.INCORRECT,ExpressionAnswerStatus.INVALID_EXPRESSION,ExpressionAnswerStatus.UNANSWERED,ExpressionAnswerStatus.UNANSWERED};
        for(int k=0;k<responses.length;k++) {var r=answers.evaluate(new ExpressionAnswerSubmission(question,responses[k])); assertEquals(states[k],r.status()); assertEquals("math",r.answerResult().questionId()); assertEquals("algebra",r.answerResult().section()); assertEquals(QuestionType.NUMERIC,r.answerResult().type()); assertTrue(r.answerResult().mandatory()); assertEquals(k<5,r.answerResult().answered()); assertEquals(k==0,r.answerResult().exactMatch()); if(k<4) {amount("21",r.expectedValue()); amount(k==0?"0":k==3?"0.51":"0.5",r.absoluteError());} }
        var badQuestion=new ExpressionQuestion("bad","math","abc",BigDecimal.ONE,BigDecimal.ZERO,false);
        assertEquals("invalid correct expression for question bad",assertThrows(IllegalStateException.class,()->answers.evaluate(new ExpressionAnswerSubmission(badQuestion,"1"))).getMessage());
        var submitted=new ExpressionExamSubmission("exam","student",List.of(new ExpressionAnswerSubmission(question,"3*7")),T,T.plusSeconds(30),T.plusSeconds(60),120,2,true,true,false,false,false,0,0,BigDecimal.ZERO,null,Map.of("algebra",60));
        var service=new ExpressionExamGradingService(answers,new ExamGradingService()); var result=service.grade(submitted,policy()); assertEquals(100,result.report().percentage()); assertTrue(result.report().passed()); assertEquals(1,result.answerEvaluations().size());
        var converted=submitted.toExamSubmission(List.of(answer())); assertEquals("exam",converted.examId()); assertEquals("student",converted.studentId()); assertEquals(T,converted.startedAt()); assertEquals(T.plusSeconds(30),converted.submittedAt()); assertEquals(T.plusSeconds(60),converted.deadline()); assertEquals(120,converted.allowedDurationSeconds()); assertEquals(2,converted.attemptNumber()); assertEquals(Map.of("algebra",60),converted.sectionPassPercentages());
        for(String text:new String[]{null," "}) assertEquals("expression must not be blank",assertThrows(ExpressionEvaluationException.class,()->evaluator.evaluate(text)).getMessage());
        assertNotNull(assertThrows(ExpressionEvaluationException.class,()->evaluator.evaluate("1/0")).getCause());
        assertThrows(NullPointerException.class,()->new MathematicalExpressionEvaluator(null)); assertThrows(NullPointerException.class,()->new ExpressionAnswerGradingService(null)); assertThrows(NullPointerException.class,()->answers.evaluate(null)); assertThrows(NullPointerException.class,()->new ExpressionAnswerSubmission(null,"1")); assertThrows(NullPointerException.class,()->new ExpressionExamGradingService(null,new ExamGradingService())); assertThrows(NullPointerException.class,()->new ExpressionExamGradingService(answers,null)); assertThrows(NullPointerException.class,()->service.grade(null,policy())); assertThrows(NullPointerException.class,()->service.grade(submitted,null));
        for(String text:new String[]{null,""}) {assertThrows(IllegalArgumentException.class,()->new ExpressionQuestion(text,"s","1",BigDecimal.ONE,BigDecimal.ZERO,false)); assertThrows(IllegalArgumentException.class,()->new ExpressionQuestion("q",text,"1",BigDecimal.ONE,BigDecimal.ZERO,false)); assertThrows(IllegalArgumentException.class,()->new ExpressionQuestion("q","s",text,BigDecimal.ONE,BigDecimal.ZERO,false));}
        assertThrows(IllegalArgumentException.class,()->new ExpressionQuestion("q","s","1",BigDecimal.ZERO,BigDecimal.ZERO,false)); assertThrows(IllegalArgumentException.class,()->new ExpressionQuestion("q","s","1",BigDecimal.ONE,decimal("-1"),false));
    }
    static PriceRequest order(Object... fields) { return changed(new PriceRequest("lesson",decimal("200"),1,MembershipLevel.NONE,null,null,false,false,false,false,0,0,"US","USD"),fields); }
    static PriceBreakdown charge(PriceRequest order) {return new AdvancedPricingService(FIXED).calculate(order);}
    static Coupon voucher(Object... fields) {return changed(new Coupon("SUMMER",CouponType.FIXED,decimal("15"),null,null,null,10,true,null,null,0),fields);}
    static Promotion sale(Object... fields) {return changed(new Promotion("June",PromotionType.PERCENT,decimal("15"),null,null,false,0,null),fields);}
    @Test void pricingIndependentDiscountAndTaxExamples() {
        String[] memberValues={"0","6","14","24","20","16"};
        for(MembershipLevel member:MembershipLevel.values()) {var r=charge(order("membershipLevel",member)); amount(memberValues[member.ordinal()],r.membershipDiscount()); amount("200",r.subtotal()); assertEquals(member!=MembershipLevel.NONE,r.appliedRules().contains("MEMBERSHIP_"+member));}
        int[] quantities={4,5,19,20,49,50,99,100}; String[] totals={"0","50","190","400","980","1500","2970","3000"};
        for(int k=0;k<quantities.length;k++) {var r=charge(order("quantity",quantities[k])); amount(totals[k],r.bulkDiscount()); assertEquals(quantities[k]==100,r.warnings().contains("The order reached the maximum allowed quantity"));}
        amount("360",charge(order("quantity",10,"membershipLevel",MembershipLevel.ENTERPRISE)).membershipDiscount());
        amount("200",charge(order("membershipLevel",MembershipLevel.INSTRUCTOR,"instructorOwnedCourse",true)).membershipDiscount());
        String[] countries={"cn","us","de","sg","gb","nz"}; String[] taxes={"12","15","38","18","40","0"};
        for(int k=0;k<countries.length;k++) amount(taxes[k],charge(order("countryCode",countries[k])).tax());
        amount("0",charge(order("taxExempt",true)).tax()); amount("7.13",charge(order("nonprofitCustomer",true)).tax());
        amount("10",charge(order("nonprofitCustomer",true)).nonprofitDiscount()); amount("16",charge(order("nonprofitCustomer",true,"membershipLevel",MembershipLevel.ENTERPRISE)).nonprofitDiscount());
        amount("16",charge(order("firstPurchase",true)).firstPurchaseDiscount()); amount("100",charge(order("firstPurchase",true,"unitPrice",decimal("2500"))).firstPurchaseDiscount());
        amount("0",charge(order("firstPurchase",true,"instructorOwnedCourse",true)).firstPurchaseDiscount()); amount("0",charge(order("firstPurchase",true,"coupon",voucher("stackable",false))).firstPurchaseDiscount());
        int[] points={0,99,100,3999,4000,4001}; String[] credit={"0","0","1","39.99","40","40"};
        for(int k=0;k<points.length;k++) amount(credit[k],charge(order("loyaltyPointsToUse",points[k],"availableLoyaltyPoints",10000)).loyaltyDiscount());
        amount("2",charge(order("loyaltyPointsToUse",500,"availableLoyaltyPoints",200)).loyaltyDiscount());
        var free=charge(order("unitPrice",BigDecimal.ZERO,"loyaltyPointsToUse",100,"availableLoyaltyPoints",100)); amount("0",free.total()); assertTrue(free.appliedRules().contains("FREE_ORDER"));
    }
    @Test void pricingDiscountPriorityCapsAndEligibility() {
        for(PromotionType kind:PromotionType.values()) amount(kind==PromotionType.PERCENT?"30":kind==PromotionType.FIXED?"15":"8",charge(order("promotion",sale("type",kind))).promotionDiscount());
        amount("160",charge(order("quantity",10,"promotion",sale("type",PromotionType.BUY_MORE_SAVE_MORE))).promotionDiscount()); amount("480",charge(order("quantity",20,"promotion",sale("type",PromotionType.BUY_MORE_SAVE_MORE))).promotionDiscount());
        amount("12",charge(order("promotion",sale("maximumDiscount",decimal("12")))).promotionDiscount());
        for(CouponType kind:CouponType.values()) amount(kind==CouponType.CASHBACK?"0":kind==CouponType.PERCENT?"30":"15",charge(order("coupon",voucher("type",kind))).couponDiscount());
        var capped=charge(order("membershipLevel",MembershipLevel.PREMIUM,"promotion",sale("value",decimal("90")),"coupon",voucher("type",CouponType.PERCENT,"value",decimal("75")),"firstPurchase",true,"nonprofitCustomer",true)); amount("140",capped.subtotal().subtract(capped.total().subtract(capped.tax()))); assertTrue(capped.appliedRules().contains("DISCOUNT_CAP")); amount("24",capped.membershipDiscount()); amount("116",capped.promotionDiscount()); amount("0",capped.couponDiscount());
        var higherSale=charge(order("promotion",sale(),"coupon",voucher("stackable",false))); amount("30",higherSale.promotionDiscount()); amount("0",higherSale.couponDiscount()); assertFalse(higherSale.appliedRules().contains("COUPON_FIXED"));
        var higherCoupon=charge(order("promotion",sale(),"coupon",voucher("stackable",false,"value",decimal("40")))); amount("0",higherCoupon.promotionDiscount()); amount("40",higherCoupon.couponDiscount()); assertFalse(higherCoupon.appliedRules().contains("PROMOTION_PERCENT"));
        LocalDate day=LocalDate.of(2026,6,1);
        amount("15",charge(order("coupon",voucher("validFrom",day,"validUntil",day,"minimumSubtotal",decimal("200"),"eligibleCourseIds",Set.of("lesson"),"maximumQuantity",1,"minimumMembership",MembershipLevel.NONE))).couponDiscount());
        amount("30",charge(order("promotion",sale("startsOn",day,"endsOn",day,"minimumQuantity",1))).promotionDiscount());
    }
    @Test void pricingIgnoredConfigurationMessages() {
        Object[][] promotions={{"name",null,"A promotion without a name was ignored"},{"name","","A promotion without a name was ignored"},{"type",null,"Promotion configuration is incomplete"},{"value",null,"Promotion configuration is incomplete"},{"value",BigDecimal.ZERO,"Promotion value must be positive"},{"startsOn",LocalDate.of(2026,6,2),"Promotion has not started"},{"endsOn",LocalDate.of(2026,5,31),"Promotion has expired"},{"membersOnly",true,"Promotion is available to members only"},{"minimumQuantity",2,"Order quantity is below the promotion minimum"}};
        for(Object[] vector:promotions) {var r=charge(order("promotion",sale(vector[0],vector[1]))); amount("0",r.promotionDiscount()); assertTrue(r.warnings().contains(vector[2]));}
        Object[][] coupons={{"code",null,"Coupon code is blank"},{"code"," ","Coupon code is blank"},{"type",null,"Coupon configuration is incomplete"},{"value",null,"Coupon configuration is incomplete"},{"value",BigDecimal.ZERO,"Coupon value must be positive"},{"remainingUses",0,"Coupon has no remaining uses"},{"validFrom",LocalDate.of(2026,6,2),"Coupon is not active yet"},{"validUntil",LocalDate.of(2026,5,31),"Coupon has expired"},{"minimumSubtotal",decimal("201"),"Order does not meet the coupon minimum subtotal"},{"eligibleCourseIds",Set.of("x"),"Coupon is not valid for this course"},{"minimumMembership",MembershipLevel.PLUS,"Membership level is too low for this coupon"}};
        for(Object[] vector:coupons) {var r=charge(order("coupon",voucher(vector[0],vector[1]))); amount("0",r.couponDiscount()); assertTrue(r.warnings().contains(vector[2]));}
        amount("0",charge(order("quantity",2,"coupon",voucher("maximumQuantity",1))).couponDiscount());
        for(String name:List.of("courseId","countryCode","currency")) for(String text:new String[]{null," "}) rejects(order(),StudentTest::charge,name+" must not be blank",name,text);
        rejects(order(),StudentTest::charge,"quantity must be positive","quantity",0); rejects(order(),StudentTest::charge,"quantity must not exceed 100","quantity",101); rejects(order(),StudentTest::charge,"membershipLevel must not be null","membershipLevel",null); rejects(order(),StudentTest::charge,"unsupported currency: XYZ","currency","XYZ");
        for(String name:List.of("loyaltyPointsToUse","availableLoyaltyPoints")) rejects(order(),StudentTest::charge,name+" must not be negative",name,-1);
        for(BigDecimal value:new BigDecimal[]{null,decimal("-0.01"),decimal("100000")}) assertThrows(net.mooctest.error.BusinessException.class,()->charge(order("unitPrice",value)));
        amount("99999.99",charge(order("unitPrice",decimal("99999.99"))).subtotal()); assertThrows(NullPointerException.class,()->charge(null));
    }

    static PurchaseSnapshot receipt(Object... f) {return changed(new PurchaseSnapshot("receipt","learner","course",decimal("240"),decimal("12"),BigDecimal.ZERO,T.minusSeconds(3600),PaymentMethod.WALLET,0,0,false,0,0,false,false,0,false,false,false,false,null),f);}
    static RefundRequest claim(Object... f) {return changed(new RefundRequest(receipt(),RefundReason.ACCIDENTAL_PURCHASE,true,0,false,false,false,false,false,false,false,false),f);}
    static RefundDecision refundResult(RefundRequest request) {return new RefundPolicyService(FIXED).evaluate(request);}
    @Test void refundPolicyTimeAndProgressBoundaryTable() {
        int[][] vectors={{2,0,240},{3,0,240},{24,5,240},{25,5,240},{168,10,240},{168,11,168},{168,30,168},{169,30,120},{336,30,120},{337,15,60},{720,15,60},{721,15,0},{720,16,0},{1,80,0}};
        for(int[] v:vectors) {var r=refundResult(claim("purchase",receipt("paidAt",T.minusSeconds(v[0]*3600L),"progressPercent",v[1]))); amount(Integer.toString(v[2]),r.refundableAmount()); assertEquals(v[2]!=0,r.approved()); assertFalse(r.manualReview()); assertEquals(v[2]==0?"NONE":"WALLET_CREDIT",r.refundRoute());}
        var rate=refundResult(claim("fullRefundRequested",false,"requestedPercentage",35)); amount("84",rate.refundableAmount());
        amount("240",refundResult(claim("fullRefundRequested",false)).refundableAmount());
        for(boolean full:List.of(false,true)) {var r=refundResult(claim("adminOverride",true,"fullRefundRequested",full,"purchase",receipt("certificateIssued",true))); amount(full?"240":"120",r.refundableAmount()); assertTrue(r.warnings().contains("ADMIN_OVERRIDE_APPLIED"));}
        var capped=refundResult(claim("purchase",receipt("paidAt",T.minusSeconds(169*3600),"progressPercent",20))); assertTrue(capped.warnings().contains("REQUESTED_PERCENTAGE_REDUCED_TO_POLICY_LIMIT"));
    }
    @Test void refundReasonSpecificEntitlementsAndEarlyRejections() {
        Object[][] rejection={{"previouslyRefunded",true,"PURCHASE_ALREADY_REFUNDED"},{"paidAt",T.plusSeconds(3600),"PURCHASE_DATE_IN_FUTURE"},{"certificateIssued",true,"CERTIFICATE_ALREADY_ISSUED"},{"downloadedResources",10,"TOO_MANY_RESOURCES_DOWNLOADED"},{"progressPercent",80,"COURSE_SUBSTANTIALLY_COMPLETED"}};
        for(Object[] v:rejection) {var r=refundResult(claim("purchase",receipt(v[0],v[1]))); assertFalse(r.approved()); assertTrue(r.reasons().contains(v[2])); amount("0",r.refundableAmount());}
        for(String flag:List.of("instructorCancelled","courseRemoved")) amount("240",refundResult(claim("purchase",receipt(flag,true,"certificateIssued",true))).refundableAmount());
        assertTrue(refundResult(claim("purchase",receipt("chargebackOpen",true))).manualReview()); assertTrue(refundResult(claim("fraudSuspected",true)).manualReview()); assertTrue(refundResult(claim("reason",RefundReason.FRAUD)).manualReview());
        for(RefundReason reason:List.of(RefundReason.TECHNICAL_ISSUE,RefundReason.MEDICAL,RefundReason.DUPLICATE_PAYMENT)) {assertFalse(refundResult(claim("reason",reason)).approved()); amount(reason==RefundReason.TECHNICAL_ISSUE?"240":"120",refundResult(claim("reason",reason,"evidenceProvided",true)).refundableAmount());}
        Object[][] overrides={{RefundReason.TECHNICAL_ISSUE,"technicalIssueConfirmed",50,"240"},{RefundReason.TECHNICAL_ISSUE,"technicalIssueConfirmed",51,"180"},{RefundReason.MEDICAL,"medicalDocumentVerified",79,"240"},{RefundReason.MEDICAL,"medicalDocumentVerified",80,"96"},{RefundReason.CONTENT_QUALITY,"contentComplaintUpheld",59,"240"},{RefundReason.CONTENT_QUALITY,"contentComplaintUpheld",60,"144"},{RefundReason.DUPLICATE_PAYMENT,"duplicatePaymentConfirmed",100,"240"}};
        for(Object[] v:overrides) amount((String)v[3],refundResult(claim("reason",v[0],v[1],true,"purchase",receipt("progressPercent",v[2]))).refundableAmount());
        assertTrue(refundResult(claim("purchase",receipt("paidAt",T.minusSeconds(800*3600),"downloadedResources",1))).reasons().contains("ACCIDENTAL_PURCHASE_AFTER_DOWNLOAD"));
        assertTrue(refundResult(claim("reason",RefundReason.OTHER,"purchase",receipt("paidAt",T.minusSeconds(800*3600)))).reasons().contains("OTHER_REASON_REQUIRES_EVIDENCE"));
        assertTrue(refundResult(claim("reason",RefundReason.OTHER,"evidenceProvided",true,"purchase",receipt("paidAt",T.minusSeconds(800*3600)))).reasons().contains("REFUND_POLICY_NOT_SATISFIED"));
    }
    @Test void refundPaymentFeesAttendanceAndPoints() {
        String[] fees={"7.20","2.40","2.40","4.80","0","0","12"};
        for(PaymentMethod method:PaymentMethod.values()) {var r=refundResult(claim("bankDetailsPresent",true,"purchase",receipt("paymentMethod",method,"paidAt",T.minusSeconds(25*3600)))); amount(fees[method.ordinal()],r.processingFee()); assertEquals(method==PaymentMethod.CRYPTO,r.manualReview()); assertEquals(method!=PaymentMethod.CRYPTO,r.approved());}
        var bank=refundResult(claim("purchase",receipt("paymentMethod",PaymentMethod.BANK_TRANSFER))); assertTrue(bank.manualReview()); assertEquals("MANUAL_REVIEW",bank.refundRoute());
        assertEquals("WALLET_CREDIT",refundResult(claim("purchase",receipt("giftPurchase",true,"paymentMethod",PaymentMethod.CARD))).refundRoute());
        int[] attends={0,1,2,4,5,10}; String[] attendFees={"4.80","4.80","16.80","16.80","28.80","28.80"};
        for(int k=0;k<attends.length;k++) {var r=refundResult(claim("purchase",receipt("downloadedResources",1,"totalLiveSessions",10,"liveSessionsAttended",attends[k]))); amount(attendFees[k],r.processingFee());}
        var coupon=refundResult(claim("fullRefundRequested",false,"requestedPercentage",50,"purchase",receipt("couponUsed",true,"discountAmount",decimal("20"),"loyaltyPointsUsed",103))); amount("110",coupon.refundableAmount()); assertEquals(25,coupon.loyaltyPointsReturned()); assertTrue(coupon.warnings().containsAll(List.of("COUPON_VALUE_NOT_REFUNDED","LOYALTY_POINTS_RETURNED_PROPORTIONALLY","POINT_RETURN_REDUCED_FOR_COUPON_ORDER")));
        var fault=refundResult(claim("reason",RefundReason.COURSE_CANCELLED,"purchase",receipt("couponUsed",true,"discountAmount",decimal("300"),"loyaltyPointsUsed",103))); amount("240",fault.refundableAmount()); assertEquals(103,fault.loyaltyPointsReturned());
        assertFalse(refundResult(claim("purchase",receipt("couponUsed",true,"discountAmount",decimal("240")))).approved());
        assertFalse(refundResult(claim("purchase",receipt("paidAmount",decimal("0.75"),"paymentMethod",PaymentMethod.CARD))).approved()); amount("0.75",refundResult(claim("purchase",receipt("paidAmount",decimal("0.75")))).refundableAmount());
        for(long delta:new long[]{-1,0,1}) assertEquals(delta<0,refundResult(claim("purchase",receipt("accessExpiresAt",T.plusSeconds(delta)))).warnings().contains("COURSE_ACCESS_EXPIRED"));
    }
    @Test void refundRejectsInvalidMoneyCountsAndIdentifiers() {
        assertThrows(NullPointerException.class,()->refundResult(null)); assertThrows(NullPointerException.class,()->refundResult(claim("purchase",null))); assertThrows(NullPointerException.class,()->refundResult(claim("reason",null)));
        for(String name:List.of("purchaseId","studentId","courseId")) for(String text:new String[]{null,""}) rejects(receipt(),r->refundResult(claim("purchase",r)),name+" must not be blank",name,text);
        for(String name:List.of("paidAmount","taxAmount","discountAmount")) for(BigDecimal value:new BigDecimal[]{null,decimal("-1")}) rejects(receipt(),r->refundResult(claim("purchase",r)),name+" must not be negative",name,value);
        for(String name:List.of("paidAt","paymentMethod")) rejects(receipt(),r->refundResult(claim("purchase",r)),name+" must not be null",name,null);
        for(int value:new int[]{-1,101}) {rejects(receipt(),r->refundResult(claim("purchase",r)),"progressPercent must be between 0 and 100","progressPercent",value); rejects(claim(),StudentTest::refundResult,"requestedPercentage must be between 0 and 100","requestedPercentage",value);}
        for(String name:List.of("downloadedResources","loyaltyPointsUsed")) rejects(receipt(),r->refundResult(claim("purchase",r)),name+" must not be negative",name,-1);
        for(String name:List.of("liveSessionsAttended","totalLiveSessions")) rejects(receipt(),r->refundResult(claim("purchase",r)),"live session counts must not be negative",name,-1);
        rejects(receipt(),r->refundResult(claim("purchase",r)),"attended sessions must not exceed total sessions","liveSessionsAttended",1);
    }
    static StudentProfile learner(Object... f) {return changed(new StudentProfile("learner",21,"GB","en",MembershipLevel.BASIC,true,false,true,true,true,false,false,true,true,10),f);}
    static CourseOffering courseOffer(Object... f) {return changed(new CourseOffering("course","teacher",EnrollmentCourseStatus.PUBLISHED,null,null,null,6,0,false,0,0,0,0,null,null,null,0,0,0,false,null,false,false,null,0,0,decimal("240")),f);}
    static EnrollmentRequest join(Object... f) {return changed(new EnrollmentRequest(learner(),courseOffer(),1,true,false,false,false,null,false,false),f);}
    static EnrollmentDecision enroll(EnrollmentRequest request) {return new EnrollmentEligibilityService(new EmptyEnrollmentHistory(),FIXED).evaluate(request);}
    @Test void enrollmentSafetyChecksAndPaymentExemptions() {
        var yes=enroll(join()); assertTrue(yes.accepted()); assertEquals(EnrollmentOutcome.ENROLLED,yes.outcome()); assertEquals(6,yes.availableSeats()); assertTrue(yes.paymentRequired()); assertEquals(List.of(),yes.reasons());
        Object[][] profile={{"accountActive",false,"ACCOUNT_INACTIVE"},{"suspended",true,"ACCOUNT_SUSPENDED"},{"emailVerified",false,"EMAIL_NOT_VERIFIED"},{"profileComplete",false,"PROFILE_INCOMPLETE"},{"termsAccepted",false,"TERMS_NOT_ACCEPTED"},{"age",17,"PARENTAL_CONSENT_REQUIRED"},{"age",-1,"INVALID_STUDENT_AGE"},{"age",121,"INVALID_STUDENT_AGE"},{"countryCode",null,"COUNTRY_MISSING"},{"countryCode"," ","COUNTRY_MISSING"}};
        for(Object[] v:profile) {var r=enroll(join("student",learner(v[0],v[1]))); assertFalse(r.accepted()); assertTrue(r.reasons().contains(v[2]));}
        assertTrue(enroll(join("student",learner("parentalConsent",true))).warnings().contains("UNNECESSARY_PARENTAL_CONSENT"));
        assertTrue(enroll(join("student",learner("age",17,"parentalConsent",true))).accepted());
        assertTrue(enroll(join("student",learner("identityVerified",false))).warnings().contains("IDENTITY_NOT_VERIFIED"));
        assertTrue(enroll(join("student",learner("identityVerified",false),"course",courseOffer("liveCourse",true))).reasons().contains("IDENTITY_REQUIRED_FOR_LIVE_COURSE"));
        for(int risk:new int[]{49,50,69,70,89,90,100}) {var r=enroll(join("student",learner("riskScore",risk))); assertEquals(risk>=90?EnrollmentOutcome.REJECTED:risk>=70?EnrollmentOutcome.MANUAL_REVIEW:EnrollmentOutcome.ENROLLED,r.outcome()); assertEquals(risk>=50&&risk<70,r.warnings().contains("ELEVATED_RISK_MONITORING"));}
        for(String waiver:List.of("auditMode","scholarshipApproved","organizationSponsored")) {var r=enroll(join(waiver,true)); assertFalse(r.paymentRequired()); assertTrue(r.warnings().contains("PAYMENT_AUTHORIZATION_NOT_REQUIRED"));}
        for(BigDecimal value:new BigDecimal[]{null,BigDecimal.ZERO,decimal("-1")}) assertFalse(enroll(join("course",courseOffer("price",value))).paymentRequired());
        assertTrue(enroll(join("paymentAuthorized",false)).reasons().contains("PAYMENT_NOT_AUTHORIZED"));
        assertTrue(enroll(join("scholarshipApproved",true,"organizationSponsored",true)).warnings().contains("SCHOLARSHIP_AND_SPONSORSHIP_BOTH_PRESENT"));
    }
    @Test void enrollmentCourseRulesAndReservedSeats() {
        Object[][] restrictions={{"status",null,"COURSE_STATUS_MISSING"},{"status",EnrollmentCourseStatus.DRAFT,"COURSE_NOT_PUBLISHED"},{"status",EnrollmentCourseStatus.CLOSED,"COURSE_CLOSED"},{"status",EnrollmentCourseStatus.ARCHIVED,"COURSE_ARCHIVED"},{"enrollmentOpensAt",T.plusSeconds(1),"ENROLLMENT_NOT_OPEN"},{"enrollmentClosesAt",T.minusSeconds(1),"ENROLLMENT_CLOSED"},{"startsAt",T.minusSeconds(1),"COURSE_ALREADY_STARTED"},{"minimumAge",22,"BELOW_MINIMUM_AGE"},{"maximumAge",20,"ABOVE_MAXIMUM_AGE"},{"allowedCountries",Set.of("US"),"COUNTRY_NOT_ALLOWED"},{"requiredLanguage","de","LANGUAGE_REQUIREMENT_NOT_MET"},{"requiredOrganizationId","university","ORGANIZATION_NOT_ALLOWED"},{"liveCourse",true,"LIVE_COURSE_WITHOUT_SCHEDULE"}};
        for(Object[] v:restrictions) assertTrue(enroll(join("course",courseOffer(v[0],v[1]))).reasons().contains(v[2]));
        assertTrue(enroll(join("course",courseOffer("enrollmentOpensAt",T,"enrollmentClosesAt",T,"startsAt",T,"minimumAge",21,"maximumAge",21,"requiredLanguage","EN","allowedCountries",Set.of("GB")))).accepted());
        var archived=enroll(join("auditMode",true,"course",courseOffer("status",EnrollmentCourseStatus.ARCHIVED,"startsAt",T.minusSeconds(1)))); assertTrue(archived.accepted()); assertTrue(archived.warnings().containsAll(List.of("ARCHIVED_COURSE_AUDIT_ONLY","LATE_AUDIT_ENROLLMENT")));
        assertTrue(enroll(join("student",learner("instructor",true),"course",courseOffer("instructorId","learner"))).reasons().contains("INSTRUCTOR_CANNOT_ENROLL_IN_OWN_COURSE"));
        for(String text:new String[]{null," "}) {assertTrue(enroll(join("student",learner("preferredLanguage",text),"course",courseOffer("requiredLanguage","en"))).reasons().contains("LANGUAGE_PROFILE_MISSING")); assertTrue(enroll(join("student",learner("preferredLanguage",text))).warnings().contains("PREFERRED_LANGUAGE_MISSING"));}
        assertTrue(enroll(join("course",courseOffer("scheduleKey","ignored"))).warnings().contains("SELF_PACED_COURSE_IGNORES_SCHEDULE"));
        assertTrue(enroll(join("accessibilitySeatRequired",true)).reasons().contains("NO_ACCESSIBILITY_SEATS"));
        for(int used:new int[]{0,1,2}) {var r=enroll(join("accessibilitySeatRequired",true,"course",courseOffer("accessibilitySeatCapacity",1,"accessibilitySeatsUsed",used))); assertEquals(used==0,r.warnings().contains("ACCESSIBILITY_SEAT_RESERVED")); assertEquals(used>=1,r.reasons().contains("ACCESSIBILITY_SEATS_FULL"));}
        assertTrue(enroll(join("course",courseOffer("accessibilitySeatsUsed",1))).warnings().contains("ACCESSIBILITY_CAPACITY_CONFIGURATION_INVALID"));
    }
    @Test void enrollmentWaitlistsGroupRulesAndOrganizations() {
        var base=join("course",courseOffer("enrolledCount",6)); assertEquals(List.of("COURSE_FULL"),enroll(base).reasons());
        var full=courseOffer("enrolledCount",7,"waitlistEnabled",true,"waitlistCapacity",1); assertTrue(enroll(join("course",full)).warnings().contains("COURSE_IS_OVERBOOKED")); assertEquals(0,enroll(join("course",full)).availableSeats());
        assertEquals(List.of("WAITLIST_NOT_ACCEPTED"),enroll(join("course",full)).reasons());
        var waiting=join("course",full,"acceptWaitlist",true); assertEquals(EnrollmentOutcome.WAITLISTED,enroll(waiting).outcome()); assertTrue(enroll(waiting).accepted()); assertFalse(enroll(waiting).paymentRequired());
        assertEquals(List.of("WAITLIST_DISABLED_BY_CAPACITY"),enroll(changed(waiting,"course",changed(full,"waitlistCapacity",0))).reasons());
        assertEquals(List.of("WAITLIST_FULL"),enroll(changed(waiting,"course",changed(full,"waitlistedCount",1))).reasons());
        assertEquals(List.of("GROUP_ENROLLMENT_CANNOT_WAITLIST"),enroll(changed(waiting,"requestedSeats",2,"organizationSponsored",true)).reasons());
        assertEquals(EnrollmentOutcome.MANUAL_REVIEW,enroll(changed(waiting,"student",learner("riskScore",70))).outcome());
        assertTrue(enroll(join("requestedSeats",2)).reasons().contains("GROUP_ENROLLMENT_REQUIRES_ENTERPRISE")); assertTrue(enroll(join("requestedSeats",2,"student",learner("membershipLevel",MembershipLevel.ENTERPRISE))).accepted());
        for(int remaining:new int[]{1,2,3,4}) assertEquals(remaining<=3,enroll(join("course",courseOffer("capacity",remaining))).warnings().contains("COURSE_ALMOST_FULL"));
        var organization=join("course",courseOffer("organizationOnly",true,"subscriptionRequired",true),"student",learner("activeSubscription",false)); assertTrue(enroll(organization).reasons().containsAll(List.of("ORGANIZATION_SPONSOR_REQUIRED","ORGANIZATION_ID_REQUIRED","ACTIVE_SUBSCRIPTION_REQUIRED")));
        assertTrue(enroll(changed(organization,"organizationSponsored",true,"organizationId","org")).accepted());
        assertTrue(enroll(join("course",courseOffer("subscriptionRequired",true),"student",learner("activeSubscription",false),"scholarshipApproved",true)).warnings().contains("SUBSCRIPTION_COVERED_BY_SCHOLARSHIP"));
    }
    @Test void enrollmentExternalHistoryThresholdsAndValidation() {
        EnrollmentHistory history=org.mockito.Mockito.mock(EnrollmentHistory.class); var service=new EnrollmentEligibilityService(history,FIXED);
        org.mockito.Mockito.when(history.alreadyEnrolled("learner","course")).thenReturn(true); assertTrue(service.evaluate(join()).reasons().contains("ALREADY_ENROLLED")); org.mockito.Mockito.when(history.alreadyEnrolled("learner","course")).thenReturn(false);
        org.mockito.Mockito.when(history.hasRefundAbuseFlag("learner")).thenReturn(true); assertEquals(EnrollmentOutcome.MANUAL_REVIEW,service.evaluate(join()).outcome()); org.mockito.Mockito.when(history.hasRefundAbuseFlag("learner")).thenReturn(false);
        var prerequisite=join("course",courseOffer("prerequisiteCourseIds",Set.of("prep"),"minimumPrerequisiteScore",65)); assertTrue(service.evaluate(prerequisite).reasons().contains("MISSING_PREREQUISITE:prep")); org.mockito.Mockito.when(history.completedCourse("learner","prep")).thenReturn(true);
        for(int score:new int[]{-1,0,64,65,94,95,100,101}) {org.mockito.Mockito.when(history.completionScore("learner","prep")).thenReturn(score); var r=service.evaluate(prerequisite); assertEquals(score>=65&&score<=100,r.accepted()); assertEquals(score>=95&&score<=100,r.warnings().contains("EXCELLENT_PREREQUISITE_SCORE:prep")); assertTrue(r.warnings().contains("ALL_PREREQUISITES_COMPLETED"));}
        assertTrue(enroll(join("course",courseOffer("prerequisiteCourseIds",Set.of(" ")))).warnings().contains("INVALID_PREREQUISITE_CONFIGURATION"));
        for(int count:new int[]{-1,0,1,2}) {org.mockito.Mockito.when(history.failedEnrollmentAttempts("learner","course")).thenReturn(count); var limited=join("course",courseOffer("maximumFailedAttempts",2)); if(count<0) assertThrows(IllegalStateException.class,()->service.evaluate(limited)); else assertEquals(count<2,service.evaluate(limited).accepted());} org.mockito.Mockito.when(history.failedEnrollmentAttempts("learner","course")).thenReturn(0);
        for(int active:new int[]{-1,1,2,3}) {org.mockito.Mockito.when(history.activeCourseCountThisWeek("learner")).thenReturn(active); var limited=join("course",courseOffer("maximumWeeklyCourses",2)); if(active<0) assertThrows(IllegalStateException.class,()->service.evaluate(limited)); else {assertEquals(active<2,service.evaluate(limited).accepted()); assertEquals(active<=2,service.evaluate(changed(limited,"student",learner("membershipLevel",MembershipLevel.PREMIUM))).accepted()); assertTrue(service.evaluate(changed(limited,"student",learner("membershipLevel",MembershipLevel.ENTERPRISE),"organizationSponsored",true)).accepted());}} org.mockito.Mockito.when(history.activeCourseCountThisWeek("learner")).thenReturn(0);
        org.mockito.Mockito.when(history.hasScheduleConflict("learner","morning")).thenReturn(true); var live=join("course",courseOffer("liveCourse",true,"scheduleKey","morning")); assertFalse(service.evaluate(live).accepted()); assertTrue(service.evaluate(changed(live,"auditMode",true)).accepted());
        for(String name:List.of("studentId")) for(String text:new String[]{null," "}) rejects(learner(),a->enroll(join("student",a)),"studentId must not be blank",name,text);
        for(String text:new String[]{null," "}) rejects(courseOffer(),a->enroll(join("course",a)),"courseId must not be blank","courseId",text);
        for(int value:new int[]{-1,101}) rejects(learner(),a->enroll(join("student",a)),"riskScore must be between 0 and 100","riskScore",value);
        rejects(join(),StudentTest::enroll,"requestedSeats must be positive","requestedSeats",0); rejects(join(),StudentTest::enroll,"requestedSeats must not exceed 50","requestedSeats",51);
        for(String field:List.of("capacity","enrolledCount")) rejects(courseOffer(),a->enroll(join("course",a)),"course capacity values must not be negative",field,-1);
        rejects(courseOffer(),a->enroll(join("course",a)),"enrollment window is invalid","enrollmentOpensAt",T.plusSeconds(1),"enrollmentClosesAt",T);
        rejects(courseOffer(),a->enroll(join("course",a)),"course age range is invalid","minimumAge",22,"maximumAge",21);
        assertThrows(NullPointerException.class,()->enroll(null)); assertThrows(NullPointerException.class,()->enroll(join("student",null))); assertThrows(NullPointerException.class,()->enroll(join("course",null)));
    }

    static ScheduleRequest meeting(Object... f) {return changed(new ScheduleRequest("course","cohort","teacher",DeliveryMode.CLASSROOM,CourseKind.LECTURE,T.plusSeconds(28*3600),T.plusSeconds(29*3600),"UTC","room",null,12,10,24,null,null,false,true,false,false,false,false,1,0,4,null,null,null,null,false,false,false),f);}
    static SchedulingDecision plan(ScheduleRequest request) {return new SchedulingService(FIXED).evaluate(request);}
    @Test void schedulingCourseDurationThresholds() {
        var normal=plan(meeting()); assertTrue(normal.approved()); assertEquals("APPROVED",normal.status()); assertEquals(60,normal.durationMinutes()); assertFalse(normal.manualReview()); assertEquals(List.of(),normal.conflicts());
        Object[][] limits={{CourseKind.LECTURE,30,180,"LECTURE_TOO_SHORT","LECTURE_LONGER_THAN_RECOMMENDED"},{CourseKind.LAB,60,240,"LAB_TOO_SHORT","LAB_TOO_LONG"},{CourseKind.EXAM,20,300,"EXAM_TOO_SHORT","EXAM_TOO_LONG"},{CourseKind.WORKSHOP,45,360,"WORKSHOP_TOO_SHORT","WORKSHOP_REQUIRES_BREAK_PLAN"},{CourseKind.WEBINAR,15,120,"WEBINAR_TOO_SHORT","WEBINAR_LONGER_THAN_RECOMMENDED"}};
        for(Object[] v:limits) for(int minutes:new int[]{(int)v[1]-1,(int)v[1],(int)v[2],(int)v[2]+1}) {
            var r=plan(meeting("courseKind",v[0],"end",meeting().start().plusSeconds(minutes*60L),"lunchTimeAllowed",true,"availableEquipment",Set.of("LAB_SAFETY_KIT")));
            assertEquals(minutes<(int)v[1],r.conflicts().contains(v[3])); assertEquals(minutes>(int)v[2],r.conflicts().contains(v[4])||r.warnings().contains(v[4])); assertEquals(minutes,r.durationMinutes());
        }
        assertTrue(plan(meeting("courseKind",CourseKind.EXAM)).warnings().contains("EXAM_SCHEDULED_WITH_SHORT_NOTICE"));
        assertFalse(plan(meeting("courseKind",CourseKind.EXAM,"start",T.plusSeconds(52*3600),"end",T.plusSeconds(53*3600))).warnings().contains("EXAM_SCHEDULED_WITH_SHORT_NOTICE"));
    }
    @Test void schedulingClockLocalHoursWeekendsAndHolidays() {
        for(int hours:new int[]{-1,0,1,2,23,24}) {Instant start=T.plusSeconds(hours*3600L); var r=plan(meeting("start",start,"end",start.plusSeconds(1800),"afterHoursAllowed",true,"weekendAllowed",true)); assertEquals(hours<0,r.conflicts().contains("START_TIME_IN_PAST")); assertEquals(hours>=0&&hours<2,r.conflicts().contains("INSUFFICIENT_BOOKING_NOTICE")); assertEquals(hours>=2&&hours<24,r.warnings().contains("SHORT_BOOKING_NOTICE"));}
        for(String date:List.of("2026-06-06T10:00:00Z","2026-06-07T10:00:00Z")) {Instant start=Instant.parse(date); assertTrue(plan(meeting("start",start,"end",start.plusSeconds(3600))).conflicts().contains("WEEKEND_NOT_ALLOWED")); assertTrue(plan(meeting("start",start,"end",start.plusSeconds(3600),"weekendAllowed",true,"recurring",true,"occurrenceCount",2)).warnings().contains("RECURRING_WEEKEND_SESSION"));}
        assertTrue(plan(meeting("holidays",Set.of(LocalDate.of(2026,6,2)))).conflicts().contains("HOLIDAY_NOT_AVAILABLE"));
        for(int hour:new int[]{7,8,20,21}) {Instant start=Instant.parse("2026-06-02T00:00:00Z").plusSeconds(hour*3600); var request=meeting("start",start,"end",start.plusSeconds(3600)); assertEquals(hour==7||hour==21,plan(request).conflicts().contains("OUTSIDE_OPERATING_HOURS")); assertEquals(hour==7||hour==21,plan(changed(request,"afterHoursAllowed",true)).warnings().contains("AFTER_HOURS_SESSION"));}
        for(int minute:new int[]{660,661,809,810}) {Instant start=Instant.parse("2026-06-02T00:00:00Z").plusSeconds(minute*60); var request=meeting("start",start,"end",start.plusSeconds(3600)); assertEquals(minute>660&&minute<810,plan(request).conflicts().contains("LUNCH_BREAK_CONFLICT")); assertEquals(minute>660&&minute<810,plan(changed(request,"lunchTimeAllowed",true)).warnings().contains("LUNCH_TIME_SESSION"));}
        Instant nearMidnight=Instant.parse("2026-06-02T23:30:00Z"); assertTrue(plan(meeting("start",nearMidnight,"end",nearMidnight.plusSeconds(3600))).conflicts().contains("SESSION_CROSSES_LOCAL_DATE"));
    }
    @Test void schedulingResourceRequirementsAndParticipantLimits() {
        for(DeliveryMode mode:DeliveryMode.values()) for(String room:new String[]{null," ","r"}) for(String link:new String[]{null," ","http://meet","https://meet"}) {
            var r=plan(meeting("deliveryMode",mode,"roomId",room,"meetingUrl",link)); boolean absentRoom=room==null||room.isBlank(),absentLink=link==null||link.isBlank();
            if(mode==DeliveryMode.CLASSROOM) assertEquals(absentRoom,r.conflicts().contains("ROOM_REQUIRED"));
            if(mode==DeliveryMode.HYBRID) assertEquals(absentRoom,r.conflicts().contains("HYBRID_ROOM_REQUIRED"));
            if(mode!=DeliveryMode.CLASSROOM) {assertEquals(absentLink,r.conflicts().contains(mode==DeliveryMode.ONLINE?"MEETING_URL_REQUIRED":"HYBRID_MEETING_URL_REQUIRED")); assertEquals(!absentLink&&!link.startsWith("https://"),r.conflicts().contains("SECURE_MEETING_URL_REQUIRED"));}
        }
        assertTrue(plan(meeting("recordingRequired",true)).conflicts().contains("RECORDING_NOT_SUPPORTED")); assertTrue(plan(meeting("recordingSupported",true)).warnings().contains("RECORDING_AVAILABLE"));
        assertTrue(plan(meeting("requiredEquipment",Set.of("BOARD"))).conflicts().contains("MISSING_EQUIPMENT:BOARD")); assertTrue(plan(meeting("requiredEquipment",Set.of("BOARD"),"availableEquipment",Set.of("BOARD"))).approved());
        assertTrue(plan(meeting("courseKind",CourseKind.LAB)).conflicts().contains("LAB_SAFETY_KIT_REQUIRED"));
        for(int size:new int[]{500,501,1000,1001}) {var r=plan(meeting("deliveryMode",DeliveryMode.ONLINE,"meetingUrl","https://m","expectedParticipants",size)); assertEquals(size>1000,r.conflicts().contains("ONLINE_PLATFORM_CAPACITY_EXCEEDED")); assertEquals(size>500&&size<=1000,r.warnings().contains("LARGE_ONLINE_EVENT"));}
        for(int capacity:new int[]{0,9,10,11,12,36,37}) {var r=plan(meeting("roomCapacity",capacity)); assertEquals(capacity==0,r.conflicts().contains("ROOM_CAPACITY_UNKNOWN")); assertEquals(capacity>0&&capacity<10,r.conflicts().contains("ROOM_TOO_SMALL_FOR_REGISTRATION")); assertEquals(capacity>=10&&capacity<12,r.warnings().contains("ROOM_BELOW_EXPECTED_CAPACITY")); assertEquals(capacity>36,r.warnings().contains("ROOM_SIGNIFICANTLY_OVERSIZED"));}
        assertTrue(plan(meeting("expectedParticipants",9)).warnings().contains("REGISTRATION_EXCEEDS_FORECAST"));
        assertTrue(plan(meeting("accessibleRoomRequired",true,"roomAccessible",false)).conflicts().contains("ACCESSIBLE_ROOM_REQUIRED")); assertFalse(plan(meeting("deliveryMode",DeliveryMode.ONLINE,"accessibleRoomRequired",true,"roomAccessible",false)).conflicts().contains("ACCESSIBLE_ROOM_REQUIRED"));
        for(int size:new int[]{30,31}) assertEquals(size>30,plan(meeting("courseKind",CourseKind.LAB,"expectedParticipants",size)).conflicts().contains("LAB_CAPACITY_EXCEEDED"));
        for(int size:new int[]{50,51}) assertEquals(size>50,plan(meeting("courseKind",CourseKind.WORKSHOP,"expectedParticipants",size)).warnings().contains("WORKSHOP_GROUP_TOO_LARGE"));
    }
    @Test void schedulingRecurrenceReviewAndAdministrativeOverride() {
        for(int count:new int[]{1,2,20,21,52,53}) {var r=plan(meeting("recurring",true,"occurrenceCount",count)); assertEquals(count==1,r.conflicts().contains("RECURRING_SESSION_REQUIRES_MULTIPLE_OCCURRENCES")); assertEquals(count>52,r.conflicts().contains("TOO_MANY_RECURRENCES")); assertEquals(count>20&&count<=52,r.warnings().contains("LONG_RECURRENCE_SERIES")); assertEquals(count>20,r.manualReview());}
        assertTrue(plan(meeting("occurrenceCount",2)).conflicts().contains("OCCURRENCE_COUNT_REQUIRES_RECURRENCE"));
        assertTrue(plan(meeting("recurring",true,"occurrenceCount",2,"courseKind",CourseKind.EXAM)).warnings().contains("RECURRING_EXAM_REQUIRES_REVIEW"));
        for(int count:new int[]{200,201}) assertEquals(count>200,plan(meeting("deliveryMode",DeliveryMode.HYBRID,"meetingUrl","https://m","expectedParticipants",count,"roomCapacity",count)).manualReview());
        for(int hours:new int[]{5,6}) assertEquals(hours==6,plan(meeting("courseKind",CourseKind.WORKSHOP,"end",meeting().start().plusSeconds(hours*3600),"lunchTimeAllowed",true)).manualReview());
        var remote=meeting("courseKind",CourseKind.EXAM,"deliveryMode",DeliveryMode.ONLINE,"meetingUrl","https://m"); assertEquals("MANUAL_REVIEW",plan(remote).status()); assertFalse(plan(changed(remote,"availableEquipment",Set.of("PROCTORING"))).manualReview());
        var override=plan(meeting("roomId",null,"administratorOverride",true)); assertTrue(override.approved()); assertTrue(override.warnings().contains("ADMINISTRATOR_OVERRIDE_APPLIED"));
        for(Object[] nonOverridable:new Object[][]{{"roomCapacity",1},{"accessibleRoomRequired",true,"roomAccessible",false},{"start",T.minusSeconds(60),"end",T.plusSeconds(60)}}) {var r=plan(changed(meeting("administratorOverride",true),nonOverridable)); assertFalse(r.approved()); assertEquals("REJECTED",r.status()); assertTrue(r.warnings().contains("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"));}
    }
    @Test void schedulingIntervalContainmentBookingsAndDailyWorkload() {
        Instant a=meeting().start(),b=meeting().end();
        for(TimeWindow available:List.of(new TimeWindow(a,b),new TimeWindow(a.minusSeconds(1),b.plusSeconds(1)))) assertTrue(plan(meeting("instructorAvailability",List.of(available))).approved());
        for(TimeWindow unavailable:List.of(new TimeWindow(a.plusSeconds(1),b),new TimeWindow(a,b.minusSeconds(1)))) assertTrue(plan(meeting("instructorAvailability",List.of(unavailable))).conflicts().contains("INSTRUCTOR_UNAVAILABLE"));
        for(TimeWindow adjacent:List.of(new TimeWindow(a.minusSeconds(10),a),new TimeWindow(b,b.plusSeconds(10)))) assertFalse(plan(meeting("maintenanceWindows",List.of(adjacent))).conflicts().contains("MAINTENANCE_WINDOW_CONFLICT"));
        assertTrue(plan(meeting("maintenanceWindows",List.of(new TimeWindow(a,b)))).conflicts().contains("MAINTENANCE_WINDOW_CONFLICT"));
        var existing=new ExistingBooking("booking","teacher","room","cohort",a,b,false,true,CourseKind.LECTURE);
        var r=plan(meeting("existingBookings",List.of(existing))); assertTrue(r.conflicts().containsAll(List.of("INSTRUCTOR_BOOKING_CONFLICT:booking","ROOM_BOOKING_CONFLICT:booking","COHORT_BOOKING_CONFLICT:booking"))); assertTrue(r.warnings().contains("CONFLICTING_BOOKING_IS_TENTATIVE:booking"));
        assertTrue(plan(meeting("existingBookings",List.of(changed(existing,"cancelled",true)))).approved());
        var previous=changed(existing,"start",a.minusSeconds(3600),"end",a,"tentative",false);
        assertTrue(plan(meeting("existingBookings",List.of(previous))).approved()); assertTrue(plan(meeting("existingBookings",List.of(previous),"minimumBufferMinutes",1)).conflicts().contains("INSTRUCTOR_BOOKING_CONFLICT:booking"));
        assertTrue(plan(meeting("existingBookings",List.of(previous),"maximumInstructorSessionsPerDay",1)).conflicts().contains("INSTRUCTOR_DAILY_SESSION_LIMIT")); assertTrue(plan(meeting("existingBookings",List.of(previous),"maximumInstructorSessionsPerDay",2)).warnings().contains("INSTRUCTOR_REACHES_DAILY_SESSION_LIMIT"));
        for(int minutes:new int[]{300,301,420,421}) {var work=changed(previous,"start",a.minusSeconds(minutes*60L)); var decision=plan(meeting("existingBookings",List.of(work))); assertEquals(minutes>420,decision.conflicts().contains("INSTRUCTOR_DAILY_HOURS_EXCEEDED")); assertEquals(minutes>300&&minutes<=420,decision.warnings().contains("INSTRUCTOR_LONG_WORKDAY"));}
        assertFalse(plan(meeting("existingBookings",List.of(existing),"administratorOverride",true)).approved());
    }
    @Test void schedulingValidationUsesSpecificDiagnosticMessages() {
        assertThrows(NullPointerException.class,()->plan(null));
        for(String name:List.of("courseId","cohortId","instructorId","zoneId")) for(String text:new String[]{null," "}) rejects(meeting(),StudentTest::plan,name+" must not be blank",name,text);
        for(String name:List.of("deliveryMode","courseKind")) rejects(meeting(),StudentTest::plan,"delivery mode and course kind are required",name,null);
        for(String name:List.of("start","end")) rejects(meeting(),StudentTest::plan,"start and end are required",name,null);
        rejects(meeting(),StudentTest::plan,"end must be after start","end",meeting().start()); rejects(meeting(),StudentTest::plan,"session duration must be less than one day","end",meeting().start().plusSeconds(86400)); rejects(meeting(),StudentTest::plan,"zoneId is invalid","zoneId","Unknown/City");
        for(String name:List.of("expectedParticipants","registeredParticipants","roomCapacity")) rejects(meeting(),StudentTest::plan,"participant and capacity values must not be negative",name,-1);
        rejects(meeting(),StudentTest::plan,"occurrenceCount must be positive","occurrenceCount",0); rejects(meeting(),StudentTest::plan,"maximumInstructorSessionsPerDay must be positive","maximumInstructorSessionsPerDay",0);
        for(int buffer:new int[]{-1,241}) rejects(meeting(),StudentTest::plan,"minimumBufferMinutes must be between 0 and 240","minimumBufferMinutes",buffer);
        for(String name:List.of("instructorAvailability","maintenanceWindows")) for(TimeWindow window:List.of(new TimeWindow(null,T),new TimeWindow(T,null),new TimeWindow(T,T))) rejects(meeting(),StudentTest::plan,name.equals("instructorAvailability")?"instructor availability is invalid":"maintenance window is invalid",name,List.of(window));
        for(String name:List.of("requiredEquipment","availableEquipment")) rejects(meeting(),StudentTest::plan,name.equals("requiredEquipment")?"required equipment must not contain blanks":"available equipment must not contain blanks",name,Set.of(" "));
        var booking=new ExistingBooking("b","i","r","c",T,T.plusSeconds(1),false,false,CourseKind.EXAM);
        for(String text:new String[]{null,""}) rejects(meeting(),StudentTest::plan,"bookingId must not be blank","existingBookings",List.of(changed(booking,"bookingId",text)));
        for(Object[] fields:new Object[][]{{"start",null},{"end",null},{"end",T}}) rejects(meeting(),StudentTest::plan,"booking window is invalid","existingBookings",List.of(changed(booking,fields)));
        rejects(meeting(),StudentTest::plan,"bookingId must be unique","existingBookings",List.of(booking,booking));
    }

    @Test
    void primitiveOperatorsWithUnequalOperands() {
        var operators = new char[]{'+', '-', '*', '/', '%', '^'};
        var answers = new double[]{11, 5, 24, 8.0 / 3, 2, 512};
        for (int i = 0; i < operators.length; i++) {
            assertEquals(answers[i], net.mooctest.expression.engine.NumberUtils.calculation(operators[i], 8, 3), 1e-12);
        }
        assertThrows(net.mooctest.expression.engine.AbnormalOperation.class,
                () -> net.mooctest.expression.engine.NumberUtils.calculation('?', 8, 3));
    }

    @Test
    void comparisonSeparatesEqualityFromOrdering() {
        String[] operators = {"<", ">", "<=", ">=", "==", "=", "!=", "<>"};
        boolean[][] expected = {
            {true, false, true, false, false, false, true, true},
            {false, false, true, true, true, true, false, false},
            {false, true, false, true, false, false, true, true}
        };
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < operators.length; j++) {
                assertEquals(expected[i][j], net.mooctest.expression.engine.NumberUtils.ComparisonOperation(operators[j], i, 1), operators[j]);
            }
        }
    }

    @Test
    void groupedArithmeticAgreesWithWorkedExamples() {
        String name = "RewriteArithmetic";
        var calculator = net.mooctest.expression.engine.BracketsCalculation2.getInstance(name);
        try {
            assertEquals(19.0, calculator.calculation("1+(2+4)*3").getResult(), 1e-10);
            assertEquals(6.0, calculator.calculation("18/(1+2)").getResult(), 1e-10);
            assertEquals(2.0, calculator.calculation("7-2-3").getResult(), 1e-10);
            assertEquals(21.0, calculator.calculation("3*(4+(3))").getResult(), 1e-10);
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calculator.check("2*(3+4"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(name);
        }
    }

    @Test
    void engineSeparatesTopLevelDelimiters() {
        assertEquals(java.util.List.of("a", "(b,c)", "d"), net.mooctest.expression.engine.StrUtils.splitByCharWhereNoB("a,(b,c),d", ','));
        assertEquals(java.util.List.of("a", "(b", "c)", "d"), net.mooctest.expression.engine.StrUtils.splitByChar("a,(b,c),d", ','));
        assertTrue(net.mooctest.expression.engine.StrUtils.containsNumber("a2Z"));
        assertFalse(net.mooctest.expression.engine.StrUtils.containsNumber("az"));
        assertEquals('z', net.mooctest.expression.engine.StrUtils.containsEnChar("12z9"));
        assertNull(net.mooctest.expression.engine.StrUtils.containsEnChar("123"));
    }

    @Test
    void namedExpressionFunctionAcceptsNumericArgumentsAndEvaluatesBody() throws Exception {
        var fn = net.mooctest.expression.engine.ExpressionFunction.parse("delta(a,b)=a*a-b");
        assertEquals("delta", fn.getName());
        assertEquals(2, fn.getParamSize());
        assertEquals(21.0, fn.run(5, 4), 1e-10);
        assertEquals(12.0, fn.run(4, 4), 1e-10);
        assertThrows(UnsupportedOperationException.class, () -> fn.run(5));
        assertThrows(net.mooctest.expression.engine.WrongFormat.class,
                () -> net.mooctest.expression.engine.ExpressionFunction.parse("delta(a,b)=a+z"));
    }

    @Test
    void builtInTimeConversionsUseDistinctUnits() {
        var dates = net.mooctest.expression.engine.FunctionPackage.DATE;
        String[] units = {"S", "Min", "Hour", "Day", "Week", "Month", "Year"};
        long[] millis = {1000L, 60000L, 3600000L, 86400000L, 604800000L, 2592000000L, 31536000000L};
        for (int i = 0; i < units.length; i++) {
            String unit = units[i];
            var toUnit = (net.mooctest.expression.engine.ManyToOneNumberFunction) dates.getFunctionByNameNoCheck("msTo" + unit);
            String prefix = Character.toLowerCase(unit.charAt(0)) + unit.substring(1);
            var toMillis = (net.mooctest.expression.engine.ManyToOneNumberFunction) dates.getFunctionByNameNoCheck(prefix + "ToMs");
            assertEquals(1.5, toUnit.run(1.5 * millis[i]), 1e-12, unit);
            assertEquals(-2.0 * millis[i], toMillis.run(-2), 1e-12, unit);
        }
    }

    @Test
    void directCourseLifecyclePreservesIdentityAndStoredState() {
        var repo = new net.mooctest.course.adapter.InMemoryCourseRepository();
        var clock = java.time.Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC);
        net.mooctest.course.port.ChapterRepository chapters = id -> java.util.List.of(new net.mooctest.course.domain.Chapter("intro", id, "First", 1));
        var service = new net.mooctest.course.service.CourseService(repo, chapters, () -> "linear", new net.mooctest.course.service.PricePolicy(), clock);
        var first = service.createCourse("  Matrix    Theory  ", new java.math.BigDecimal("9.995"));
        assertEquals(new net.mooctest.course.domain.Course("linear", "Matrix Theory", new java.math.BigDecimal("10.00"),
                net.mooctest.course.domain.CourseStatus.DRAFT, java.time.Instant.EPOCH), first);
        var published = service.publishCourse("linear");
        assertEquals(net.mooctest.course.domain.CourseStatus.PUBLISHED, published.status());
        assertEquals(published, repo.findById("linear").orElseThrow());
        var duplicate = assertThrows(net.mooctest.error.BusinessException.class,
                () -> service.createCourse("matrix theory", java.math.BigDecimal.ZERO));
        assertEquals(net.mooctest.error.ErrorCode.DUPLICATE_COURSE_TITLE, duplicate.code());
        var catalog = new net.mooctest.course.service.CatalogService(service, chapters,
                ids -> java.util.List.of(new net.mooctest.course.domain.Video("formula", "intro", "Formula", 45, 1)));
        assertEquals(new net.mooctest.course.domain.CourseCatalog("linear", java.util.List.of(
            new net.mooctest.course.domain.ChapterView("intro", "First", java.util.List.of(
                new net.mooctest.course.domain.VideoView("formula", "Formula", 45))))), catalog.getCatalog("linear"));
    }
    @Test
    void intervalCalculatorsEvaluateEndpointsAndSteps() throws Exception {
        String sumName = "RewriteSums";
        String productName = "RewriteProducts";
        var sum = net.mooctest.expression.engine.FastSumOfIntervalsBrackets.getInstance(sumName);
        var product = net.mooctest.expression.engine.FastMultiplyOfIntervalsBrackets.getInstance(productName);
        try {
            sum.check("1+1,5");
            assertTrue(sum.isCache("1+1,5"));
            assertEquals(14, sum.calculation("1+1,5").getResult(), 1e-12);
            assertEquals(14, sum.calculation("1+1,5").getResult(), 1e-12);
            assertEquals(4, sum.calculation("4", "4").getResult(), 1e-12);
            assertEquals(18.0, sum.compile("3,6", true).calculation(true).getResult());
            assertEquals(48, product.calculation("2,6").getResult(), 1e-12);
            assertEquals(48.0, product.compile("2,6", true).calculation(true).getResult());
            sum.setStartSharedPool(false);
            assertFalse(sum.isStartSharedPool());
            assertEquals(7, sum.calculation("3,4").getResult(), 1e-12);
            sum.clearCache();
            assertFalse(sum.isCache("1+1,5"));
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> sum.check("3"));
            assertThrows(net.mooctest.expression.engine.ExtractException.class, () -> net.mooctest.expression.engine.FastMultiplyOfIntervalsBrackets.getInstance(sumName));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(sumName);
            net.mooctest.expression.engine.CalculationManagement.unregister(productName);
        }
    }

    @Test
    void functionCalculatorResolvesNestedParametersAndClearsItsCache() throws Exception {
        String label = "RewriteFunctions";
        String name = "rewritecube";
        var fn = net.mooctest.expression.engine.ExpressionFunction.parse(name + "(a)=a*a*a");
        var calculator = net.mooctest.expression.engine.FunctionFormulaCalculation2.getInstance(label);
        assertTrue(net.mooctest.expression.engine.CalculationManagement.register(fn));
        try {
            String expression = name + "(2)+1";
            calculator.check(expression);
            assertTrue(calculator.isCache(expression));
            calculator.check(expression);
            assertEquals(9, calculator.calculation(expression).getResult(), 1e-12);
            assertEquals(9, calculator.calculation(expression).getResult(), 1e-12);
            assertEquals(512, calculator.calculation(name + "(" + name + "(2))").getResult(), 1e-12);
            assertEquals(8, (Double) calculator.explain(name + "(2)", true).getResult(), 1e-12);
            assertEquals(6, calculator.calculation("6").getResult(), 1e-12);
            calculator.clearCache();
            assertFalse(calculator.isCache(expression));
            calculator.setStartSharedPool(false);
            assertFalse(calculator.isStartSharedPool());
            assertEquals(28, calculator.calculation(name + "(3)+1").getResult(), 1e-12);
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calculator.check(name + "(2"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregisterF(name);
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void precisionAndComparisonCalculatorsKeepTheirResultTypes() {
        String name = "RewriteBoolean";
        var calculator = net.mooctest.expression.engine.BooleanCalculation2.getInstance(name);
        try {
            assertEquals(true, calculator.calculation("(2+3)<=5", true).getResult());
            assertEquals(false, calculator.calculation("2+3<5", true).getResult());
            assertEquals(true, calculator.calculation("4<>5", true).getResult());
            assertEquals(false, calculator.calculation("4!=4", true).getResult());
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(name);
        }
    }


    @Test
    void conditionalFunctionsChooseTheSpecifiedValue() {
        var branch = net.mooctest.expression.engine.FunctionPackage.BRANCH;
        String[] names = {"ifEq", "ifGt", "ifLt", "ifGe", "ifLe"};
        double[][] results = {{23, 11, 23}, {23, 23, 11}, {11, 23, 23}, {23, 11, 11}, {11, 11, 23}};
        for (int i = 0; i < names.length; i++) {
            var fn = (net.mooctest.expression.engine.ManyToOneNumberFunction) branch.getFunctionByNameNoCheck(names[i]);
            for (int x = 0; x <= 2; x++) {
                assertEquals(results[i][x], fn.run(x, 1, 11, 23), 1e-12);
            }
            assertThrows(UnsupportedOperationException.class, () -> fn.run(1, 1, 11));
        }
    }

    @Test
    void mathPackageUsesNumericArguments() {
        var math = net.mooctest.expression.engine.FunctionPackage.MATH;
        var examples = java.util.Map.of(
                "sum", new double[]{10, 7, -2, 5}, "pow", new double[]{81, 3, 4},
                "sqrt", new double[]{7, 49}, "factorial", new double[]{120, 5},
                "sin", new double[]{0.5, Math.PI / 6}, "cos", new double[]{0.5, Math.PI / 3},
                "tan", new double[]{1, Math.PI / 4}, "cot", new double[]{1, Math.PI / 4},
                "sec", new double[]{2, Math.PI / 3}, "csc", new double[]{2, Math.PI / 6});
        examples.forEach((name, sample) -> {
            var fn = (net.mooctest.expression.engine.ManyToOneNumberFunction) math.getFunctionByNameNoCheck(name);
            assertEquals(sample[0], fn.run(java.util.Arrays.copyOfRange(sample, 1, sample.length)), 1e-10, name);
        });
        assertNull(math.getFunctionByNameNoCheck("unknown"));
    }

    @Test
    void complexCalculatorSeparatesParenthesizedRealAndImaginaryParts() throws Exception {
        String label = "RewriteComplex";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            calc.check("(2+3)+4i");
            var positive = calc.compile(" (2+3) + 4i ", true);
            assertEquals(5.0, positive.calculation(true).getResult());
            assertEquals(new net.mooctest.expression.engine.CalculationComplexResults(0, 5, 4, label).toString(),
                    positive.calculation(true).toString());
            assertTrue(positive.isUnBigDecimal());
            assertTrue(positive.isBigDecimal());
            assertNotNull(positive.getFunctionExpression1());
            assertNotNull(positive.getFunctionExpression2());
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calc.check("7+8"));
            assertThrows(RuntimeException.class, () -> calc.check("7"));
            assertThrows(UnsupportedOperationException.class, () -> calc.explain("7+8", true));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void compilerRejectsIncompleteSignaturesBeforeCalculating() {
        var malformed = java.util.List.of("f(a)", "f(a)=a=1", "f=a", "f2(a)=a");
        for (var signature : malformed) {
            assertThrows(IllegalArgumentException.class,
                    () -> net.mooctest.expression.engine.JvmExpressionFunction.convertToStaticMethodSource(signature));
        }
        assertEquals("public static double weighted(double a, double b) { return 3*a+b; }",
                net.mooctest.expression.engine.JvmExpressionFunction.convertToStaticMethodSource("weighted(a,b)=3*a+b"));
        assertThrows(IllegalArgumentException.class,
                () -> net.mooctest.expression.engine.EquationSolver.compile("x+2", "isolated"));
        assertThrows(NumberFormatException.class,
                () -> net.mooctest.expression.engine.EquationSolver.compile("x+2=abc", "isolated"));
    }

    @Test
    void pricePolicyEnforcesBoundsBeforeScaling() {
        var policy = new net.mooctest.course.service.PricePolicy();
        var invalid = new java.math.BigDecimal[]{null, new java.math.BigDecimal("-0.001"), new java.math.BigDecimal("99999.991")};
        for (var price : invalid) {
            var error = assertThrows(net.mooctest.error.BusinessException.class, () -> policy.normalizePrice(price));
            assertEquals(net.mooctest.error.ErrorCode.INVALID_PRICE, error.code());
        }
        assertEquals(new java.math.BigDecimal("99999.99"), policy.normalizePrice(new java.math.BigDecimal("99999.99")));
        assertEquals(new java.math.BigDecimal("0.00"), policy.normalizePrice(java.math.BigDecimal.ZERO));
        var price = new java.math.BigDecimal("25.35");
        assertEquals(new java.math.BigDecimal("5.07"), policy.discountedPrice(price, 80));
        assertEquals(price, policy.discountedPrice(price, 0));
        for (int invalidDiscount : new int[]{-1, 81}) {
            assertEquals(net.mooctest.error.ErrorCode.INVALID_DISCOUNT, assertThrows(net.mooctest.error.BusinessException.class,
                    () -> policy.discountedPrice(price, invalidDiscount)).code());
        }
    }

    @Test
    void restHandlerPreservesErrorCodeAndDetails() {
        var handler = new net.mooctest.course.web.RestExceptionHandler();
        var codes = net.mooctest.error.ErrorCode.class;
        var expected = java.util.Map.of(
                "COURSE_NOT_FOUND", 404, "DUPLICATE_COURSE_TITLE", 409, "COURSE_ALREADY_PUBLISHED", 409,
                "COURSE_HAS_NO_CHAPTERS", 422, "INVALID_COURSE_TITLE", 400, "INVALID_PRICE", 400, "INVALID_DISCOUNT", 400);
        expected.forEach((name, status) -> {
            var response = handler.handleBusinessException(new net.mooctest.error.BusinessException(Enum.valueOf(codes, name), "issue"));
            assertEquals(status.intValue(), response.getStatusCode().value());
            assertFalse(response.getBody().success());
            assertNull(response.getBody().data());
            assertEquals(name, response.getBody().error().code());
            assertEquals("issue", response.getBody().error().message());
        });
    }

    @Test
    void basicFunctionCalculatorHandlesIntegralAndFractionalResults() throws Exception {
        String label = "RewriteBasicFunctions";
        var run = net.mooctest.expression.engine.FunctionFormulaCalculation.getInstance(label);
        var function = net.mooctest.expression.engine.ExpressionFunction.parse("rewritehalve(a)=a/2");
        assertTrue(net.mooctest.expression.engine.CalculationManagement.register(function));
        try {
            assertEquals("rewritehalve(6)+1", run.formatStr(" rewritehalve( 6 ) + 1 "));
            run.check("rewritehalve(123)");
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> run.check("rewritehalve(123"));
            assertEquals(4, run.calculation("rewritehalve((2+4))+1").getResult(), 1e-12);
            assertEquals(3.5, run.calculation("rewritehalve(5) + 1").getResult(), 1e-12);
            assertEquals(9, run.calculation("(2+1)*3").getResult(), 1e-12);
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregisterF("rewritehalve");
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void factorialAndDecimalParsingRespectCacheAndPrecisionModes() {
        boolean prior = net.mooctest.expression.engine.Mathematical_Expression.Options.isUseCache();
        try {
            for (boolean useCache : new boolean[]{false, true}) {
                net.mooctest.expression.engine.Mathematical_Expression.Options.setUseCache(useCache);
                for (int i = 0; i < 2; i++) {
                    assertEquals(-73.0625, net.mooctest.expression.engine.StrUtils.stringToDouble("-73.0625"), 0);
                    assertEquals(720, net.mooctest.expression.engine.StrUtils.stringToDouble("6!"), 0);
                    assertEquals(new java.math.BigDecimal("-73.0625"), net.mooctest.expression.engine.StrUtils.stringToBigDecimal("-73.0625"));
                    assertEquals(0, new java.math.BigDecimal("720").compareTo(net.mooctest.expression.engine.StrUtils.stringToBigDecimal("6!")));
                }
                assertThrows(UnsupportedOperationException.class, () -> net.mooctest.expression.engine.StrUtils.stringToBigDecimal("-1!"));
                assertThrows(UnsupportedOperationException.class, () -> net.mooctest.expression.engine.StrUtils.stringToDouble("-1!"));
                assertThrows(NumberFormatException.class, () -> net.mooctest.expression.engine.StrUtils.stringToDouble("1..1"));
            }
        } finally {
            net.mooctest.expression.engine.Mathematical_Expression.Options.setUseCache(prior);
        }
    }

    @Test
    void parsedFunctionSurvivesSerializationRoundTrip() throws Exception {
        var function = net.mooctest.expression.engine.ExpressionFunction.parse("rewritegap(a,b)=a-b+a");
        var bytes = new java.io.ByteArrayOutputStream();
        try (var output = new java.io.ObjectOutputStream(bytes)) {
            function.saveTo(output);
        }
        try (var input = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            var restored = net.mooctest.expression.engine.ExpressionFunction.readFrom(input);
            assertEquals("rewritegap", restored.getName());
            assertEquals("rewritegap(a,b)=a-b+a", restored.toString());
            assertEquals(13, restored.run(8, 3), 1e-12);
        }
    }

    @Test
    void rangeArithmeticAgreesWithEnumeratedTerms() {
        String label = "RewriteRangeOracle";
        var calc = net.mooctest.expression.engine.BracketsCalculation2.getInstance(label);
        try {
            for (int start = 1; start <= 3; start++) {
                for (int end = start; end <= start + 4; end++) {
                    int consecutiveTotal = 0;
                    for (int x = start; x <= end; x++) consecutiveTotal += x;
                    assertEquals(consecutiveTotal, net.mooctest.expression.engine.NumberUtils.sumOfRange(start, end));
                    assertEquals(consecutiveTotal, net.mooctest.expression.engine.NumberUtils.sumOfRange((double) end, start), 1e-12);
                    for (int step = 1; step <= 3; step++) {
                        int total = 0;
                        int product = 1;
                        for (int x = start; x <= end; x += step) {
                            total += x;
                            product *= x;
                        }
                        assertEquals(total, net.mooctest.expression.engine.NumberUtils.sumOfRange(start, end, step), 1e-12);
                        assertEquals(product, net.mooctest.expression.engine.NumberUtils.MultiplyOfRange(start, end, step), 1e-12);
                        String sum = net.mooctest.expression.engine.NumberUtils.sumOfRangeString(start, end, step);
                        String multiply = net.mooctest.expression.engine.NumberUtils.MultiplyOfRangeString(start, end, step);
                        assertEquals(total, calc.calculation(sum).getResult(), 1e-12);
                        assertEquals(product, Double.parseDouble(multiply), 1e-12);
                    }
                }
            }
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void prefixCompilerProducesExactDecimalAndRejectsIncompleteSyntax() throws Exception {
        String label = "RewritePrefix";
        var calc = net.mooctest.expression.engine.PrefixExpressionOperation.getInstance(label);
        try {
            calc.check("0.1+0.2");
            var exact = (net.mooctest.expression.engine.CalculationNumberResults)
                    calc.compileBigDecimal("0.1+0.2", true).calculationBigDecimals(true);
            assertEquals(0, new java.math.BigDecimal("0.3").compareTo(exact.getBigDecimalResult()));
            var compiled = calc.compile("12/3+4*2", true);
            assertEquals(12.0, compiled.calculation(true).getResult());
            assertEquals(12.0, compiled.calculation(true).getResult());
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calc.check("3+"));
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calc.check("3@2"));
            assertEquals(11.0, (Double) calc.explain("3+4*2", true).getResult());
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void numericalSolversFindTheIntersectionOfAnAffineFunction() throws Exception {
        var affine = new net.mooctest.expression.engine.ManyToOneNumberFunction("affine") {
            @Override public double run(double... arguments) {
                return 4 * arguments[0] - 7;
            }
        };
        var solver = new net.mooctest.expression.engine.EquationSolver(
                "4*x-7", 13, "RewriteAffine", affine) {};
        solver.setNewtonInitialX(-8);
        solver.setMaxIter(15);
        solver.setTol(1e-9);
        assertEquals(5.0, solver.calculation(false).getResult(), 1e-8);
        assertEquals(13, solver.getResult());
        assertEquals(-8, solver.getNewtonInitialX());
        assertEquals(15, solver.getMaxIter());
        assertEquals(1e-9, solver.getTol());
        assertThrows(ArithmeticException.class, () -> solver.solveNewton(1e-9, 1));
        solver.setBisectionLeft(-3);
        solver.setBisectionRight(13);
        solver.setUseNewton(false);
        assertEquals(-3, solver.getBisectionLeft());
        assertEquals(13, solver.getBisectionRight());
        assertFalse(solver.isUseNewton());
        assertEquals(5, solver.calculation(false).getResult());
        var bisected = solver.solveBisection(1e-9, 4);
        assertEquals(5, bisected[0]);
        assertEquals(1, bisected[1]);
        solver.setBisectionLeft(6);
        assertThrows(IllegalArgumentException.class, () -> solver.solveBisection(1e-9, 15));
        assertFalse(solver.isBigDecimal());
        assertTrue(solver.isUnBigDecimal());
        assertThrows(UnsupportedOperationException.class, solver::convertToMultiPrecisionSupported);
        assertThrows(UnsupportedOperationException.class, () -> solver.calculationBigDecimals(true));
    }


    // =================================================================
    // Reference suite: expression engine, grading, progress, course, pricing,
    // refund, enrollment, scheduling, equation solving.
    // =================================================================

    @BeforeEach
    void resetEngineState() {
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.Options.setUseBigDecimal(false);
        // Isolate the static component registry between tests so every fresh-name
        // getInstance call deterministically exercises its registration path.
        try {
            Field registry = CalculationManagement.class.getDeclaredField("STRING_CALCULATION_HASH_MAP");
            registry.setAccessible(true);
            ((Map<?, ?>) registry.get(null)).clear();
        } catch (Exception e) {
            throw new AssertionError("cannot reset component registry", e);
        }
    }

    @AfterEach
    void restoreEngineState() {
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.Options.setUseBigDecimal(false);
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    /** Probe subclass exposing the protected expression-builder accessor of {@link NameExpression}. */
    private static final class ProbeNameExpression extends NameExpression {
        ProbeNameExpression() {
            super("1+2", "probe");
        }

        StringBuilder expressionBuilder() {
            return getExpressionStrBuilder();
        }

        @Override
        public boolean isBigDecimal() {
            return false;
        }

        @Override
        public boolean isUnBigDecimal() {
            return true;
        }

        @Override
        public void convertToMultiPrecisionSupported() {
            throw new UnsupportedOperationException("probe");
        }

        @Override
        public CalculationResults calculation(boolean isCopy) {
            throw new UnsupportedOperationException("probe");
        }

        @Override
        public CalculationResults calculationBigDecimals(boolean isCopy) {
            throw new UnsupportedOperationException("probe");
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: string/number utilities
    // ------------------------------------------------------------------
    @Nested
    class EngineUtilsTests {

        @Test
        void splitByCharSplitsAndTrimsSegments() {
            assertEquals(List.of("1", "2", "3"), StrUtils.splitByChar("1,2,3", ','));
            assertEquals(List.of("1", "2", "3"), StrUtils.splitByChar(" 1 , 2 , 3 ", ','));
            assertEquals(List.of(""), StrUtils.splitByChar("", ','));
            assertEquals(List.of("abc"), StrUtils.splitByChar("abc", ','));
            assertEquals(List.of("1", "", "2"), StrUtils.splitByChar("1,,2", ','));
        }

        @Test
        void removeEmptyStripsSpaceCharacters() {
            assertEquals("123", StrUtils.removeEmpty("1 2 3"));
            assertEquals("abc", StrUtils.removeEmpty("abc"));
            assertEquals("", StrUtils.removeEmpty("   "));
        }

        @Test
        void stringToDoubleParsesPlainAndFactorialNumbers() {
            assertEquals(3.5, StrUtils.stringToDouble("3.5"));
            assertEquals(6.0, StrUtils.stringToDouble("3!"));
            assertEquals(0.0, StrUtils.stringToDouble("0!"));
            assertEquals(1.0, StrUtils.stringToDouble("1!"));
            assertEquals(120.0, StrUtils.stringToDouble("5!"));
        }

        @Test
        void stringToDoubleRejectsNegativeFactorialAndMalformedText() {
            assertThrows(UnsupportedOperationException.class, () -> StrUtils.stringToDouble("-3!"));
            assertThrows(NumberFormatException.class, () -> StrUtils.stringToDouble("abc"));
            assertThrows(StringIndexOutOfBoundsException.class, () -> StrUtils.stringToDouble(""));
        }

        @Test
        void stringToDoubleUsesCacheWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            assertEquals(7.5, StrUtils.stringToDouble("7.5"));
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("7.5".hashCode()));
            assertEquals(7.5, StrUtils.stringToDouble("7.5"));
        }

        @Test
        void stringToDoubleCachesFactorialResultWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            assertEquals(120.0, StrUtils.stringToDouble("5!"));
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("5!".hashCode()));
            assertEquals(120.0, StrUtils.stringToDouble("5!"));
        }

        @Test
        void cacheLookupsReturnCachedValuesVerbatim() {
            Mathematical_Expression.Options.setUseCache(true);
            Mathematical_Expression.Options.cacheCalculation("7.5".hashCode(), 999.0);
            assertEquals(999.0, StrUtils.stringToDouble("7.5"));
            Mathematical_Expression.Options.cacheCalculation("7.5".hashCode(), 7.5);

            Mathematical_Expression.Options.cacheCalculation("2.5".hashCode(), new BigDecimal("999"));
            assertEquals(0, new BigDecimal("999").compareTo(StrUtils.stringToBigDecimal("2.5")));
            Mathematical_Expression.Options.cacheCalculation("2.5".hashCode(), new BigDecimal("2.5"));
        }

        @Test
        void stringToBigDecimalComputesFactorialOfZero() {
            assertEquals(0, BigDecimal.ZERO.compareTo(StrUtils.stringToBigDecimal("0!")));
        }

        @Test
        void nameExpressionExposesExpressionBuilder() {
            ProbeNameExpression probe = new ProbeNameExpression();
            assertNotNull(probe.expressionBuilder());
            assertEquals("1+2", probe.expressionBuilder().toString());
        }

        @Test
        void stringToBigDecimalParsesPlainAndFactorialNumbers() {
            assertEquals(0, new BigDecimal("2.5").compareTo(StrUtils.stringToBigDecimal("2.5")));
            assertEquals(0, BigDecimal.valueOf(6).compareTo(StrUtils.stringToBigDecimal("3!")));
            assertThrows(UnsupportedOperationException.class, () -> StrUtils.stringToBigDecimal("-3!"));
            assertThrows(NumberFormatException.class, () -> StrUtils.stringToBigDecimal("abc"));
        }

        @Test
        void stringToBigDecimalIgnoresNonBigDecimalCacheEntriesAndCachesOwnResult() {
            Mathematical_Expression.Options.setUseCache(true);
            StrUtils.stringToDouble("8.25");
            assertEquals(0, new BigDecimal("8.25").compareTo(StrUtils.stringToBigDecimal("8.25")));
            assertEquals(0, new BigDecimal("4.5").compareTo(StrUtils.stringToBigDecimal("4.5")));
            assertTrue(Mathematical_Expression.Options.getCacheCalculation("4.5".hashCode()) instanceof BigDecimal);
            assertEquals(0, new BigDecimal("4.5").compareTo(StrUtils.stringToBigDecimal("4.5")));
        }

        @Test
        void stringToBigDecimalUsesCacheForFactorialResults() {
            Mathematical_Expression.Options.setUseCache(true);
            assertEquals(0, BigDecimal.valueOf(6).compareTo(StrUtils.stringToBigDecimal("3!")));
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("3!".hashCode()));
            assertEquals(0, BigDecimal.valueOf(6).compareTo(StrUtils.stringToBigDecimal("3!")));
        }

        @Test
        void isAnOperatorRecognizesOnlyArithmeticOperators() {
            for (char op : new char[]{'+', '-', '*', '/', '%', '^'}) {
                assertTrue(StrUtils.IsAnOperator(op));
            }
            for (char c : new char[]{'a', '=', '!', '(', ' ', '0'}) {
                assertFalse(StrUtils.IsAnOperator(c));
            }
        }

        @Test
        void isANumberValidatesDigitsOnly() {
            assertTrue(StrUtils.IsANumber("123"));
            assertTrue(StrUtils.IsANumber(""));
            assertFalse(StrUtils.IsANumber("12a"));
            assertFalse(StrUtils.IsANumber("1.5"));
            assertFalse(StrUtils.IsANumber(" 1"));
            assertTrue(StrUtils.IsANumber('0'));
            assertTrue(StrUtils.IsANumber('9'));
            assertFalse(StrUtils.IsANumber('a'));
            assertFalse(StrUtils.IsANumber('.'));
        }

        @Test
        void numberUtilsCalculationSupportsAllOperatorsAndRejectsUnknown() {
            assertEquals(5.0, NumberUtils.calculation('+', 2, 3));
            assertEquals(-1.0, NumberUtils.calculation('-', 2, 3));
            assertEquals(6.0, NumberUtils.calculation('*', 2, 3));
            assertEquals(2.0, NumberUtils.calculation('/', 6, 3));
            assertEquals(1.0, NumberUtils.calculation('%', 7, 3));
            assertEquals(8.0, NumberUtils.calculation('^', 2, 3));
            assertThrows(AbnormalOperation.class, () -> NumberUtils.calculation('x', 1, 2));
        }

        @Test
        void priorityComparisonOnlyTreatsLowPriorityBeforeHighPriorityAsTrue() {
            assertTrue(NumberUtils.PriorityComparison('+', '*'));
            assertTrue(NumberUtils.PriorityComparison('+', '/'));
            assertTrue(NumberUtils.PriorityComparison('-', '%'));
            assertTrue(NumberUtils.PriorityComparison('-', '^'));
            assertFalse(NumberUtils.PriorityComparison('+', '+'));
            assertFalse(NumberUtils.PriorityComparison('-', '-'));
            assertFalse(NumberUtils.PriorityComparison('*', '+'));
            assertFalse(NumberUtils.PriorityComparison('/', '+'));
            assertFalse(NumberUtils.PriorityComparison('%', '-'));
            assertFalse(NumberUtils.PriorityComparison('^', '+'));
        }

        @Test
        void comparisonOperationSupportsEveryComparisonOperator() {
            assertTrue(NumberUtils.ComparisonOperation("<", 1, 2));
            assertFalse(NumberUtils.ComparisonOperation("<", 2, 2));
            assertTrue(NumberUtils.ComparisonOperation(">", 2, 1));
            assertTrue(NumberUtils.ComparisonOperation("<=", 2, 2));
            assertTrue(NumberUtils.ComparisonOperation(">=", 2, 2));
            assertTrue(NumberUtils.ComparisonOperation("=", 2, 2));
            assertTrue(NumberUtils.ComparisonOperation("==", 2, 2));
            assertFalse(NumberUtils.ComparisonOperation("==", 2, 3));
            assertTrue(NumberUtils.ComparisonOperation("!=", 2, 3));
            assertTrue(NumberUtils.ComparisonOperation("<>", 2, 3));
            assertThrows(AbnormalOperation.class, () -> NumberUtils.ComparisonOperation("?", 1, 2));
        }

        @Test
        void comparisonOperationBoundaryCasesReturnFalse() {
            assertFalse(NumberUtils.ComparisonOperation(">", 2, 2));
            assertFalse(NumberUtils.ComparisonOperation("<=", 2, 1));
            assertFalse(NumberUtils.ComparisonOperation(">=", 1, 2));
            assertFalse(NumberUtils.ComparisonOperation("!=", 2, 2));
        }

        @Test
        void sumOfRangeStringWithOffsetStartAndStep() {
            assertEquals("4.0*2.0+4.0*(4.0-1)*2.0/2", NumberUtils.sumOfRangeString(2, 8, 2));
        }

        @Test
        void sumOfRangeStringAdjustsEndWhenStepDoesNotDivideRange() {
            assertEquals("3.0*2.0+3.0*(3.0-1)*3.0/2", NumberUtils.sumOfRangeString(2, 8, 3));
        }

        @Test
        void multiplyOfRangeWithEqualBoundsReturnsStartImmediately() throws Throwable {
            // Bounded loops run in a daemon thread: a mutated zero/negative step must
            // surface as a deterministic assertion failure, never as a hung fork.
            FutureTask<Void> task = new FutureTask<>(() -> {
                assertEquals(3.0, NumberUtils.MultiplyOfRange(3, 3, 0));
                assertEquals("3.0", NumberUtils.MultiplyOfRangeString(3, 3, 0));
                assertEquals(280.0, NumberUtils.MultiplyOfRange(1, 10, 3));
                assertEquals("280.0", NumberUtils.MultiplyOfRangeString(1, 10, 3));
                assertEquals("((0.0*0.0))+((1.0*1.0))+((2.0*2.0))",
                        CumulativeCalculation.getInstance("cumulativeTimeoutInstance").formatStr("n[0,2,1](n*n)"));
            }, null);
            Thread thread = new Thread(task, "range-product-bounds");
            thread.setDaemon(true);
            thread.start();
            try {
                task.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException timeout) {
                throw new AssertionError("finite range product did not return within one second", timeout);
            } catch (ExecutionException failure) {
                throw failure.getCause();
            } finally {
                task.cancel(true);
            }
        }

        @Test
        void multiplyOfRangeReachesTheFinalTermOnTheTestThread() {
            assertEquals(280.0, NumberUtils.MultiplyOfRange(1, 10, 3));
            assertEquals("280.0", NumberUtils.MultiplyOfRangeString(1, 10, 3));
        }

        @Test
        void factorialComputesRecursively() {
            assertEquals(0.0, NumberUtils.factorial(0));
            assertEquals(1.0, NumberUtils.factorial(1));
            assertEquals(120.0, NumberUtils.factorial(5));
            assertEquals(0.5, NumberUtils.factorial(0.5));
        }

        @Test
        void divideByTenUsesBitShiftDivision() {
            assertEquals(1, NumberUtils.divideByTen(10));
            assertEquals(0, NumberUtils.divideByTen(9));
            assertEquals(0, NumberUtils.divideByTen(0));
        }

        @Test
        void sumOfRangeStringBuildsArithmeticSeriesFormula() {
            assertEquals("5.0", NumberUtils.sumOfRangeString(5, 5));
            assertEquals("(1.0+3.0)*3.0/2", NumberUtils.sumOfRangeString(1, 3));
            assertEquals("(3.0+1.0)*3.0/2", NumberUtils.sumOfRangeString(3, 1));
        }

        @Test
        void sumOfRangeStringWithStepDelegatesAndAdjustsForStep() {
            assertEquals("(1.0+3.0)*3.0/2", NumberUtils.sumOfRangeString(1, 3, 1));
            assertEquals("5.0", NumberUtils.sumOfRangeString(5, 5, 2));
            assertEquals("6.0*0.0+6.0*(6.0-1)*2.0/2", NumberUtils.sumOfRangeString(0, 10, 2));
            assertEquals("3.0*0.0+3.0*(3.0-1)*2.0/2", NumberUtils.sumOfRangeString(0, 5, 2));
        }

        @Test
        void multiplyOfRangeMultipliesElementsWithinReachableRange() {
            assertEquals(2.0, NumberUtils.MultiplyOfRange(2, 2, 1));
            assertEquals(6.0, NumberUtils.MultiplyOfRange(1, 3, 1));
            assertEquals(3.0, NumberUtils.MultiplyOfRange(1, 3, 2));
            assertEquals(3.0, NumberUtils.MultiplyOfRange(1, 4, 2));
        }

        @Test
        void multiplyOfRangeStringReturnsResultAsString() {
            assertEquals("2.0", NumberUtils.MultiplyOfRangeString(2, 2, 1));
            assertEquals("6.0", NumberUtils.MultiplyOfRangeString(1, 3, 1));
            assertEquals("3.0", NumberUtils.MultiplyOfRangeString(1, 4, 2));
        }

        @Test
        void constantRegionExposesConfiguredConstants() {
            assertEquals(1.57F, ConstantRegion.VERSION);
            assertEquals("null", ConstantRegion.STRING_NULL);
            assertEquals('(', ConstantRegion.LEFT_BRACKET);
            assertEquals(')', ConstantRegion.RIGHT_BRACKET);
            assertEquals(">", ConstantRegion.GREATER_THAN_SIGN);
            assertEquals("<", ConstantRegion.LESS_THAN_SIGN);
            assertEquals("!=", ConstantRegion.NOT_EQUAL_SIGN1);
            assertEquals("<>", ConstantRegion.NOT_EQUAL_SIGN2);
            assertEquals("<=", ConstantRegion.LESS_THAN_OR_EQUAL_TO_SIGN);
            assertEquals(">=", ConstantRegion.GREATER_THAN_OR_EQUAL_TO_SIGN);
            assertEquals("==", ConstantRegion.EQUAL_SIGN2);
            assertEquals("pow", ConstantRegion.SIGN_NAME.get('^'));
            assertEquals("_LB", ConstantRegion.SIGN_NAME.get('('));
            assertEquals("RB_", ConstantRegion.SIGN_NAME.get(')'));
            assertEquals("", ConstantRegion.SIGN_NAME.get(' '));
            assertNull(ConstantRegion.SIGN_NAME.get('+'));
        }

        @Test
        void calculationOptimizedSupportsAllBigDecimalOperations() {
            BigDecimal two = new BigDecimal("2");
            BigDecimal three = new BigDecimal("3");
            BigDecimal seven = new BigDecimal("7");
            assertEquals(0, new BigDecimal("5").compareTo(CalculationOptimized.calculation('+', two, three)));
            assertEquals(0, new BigDecimal("-1").compareTo(CalculationOptimized.calculation('-', two, three)));
            assertEquals(0, new BigDecimal("6").compareTo(CalculationOptimized.calculation('*', two, three)));
            assertEquals(0, new BigDecimal("3").compareTo(CalculationOptimized.calculation('/', new BigDecimal("6"), two)));
            assertEquals(0, new BigDecimal("1").compareTo(CalculationOptimized.calculation('%', seven, three)));
            assertEquals(0, new BigDecimal("8").compareTo(CalculationOptimized.calculation('^', two, three)));
            assertThrows(AbnormalOperation.class, () -> CalculationOptimized.calculation('x', two, three));
        }

        @Test
        void engineOptionsToggleBigDecimalAndCacheModes() {
            assertFalse(Mathematical_Expression.Options.isUseBigDecimal());
            Mathematical_Expression.Options.setUseBigDecimal(true);
            assertTrue(Mathematical_Expression.Options.isUseBigDecimal());
            assertFalse(Mathematical_Expression.Options.isUseCache());
            Mathematical_Expression.Options.setUseCache(true);
            assertTrue(Mathematical_Expression.Options.isUseCache());
        }

        @Test
        void engineOptionsCacheStoresNumbersAndExpressionsAndClearOnDisable() {
            Mathematical_Expression.Options.setUseCache(true);
            Mathematical_Expression.Options.cacheCalculation(42, 3.25);
            assertEquals(3.25, Mathematical_Expression.Options.getCacheCalculation(42).doubleValue());
            assertNull(Mathematical_Expression.Options.getCacheCalculation(99));

            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("optCacheInstance");
            PackExpression pack = BracketsExpression.compile("1+1", true, calculator);
            Mathematical_Expression.Options.cacheCalculation("formulaKey", pack);
            assertSame(pack, Mathematical_Expression.Options.getCacheCalculation("formulaKey"));
            assertNull(Mathematical_Expression.Options.getCacheCalculation("missingKey"));

            Mathematical_Expression.Options.setUseCache(false);
            assertNull(Mathematical_Expression.Options.getCacheCalculation(42));
            assertNull(Mathematical_Expression.Options.getCacheCalculation("formulaKey"));
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: ArithmeticExpression (decimal parser)
    // ------------------------------------------------------------------
    @Nested
    class ManagementAndResultsTests {



        @Test
        void lookupMissingNameReturnsNullAndIsRegisterFalse() {
            assertNull(CalculationManagement.getCalculationByName("absent-name-xyz"));
            assertFalse(CalculationManagement.isRegister("absent-name-xyz"));
        }

        @Test
        void booleanCalculationGetInstanceRejectsWrongRegisteredType() {
            String name = "wrongTypeBoolean";
            BracketsCalculation2.getInstance(name);
            assertThrows(ExtractException.class, () -> BooleanCalculation2.getInstance(name));
        }

        @Test
        void bracketsCalculationGetInstanceRejectsWrongRegisteredType() {
            String name = "wrongTypeBrackets";
            BooleanCalculation2.getInstance(name);
            assertThrows(ExtractException.class, () -> BracketsCalculation2.getInstance(name));
        }

        @Test
        void prefixOperationGetInstanceRejectsWrongRegisteredType() {
            String name = "wrongTypePrefix";
            BooleanCalculation2.getInstance(name);
            assertThrows(ExtractException.class, () -> PrefixExpressionOperation.getInstance(name));
        }

        @Test
        void cumulativeGetInstanceRejectsWrongRegisteredType() {
            String name = "wrongTypeCumulative";
            BooleanCalculation2.getInstance(name);
            assertThrows(ExtractException.class, () -> CumulativeCalculation.getInstance(name));
        }

        @Test
        void cumulativeInstanceIsAlsoABracketsCalculation() {
            String name = "cumulativeAsBrackets";
            CumulativeCalculation.getInstance(name);
            assertTrue(BracketsCalculation2.getInstance(name) instanceof CumulativeCalculation);
        }

        @Test
        void calculationNumberResultsExposesStoredData() {
            CalculationNumberResults r = new CalculationNumberResults(3, 1.5, "src");
            assertEquals(3, r.getResultLayers());
            assertEquals(1.5, r.getResult());
            assertEquals("src", r.getCalculationSourceName());
            assertEquals(0, new BigDecimal("1.5").compareTo(r.getBigDecimalResult()));
            r.setSource("other");
            assertEquals("other", r.getCalculationSourceName());
            assertTrue(r.toString().contains("result=1.5"));
        }

        @Test
        void calculationNumberResultsCompareUsesCeilingOfDifference() {
            CalculationNumberResults a = new CalculationNumberResults(0, 1.5, "a");
            CalculationNumberResults b = new CalculationNumberResults(0, 1.0, "b");
            CalculationNumberResults c = new CalculationNumberResults(0, 2.0, "c");
            assertEquals(1, a.compareTo(b));
            assertEquals(0, a.compareTo(c));
            assertEquals(-1, b.compareTo(c));
            assertEquals(0, a.compareTo(new CalculationNumberResults(0, 1.5, "a2")));
            assertEquals(1, a.compareTo(null));
            assertEquals(-1, a.compare(b, c));
        }

        @Test
        void calculationBooleanResultsExposesStoredData() {
            CalculationBooleanResults r = new CalculationBooleanResults(true, "src", 2, 1.0, 2.0);
            assertTrue(r.getResult());
            assertEquals(2, r.getResultLayers());
            assertEquals("src", r.getCalculationSourceName());
            assertEquals(1.0, r.getLeft());
            assertEquals(2.0, r.getRight());
            r.setSource("other");
            assertEquals("other", r.getCalculationSourceName());
            assertTrue(r.toString().contains("result=true"));
            assertTrue(r.toString().contains("left=1.0"));
        }

        @Test
        void calculationBooleanResultsCompareByBooleanValue() {
            CalculationBooleanResults t1 = new CalculationBooleanResults(true, "a", 0, 0, 0);
            CalculationBooleanResults t2 = new CalculationBooleanResults(true, "b", 0, 0, 0);
            CalculationBooleanResults f = new CalculationBooleanResults(false, "c", 0, 0, 0);
            assertEquals(0, t1.compareTo(t2));
            assertEquals(1, t1.compareTo(f));
            assertEquals(-1, f.compareTo(t1));
            assertEquals(1, t1.compareTo(null));
            assertEquals(1, t1.compare(t1, f));
            assertEquals(-1, f.compare(f, t1));
        }

        @Test
        void logResultsStoresResultButRejectsUnsupportedOperations() {
            LogResults log = new LogResults("logName");
            assertNull(log.getResult());
            assertEquals("logName", log.getCalculationSourceName());
            log.setResult(3.0);
            assertEquals(3.0, log.getResult());
            assertThrows(UnsupportedOperationException.class, log::getResultLayers);
            assertThrows(UnsupportedOperationException.class, () -> log.setSource("x"));
            assertThrows(UnsupportedOperationException.class, log::getBigDecimalResult);
        }

        @Test
        void logResultsCanBeFormatted() {
            LogResults log = new LogResults("logName");
            log.setResult(1.0);
            String formatted = log.explain(VarFormatter.JSON);
            assertNotNull(formatted);
            assertNotEquals("", formatted);
        }

        @Test
        void defaultResultInterfaceMethodsThrowForUnsupportedResultTypes() {
            CalculationNumberResults number = new CalculationNumberResults(0, 1.0, "s");
            assertThrows(UnsupportedOperationException.class, () -> number.explain(VarFormatter.JSON));
            CalculationBooleanResults bool = new CalculationBooleanResults(true, "s", 0, 0, 0);
            assertThrows(UnsupportedOperationException.class, bool::getBigDecimalResult);
        }

        @Test
        void exceptionClassesCarryMessageAndCause() {
            AbnormalOperation a1 = new AbnormalOperation();
            AbnormalOperation a2 = new AbnormalOperation("m");
            AbnormalOperation a3 = new AbnormalOperation("m", new IllegalStateException("c"));
            AbnormalOperation a4 = new AbnormalOperation(new IllegalStateException("c"));
            assertNull(a1.getMessage());
            assertEquals("m", a2.getMessage());
            assertEquals("m", a3.getMessage());
            assertEquals("c", a3.getCause().getMessage());
            assertEquals("c", a4.getCause().getMessage());

            ExtractException e1 = new ExtractException();
            ExtractException e2 = new ExtractException("m");
            ExtractException e3 = new ExtractException("m", new IllegalStateException("c"));
            ExtractException e4 = new ExtractException(new IllegalStateException("c"));
            assertNull(e1.getMessage());
            assertEquals("m", e2.getMessage());
            assertEquals("m", e3.getMessage());
            assertEquals("c", e4.getCause().getMessage());

            WrongFormat w1 = new WrongFormat();
            WrongFormat w2 = new WrongFormat("m");
            WrongFormat w3 = new WrongFormat("m", new IllegalStateException("c"));
            WrongFormat w4 = new WrongFormat(new IllegalStateException("c"));
            assertNull(w1.getMessage());
            assertEquals("m", w2.getMessage());
            assertEquals("m", w3.getMessage());
            assertEquals("c", w4.getCause().getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: format validation of number calculations
    // ------------------------------------------------------------------
    @Nested
    class NumberCalculationCheckTests {

        private final BracketsCalculation2 calculator = BracketsCalculation2.getInstance("numberCheckInstance");

        @Test
        void checkRejectsEmptyExpression() {
            WrongFormat e = assertThrows(WrongFormat.class, () -> calculator.check(""));
            assertTrue(e.getMessage().contains("空"));
        }

        @Test
        void checkAcceptsValidExpressions() {
            assertDoesNotThrow(() -> calculator.check("1+2"));
            assertDoesNotThrow(() -> calculator.check("(1+2)"));
            assertDoesNotThrow(() -> calculator.check("1-2"));
            assertDoesNotThrow(() -> calculator.check("1--2"));
            assertDoesNotThrow(() -> calculator.check("1+-2"));
            assertDoesNotThrow(() -> calculator.check("1..2"));
            assertDoesNotThrow(() -> calculator.check("3!"));
            assertDoesNotThrow(() -> calculator.check("!"));
            assertDoesNotThrow(() -> calculator.check("1 + 2"));
            assertDoesNotThrow(() -> calculator.check("1+2 "));
            assertDoesNotThrow(() -> calculator.check("1+2  "));
            assertDoesNotThrow(() -> calculator.check("1.5*2^3/4%2"));
            assertDoesNotThrow(() -> calculator.check("2(3)"));
        }

        @Test
        void checkRejectsTrailingOperator() {
            WrongFormat e = assertThrows(WrongFormat.class, () -> calculator.check("1+"));
            assertTrue(e.getMessage().contains("最后一个字符"));
        }

        @Test
        void checkRejectsUnknownSymbolsAndDoubleOperators() {
            assertThrows(WrongFormat.class, () -> calculator.check("1a2"));
            assertThrows(WrongFormat.class, () -> calculator.check("1**2"));
        }

        @Test
        void checkRejectsUnbalancedParentheses() {
            assertThrows(WrongFormat.class, () -> calculator.check("(1+2"));
            assertThrows(WrongFormat.class, () -> calculator.check("1+2)"));
            assertThrows(WrongFormat.class, () -> calculator.check(")"));
        }

        @Test
        void checkReportsExactBracketCountDifference() {
            WrongFormat e = assertThrows(WrongFormat.class, () -> calculator.check("(1+2))"));
            assertTrue(e.getMessage().contains("[1]"));
        }

        @Test
        void checkHandlesMidExpressionClosingBracketAndPlusAfterOperator() {
            assertDoesNotThrow(() -> calculator.check("(1+2)*2"));
            assertDoesNotThrow(() -> calculator.check("1++2"));
        }

        @Test
        void prefixExpressionOperationCheckRejectsBracketsAndBadEndings() {
            PrefixExpressionOperation op = PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);
            assertDoesNotThrow(() -> op.check("1+2"));
            assertDoesNotThrow(() -> op.check("1--2"));
            assertDoesNotThrow(() -> op.check("1 2"));
            assertThrows(WrongFormat.class, () -> op.check("1+"));
            assertThrows(WrongFormat.class, () -> op.check("(1+2)"));
            assertThrows(WrongFormat.class, () -> op.check("(1+2"));
            assertThrows(WrongFormat.class, () -> op.check("1)+2"));
            assertThrows(WrongFormat.class, () -> op.check("1a2"));
            assertThrows(WrongFormat.class, () -> op.check("1**2"));
            assertThrows(WrongFormat.class, () -> op.check("1!2"));
        }

        @Test
        void prefixOperationCheckHandlesDecimalsAndTrailingSpaces() {
            PrefixExpressionOperation op = PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);
            assertDoesNotThrow(() -> op.check("1.5+2"));
            assertDoesNotThrow(() -> op.check("1+2 "));
            assertDoesNotThrow(() -> op.check("1+2  "));
        }

        @Test
        void cumulativeCheckValidatesIntervalAndDelegatesInnerFormula() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeCheckInstance");
            assertDoesNotThrow(() -> calc.check("n[0,10,1](1+n)"));
            assertDoesNotThrow(() -> calc.check("x[0,10,1](1+x)"));
            assertThrows(WrongFormat.class, () -> calc.check("n[0,10](1+n)"));
            assertThrows(WrongFormat.class, () -> calc.check("n(1+n)"));
            assertThrows(WrongFormat.class, () -> calc.check("n[0,10,1]"));
            assertThrows(WrongFormat.class, () -> calc.check("n[0,2,1][0,2,1](1+n)"));
            assertThrows(WrongFormat.class, () -> calc.check("n[0,10,1,2](1+n)"));
        }

        @Test
        void booleanCalculationCheckAcceptsComparisonsAndRejectsNonBooleans() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanCheckInstance");
            for (String ok : new String[]{"1>2", "1<2", "1>=2", "1<=2", "1==2", "1!=2", "1<>2", "1=2", "1+2>3+4"}) {
                assertDoesNotThrow(() -> calc.check(ok), ok);
            }
            assertDoesNotThrow(() -> calc.check("null>1"));
            assertDoesNotThrow(() -> calc.check("1>null"));
            assertThrows(WrongFormat.class, () -> calc.check("1+2"));
            assertThrows(WrongFormat.class, () -> calc.check(""));
            assertThrows(WrongFormat.class, () -> calc.check("1>2>3"));
            assertThrows(WrongFormat.class, () -> calc.check(">1"));
            assertThrows(WrongFormat.class, () -> calc.check("1>"));
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: PrefixExpression compilation and evaluation
    // ------------------------------------------------------------------
    @Nested
    class PrefixExpressionTests {

        @Test
        void isBackIsOptAppendsNumberCharacters() {
            StringBuilder sb = new StringBuilder();
            PrefixExpression.isBackIsOpt(sb, '1');
            PrefixExpression.isBackIsOpt(sb, '2');
            PrefixExpression.isBackIsOpt(sb, '.');
            PrefixExpression.isBackIsOpt(sb, '5');
            PrefixExpression.isBackIsOpt(sb, '-');
            PrefixExpression.isBackIsOpt(sb, '!');
            PrefixExpression.isBackIsOpt(sb, 'a');
            assertEquals("12.5-!", sb.toString());
        }

        @Test
        void compileEvaluatesWithEnginePrecedenceRules() {
            assertEquals(3.0, PrefixExpression.compile("1+2", "t").calculationCache(false).getResult());
            assertEquals(9.0, PrefixExpression.compile("1+2*3", "t").calculationCache(false).getResult());
            assertEquals(5.0, PrefixExpression.compile("1*2+3", "t").calculationCache(false).getResult());
            assertEquals(-4.0, PrefixExpression.compile("1-2-3", "t").calculationCache(false).getResult());
            assertEquals(9.0, PrefixExpression.compile("2^3+1", "t").calculationCache(false).getResult());
            assertEquals(27.0, PrefixExpression.compile("1+2^3", "t").calculationCache(false).getResult());
        }

        @Test
        void compileStoresExpressionAndCalculationName() {
            PrefixExpression p = PrefixExpression.compile("1+2", "myName");
            assertEquals("myName", p.getCalculationName());
            assertEquals("1+2 -> 1.0+0", p.getExpressionStr());
        }

        @Test
        void compileBigDecimalEvaluatesWithBigDecimalStacks() {
            PrefixExpression p = PrefixExpression.compileBigDecimal("1+2", "t");
            assertTrue(p.isBigDecimal());
            assertFalse(p.isUnBigDecimal());
            CalculationNumberResults r = p.calculationBigDecimals(false);
            assertEquals(3.0, r.getResult());
            assertEquals(0, new BigDecimal("3").compareTo(r.getBigDecimalResult()));
            assertEquals("1+2 -> 1+0", p.getExpressionStr());
            assertEquals(2, p.getBigDecimalsR(true).size());
            assertEquals(2, p.getBigDecimalsR(false).size());
        }

        @Test
        void calculationRejectsWrongPrecisionMode() {
            PrefixExpression decimal = PrefixExpression.compileBigDecimal("1+2", "t");
            assertThrows(UnsupportedOperationException.class, () -> decimal.calculation(false));
            PrefixExpression plain = PrefixExpression.compile("1+2", "t");
            assertThrows(UnsupportedOperationException.class, () -> plain.calculationBigDecimals(false));
        }

        @Test
        void calculationCacheWithoutCopyDisablesExpression() {
            PrefixExpression p = PrefixExpression.compile("1+2", "t");
            assertTrue(p.isAvailable());
            assertEquals(3.0, p.calculationCache(false).getResult());
            assertFalse(p.isAvailable());
            assertEquals(3.0, p.calculationCache(false).getResult());
            assertThrows(UnsupportedOperationException.class, () -> p.getDoubleStack(false));
            assertThrows(UnsupportedOperationException.class, () -> p.getCharacterStack(false));
            assertThrows(UnsupportedOperationException.class, () -> p.getBigDecimalsR(false));
        }

        @Test
        void calculationCacheWithCopyKeepsExpressionUsable() {
            PrefixExpression p = PrefixExpression.compile("1+2", "t");
            assertEquals(3.0, p.calculationCache(true).getResult());
            assertTrue(p.isAvailable());
            assertEquals(3.0, p.calculationCache(true).getResult());
        }

        @Test
        void stackAccessorsSupportCopyAndRawAccess() {
            PrefixExpression p = PrefixExpression.compile("1+2", "t");
            Stack<Double> copy = p.getDoubleStack(true);
            assertEquals(2, copy.size());
            copy.push(99.0);
            assertEquals(2, p.getDoubleStack(true).size());
            assertEquals(2, p.getDoubleStack(false).size());
            assertEquals(1, p.getCharacterStack(false).size());
            assertEquals(1, p.getCharacterStack(true).size());
            assertNull(p.getBigDecimalsR(false));
        }

        @Test
        void convertToMultiPrecisionFromPlainMode() {
            PrefixExpression p = PrefixExpression.compile("1+2", "t");
            assertFalse(p.isBigDecimal());
            assertTrue(p.isUnBigDecimal());
            p.convertToMultiPrecisionSupported();
            assertTrue(p.isBigDecimal());
            assertTrue(p.isUnBigDecimal());
            assertEquals(3.0, p.calculation(true).getResult());
            assertEquals(3.0, p.calculationBigDecimals(true).getResult());
        }

        @Test
        void convertToMultiPrecisionFromBigDecimalMode() {
            PrefixExpression p = PrefixExpression.compileBigDecimal("1+2", "t");
            assertTrue(p.isBigDecimal());
            assertFalse(p.isUnBigDecimal());
            p.convertToMultiPrecisionSupported();
            assertTrue(p.isUnBigDecimal());
            assertEquals(3.0, p.calculation(true).getResult());
            assertEquals(3.0, p.calculationBigDecimals(true).getResult());
        }

        @Test
        void convertToMultiPrecisionIsIdempotentWhenBothModesSupported() {
            PrefixExpression p = PrefixExpression.compile("1+2", "t");
            p.convertToMultiPrecisionSupported();
            assertDoesNotThrow(p::convertToMultiPrecisionSupported);
            PrefixExpression q = PrefixExpression.compileBigDecimal("1+2", "t");
            q.convertToMultiPrecisionSupported();
            assertDoesNotThrow(q::convertToMultiPrecisionSupported);
        }

        @Test
        void stackAccessorsHonorCopyFlag() {
            PrefixExpression big = PrefixExpression.compileBigDecimal("1+2", "stackCopyName");
            Stack<BigDecimal> copy = big.getBigDecimalsR(true);
            assertNotSame(big.getBigDecimalsR(false), copy);
            PrefixExpression plain = PrefixExpression.compile("1+2", "stackCopyName2");
            Stack<Character> charsCopy = plain.getCharacterStack(true);
            assertNotSame(plain.getCharacterStack(false), charsCopy);
        }

        // ------------------------------------------------------------------
        // 白盒测试（反射）: 仅用于覆盖公开 API 不可达的私有分支，断言真实行为
        // ------------------------------------------------------------------


        @Test
        void convertToMultiPrecisionAfterCloseStillShortCircuits() {
            PrefixExpression p = PrefixExpression.compile("1+2", "closeConvertName");
            p.convertToMultiPrecisionSupported();
            assertTrue(p.isBigDecimal());
            assertEquals(3.0, (Double) p.calculationCache(false).getResult());
            assertFalse(p.isAvailable());
            assertDoesNotThrow(p::convertToMultiPrecisionSupported);
        }

        @Test
        void compileUsesCacheWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            PrefixExpression first = PrefixExpression.compile("1+2", "cachedName");
            PrefixExpression second = PrefixExpression.compile("1+2", "cachedName");
            assertSame(first, second);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("1+2"));
        }

        @Test
        void compileBigDecimalUsesCacheWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            PrefixExpression first = PrefixExpression.compileBigDecimal("1+2", "cachedName");
            PrefixExpression second = PrefixExpression.compileBigDecimal("1+2", "cachedName");
            assertSame(first, second);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("1+2uB"));
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: PrefixExpressionOperation component
    // ------------------------------------------------------------------
    @Nested
    class PrefixExpressionOperationTests {

        private final PrefixExpressionOperation op =
                PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);

        @Test
        void formatStrReplacesAmbiguousPlusMinusAndAppendsZero() {
            assertEquals("1+2+0", op.formatStr("1+2"));
            assertEquals("1-2+0", op.formatStr("1+-2"));
            assertEquals("1-2-0+0", op.formatStr("1+-2-0"));
        }

        @Test
        void compileAppliesFormattingOnlyWhenRequired() {
            assertEquals("1+2+0 -> 3.0+0", op.compile("1+2", true).getExpressionStr());
            assertEquals("1+2 -> 1.0+0", op.compile("1+2", false).getExpressionStr());
            assertEquals("1+2+0 -> 3+0", op.compileBigDecimal("1+2", true).getExpressionStr());
            assertEquals("1+2 -> 1+0", op.compileBigDecimal("1+2", false).getExpressionStr());
        }

        @Test
        void calculationUsesBigDecimalPathByDefaultAndDoublePathWhenEnabled() {
            assertEquals(3.0, op.calculation("1+2", true).getResult());
            assertEquals(3.0, op.calculation("1+2", false).getResult());
            assertEquals(0, new BigDecimal("3").compareTo(op.calculation("1+2", true).getBigDecimalResult()));
            Mathematical_Expression.Options.setUseBigDecimal(true);
            assertEquals(3.0, op.calculation("1+2", true).getResult());
        }

        @Test
        void explainRecordsResultForSimpleExpression() {
            LogResults log = op.explain("1+2", true);
            assertEquals(3.0, log.getResult());
            assertTrue(log.getCalculationSourceName().startsWith("f_"));
            assertThrows(UnsupportedOperationException.class, log::getResultLayers);
            assertThrows(UnsupportedOperationException.class, () -> log.setSource("x"));
        }

        @Test
        void explainHandlesLowerPriorityOperators() {
            assertEquals(7.0, PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME)
                    .explain("1+2*3", true).getResult());
        }

        @Test
        void explainHandlesHigherPriorityOperators() {
            LogResults log = PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME)
                    .explain("1*2+3", true);
            assertEquals(5.0, log.getResult());
        }

        @Test
        void getInstanceRegistersCachesAndNeverReturnsNull() {
            PrefixExpressionOperation fresh = PrefixExpressionOperation.getInstance("peoFreshNullInstance");
            assertNotNull(fresh);
            PrefixExpressionOperation cached = PrefixExpressionOperation.getInstance("peoFreshNullInstance");
            assertNotNull(cached);
            assertSame(fresh, cached);
        }

        @Test
        void checkReportsBracketMessagesForParentheses() {
            WrongFormat left = assertThrows(WrongFormat.class, () -> op.check("(1+2)+3"));
            assertTrue(left.getMessage().contains("括号"));
            WrongFormat right = assertThrows(WrongFormat.class, () -> op.check("1)+2"));
            assertTrue(right.getMessage().contains("括号"));
        }

        @Test
        void calculationCachesUnderBigDecimalKeyByDefault() {
            Mathematical_Expression.Options.setUseCache(true);
            Mathematical_Expression.Options.setUseBigDecimal(false);
            op.calculation("9.5+0.5", false);
            assertNull(Mathematical_Expression.Options.getCacheCalculation("9.5+0.5"));
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("9.5+0.5uB"));
        }

        @Test
        void explainBuildsObservableLogTreeStructure() {
            LogResults log = op.explain("1+2", true);
            String name = log.getCalculationSourceName();
            assertNotEquals("", log.getPrefix());
            assertFalse(log.isNameJoin());
            List<DataObj> nodes = new ArrayList<>();
            collectTree(log, nodes);
            assertTrue(nodes.stream().anyMatch(d -> d.getName().equals(name + "_优先")));
            assertTrue(nodes.stream().anyMatch(d -> d.getName().equals(name + "_最终")));
            assertTrue(nodes.stream().anyMatch(d -> d.containsKey("result")
                    && new BigDecimal(d.get("result").toString()).compareTo(new BigDecimal("3")) == 0));
            assertTrue(nodes.stream().anyMatch(d -> d.containsKey("result") && !d.getPrefix().isEmpty()));

            LogResults prec = op.explain("1*2+3", true);
            List<DataObj> pnodes = new ArrayList<>();
            collectTree(prec, pnodes);
            assertTrue(pnodes.stream().anyMatch(d -> d.getName()
                    .equals("f_" + ("1.0 * 2.0").hashCode() + "_计算") && !d.getPrefix().isEmpty()));
        }

        private static void collectTree(DataObj node, List<DataObj> out) {
            out.add(node);
            for (Object value : node.values()) {
                if (value instanceof DataObj) {
                    collectTree((DataObj) value, out);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: bracket calculation components
    // ------------------------------------------------------------------
    @Nested
    class BracketsCalculationTests {

        @Test
        void formatStrRemovesWhitespace() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsFormatInstance");
            assertEquals("1+2", calculator.formatStr(" 1 + 2 "));
            assertEquals("1+2", calculator.formatStr("1+2"));
        }

        @Test
        void deprecatedBinaryCalculationComputesResultOrFailsWithoutOperator() {
            BracketsCalculation calculator = BracketsCalculation2.getInstance("bracketsDeprecatedInstance");
            assertEquals(3.0, calculator.calculation2("1+2"));
            assertEquals(-3.0, calculator.calculation2("5-2"));
            assertThrows(AbnormalOperation.class, () -> calculator.calculation2("12"));
        }

        @Test
        void getInstanceReturnsCachedInstance() {
            BracketsCalculation2 first = BracketsCalculation2.getInstance("bracketsSameInstance");
            BracketsCalculation2 second = BracketsCalculation2.getInstance("bracketsSameInstance");
            assertSame(first, second);
        }

        @Test
        void calculationHandlesNestedParentheses() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsCalcInstance");
            assertEquals(3.0, calculator.calculation("1+2").getResult());
            assertEquals(9.0, calculator.calculation("(1+2)*3").getResult());
            assertEquals(21.0, calculator.calculation("(1+2)*(3+4)").getResult());
            assertEquals(10.0, calculator.calculation("((1+2)*2)+4").getResult());
            assertEquals(3.0, calculator.calculation("((1+2))").getResult());
            assertEquals(6.0, calculator.calculation("(1+(2+3))").getResult());
        }

        @Test
        void calculationBigDecimalPathComputesHighPrecisionResults() {
            Mathematical_Expression.Options.setUseBigDecimal(true);
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsBigInstance");
            CalculationNumberResults r = calculator.calculation("(1+2)*3");
            assertEquals(9.0, r.getResult());
            assertEquals(0, new BigDecimal("9").compareTo(r.getBigDecimalResult()));
        }

        @Test
        void compileWrapsInnerExpressionWithPackName() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsCompileInstance");
            PackExpression pack = calculator.compile("1+2", true);
            assertEquals("1+2 -> 1+2+0 -> 3.0+0", pack.getExpressionStr());
            assertTrue(pack.getCalculationName().endsWith(":{PrefixExpressionOperation}"));
            NameExpression inner = pack.dismantling();
            assertEquals("PrefixExpressionOperation", inner.getCalculationName());
        }

        @Test
        void packCalculationSetsSourceAndCachesResults() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsPackInstance");
            PackExpression pack = calculator.compile("1+2", true);
            CalculationResults first = pack.calculationCache(false);
            assertEquals(3.0, (Double) first.getResult());
            assertEquals(pack.getCalculationName(), first.getCalculationSourceName());
            CalculationResults second = pack.calculationCache(false);
            assertSame(first, second);
            assertEquals(3.0, (Double) pack.calculation(true).getResult());
        }

        @Test
        void compileBigDecimalProducesBigDecimalCapablePack() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsBigCompileInstance");
            PackExpression pack = calculator.compileBigDecimal("1+2", true);
            assertTrue(pack.isBigDecimal());
            CalculationResults r = pack.calculationBigDecimalsCache(false);
            assertEquals(3.0, (Double) r.getResult());
            assertEquals(0, new BigDecimal("3").compareTo(r.getBigDecimalResult()));
            assertEquals(pack.getCalculationName(), r.getCalculationSourceName());
            assertFalse(pack.isAvailable());
            CalculationResults cachedAgain = pack.calculationBigDecimalsCache(false);
            assertSame(r, cachedAgain);
            assertTrue(pack.calculationBigDecimalsCache(true) != null);
        }

        @Test
        void bracketsExpressionCompileUsesCacheWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsExprCacheInstance");
            PackExpression first = BracketsExpression.compile("1+2", true, calculator);
            PackExpression second = BracketsExpression.compile("1+2", true, calculator);
            assertSame(first, second);
        }

        @Test
        void bracketsExpressionCompileBigDecimalUsesCacheWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsExprBigCacheInstance");
            PackExpression first = BracketsExpression.compileBigDecimal("1+2", true, calculator);
            PackExpression second = BracketsExpression.compileBigDecimal("1+2", true, calculator);
            assertSame(first, second);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("1+2uB"));
        }

        @Test
        void explainBuildsNestedLogTree() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsExplainInstance");
            LogResults simple = calculator.explain("1+2", true);
            assertEquals(3.0, simple.getResult());
            assertTrue(simple.getCalculationSourceName().startsWith("f_"));
            LogResults nested = calculator.explain("(1+2)*3", true);
            assertEquals(9.0, nested.getResult());
            LogResults deep = calculator.explain("(1+2)*(3+4)", true);
            assertEquals(21.0, deep.getResult());
        }

        @Test
        void explainWithoutFormattingSkipsSpacesInRawFormula() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsRawSpacesInstance");
            assertEquals(3.0, calculator.explain("1 + 2", false).getResult());
        }

        @Test
        void explainNestedFormulaBuildsObservableTree() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsNestedTreeInstance");
            LogResults nested = calculator.explain("(1+2)*3", true);
            assertNotEquals("", nested.getPrefix());
            assertTrue(nested.containsKey("f_" + "1+2".hashCode()));
            assertTrue(nested.containsKey("f_" + "3.0*3".hashCode()));
        }

        @Test
        void calculationCachesUnderDoubleKeyByDefault() {
            Mathematical_Expression.Options.setUseCache(true);
            Mathematical_Expression.Options.setUseBigDecimal(false);
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsCacheKeyInstance");
            calculator.calculation("(1+2)*3", true);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("(1+2)*3"));
            assertNull(Mathematical_Expression.Options.getCacheCalculation("(1+2)*3uB"));
        }

        @Test
        void bracketsExpressionCompileHandlesNestedAndRawFormulas() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsExprNestedInstance");
            PackExpression nested = BracketsExpression.compile("((1+2))", true, calculator);
            assertEquals(3.0, (Double) nested.calculation(true).getResult());
            PackExpression raw = BracketsExpression.compile("1 + 2", false, calculator);
            assertEquals(3.0, (Double) raw.calculation(true).getResult());
        }

        @Test
        void packExpressionReflectsInnerPrecisionMode() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsPackModeInstance");
            PackExpression pack = calculator.compile("1+2", true);
            assertFalse(pack.isBigDecimal());
            assertTrue(pack.isUnBigDecimal());
            PackExpression big = calculator.compileBigDecimal("1+2", true);
            assertTrue(big.isBigDecimal());
            assertFalse(big.isUnBigDecimal());
            pack.convertToMultiPrecisionSupported();
            assertTrue(pack.isBigDecimal());
            assertTrue(pack.isUnBigDecimal());
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: boolean comparison component
    // ------------------------------------------------------------------
    @Nested
    class BooleanCalculationTests {

        @Test
        void formatAndNameAccessors() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanAccessorInstance");
            assertEquals("1>2", calc.formatStr(" 1 > 2 "));
            assertEquals("booleanAccessorInstance", calc.getName());
        }

        @Test
        void calculationEvaluatesEveryComparisonOperator() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanCalcInstance");
            assertFalse(calc.calculation("1>2").getResult());
            assertTrue(calc.calculation("2>1").getResult());
            assertTrue(calc.calculation("1<2").getResult());
            assertFalse(calc.calculation("2<1").getResult());
            assertTrue(calc.calculation("1<=1").getResult());
            assertTrue(calc.calculation("1>=1").getResult());
            assertTrue(calc.calculation("1==1").getResult());
            assertFalse(calc.calculation("1==2").getResult());
            assertTrue(calc.calculation("1!=2").getResult());
            assertTrue(calc.calculation("1<>2").getResult());
            assertFalse(calc.calculation("1=2").getResult());
        }

        @Test
        void calculationStoresLeftRightAndLayerCount() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanDataInstance");
            CalculationBooleanResults r = calc.calculation("1 > 2");
            assertEquals(1.0, r.getLeft());
            assertEquals(2.0, r.getRight());
            assertEquals(2, r.getResultLayers());
            assertFalse(r.getResult());
            CalculationBooleanResults nested = calc.calculation("1+1>1");
            assertEquals(2, nested.getResultLayers());
            assertTrue(nested.getResult());
        }

        @Test
        void calculationWithoutFormattingKeepsRawFormula() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanRawInstance");
            assertFalse(calc.calculation("1>2", false).getResult());
            assertTrue(calc.calculation("2>1", false).getResult());
        }

        @Test
        void nullOperandBranchesFailBecauseOperatorIsLost() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanNullInstance");
            assertThrows(AbnormalOperation.class, () -> calc.calculation("null>5"));
            assertThrows(AbnormalOperation.class, () -> calc.calculation("null==null"));
            assertThrows(AbnormalOperation.class, () -> calc.calculation("5>null"));
        }



        @Test
        void defaultExplainIsUnsupported() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanExplainInstance");
            assertThrows(UnsupportedOperationException.class, () -> calc.explain("1>2", true));
        }

        @Test
        void getInstanceRejectsNonBooleanComponent() {
            PrefixExpressionOperation.getInstance("booleanWrongTypeInstance");
            assertThrows(ExtractException.class, () -> BooleanCalculation2.getInstance("booleanWrongTypeInstance"));
        }

        @Test
        void getInstanceRegistersCachesAndNeverReturnsNull() {
            BooleanCalculation2 first = BooleanCalculation2.getInstance("booleanRepeatInstance");
            BooleanCalculation2 second = BooleanCalculation2.getInstance("booleanRepeatInstance");
            assertNotNull(first);
            assertNotNull(second);
            assertSame(first, second);

            BooleanCalculation2 fresh = BooleanCalculation2.getInstance("booleanFreshNullInstance");
            assertNotNull(fresh);
            BooleanCalculation2 cached = BooleanCalculation2.getInstance("booleanFreshNullInstance");
            assertNotNull(cached);
            assertSame(fresh, cached);
        }

        @Test
        void legacyBooleanCheckValidatesSidesAndReportsOperatorCount() {
            BooleanCalculation legacy = BooleanCalculation2.getInstance("booleanLegacyCheckInstance");
            assertThrows(WrongFormat.class, () -> legacy.check("1+>3"));
            assertThrows(WrongFormat.class, () -> legacy.check("1+2>3+"));
            WrongFormat e = assertThrows(WrongFormat.class, () -> legacy.check("1+2"));
            assertTrue(e.getMessage().contains("[0]"));
        }



        @Test
        void calculationFormatsSpacedComparisons() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanSpacedInstance");
            assertFalse(calc.calculation(" 1 > 2 ").getResult());
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: cumulative (sigma) calculation component
    // ------------------------------------------------------------------
    @Nested
    class CumulativeCalculationTests {

        @Test
        void formatStrExpandsIntervalIntoSumOfTerms() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeFormatInstance");
            assertEquals("((0.0*0.0))+((1.0*1.0))+((2.0*2.0))", calc.formatStr("n[0,2,1](n*n)"));
            assertEquals("((3.0+1))", calc.formatStr("n[3,3,1](n+1)"));
            assertEquals("((1+0.0))", calc.formatStr("n[0,0,1](1+n)"));
        }

        @Test
        void formatStrFailsForDescendingIntervalWithoutIterations() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeEmptyInstance");
            assertThrows(StringIndexOutOfBoundsException.class, () -> calc.formatStr("n[0,-1,1](n)"));
        }

        @Test
        void calculationSumsTermsOfInterval() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeCalcInstance");
            assertEquals(5.0, calc.calculation("n[0,2,1](n*n)").getResult());
            assertEquals(4.0, calc.calculation("n[3,3,1](n+1)").getResult());
        }

        @Test
        void calculationWithoutFormattingUsesRawFormula() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeRawInstance");
            assertEquals(5.0, calc.calculation("(0.0*0.0)+(1.0*1.0)+(2.0*2.0)", false).getResult());
        }

        @Test
        void explainReturnsSumOfInterval() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeExplainInstance");
            assertEquals(5.0, calc.explain("n[0,2,1](n*n)", true).getResult());
        }

        @Test
        void compileAndCompileBigDecimalEvaluateIntervalSums() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeCompileInstance");
            assertEquals(5.0, (Double) calc.compile("n[0,2,1](n*n)", true).calculationCache(false).getResult());
            assertEquals(5.0, (Double) calc.compile("((0.0*0.0))+((1.0*1.0))+((2.0*2.0))", false)
                    .calculationCache(false).getResult());
            CalculationResults r = calc.compileBigDecimal("n[0,2,1](n*n)", true).calculationBigDecimalsCache(false);
            assertEquals(5.0, (Double) r.getResult());
            assertEquals(0, new BigDecimal("5").compareTo(r.getBigDecimalResult()));
            assertEquals(5.0, (Double) calc.compileBigDecimal("((0.0*0.0))+((1.0*1.0))+((2.0*2.0))", false)
                    .calculationBigDecimalsCache(false).getResult());
            assertEquals(5.0, calc.explain("((0.0*0.0))+((1.0*1.0))+((2.0*2.0))", false).getResult());
        }

        @Test
        void getInstanceRejectsNonCumulativeComponent() {
            PrefixExpressionOperation.getInstance("cumulativeWrongTypeInstance");
            assertThrows(ExtractException.class, () -> CumulativeCalculation.getInstance("cumulativeWrongTypeInstance"));
        }

        @Test
        void getInstanceRegistersCachesAndNeverReturnsNull() {
            CumulativeCalculation first = CumulativeCalculation.getInstance("cumulativeRepeatInstance");
            CumulativeCalculation second = CumulativeCalculation.getInstance("cumulativeRepeatInstance");
            assertNotNull(first);
            assertNotNull(second);
            assertSame(first, second);

            CumulativeCalculation fresh = CumulativeCalculation.getInstance("cumulativeFreshNullInstance");
            assertNotNull(fresh);
            CumulativeCalculation cached = CumulativeCalculation.getInstance("cumulativeFreshNullInstance");
            assertNotNull(cached);
            assertSame(fresh, cached);
        }

        @Test
        void checkValidatesInnerFormula() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeInnerCheckInstance");
            assertThrows(WrongFormat.class, () -> calc.check("n[0,2,1](1a2)"));
        }

        @Test
        void formatStrRejectsAbsurdIntervalsBeforeIterating() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeOverflowInstance");
            // The capacity term format.length() * |end - start| overflows int arithmetic;
            // the descending endpoints would otherwise start iterating from 1e300.
            assertThrows(NegativeArraySizeException.class, () -> calc.formatStr("n[1e300,-1e300,1](n)"));
        }
    }

    // ------------------------------------------------------------------
    // Grading domain: records and enums
    // ------------------------------------------------------------------
    @Nested
    class GradingDomainTests {

        @Test
        void answerResultExposesAllComponents() {
            AnswerResult r = new AnswerResult("q1", QuestionType.MULTIPLE_CHOICE, "S2",
                    new BigDecimal("8.5"), true, false, false, 4, 2, 1,
                    new BigDecimal("0.25"), new BigDecimal("0.5"), 60, 3, 5,
                    true, true, true, true);
            assertEquals("q1", r.questionId());
            assertEquals(QuestionType.MULTIPLE_CHOICE, r.type());
            assertEquals("S2", r.section());
            assertBigDecimalEquals("8.5", r.maximumPoints());
            assertTrue(r.mandatory());
            assertFalse(r.answered());
            assertFalse(r.exactMatch());
            assertEquals(4, r.correctOptionCount());
            assertEquals(2, r.selectedCorrectOptions());
            assertEquals(1, r.selectedIncorrectOptions());
            assertBigDecimalEquals("0.25", r.numericError());
            assertBigDecimalEquals("0.5", r.numericTolerance());
            assertEquals(60, r.rubricPercent());
            assertEquals(3, r.passedTests());
            assertEquals(5, r.totalTests());
            assertTrue(r.compilationSucceeded());
            assertTrue(r.plagiarismFlag());
            assertTrue(r.timedOut());
            assertTrue(r.manualReviewRequired());
        }

        @Test
        void questionTypeEnumeratesSevenKinds() {
            assertEquals(7, QuestionType.values().length);
            assertArrayEquals(new QuestionType[]{QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE,
                    QuestionType.TRUE_FALSE, QuestionType.FILL_BLANK, QuestionType.NUMERIC,
                    QuestionType.ESSAY, QuestionType.PROGRAMMING}, QuestionType.values());
        }

        @Test
        void examSubmissionDefendsAgainstNullCollections() {
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            ExamSubmission s = new ExamSubmission("e", "s", null, start, start.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, BigDecimal.ZERO, null, null);
            assertEquals(List.of(), s.answers());
            assertEquals(Map.of(), s.sectionPassPercentages());
            assertEquals("e", s.examId());
            assertEquals("s", s.studentId());
        }

        @Test
        void examSubmissionFreezesCopiedCollections() {
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            AnswerResult answer = new AnswerResult("q1", QuestionType.NUMERIC, "S", BigDecimal.TEN,
                    false, true, true, 0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            List<AnswerResult> src = new ArrayList<>(List.of(answer));
            Map<String, Integer> passMap = new HashMap<>();
            passMap.put("S", 50);
            ExamSubmission s = new ExamSubmission("e", "s", src, start, start.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, BigDecimal.ZERO, null, passMap);
            src.clear();
            passMap.clear();
            assertEquals(1, s.answers().size());
            assertEquals(50, s.sectionPassPercentages().get("S"));
            assertThrows(UnsupportedOperationException.class, () -> s.answers().add(answer));
            assertThrows(UnsupportedOperationException.class, () -> s.sectionPassPercentages().put("X", 1));
        }

        @Test
        void gradeReportDefendsAgainstNullCollectionsAndFreezesCopies() {
            GradeReport empty = new GradeReport(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 0,
                    "F", false, false, null, null, null);
            assertEquals(Map.of(), empty.sectionPercentages());
            assertEquals(List.of(), empty.failures());
            assertEquals(List.of(), empty.warnings());

            GradeReport r = new GradeReport(new BigDecimal("5.00"), new BigDecimal("5.00"), BigDecimal.TEN, 50,
                    "F", false, true, Map.of("S", 50), List.of("f1"), List.of("w1"));
            assertBigDecimalEquals("5.00", r.rawPoints());
            assertBigDecimalEquals("5.00", r.finalPoints());
            assertBigDecimalEquals("10", r.maximumPoints());
            assertEquals(50, r.percentage());
            assertEquals("F", r.letterGrade());
            assertFalse(r.passed());
            assertTrue(r.manualReview());
            assertEquals(50, r.sectionPercentages().get("S"));
            assertEquals(List.of("f1"), r.failures());
            assertEquals(List.of("w1"), r.warnings());
            assertThrows(UnsupportedOperationException.class, () -> r.failures().add("x"));
        }

        @Test
        void gradingPolicyExposesAllComponents() {
            GradingPolicy p = new GradingPolicy(60, true, true, new BigDecimal("0.5"), new BigDecimal("0.25"),
                    10, 50, 40, true, true, 3, true, true, true);
            assertEquals(60, p.passPercentage());
            assertTrue(p.partialCreditEnabled());
            assertTrue(p.negativeMarkingEnabled());
            assertBigDecimalEquals("0.5", p.wrongAnswerPenaltyRate());
            assertBigDecimalEquals("0.25", p.unansweredPenaltyRate());
            assertEquals(10, p.latePenaltyPercentPerHour());
            assertEquals(50, p.maximumLatePenaltyPercent());
            assertEquals(40, p.minimumSectionPercentage());
            assertTrue(p.allMandatoryQuestionsRequired());
            assertTrue(p.identityVerificationRequired());
            assertEquals(3, p.maximumAttempts());
            assertTrue(p.failOnIntegrityViolation());
            assertTrue(p.capScoreAtOneHundred());
            assertTrue(p.programmingCompilationRequired());
        }
    }

    // ------------------------------------------------------------------
    // Grading expression: question, submission, evaluation records
    // ------------------------------------------------------------------
    @Nested
    class ExpressionGradingTests {

        private static ExpressionQuestion question(String id, String correct, String maxPoints,
                                                   String tolerance, boolean mandatory) {
            return new ExpressionQuestion(id, "math", correct, new BigDecimal(maxPoints),
                    new BigDecimal(tolerance), mandatory);
        }

        private static AnswerResult numericAnswer(String id, boolean answered, boolean exact,
                                                   BigDecimal error, BigDecimal tolerance) {
            return new AnswerResult(id, QuestionType.NUMERIC, "math", BigDecimal.TEN, false, answered, exact,
                    0, 0, 0, error, tolerance, 0, 0, 0, false, false, false, false);
        }

        @Test
        void expressionQuestionValidatesEveryField() {
            ExpressionQuestion q = question("q1", "2+2", "10", "0.1", true);
            assertEquals("q1", q.questionId());
            assertEquals("math", q.section());
            assertEquals("2+2", q.correctExpression());
            assertBigDecimalEquals("10", q.maximumPoints());
            assertBigDecimalEquals("0.1", q.tolerance());
            assertTrue(q.mandatory());
            assertThrows(IllegalArgumentException.class, () -> question(null, "2+2", "10", "0.1", true));
            assertThrows(IllegalArgumentException.class, () -> question(" ", "2+2", "10", "0.1", true));
            assertThrows(IllegalArgumentException.class, () -> question("q", null, "10", "0.1", true));
            assertThrows(IllegalArgumentException.class, () -> question("q", "", "10", "0.1", true));
            assertThrows(NullPointerException.class, () -> question("q", "2+2", null, "0.1", true));
            assertThrows(NullPointerException.class, () -> question("q", "2+2", "10", null, true));
            assertThrows(IllegalArgumentException.class, () -> question("q", "2+2", "0", "0.1", true));
            assertThrows(IllegalArgumentException.class, () -> question("q", "2+2", "-5", "0.1", true));
            assertThrows(IllegalArgumentException.class, () -> question("q", "2+2", "10", "-0.1", true));
            assertDoesNotThrow(() -> question("q", "2+2", "10", "0", true));
        }

        @Test
        void expressionAnswerSubmissionRequiresQuestion() {
            assertThrows(NullPointerException.class, () -> new ExpressionAnswerSubmission(null, "2+2"));
            ExpressionAnswerSubmission s = new ExpressionAnswerSubmission(question("q1", "2+2", "10", "0", true), "2+2");
            assertEquals("q1", s.question().questionId());
            assertEquals("2+2", s.submittedExpression());
        }

        @Test
        void expressionAnswerEvaluationRequiresAnswerResultAndStatus() {
            AnswerResult answer = numericAnswer("q1", true, true, null, BigDecimal.ZERO);
            assertThrows(NullPointerException.class, () -> new ExpressionAnswerEvaluation(
                    null, ExpressionAnswerStatus.EXACT, null, null, null, "m"));
            assertThrows(NullPointerException.class, () -> new ExpressionAnswerEvaluation(
                    answer, null, null, null, null, "m"));
            ExpressionAnswerEvaluation ev = new ExpressionAnswerEvaluation(answer, ExpressionAnswerStatus.EXACT,
                    new BigDecimal("4"), new BigDecimal("4"), BigDecimal.ZERO, "EXACT");
            assertSame(answer, ev.answerResult());
            assertEquals(ExpressionAnswerStatus.EXACT, ev.status());
            assertBigDecimalEquals("4", ev.expectedValue());
            assertBigDecimalEquals("4", ev.actualValue());
            assertBigDecimalEquals("0", ev.absoluteError());
            assertEquals("EXACT", ev.message());
        }

        @Test
        void expressionAnswerStatusEnumeratesFiveStates() {
            assertEquals(5, ExpressionAnswerStatus.values().length);
        }

        @Test
        void expressionEvaluationExceptionCarriesMessageAndCause() {
            ExpressionEvaluationException e1 = new ExpressionEvaluationException("m");
            assertEquals("m", e1.getMessage());
            assertNull(e1.getCause());
            ExpressionEvaluationException e2 = new ExpressionEvaluationException("m", new IllegalStateException("c"));
            assertEquals("m", e2.getMessage());
            assertEquals("c", e2.getCause().getMessage());
        }

        @Test
        void expressionExamGradeRequiresReportAndFreezesEvaluations() {
            GradeReport report = new GradeReport(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 0, "F",
                    false, false, Map.of(), List.of(), List.of());
            assertThrows(NullPointerException.class, () -> new ExpressionExamGrade(null, List.of()));
            ExpressionExamGrade g = new ExpressionExamGrade(report, null);
            assertSame(report, g.report());
            assertEquals(List.of(), g.answerEvaluations());
        }

        @Test
        void expressionExamSubmissionFreezesCollectionsAndConvertsToExamSubmission() {
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            Instant submit = start.plusSeconds(300);
            Instant deadline = start.plusSeconds(3600);
            ExpressionAnswerSubmission a1 = new ExpressionAnswerSubmission(question("q1", "2+2", "10", "0", true), "2+2");
            ExpressionExamSubmission s = new ExpressionExamSubmission("exam", "stu", List.of(a1),
                    start, submit, deadline, 3600, 2, true, true, false, true, false, 1, 2,
                    new BigDecimal("1.5"), 90, Map.of("math", 60));
            assertEquals("exam", s.examId());
            assertEquals("stu", s.studentId());
            assertEquals(1, s.answers().size());

            ExpressionExamSubmission empty = new ExpressionExamSubmission("exam", "stu", null,
                    start, submit, deadline, 3600, 2, true, true, false, true, false, 1, 2,
                    new BigDecimal("1.5"), 90, null);
            assertEquals(List.of(), empty.answers());
            assertEquals(Map.of(), empty.sectionPassPercentages());

            AnswerResult evaluated = numericAnswer("q1", true, true, null, BigDecimal.ZERO);
            ExamSubmission converted = s.toExamSubmission(List.of(evaluated));
            assertEquals("exam", converted.examId());
            assertEquals("stu", converted.studentId());
            assertEquals(List.of(evaluated), converted.answers());
            assertEquals(start, converted.startedAt());
            assertEquals(submit, converted.submittedAt());
            assertEquals(deadline, converted.deadline());
            assertEquals(3600, converted.allowedDurationSeconds());
            assertEquals(2, converted.attemptNumber());
            assertTrue(converted.identityVerified());
            assertTrue(converted.termsAccepted());
            assertFalse(converted.suspendedAccount());
            assertTrue(converted.proctorDisconnected());
            assertFalse(converted.ipAddressChanged());
            assertEquals(1, converted.fullscreenExitCount());
            assertEquals(2, converted.copyPasteEventCount());
            assertBigDecimalEquals("1.5", converted.bonusPoints());
            assertEquals(90, converted.instructorOverridePercent());
            assertEquals(60, converted.sectionPassPercentages().get("math"));
        }
    }

    // ------------------------------------------------------------------
    // Grading expression: ExpressionAnswerGradingService
    // ------------------------------------------------------------------
    @Nested
    class ExpressionAnswerGradingServiceTests {

        private static ExpressionAnswerGradingService gradingService() {
            return new ExpressionAnswerGradingService(expression -> {
                switch (expression) {
                    case "2+2":
                        return new BigDecimal("4");
                    case "2*3":
                        return new BigDecimal("6");
                    default:
                        throw new ExpressionEvaluationException("unexpected expression: " + expression);
                }
            });
        }

        private static ExpressionQuestion question(String id, String correct, String tolerance, boolean mandatory) {
            return new ExpressionQuestion(id, "math", correct, BigDecimal.TEN, new BigDecimal(tolerance), mandatory);
        }

        @Test
        void constructorRequiresEvaluator() {
            assertThrows(NullPointerException.class, () -> new ExpressionAnswerGradingService(null));
        }

        @Test
        void evaluateRequiresSubmission() {
            assertThrows(NullPointerException.class, () -> gradingService().evaluate(null));
        }

        @Test
        void evaluateBlankSubmissionIsUnanswered() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2+2", "0.1", true);
            for (String blank : new String[]{null, "", "   "}) {
                ExpressionAnswerEvaluation ev = service.evaluate(new ExpressionAnswerSubmission(q, blank));
                assertEquals(ExpressionAnswerStatus.UNANSWERED, ev.status());
                assertEquals("ANSWER_NOT_PROVIDED", ev.message());
                assertFalse(ev.answerResult().answered());
                assertFalse(ev.answerResult().exactMatch());
                assertNull(ev.answerResult().numericError());
                assertNull(ev.expectedValue());
                assertNull(ev.actualValue());
                assertNull(ev.absoluteError());
                assertEquals("q1", ev.answerResult().questionId());
                assertEquals(QuestionType.NUMERIC, ev.answerResult().type());
            }
        }

        @Test
        void evaluateWrapsInvalidCorrectExpressionInIllegalState() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "broken", "0.1", true);
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> service.evaluate(new ExpressionAnswerSubmission(q, "2+2")));
            assertEquals("invalid correct expression for question q1", e.getMessage());
            assertTrue(e.getCause() instanceof ExpressionEvaluationException);
        }

        @Test
        void evaluateFlagsInvalidStudentExpression() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2+2", "0.1", false);
            ExpressionAnswerEvaluation ev = service.evaluate(new ExpressionAnswerSubmission(q, "broken"));
            assertEquals(ExpressionAnswerStatus.INVALID_EXPRESSION, ev.status());
            assertEquals("INVALID_STUDENT_EXPRESSION", ev.message());
            assertTrue(ev.answerResult().answered());
            assertFalse(ev.answerResult().exactMatch());
            assertBigDecimalEquals("4", ev.expectedValue());
            assertNull(ev.actualValue());
            assertNull(ev.absoluteError());
        }

        @Test
        void evaluateExactMatchPopulatesFullResult() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2+2", "0.1", true);
            ExpressionAnswerEvaluation ev = service.evaluate(new ExpressionAnswerSubmission(q, "2+2"));
            assertEquals(ExpressionAnswerStatus.EXACT, ev.status());
            assertEquals("EXACT", ev.message());
            assertTrue(ev.answerResult().exactMatch());
            assertBigDecimalEquals("4", ev.expectedValue());
            assertBigDecimalEquals("4", ev.actualValue());
            assertBigDecimalEquals("0", ev.absoluteError());
            AnswerResult a = ev.answerResult();
            assertEquals("q1", a.questionId());
            assertEquals(QuestionType.NUMERIC, a.type());
            assertEquals("math", a.section());
            assertBigDecimalEquals("10", a.maximumPoints());
            assertTrue(a.mandatory());
            assertBigDecimalEquals("0.1", a.numericTolerance());
            assertBigDecimalEquals("0", a.numericError());
        }

        @Test
        void evaluateWithinToleranceOnBothSides() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2*3", "0.1", false);
            // actual 4.05 via another evaluator
            ExpressionAnswerGradingService tolerant = new ExpressionAnswerGradingService(expression ->
                    expression.equals("2*3") ? new BigDecimal("6") : new BigDecimal("6.05"));
            ExpressionAnswerEvaluation high = tolerant.evaluate(new ExpressionAnswerSubmission(q, "other"));
            assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE, high.status());
            assertEquals("WITHIN_TOLERANCE", high.message());
            assertFalse(high.answerResult().exactMatch());
            assertBigDecimalEquals("0.05", high.absoluteError());
            ExpressionAnswerGradingService low = new ExpressionAnswerGradingService(expression ->
                    expression.equals("2*3") ? new BigDecimal("6") : new BigDecimal("5.95"));
            ExpressionAnswerEvaluation below = low.evaluate(new ExpressionAnswerSubmission(q, "other"));
            assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE, below.status());
            assertBigDecimalEquals("0.05", below.absoluteError());
        }

        @Test
        void evaluateBoundaryErrorExactlyAtTolerance() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2*3", "0.1", false);
            ExpressionAnswerGradingService edge = new ExpressionAnswerGradingService(expression ->
                    expression.equals("2*3") ? new BigDecimal("6") : new BigDecimal("6.1"));
            ExpressionAnswerEvaluation evaluation = edge.evaluate(new ExpressionAnswerSubmission(q, "other"));
            assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE, evaluation.status());
            assertBigDecimalEquals("0.1", evaluation.absoluteError());
        }

        @Test
        void evaluateIncorrectBeyondTolerance() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2+2", "0.1", false);
            ExpressionAnswerEvaluation ev = service.evaluate(new ExpressionAnswerSubmission(q, "2*3"));
            assertEquals(ExpressionAnswerStatus.INCORRECT, ev.status());
            assertEquals("INCORRECT", ev.message());
            assertFalse(ev.answerResult().exactMatch());
            assertBigDecimalEquals("2", ev.absoluteError());
            assertBigDecimalEquals("6", ev.actualValue());
        }

        @Test
        void evaluateUsesToleranceZeroForStrictComparison() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2+2", "0", false);
            ExpressionAnswerEvaluation wrong = service.evaluate(new ExpressionAnswerSubmission(q, "2*3"));
            assertEquals(ExpressionAnswerStatus.INCORRECT, wrong.status());
            ExpressionAnswerEvaluation exact = service.evaluate(new ExpressionAnswerSubmission(q, "2+2"));
            assertEquals(ExpressionAnswerStatus.EXACT, exact.status());
        }
    }

    // ------------------------------------------------------------------
    // Grading expression: MathematicalExpressionEvaluator
    // ------------------------------------------------------------------
    @Nested
    class MathematicalExpressionEvaluatorTests {

        @Test
        void defaultEvaluatorUsesArithmeticPathForDecimalExpressions() {
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator();
            assertBigDecimalEquals("3", evaluator.evaluate("1+2"));
            assertBigDecimalEquals("9", evaluator.evaluate("(1+2)*3"));
            assertBigDecimalEquals("3.75", evaluator.evaluate("1.5+2.25"));
            assertBigDecimalEquals("0.3", evaluator.evaluate("0.1+0.2"));
        }

        @Test
        void defaultEvaluatorFallsBackToEngineForUnsupportedOperators() {
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator();
            assertBigDecimalEquals("8", evaluator.evaluate("2^3"));
            assertBigDecimalEquals("10", evaluator.evaluate("2+2^3"));
            assertBigDecimalEquals("6", evaluator.evaluate("3!"));
        }

        @Test
        void evaluatorRejectsBlankExpressions() {
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator();
            for (String bad : new String[]{null, "", "   "}) {
                ExpressionEvaluationException e = assertThrows(ExpressionEvaluationException.class,
                        () -> evaluator.evaluate(bad));
                assertEquals("expression must not be blank", e.getMessage());
            }
        }

        @Test
        void evaluatorWrapsEngineFailures() {
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator();
            ExpressionEvaluationException e = assertThrows(ExpressionEvaluationException.class,
                    () -> evaluator.evaluate("abc"));
            assertTrue(e.getMessage().contains("unable to evaluate expression"));
            assertThrows(ExpressionEvaluationException.class, () -> evaluator.evaluate("1/0"));
        }

        @Test
        void evaluatorWithCustomCalculatorUsesEnginePathOnly() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("customEvaluatorCalculator");
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator(calculator);
            assertBigDecimalEquals("3", evaluator.evaluate("1+2"));
            assertBigDecimalEquals("8", evaluator.evaluate("2^3"));
            assertBigDecimalEquals("6", evaluator.evaluate("3!"));
            assertBigDecimalEquals("21", evaluator.evaluate("(1+2)*(3+4)"));
        }

        @Test
        void evaluatorConstructorRejectsNullCalculator() {
            assertThrows(NullPointerException.class,
                    () -> new MathematicalExpressionEvaluator((BracketsCalculation2) null));
        }


    }

    // ------------------------------------------------------------------
    // Grading service: ExamGradingService
    // ------------------------------------------------------------------
    @Nested
    class ExamGradingServiceTests {

        private static final Instant T0 = Instant.parse("2026-10-01T08:00:00Z");
        private static final AtomicInteger IDS = new AtomicInteger();

        private static AnswerResult ans(QuestionType type, String section, String max, boolean mandatory,
                                        boolean answered, boolean exact, int correctOptions, int selectedCorrect,
                                        int selectedIncorrect, String numericError, String numericTolerance,
                                        int rubric, int passed, int total, boolean compiled, boolean plag,
                                        boolean timedOut, boolean manual) {
            return new AnswerResult("q" + IDS.incrementAndGet(), type, section, new BigDecimal(max), mandatory,
                    answered, exact, correctOptions, selectedCorrect, selectedIncorrect,
                    numericError == null ? null : new BigDecimal(numericError),
                    numericTolerance == null ? null : new BigDecimal(numericTolerance),
                    rubric, passed, total, compiled, plag, timedOut, manual);
        }

        private static AnswerResult ans(QuestionType type, String max, boolean answered, boolean exact) {
            return ans(type, "S1", max, false, answered, exact, 0, 0, 0, null, null, 0, 0, 0,
                    false, false, false, false);
        }

        private static AnswerResult raw(String id, QuestionType type, String section, String max,
                                        boolean mandatory, boolean answered, boolean exact, int correctOptions,
                                        int selectedCorrect, int selectedIncorrect, String numericError,
                                        String numericTolerance, int rubric, int passed, int total,
                                        boolean compiled, boolean plag, boolean timedOut, boolean manual) {
            return new AnswerResult(id, type, section, max == null ? null : new BigDecimal(max), mandatory,
                    answered, exact, correctOptions, selectedCorrect, selectedIncorrect,
                    numericError == null ? null : new BigDecimal(numericError),
                    numericTolerance == null ? null : new BigDecimal(numericTolerance),
                    rubric, passed, total, compiled, plag, timedOut, manual);
        }

        private static ExamSubmission sub(String examId, String studentId, List<AnswerResult> answers,
                                          Instant start, Instant submit, Instant deadline, int allowedSeconds,
                                          int attempt, boolean identityVerified, boolean termsAccepted,
                                          boolean suspended, boolean proctor, boolean ipChanged, int fullscreen,
                                          int copyPaste, String bonus, Integer override,
                                          Map<String, Integer> sectionPass) {
            return new ExamSubmission(examId, studentId, answers, start, submit, deadline, allowedSeconds, attempt,
                    identityVerified, termsAccepted, suspended, proctor, ipChanged, fullscreen, copyPaste,
                    new BigDecimal(bonus), override, sectionPass);
        }

        private static ExamSubmission sub(List<AnswerResult> answers, Instant start, Instant submit,
                                          Instant deadline, int allowedSeconds, int attempt,
                                          boolean identityVerified, boolean termsAccepted, boolean suspended,
                                          boolean proctor, boolean ipChanged, int fullscreen, int copyPaste,
                                          String bonus, Integer override, Map<String, Integer> sectionPass) {
            return sub("exam1", "stu1", answers, start, submit, deadline, allowedSeconds, attempt,
                    identityVerified, termsAccepted, suspended, proctor, ipChanged, fullscreen, copyPaste,
                    bonus, override, sectionPass);
        }

        private static ExamSubmission sub(List<AnswerResult> answers) {
            return sub(answers, T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of());
        }

        private static GradingPolicy pol(int pass, boolean partial, boolean negative, String wrongRate,
                                         String unansweredRate, int latePerHour, int maxLate, int minSection,
                                         boolean allMandatory, boolean idRequired, int maxAttempts,
                                         boolean failIntegrity, boolean cap, boolean compilationRequired) {
            return new GradingPolicy(pass, partial, negative, new BigDecimal(wrongRate),
                    new BigDecimal(unansweredRate), latePerHour, maxLate, minSection, allMandatory, idRequired,
                    maxAttempts, failIntegrity, cap, compilationRequired);
        }

        private static GradingPolicy pol() {
            return pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true);
        }

        private final ExamGradingService service = new ExamGradingService();

        @Test
        void singleChoiceCorrectAwardsFullCredit() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))), pol());
            assertBigDecimalEquals("10.00", report.rawPoints());
            assertBigDecimalEquals("10.00", report.finalPoints());
            assertBigDecimalEquals("10", report.maximumPoints());
            assertEquals(100, report.percentage());
            assertEquals("A+", report.letterGrade());
            assertTrue(report.passed());
            assertFalse(report.manualReview());
            assertEquals(100, report.sectionPercentages().get("S1"));
            assertTrue(report.failures().isEmpty());
            assertTrue(report.warnings().isEmpty());
        }

        @Test
        void trueFalseWrongWithoutNegativeMarkingGivesZero() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.TRUE_FALSE, "10", true, false))), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertEquals(0, report.percentage());
            assertEquals("F", report.letterGrade());
            assertFalse(report.passed());
            assertTrue(report.failures().contains("OVERALL_SCORE_BELOW_PASS_MARK"));
        }

        @Test
        void negativeMarkingDeductsWrongAnswerPenalty() {
            GradeReport report = service.grade(sub(List.of(
                    ans(QuestionType.SINGLE_CHOICE, "10", true, true),
                    ans(QuestionType.SINGLE_CHOICE, "10", true, false))),
                    pol(60, true, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.rawPoints());
            assertEquals(25, report.percentage());
        }

        @Test
        void unansweredQuestionAppliesPenaltyRateWhenConfigured() {
            GradeReport report = service.grade(sub(List.of(
                    ans(QuestionType.SINGLE_CHOICE, "10", true, true),
                    ans(QuestionType.SINGLE_CHOICE, "10", false, false))),
                    pol(60, true, false, "0.50", "0.25", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("7.50", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("UNANSWERED_PENALTY:")));
        }

        @Test
        void unansweredWithoutPenaltyGivesZero() {
            GradeReport report = service.grade(sub(List.of(
                    ans(QuestionType.SINGLE_CHOICE, "10", true, true),
                    ans(QuestionType.SINGLE_CHOICE, "10", false, false))), pol());
            assertBigDecimalEquals("10.00", report.rawPoints());
            assertTrue(report.warnings().stream().noneMatch(w -> w.startsWith("UNANSWERED_PENALTY:")));
        }

        @Test
        void multipleChoiceExactMatchGivesFullCredit() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, true,
                    3, 3, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void multipleChoiceExactFlagOverridesPartialCounts() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, true,
                    3, 1, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void multipleChoiceWithNoCorrectOptionsGivesZeroAndWarning() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("MULTIPLE_CHOICE_HAS_NO_CORRECT_OPTIONS:")));
        }

        @Test
        void multipleChoiceWithoutPartialCreditGivesZeroWhenWrong() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 1, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)),
                    pol(60, false, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", report.rawPoints());
        }

        @Test
        void multipleChoiceWithoutPartialCreditAndNegativeMarkingPenalizesIncorrectSelections() {
            AnswerResult wrong = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 1, 1, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true), wrong)),
                    pol(60, false, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.rawPoints());

            AnswerResult noIncorrect = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 1, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport zeroed = service.grade(sub(List.of(noIncorrect)),
                    pol(60, false, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", zeroed.rawPoints());
        }

        @Test
        void multipleChoicePartialCreditScoresSelectedOptionsAndDeductsIncorrect() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 2, 1, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("5.83", report.rawPoints());
        }

        @Test
        void multipleChoicePartialCreditWithNegativeMarkingUsesPenaltyRate() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 2, 1, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)),
                    pol(60, true, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.rawPoints());
        }

        @Test
        void multipleChoiceOnlyIncorrectSelectionWarnsAndScoresZeroAfterClamp() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 0, 1, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("ONLY_INCORRECT_OPTIONS_SELECTED:")));
        }

        @Test
        void multipleChoiceAllCorrectSelectedWithoutExactFlagStillFullCredit() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    2, 2, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void fillBlankExactMatchGivesFullCredit() {
            assertBigDecimalEquals("10.00", service.grade(
                    sub(List.of(ans(QuestionType.FILL_BLANK, "10", true, true))), pol()).rawPoints());
        }

        @Test
        void fillBlankPartialCreditUsesRubricPercent() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 40, 0, 0, false, false, false, false);
            assertBigDecimalEquals("4.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void fillBlankWithoutRubricAndNegativeMarkingPenalizes() {
            AnswerResult wrong = ans(QuestionType.FILL_BLANK, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.FILL_BLANK, "10", true, true), wrong)),
                    pol(60, true, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.rawPoints());
        }

        @Test
        void fillBlankWithoutRubricAndNoNegativeGivesZero() {
            AnswerResult wrong = ans(QuestionType.FILL_BLANK, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("0.00", service.grade(sub(List.of(wrong)), pol()).rawPoints());
        }

        @Test
        void numericExactMatchGivesFullCredit() {
            assertBigDecimalEquals("10.00", service.grade(
                    sub(List.of(ans(QuestionType.NUMERIC, "10", true, true))), pol()).rawPoints());
        }

        @Test
        void numericWithinToleranceGivesFullCredit() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "0.05", "0.1", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericMissingToleranceDataWarnsAndGivesZero() {
            AnswerResult noError = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, null, "0.1", 0, 0, 0, false, false, false, false);
            AnswerResult noTolerance = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "0.05", null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(noError, noTolerance)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertEquals(2, report.warnings().stream().filter(w -> w.startsWith("NUMERIC_TOLERANCE_DATA_MISSING:")).count());
        }

        @Test
        void numericPartialCreditScalesByErrorRatio() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "3", "1", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("7.78", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericPartialCreditWithNegativeError() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "-3", "1", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("7.78", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericBeyondTenTimesToleranceGivesZero() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "10.5", "1", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("0.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericZeroToleranceSkipsPartialCredit() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "0.5", "0", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("0.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericNegativeMarkingPenalizesWrongAnswers() {
            AnswerResult wrong = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "11", "1", 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.NUMERIC, "10", true, true), wrong)),
                    pol(60, true, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.rawPoints());
        }

        @Test
        void essayExactMatchGivesFullCredit() {
            AnswerResult essay = ans(QuestionType.ESSAY, "S1", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(essay)), pol()).rawPoints());
        }

        @Test
        void essayRubricScalesCredit() {
            AnswerResult essay = ans(QuestionType.ESSAY, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 60, 0, 0, false, false, false, false);
            assertBigDecimalEquals("6.00", service.grade(sub(List.of(essay)), pol()).rawPoints());
        }

        @Test
        void essayWithoutRubricGivesZero() {
            AnswerResult essay = ans(QuestionType.ESSAY, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("0.00", service.grade(sub(List.of(essay)), pol()).rawPoints());
        }

        @Test
        void essayManualReviewAddsWarning() {
            AnswerResult essay = ans(QuestionType.ESSAY, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 60, 0, 0, false, false, false, true);
            GradeReport report = service.grade(sub(List.of(essay)), pol());
            assertBigDecimalEquals("6.00", report.rawPoints());
            assertTrue(report.warnings().contains("ESSAY_SCORE_IS_PROVISIONAL:q" + IDS.get()));
            assertTrue(report.warnings().contains("MANUAL_GRADING_REQUIRED:q" + IDS.get()));
            assertTrue(report.warnings().contains("RESULT_PENDING_MANUAL_REVIEW"));
            assertTrue(report.manualReview());
        }

        @Test
        void programWithoutCompilationFailsWhenRequired() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(program)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("PROGRAM_DID_NOT_COMPILE:")));
        }

        @Test
        void programWithoutCompilationGetsTenthCreditWhenNotRequired() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(program)),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, false));
            assertBigDecimalEquals("1.00", report.rawPoints());
        }

        @Test
        void programCompiledWithoutTestsAndManualReviewGivesZero() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, true, false, false, true);
            GradeReport report = service.grade(sub(List.of(program)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("PROGRAM_HAS_NO_AUTOMATED_TESTS:")));
        }

        @Test
        void programAllTestsPassedGivesFullCredit() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 4, 4, true, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(program)), pol()).rawPoints());
        }

        @Test
        void programWithoutPartialCreditGivesZeroWhenSomeTestsFail() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 3, 4, true, false, false, false);
            GradeReport report = service.grade(sub(List.of(program)),
                    pol(60, false, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", report.rawPoints());
        }

        @Test
        void programPartialCreditScalesByPassRate() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 3, 4, true, false, false, false);
            assertBigDecimalEquals("7.50", service.grade(sub(List.of(program)), pol()).rawPoints());
        }

        @Test
        void programTimeoutAppliesBothReductions() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 3, 4, true, false, true, false);
            assertBigDecimalEquals("4.50", service.grade(sub(List.of(program)), pol()).rawPoints());
        }

        @Test
        void programPlagiarismZeroesScore() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 3, 4, true, true, false, false);
            GradeReport report = service.grade(sub(List.of(program)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.failures().stream().anyMatch(f -> f.startsWith("ANSWER_INTEGRITY_VIOLATION:")));
            assertTrue(report.manualReview());
        }

        @Test
        void timedOutAnswerLosesTwentyPercent() {
            AnswerResult answer = ans(QuestionType.SINGLE_CHOICE, "S1", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, true, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("8.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("QUESTION_TIMED_OUT:")));
        }

        @Test
        void timedOutZeroScoreIsNotRescaled() {
            AnswerResult answer = ans(QuestionType.SINGLE_CHOICE, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, true, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("QUESTION_TIMED_OUT:")));
        }

        @Test
        void mandatoryAnsweredCorrectlySkipsMandatoryFailureAndWarning() {
            AnswerResult mandatory = ans(QuestionType.SINGLE_CHOICE, "S1", "10", true, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(mandatory)), pol());
            assertBigDecimalEquals("10.00", report.rawPoints());
            assertTrue(report.failures().stream().noneMatch(f -> f.startsWith("MANDATORY_QUESTION_UNANSWERED:")));
            assertTrue(report.warnings().stream().noneMatch(w -> w.startsWith("MANDATORY_QUESTION_NO_CREDIT:")));
            assertTrue(report.passed());
        }

        @Test
        void fillBlankPartialCreditRequiresPolicyEnabled() {
            AnswerResult wrong = ans(QuestionType.FILL_BLANK, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 40, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(wrong)),
                    pol(60, false, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", report.rawPoints());
        }

        @Test
        void numericPartialCreditRequiresPolicyEnabled() {
            AnswerResult wrong = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "3", "1", 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(wrong)),
                    pol(60, false, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", report.rawPoints());
        }

        @Test
        void bonusPointsAreAddedAndWarned() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "5", null, Map.of()),
                    pol());
            assertBigDecimalEquals("15.00", report.finalPoints());
            assertEquals(150, report.percentage());
            assertTrue(report.warnings().contains("BONUS_POINTS_APPLIED"));
        }

        @Test
        void capScoreAtOneHundredClampsPercentageAndAdjusted() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "5", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, true, true));
            assertEquals(100, report.percentage());
            assertBigDecimalEquals("10.00", report.finalPoints());
        }

        @Test
        void lateSubmissionDeductsPerHourPenalty() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), T0.plusSeconds(540), 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of()), pol());
            assertBigDecimalEquals("9.00", report.finalPoints());
            assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:10"));
        }

        @Test
        void lateSubmissionUnderOneMinuteStillChargesOneHour() {
            Instant submit = T0.plusSeconds(600);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, submit.minusSeconds(30), 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of()), pol());
            assertBigDecimalEquals("9.00", report.finalPoints());
            assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:10"));
        }

        @Test
        void lateSubmissionChargesWholeHours() {
            Instant submit = T0.plusSeconds(600);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, submit.minusSeconds(121 * 60), 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of()), pol());
            assertBigDecimalEquals("7.00", report.finalPoints());
            assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:30"));
        }

        @Test
        void latePenaltyRespectsMaximumCap() {
            Instant submit = T0.plusSeconds(600);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, submit.minusSeconds(2 * 3600), 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 60, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.finalPoints());
            assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:50"));
        }

        @Test
        void latePenaltyAtHundredPercentFailsSubmission() {
            Instant submit = T0.plusSeconds(600);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, submit.minusSeconds(3600), 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 100, 100, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", report.finalPoints());
            assertTrue(report.failures().contains("SUBMISSION_TOO_LATE"));
        }

        @Test
        void noLatePenaltyWithoutDeadlineOrWhenSubmittedInTime() {
            Instant submit = T0.plusSeconds(600);
            GradeReport noDeadline = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, null, 3600, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()), pol());
            assertBigDecimalEquals("10.00", noDeadline.finalPoints());
            GradeReport inTime = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, submit.plusSeconds(60), 3600, 1, true, true, false, false, false, 0, 0, "0", null,
                    Map.of()), pol());
            assertBigDecimalEquals("10.00", inTime.finalPoints());
        }

        @Test
        void shortOverrunUsesGracePeriod() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(130), null, 100, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("10.00", report.finalPoints());
            assertTrue(report.warnings().contains("EXAM_GRACE_PERIOD_USED"));
        }

        @Test
        void exactlySixtySecondsOverrunStillUsesGracePeriod() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(160), null, 100, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("10.00", report.finalPoints());
            assertTrue(report.warnings().contains("EXAM_GRACE_PERIOD_USED"));
        }

        @Test
        void exactlyThreeHundredSecondsOverrunIsMinor() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(400), null, 100, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("9.50", report.finalPoints());
            assertTrue(report.warnings().contains("EXAM_DURATION_MINOR_OVERRUN"));
            assertFalse(report.failures().contains("EXAM_DURATION_EXCEEDED"));
        }

        @Test
        void durationExactlyAtAllowedTimeHasNoPenalty() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 600, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("10.00", report.finalPoints());
            assertTrue(report.warnings().stream().noneMatch(w -> w.startsWith("EXAM_")));
        }

        @Test
        void minorOverrunReducesScoreByFivePercent() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(161), null, 100, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("9.50", report.finalPoints());
            assertTrue(report.warnings().contains("EXAM_DURATION_MINOR_OVERRUN"));
        }

        @Test
        void majorOverrunReducesByTwentyPercentAndFails() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(401), null, 100, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("8.00", report.finalPoints());
            assertTrue(report.failures().contains("EXAM_DURATION_EXCEEDED"));
        }

        @Test
        void zeroAllowedDurationSkipsDurationChecks() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 0, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("10.00", report.finalPoints());
        }

        @Test
        void attemptsAboveMaximumZeroTheScore() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 4, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("0.00", report.finalPoints());
            assertTrue(report.failures().contains("MAXIMUM_ATTEMPTS_EXCEEDED"));
        }

        @Test
        void finalAttemptAddsWarning() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 3, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("10.00", report.finalPoints());
            assertTrue(report.warnings().contains("FINAL_ALLOWED_ATTEMPT"));
        }

        @Test
        void sectionBelowMinimumFailsAndListsFailure() {
            AnswerResult a = ans(QuestionType.SINGLE_CHOICE, "S-A", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            AnswerResult b = ans(QuestionType.SINGLE_CHOICE, "S-B", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(a, b)),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 50, true, false, 3, false, false, true));
            assertEquals(100, report.sectionPercentages().get("S-A"));
            assertEquals(0, report.sectionPercentages().get("S-B"));
            assertTrue(report.failures().contains("SECTION_BELOW_PASS_MARK:S-B"));
            assertFalse(report.passed());
        }

        @Test
        void perSectionOverrideCanLowerTheBar() {
            AnswerResult a = ans(QuestionType.SINGLE_CHOICE, "S-A", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            AnswerResult b = ans(QuestionType.SINGLE_CHOICE, "S-B", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(a, b), T0, T0.plusSeconds(600), null, 3600, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of("S-B", 0)),
                    pol(40, true, false, "0.50", "0.00", 10, 50, 50, true, false, 3, false, false, true));
            assertTrue(report.passed());
            assertTrue(report.failures().stream().noneMatch(f -> f.startsWith("SECTION_BELOW_PASS_MARK")));
        }

        @Test
        void requiredSectionMissingFromAnswersIsReported() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", null,
                    Map.of("S-C", 80)), pol());
            assertEquals(0, report.sectionPercentages().get("S-C"));
            assertTrue(report.failures().contains("REQUIRED_SECTION_MISSING:S-C"));
            assertFalse(report.passed());
        }

        @Test
        void emptySubmissionScoresZeroPercent() {
            GradeReport report = service.grade(sub(List.of()), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertEquals(0, report.percentage());
            assertEquals("F", report.letterGrade());
            assertFalse(report.passed());
            assertTrue(report.failures().contains("OVERALL_SCORE_BELOW_PASS_MARK"));
        }

        @Test
        void termsNotAcceptedAddsFailure() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, false, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.failures().contains("EXAM_TERMS_NOT_ACCEPTED"));
            assertTrue(report.passed());
        }

        @Test
        void suspendedAccountAddsFailureAndBlocksWhenConfigured() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, true, false, false, 0, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, true, false, true));
            assertTrue(report.failures().contains("ACCOUNT_SUSPENDED"));
            assertFalse(report.passed());
        }

        @Test
        void identityVerificationRequiredAndMissingFails() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, false, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, true, 3, true, false, true));
            assertTrue(report.failures().contains("IDENTITY_NOT_VERIFIED"));
            assertFalse(report.passed());
        }

        @Test
        void identityNotVerifiedOnlyWarnsWhenNotRequired() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, false, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("IDENTITY_NOT_VERIFIED"));
            assertTrue(report.passed());
        }

        @Test
        void identityRequiredAndVerifiedStaysSilent() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, true, 3, false, false, true));
            assertTrue(report.failures().stream().noneMatch(f -> f.contains("IDENTITY")));
            assertTrue(report.warnings().stream().noneMatch(w -> w.contains("IDENTITY")));
            assertTrue(report.passed());
        }

        @Test
        void integrityScanIgnoresNonIntegrityFailures() {
            AnswerResult correct = ans(QuestionType.SINGLE_CHOICE, "S1", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            AnswerResult mandatory = ans(QuestionType.SINGLE_CHOICE, "S1", "10", true, false, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(correct, mandatory)),
                    pol(40, true, false, "0.50", "0.00", 10, 50, 0, false, false, 3, true, false, true));
            assertTrue(report.failures().stream().anyMatch(f -> f.startsWith("MANDATORY_QUESTION_UNANSWERED:")));
            assertTrue(report.passed());
        }

        @Test
        void proctorDisconnectionWarnsAndTriggersManualReview() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, true, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("PROCTOR_CONNECTION_LOST"));
            assertTrue(report.warnings().contains("RESULT_PENDING_MANUAL_REVIEW"));
            assertTrue(report.manualReview());
        }

        @Test
        void ipAddressChangeWarns() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, true, 0, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("IP_ADDRESS_CHANGED"));
        }

        @Test
        void singleFullscreenExitWarns() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 1, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("FULLSCREEN_EXIT_RECORDED"));
        }

        @Test
        void twoFullscreenExitsWarn() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 2, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("MULTIPLE_FULLSCREEN_EXITS"));
        }

        @Test
        void excessiveFullscreenExitsFailAndTriggerReview() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 3, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, true, false, true));
            assertTrue(report.failures().contains("EXCESSIVE_FULLSCREEN_EXITS"));
            assertTrue(report.manualReview());
            assertFalse(report.passed());
        }

        @Test
        void fewCopyPasteEventsWarn() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 3, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("COPY_PASTE_EVENTS_RECORDED"));
        }

        @Test
        void excessiveCopyPasteFailsAndTriggersReview() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 5, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, true, false, true));
            assertTrue(report.failures().contains("EXCESSIVE_COPY_PASTE_EVENTS"));
            assertTrue(report.manualReview());
            assertFalse(report.passed());
        }

        @Test
        void mandatoryUnansweredQuestionFailsAndWarns() {
            AnswerResult mandatory = ans(QuestionType.SINGLE_CHOICE, "S1", "10", true, false, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(mandatory)), pol());
            assertTrue(report.failures().stream().anyMatch(f -> f.startsWith("MANDATORY_QUESTION_UNANSWERED:")));
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("MANDATORY_QUESTION_NO_CREDIT:")));
            assertFalse(report.passed());
        }

        @Test
        void mandatoryRequirementCanBeRelaxedByPolicy() {
            AnswerResult mandatory = ans(QuestionType.SINGLE_CHOICE, "S1", "10", true, false, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true), mandatory)),
                    pol(40, true, false, "0.50", "0.00", 10, 50, 0, false, false, 3, false, false, true));
            assertTrue(report.passed());
            assertTrue(report.failures().stream().anyMatch(f -> f.startsWith("MANDATORY_QUESTION_UNANSWERED:")));
        }

        @Test
        void answerIntegrityViolationFailsWhenConfigured() {
            AnswerResult plag = ans(QuestionType.SINGLE_CHOICE, "S1", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, true, false, false);
            GradeReport report = service.grade(sub(List.of(plag)),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, true, false, true));
            assertTrue(report.failures().stream().anyMatch(f -> f.startsWith("ANSWER_INTEGRITY_VIOLATION:")));
            assertTrue(report.manualReview());
            assertFalse(report.passed());
        }

        @Test
        void manualReviewRequiredAnswerWarnsAndFlagsReport() {
            AnswerResult manual = ans(QuestionType.SINGLE_CHOICE, "S1", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, true);
            GradeReport report = service.grade(sub(List.of(manual)), pol());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("MANUAL_GRADING_REQUIRED:")));
            assertTrue(report.warnings().contains("RESULT_PENDING_MANUAL_REVIEW"));
            assertTrue(report.manualReview());
        }

        @Test
        void instructorOverrideReplacesScoreAndWarns() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", 50, Map.of()),
                    pol());
            assertEquals(50, report.percentage());
            assertBigDecimalEquals("5.00", report.finalPoints());
            assertTrue(report.warnings().contains("INSTRUCTOR_OVERRIDE_APPLIED"));
            assertTrue(report.failures().contains("OVERALL_SCORE_BELOW_PASS_MARK"));
        }

        @Test
        void letterGradeFollowsScale() {
            Map<Integer, String> expected = Map.of(
                    95, "A+", 90, "A", 85, "A-", 80, "B+", 75, "B", 70, "C+", 65, "C", 60, "D");
            for (Map.Entry<Integer, String> entry : expected.entrySet()) {
                AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                        0, 0, 0, null, null, entry.getKey(), 0, 0, false, false, false, false);
                GradeReport report = service.grade(sub(List.of(answer)), pol());
                assertEquals(entry.getKey(), report.percentage());
                assertEquals(entry.getValue(), report.letterGrade(), "grade for " + entry.getKey() + "%");
            }
        }

        @Test
        void failedExamWithDecentScoreKeepsLetterGrade() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                    0, 0, 0, null, null, 65, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)),
                    pol(70, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertEquals("C", report.letterGrade());
            assertFalse(report.passed());
        }

        @Test
        void failingScoreBelowSixtyAlwaysFailsWithFGrade() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                    0, 0, 0, null, null, 55, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertEquals("F", report.letterGrade());
        }

        @Test
        void percentageExactlyAtPassMarkPasses() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                    0, 0, 0, null, null, 60, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertEquals(60, report.percentage());
            assertTrue(report.passed());
            assertEquals("D", report.letterGrade());
        }

        @Test
        void failedAtPassMarkDoesNotAddOverallFailure() {
            AnswerResult strong = ans(QuestionType.FILL_BLANK, "S-A", "100", false, true, false,
                    0, 0, 0, null, null, 100, 0, 0, false, false, false, false);
            AnswerResult weak = ans(QuestionType.FILL_BLANK, "S-B", "100", false, true, false,
                    0, 0, 0, null, null, 20, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(strong, weak)),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 50, true, false, 3, false, false, true));
            assertEquals(60, report.percentage());
            assertFalse(report.passed());
            assertTrue(report.failures().contains("SECTION_BELOW_PASS_MARK:S-B"));
            assertFalse(report.failures().contains("OVERALL_SCORE_BELOW_PASS_MARK"));
        }

        @Test
        void failedExamAtSixtyPercentKeepsLetterGradeD() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                    0, 0, 0, null, null, 60, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)),
                    pol(70, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertEquals("D", report.letterGrade());
            assertFalse(report.passed());
        }

        @Test
        void passingScoreBelowSixtyStillGetsFGrade() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                    0, 0, 0, null, null, 55, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)),
                    pol(50, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertTrue(report.passed());
            assertEquals("F", report.letterGrade());
        }

        @Test
        void cleanSubmissionPassesWithIntegrityPolicyEnabled() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, true, false, true));
            assertTrue(report.passed());
        }

        @Test
        void mandatoryUnansweredAloneFailsWhenOtherwisePassing() {
            AnswerResult mandatory = ans(QuestionType.SINGLE_CHOICE, "S1", "10", true, false, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true), mandatory)),
                    pol(40, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertEquals(50, report.percentage());
            assertFalse(report.passed());
        }

        @Test
        void multipleChoiceWithZeroSelectionsHasNoOnlyIncorrectWarning() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().noneMatch(w -> w.startsWith("ONLY_INCORRECT_OPTIONS_SELECTED:")));
        }

        @Test
        void numericErrorExactlyAtToleranceGivesFullCredit() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "0.1", "0.1", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericZeroErrorAndZeroToleranceGivesFullCredit() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "0", "0", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void programFullPassWithoutPartialCreditStillFull() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 4, 4, true, false, false, false);
            GradeReport report = service.grade(sub(List.of(program)),
                    pol(60, false, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("10.00", report.rawPoints());
        }

        @Test
        void instructorOverrideBoundaryValuesAreValid() {
            GradeReport zero = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", 0, Map.of()),
                    pol());
            assertEquals(0, zero.percentage());
            GradeReport hundred = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", 100, Map.of()),
                    pol());
            assertEquals(100, hundred.percentage());
        }

        @Test
        void sectionPassPercentageOfHundredIsValid() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", null,
                    Map.of("S1", 100)), pol());
            assertTrue(report.passed());
        }

        @Test
        void rubricPercentOfHundredIsValid() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 100, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void policyBoundaryRatesAreValid() {
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "0", "0.00", 10, 50, 0, true, false, 3, false, false, true)).rawPoints());
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "1.00", "0.00", 10, 50, 0, true, false, 3, false, false, true)).rawPoints());
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "0.50", "1.00", 10, 50, 0, true, false, 3, false, false, true)).rawPoints());
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "0.50", "0.00", 0, 0, 0, true, false, 3, false, false, true)).rawPoints());
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 100, true, false, 3, false, false, true)).rawPoints());
        }

        @Test
        void passPercentageBoundaryValuesAreValid() {
            GradeReport zero = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(0, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertTrue(zero.passed());
            GradeReport hundred = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(100, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertTrue(hundred.passed());
        }

        // ----- validation of ExamGradingService -----

        @Test
        void gradeRequiresNonNullArguments() {
            assertThrows(NullPointerException.class, () -> service.grade(null, pol()));
            assertThrows(NullPointerException.class, () -> service.grade(sub(List.of()), null));
        }

        @Test
        void validateRejectsBlankIds() {
            ExamSubmission blankExam = sub("", "stu1", List.of(), T0, T0.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            ExamSubmission nullExam = sub(null, "stu1", List.of(), T0, T0.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            ExamSubmission blankStudent = sub("exam1", " ", List.of(), T0, T0.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            assertThrows(IllegalArgumentException.class, () -> service.grade(blankExam, pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(nullExam, pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(blankStudent, pol()));
        }

        @Test
        void validateRejectsMissingOrInvertedTimestamps() {
            ExamSubmission noStart = sub(List.of(), null, T0.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            ExamSubmission noSubmit = sub(List.of(), T0, null, null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            ExamSubmission inverted = sub(List.of(), T0.plusSeconds(1), T0, null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            assertThrows(IllegalArgumentException.class, () -> service.grade(noStart, pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(noSubmit, pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(inverted, pol()));
        }

        @Test
        void validateRejectsInvalidCountersAndBonuses() {
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, -1, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 0, true, true, false, false, false, 0, 0, "0", null, Map.of()), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, -1, 0, "0", null, Map.of()), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, -1, "0", null, Map.of()), pol()));
            ExamSubmission nullBonus = new ExamSubmission("exam1", "stu1", List.of(), T0, T0.plusSeconds(1), null,
                    0, 1, true, true, false, false, false, 0, 0, null, null, Map.of());
            assertThrows(IllegalArgumentException.class, () -> service.grade(nullBonus, pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "-1", null, Map.of()), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "0", -1, Map.of()), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "0", 101, Map.of()), pol()));
        }

        @Test
        void validatePolicyRejectsOutOfRangeValues() {
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(-1, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(101, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true)));
            GradingPolicy nullWrong = new GradingPolicy(60, true, false, null, BigDecimal.ZERO, 10, 50, 0,
                    true, false, 3, false, false, true);
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()), nullWrong));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "-0.1", "0.00", 10, 50, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "1.5", "0.00", 10, 50, 0, true, false, 3, false, false, true)));
            GradingPolicy nullUnanswered = new GradingPolicy(60, true, false, new BigDecimal("0.50"), null,
                    10, 50, 0, true, false, 3, false, false, true);
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()), nullUnanswered));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "-0.1", 10, 50, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "1.5", 10, 50, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", -1, 50, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", 10, -1, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 101, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, -1, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 101, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 0, false, false, true)));
        }

        @Test
        void validateAnswerRejectsMalformedAnswers() {
            GradingPolicy policy = pol();
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", null, "S", "10", false, true, true, 0, 0, 0, null, null, 0, 0, 0,
                            false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw(" ", QuestionType.NUMERIC, "S", "10", false, true, true, 0, 0, 0, null, null,
                            0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.NUMERIC, "", "10", false, true, true, 0, 0, 0, null, null,
                            0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.NUMERIC, "S", null, false, true, true, 0, 0, 0, null, null,
                            0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.NUMERIC, "S", "0", false, true, true, 0, 0, 0, null, null,
                            0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.MULTIPLE_CHOICE, "S", "10", false, true, true, -1, 0, 0,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.MULTIPLE_CHOICE, "S", "10", false, true, true, 1, -1, 0,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.MULTIPLE_CHOICE, "S", "10", false, true, true, 1, 0, -1,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.MULTIPLE_CHOICE, "S", "10", false, true, true, 2, 3, 0,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.NUMERIC, "S", "10", false, true, true, 0, 0, 0, null, "-0.5",
                            0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.FILL_BLANK, "S", "10", false, true, true, 0, 0, 0, null, null,
                            -1, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.FILL_BLANK, "S", "10", false, true, true, 0, 0, 0, null, null,
                            101, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.PROGRAMMING, "S", "10", false, true, true, 0, 0, 0, null, null,
                            0, -1, 0, true, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.PROGRAMMING, "S", "10", false, true, true, 0, 0, 0, null, null,
                            0, 0, -1, true, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.PROGRAMMING, "S", "10", false, true, true, 0, 0, 0, null, null,
                            0, 3, 2, true, false, false, false))), policy));
        }

        @Test
        void validateRejectsDuplicateQuestionIds() {
            AnswerResult first = raw("qdup", QuestionType.NUMERIC, "S", "10", false, true, true, 0, 0, 0,
                    null, null, 0, 0, 0, false, false, false, false);
            AnswerResult second = raw("qdup", QuestionType.NUMERIC, "S", "10", false, true, true, 0, 0, 0,
                    null, null, 0, 0, 0, false, false, false, false);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.grade(sub(List.of(first, second)), pol()));
            assertEquals("questionId must be unique", e.getMessage());
        }

        @Test
        void validateRejectsBadSectionPassPercentages() {
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "0", null, Map.of("", 50)), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "0", null, Map.of("S", -1)), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "0", null, Map.of("S", 101)), pol()));
        }

        @Test
        void validateRejectsNullStudentId() {
            ExamSubmission nullStudent = sub("exam1", null, List.of(), T0, T0.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            assertThrows(IllegalArgumentException.class, () -> service.grade(nullStudent, pol()));
        }

        @Test
        void validateRejectsNullQuestionIdAndSection() {
            GradingPolicy policy = pol();
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw(null, QuestionType.NUMERIC, "S", "10", false, true, true, 0, 0, 0,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.NUMERIC, null, "10", false, true, true, 0, 0, 0,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
        }

        @Test
        void unansweredProgrammingAnswerPassesValidation() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, false, false,
                    0, 0, 0, null, null, 0, 0, 0, true, false, false, false);
            GradeReport report = service.grade(sub(List.of(program)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
        }

        // ------------------------------------------------------------------
        // 白盒测试（反射）: 仅用于覆盖公开 API 不可达的私有分支，断言真实行为
        // ------------------------------------------------------------------






        @Test
        void validateAnswerRequiresTestsOrReviewForCompiledPrograms() {
            GradingPolicy policy = pol();
            AnswerResult invalid = raw("q1", QuestionType.PROGRAMMING, "S", "10", false, true, true, 0, 0, 0,
                    null, null, 0, 0, 0, true, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(invalid)), policy));
            AnswerResult valid = raw("q2", QuestionType.PROGRAMMING, "S", "10", false, true, true, 0, 0, 0,
                    null, null, 0, 0, 0, true, false, false, true);
            assertBigDecimalEquals("0.00", service.grade(sub(List.of(valid)), policy).rawPoints());
        }
    }

    // ------------------------------------------------------------------
    // Grading expression: ExpressionExamGradingService orchestration
    // ------------------------------------------------------------------
    @Nested
    class ExpressionExamGradingServiceTests {

        @Test
        void constructorRequiresBothServices() {
            ExpressionAnswerGradingService answerService =
                    new ExpressionAnswerGradingService(expression -> BigDecimal.ONE);
            assertThrows(NullPointerException.class,
                    () -> new ExpressionExamGradingService(null, new ExamGradingService()));
            assertThrows(NullPointerException.class,
                    () -> new ExpressionExamGradingService(answerService, null));
        }

        @Test
        void gradeRequiresSubmissionAndPolicy() {
            ExpressionExamGradingService service = new ExpressionExamGradingService(
                    new ExpressionAnswerGradingService(expression -> BigDecimal.ONE), new ExamGradingService());
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            ExpressionExamSubmission submission = new ExpressionExamSubmission("exam", "stu", List.of(),
                    start, start.plusSeconds(60), null, 3600, 1, true, true, false, false, false, 0, 0,
                    BigDecimal.ZERO, null, Map.of());
            assertThrows(NullPointerException.class, () -> service.grade(null, ExamGradingServiceTests.pol()));
            assertThrows(NullPointerException.class, () -> service.grade(submission, null));
        }

        @Test
        void gradeEvaluatesEachAnswerAndBuildsExamGrade() {
            ExpressionAnswerGradingService answerService = new ExpressionAnswerGradingService(expression -> {
                switch (expression) {
                    case "2+2":
                        return new BigDecimal("4");
                    case "2*3":
                        return new BigDecimal("6");
                    case "4":
                        return new BigDecimal("4");
                    default:
                        throw new ExpressionEvaluationException("unexpected: " + expression);
                }
            });
            ExpressionExamGradingService service = new ExpressionExamGradingService(answerService,
                    new ExamGradingService());
            ExpressionQuestion q1 = new ExpressionQuestion("q1", "math", "2+2", BigDecimal.TEN,
                    BigDecimal.ZERO, false);
            ExpressionQuestion q2 = new ExpressionQuestion("q2", "math", "2*3", BigDecimal.TEN,
                    BigDecimal.ZERO, false);
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            ExpressionExamSubmission submission = new ExpressionExamSubmission("exam", "stu",
                    List.of(new ExpressionAnswerSubmission(q1, "2+2"), new ExpressionAnswerSubmission(q2, "4")),
                    start, start.plusSeconds(60), null, 3600, 1, true, true, false, false, false, 0, 0,
                    BigDecimal.ZERO, null, Map.of());
            ExpressionExamGrade grade = service.grade(submission, ExamGradingServiceTests.pol());
            assertEquals(2, grade.answerEvaluations().size());
            assertEquals(ExpressionAnswerStatus.EXACT, grade.answerEvaluations().get(0).status());
            assertEquals(ExpressionAnswerStatus.INCORRECT, grade.answerEvaluations().get(1).status());
            assertEquals("q1", grade.answerEvaluations().get(0).answerResult().questionId());
            assertEquals("q2", grade.answerEvaluations().get(1).answerResult().questionId());
            assertBigDecimalEquals("10.00", grade.report().rawPoints());
            assertEquals(50, grade.report().percentage());
        }

        @Test
        void gradeMarksBlankSubmissionUnanswered() {
            ExpressionAnswerGradingService answerService =
                    new ExpressionAnswerGradingService(expression -> new BigDecimal("4"));
            ExpressionExamGradingService service = new ExpressionExamGradingService(answerService,
                    new ExamGradingService());
            ExpressionQuestion q1 = new ExpressionQuestion("q1", "math", "2+2", BigDecimal.TEN,
                    BigDecimal.ZERO, true);
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            ExpressionExamSubmission submission = new ExpressionExamSubmission("exam", "stu",
                    List.of(new ExpressionAnswerSubmission(q1, "")),
                    start, start.plusSeconds(60), null, 3600, 1, true, true, false, false, false, 0, 0,
                    BigDecimal.ZERO, null, Map.of());
            ExpressionExamGrade grade = service.grade(submission, ExamGradingServiceTests.pol());
            assertEquals(ExpressionAnswerStatus.UNANSWERED, grade.answerEvaluations().get(0).status());
            assertBigDecimalEquals("0.00", grade.report().rawPoints());
            assertTrue(grade.report().failures().stream().anyMatch(f -> f.startsWith("MANDATORY_QUESTION_UNANSWERED:")));
        }
    }

    // ------------------------------------------------------------------
    // Progress domain records
    // ------------------------------------------------------------------
    @Nested
    class ProgressDomainTests {

        @Test
        void lessonProgressExposesAllComponents() {
            LessonProgress lesson = new LessonProgress("l1", LessonType.QUIZ, true, 2, 80, 90, 1, 3,
                    true, false, true, 45, 60, false);
            assertEquals("l1", lesson.lessonId());
            assertEquals(LessonType.QUIZ, lesson.type());
            assertTrue(lesson.mandatory());
            assertEquals(2, lesson.order());
            assertEquals(80, lesson.completionPercent());
            assertEquals(90, lesson.score());
            assertEquals(1, lesson.attempts());
            assertEquals(3, lesson.maximumAttempts());
            assertTrue(lesson.submitted());
            assertFalse(lesson.approved());
            assertTrue(lesson.plagiarismFlag());
            assertEquals(45, lesson.attendedMinutes());
            assertEquals(60, lesson.requiredMinutes());
            assertFalse(lesson.unlocked());
        }

        @Test
        void lessonTypeEnumeratesSixKinds() {
            assertEquals(6, LessonType.values().length);
        }

        @Test
        void courseProgressSnapshotDefendsAgainstNullLessons() {
            CourseProgressSnapshot snapshot = new CourseProgressSnapshot("s1", "c1", null, false, 0, 0,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertEquals(List.of(), snapshot.lessons());
            assertEquals("s1", snapshot.studentId());
            assertEquals("c1", snapshot.courseId());
            assertTrue(snapshot.coursePublished());
        }

        @Test
        void progressEvaluationDefendsAgainstNullCollections() {
            ProgressEvaluation empty = new ProgressEvaluation(50, 1, 2, false, false, null, null, null);
            assertEquals(List.of(), empty.blockers());
            assertEquals(List.of(), empty.warnings());
            ProgressEvaluation full = new ProgressEvaluation(50, 1, 2, false, false, "next",
                    List.of("b1"), List.of("w1"));
            assertEquals(50, full.overallPercent());
            assertEquals(1, full.completedLessons());
            assertEquals(2, full.totalLessons());
            assertFalse(full.courseCompleted());
            assertFalse(full.certificateEligible());
            assertEquals("next", full.nextLessonId());
            assertEquals(List.of("b1"), full.blockers());
            assertEquals(List.of("w1"), full.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Progress service: LearningProgressService
    // ------------------------------------------------------------------
    @Nested
    class LearningProgressServiceTests {

        private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

        private static LearningProgressService service() {
            return new LearningProgressService(Clock.fixed(NOW, ZoneOffset.UTC));
        }

        private static LessonProgress lesson(String id, LessonType type, boolean mandatory, int order,
                                             int completion, int score, int attempts, int maxAttempts,
                                             boolean submitted, boolean approved, boolean plag,
                                             int attended, int required, boolean unlocked) {
            return new LessonProgress(id, type, mandatory, order, completion, score, attempts, maxAttempts,
                    submitted, approved, plag, attended, required, unlocked);
        }

        private static LessonProgress lesson(String id, LessonType type, boolean mandatory, int order,
                                             boolean complete, boolean unlocked) {
            int completion = complete ? 100 : 0;
            int score = complete ? 100 : 0;
            boolean submitted = complete;
            boolean approved = complete;
            int attended = complete ? 60 : 0;
            int required = type == LessonType.LIVE_SESSION ? 60 : 0;
            return lesson(id, type, mandatory, order, completion, score, 0, 0, submitted, approved,
                    false, attended, required, unlocked);
        }

        private static CourseProgressSnapshot snap(List<LessonProgress> lessons, int requiredOverall,
                                                   int requiredQuiz, boolean sequential, boolean identityVerified,
                                                   boolean codeOfConduct, boolean subscriptionActive,
                                                   boolean suspended, boolean published, Instant enrolledAt,
                                                   Instant expiresAt, boolean certificateEnabled,
                                                   String certificateName, boolean finalProjectRequired,
                                                   boolean outstandingBalance,
                                                   boolean instructorApprovalRequired) {
            return new CourseProgressSnapshot("s1", "c1", lessons, sequential, requiredOverall, requiredQuiz,
                    identityVerified, codeOfConduct, subscriptionActive, suspended, published, enrolledAt,
                    expiresAt, certificateEnabled, certificateName, finalProjectRequired, outstandingBalance,
                    instructorApprovalRequired);
        }

        private static CourseProgressSnapshot snap(List<LessonProgress> lessons) {
            return snap(lessons, 0, 0, false, true, true, true, false, true, null, null, false, null,
                    false, false, false);
        }

        @Test
        void completedCourseWithSingleVideo() {
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(lesson("v1", LessonType.VIDEO,
                    false, 0, true, true))));
            assertEquals(100, evaluation.overallPercent());
            assertEquals(1, evaluation.completedLessons());
            assertEquals(1, evaluation.totalLessons());
            assertTrue(evaluation.courseCompleted());
            assertFalse(evaluation.certificateEligible());
            assertNull(evaluation.nextLessonId());
            assertTrue(evaluation.blockers().isEmpty());
            assertEquals(List.of("CERTIFICATE_DISABLED"), evaluation.warnings());
        }

        @Test
        void nearCompletionVideoWarnsAndReportsNextLesson() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, 75, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(video)));
            assertEquals(73, evaluation.overallPercent());
            assertEquals(0, evaluation.completedLessons());
            assertTrue(evaluation.warnings().contains("VIDEO_NEAR_COMPLETION:v1"));
            assertEquals("v1", evaluation.nextLessonId());
            assertTrue(evaluation.courseCompleted());
            assertTrue(evaluation.warnings().contains("COMPLETION_STATE_CONFLICT"));
        }

        @Test
        void articleEarnsWeightOnlyWhenComplete() {
            LessonProgress complete = lesson("a1", LessonType.ARTICLE, false, 0, true, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(complete)));
            assertEquals(100, done.overallPercent());
            assertTrue(done.courseCompleted());

            LessonProgress partial = lesson("a2", LessonType.ARTICLE, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation half = service().evaluate(snap(List.of(partial)));
            assertEquals(50, half.overallPercent());
            assertEquals("a2", half.nextLessonId());
            assertFalse(half.warnings().contains("VIDEO_NEAR_COMPLETION:a2"));
        }

        @Test
        void quizRequiresSubmissionAndRequiredScore() {
            LessonProgress passed = lesson("q1", LessonType.QUIZ, false, 0, 100, 80, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(passed), 0, 60, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(done.courseCompleted());

            LessonProgress low = lesson("q2", LessonType.QUIZ, false, 0, 100, 50, 1, 0,
                    true, false, false, 0, 0, true);
            ProgressEvaluation retry = service().evaluate(snap(List.of(low), 0, 60, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(retry.warnings().contains("QUIZ_SCORE_TOO_LOW:q2"));
            assertEquals(50, retry.overallPercent());

            LessonProgress untouched = lesson("q3", LessonType.QUIZ, false, 0, 100, 80, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation none = service().evaluate(snap(List.of(untouched), 0, 60, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertEquals(0, none.overallPercent());
            assertEquals("q3", none.nextLessonId());
        }

        @Test
        void assignmentEarnsWeightPerState() {
            LessonProgress approved = lesson("a1", LessonType.ASSIGNMENT, false, 0, 100, 0, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(approved)));
            assertEquals(100, done.overallPercent());
            assertEquals(1, done.completedLessons());
            assertTrue(done.courseCompleted());

            LessonProgress pending = lesson("a2", LessonType.ASSIGNMENT, false, 0, 100, 0, 1, 0,
                    true, false, false, 0, 0, true);
            ProgressEvaluation awaiting = service().evaluate(snap(List.of(pending)));
            assertEquals(48, awaiting.overallPercent());
            assertTrue(awaiting.warnings().contains("ASSIGNMENT_AWAITING_REVISION:a2"));

            LessonProgress untouched = lesson("a3", LessonType.ASSIGNMENT, false, 0, 0, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation none = service().evaluate(snap(List.of(untouched)));
            assertEquals(0, none.overallPercent());
        }

        @Test
        void liveSessionRequiresAttendanceThreshold() {
            LessonProgress enough = lesson("l1", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 80, 100, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(enough)));
            assertEquals(80, done.overallPercent());
            assertTrue(done.courseCompleted());

            LessonProgress shortAttendance = lesson("l2", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 79, 100, true);
            ProgressEvaluation low = service().evaluate(snap(List.of(shortAttendance)));
            assertTrue(low.warnings().contains("LIVE_ATTENDANCE_TOO_SHORT:l2"));
            assertEquals(75, low.overallPercent());

            LessonProgress zeroRequiredAttended = lesson("l3", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation absent = service().evaluate(snap(List.of(zeroRequiredAttended)));
            assertEquals(0, absent.overallPercent());
            assertEquals(0, absent.completedLessons());

            LessonProgress zeroRequiredPresent = lesson("l4", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 1, 0, true);
            ProgressEvaluation present = service().evaluate(snap(List.of(zeroRequiredPresent)));
            assertEquals(100, present.overallPercent());

            LessonProgress overAttended = lesson("l5", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 120, 100, true);
            ProgressEvaluation capped = service().evaluate(snap(List.of(overAttended)));
            assertEquals(100, capped.overallPercent());
        }

        @Test
        void projectRequiresSubmissionApprovalAndIntegrity() {
            LessonProgress approved = lesson("p1", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(approved)));
            assertEquals(100, done.overallPercent());
            assertTrue(done.courseCompleted());

            LessonProgress pending = lesson("p2", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, false, false, 0, 0, true);
            ProgressEvaluation awaiting = service().evaluate(snap(List.of(pending)));
            assertEquals(33, awaiting.overallPercent());

            LessonProgress plag = lesson("p3", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, true, true, 0, 0, true);
            ProgressEvaluation flagged = service().evaluate(snap(List.of(plag)));
            assertTrue(flagged.warnings().contains("PROJECT_HELD_FOR_INTEGRITY_REVIEW:p3"));
            assertTrue(flagged.blockers().contains("PLAGIARISM_REVIEW:p3"));
            assertFalse(flagged.courseCompleted());
        }

        @Test
        void mandatoryIncompleteLessonBlocksCompletion() {
            LessonProgress mandatory = lesson("v1", LessonType.VIDEO, true, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(mandatory)));
            assertTrue(evaluation.blockers().contains("MANDATORY_LESSON_INCOMPLETE:v1"));
            assertFalse(evaluation.courseCompleted());
            assertEquals("v1", evaluation.nextLessonId());
        }

        @Test
        void attemptLimitReachedBlocksCompletion() {
            LessonProgress quiz = lesson("q1", LessonType.QUIZ, false, 0, 100, 30, 3, 3,
                    true, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(quiz), 0, 60, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(evaluation.blockers().contains("ATTEMPT_LIMIT_REACHED:q1"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        void attemptLimitOnlyBlocksWhenAtLimitAndIncomplete() {
            LessonProgress noCap = lesson("a1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress underCap = lesson("a2", LessonType.VIDEO, false, 1, 50, 0, 2, 5,
                    false, false, false, 0, 0, true);
            LessonProgress atCapButComplete = lesson("a3", LessonType.VIDEO, false, 2, 100, 0, 3, 3,
                    true, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(noCap, underCap, atCapButComplete)));
            assertTrue(evaluation.blockers().stream().noneMatch(b -> b.startsWith("ATTEMPT_LIMIT_REACHED")));
        }

        @Test
        void lockedLessonWithProgressWarnsAndIsNotNextLesson() {
            LessonProgress locked = lesson("v1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, false);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(locked)));
            assertTrue(evaluation.warnings().contains("LOCKED_LESSON_HAS_PROGRESS:v1"));
            assertNull(evaluation.nextLessonId());
            assertTrue(evaluation.courseCompleted());
        }

        @Test
        void sequentialLearningRequiresPreviousCompletion() {
            LessonProgress first = lesson("v1", LessonType.VIDEO, true, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress second = lesson("v2", LessonType.VIDEO, false, 1, true, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(first, second), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(evaluation.blockers().contains("MANDATORY_LESSON_INCOMPLETE:v1"));
            assertTrue(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:v2"));
            assertEquals("v1", evaluation.nextLessonId());
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        void sequentialLearningAllowsHalfCompletedNonMandatoryLessons() {
            LessonProgress first = lesson("v1", LessonType.VIDEO, false, 0, 60, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress second = lesson("v2", LessonType.VIDEO, false, 1, true, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(first, second), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:v2"));

            LessonProgress low = lesson("v3", LessonType.VIDEO, false, 0, 40, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress follow = lesson("v4", LessonType.VIDEO, false, 1, true, true);
            ProgressEvaluation blocked = service().evaluate(snap(List.of(low, follow), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(blocked.warnings().contains("SEQUENCE_RULE_BYPASSED:v4"));
        }

        @Test
        void lessonsAreSortedByOrderIgnoringInputOrder() {
            LessonProgress later = lesson("late", LessonType.VIDEO, true, 1, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress earlier = lesson("early", LessonType.VIDEO, false, 0, true, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(later, earlier), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:early"));
            assertEquals("late", evaluation.nextLessonId());
        }

        @Test
        void sequentialLockedChainLeavesNoAccessibleNextLesson() {
            LessonProgress lockedFirst = lesson("l1", LessonType.VIDEO, false, 0, 40, 0, 0, 0,
                    false, false, false, 0, 0, false);
            LessonProgress openSecond = lesson("l2", LessonType.VIDEO, false, 1, 40, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(lockedFirst, openSecond), 80, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertNull(evaluation.nextLessonId());
            assertTrue(evaluation.warnings().contains("NO_ACCESSIBLE_NEXT_LESSON"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        void sequentialLockedLessonIsNotReportedAsBypassed() {
            LessonProgress first = lesson("l1", LessonType.VIDEO, false, 0, 40, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress locked = lesson("l2", LessonType.VIDEO, false, 1, 40, 0, 0, 0,
                    false, false, false, 0, 0, false);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(first, locked), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:l2"));
            assertEquals("l1", evaluation.nextLessonId());
        }

        @Test
        void emptyCourseAddsBlockerAndIsNotCompleted() {
            ProgressEvaluation evaluation = service().evaluate(snap(List.of()));
            assertTrue(evaluation.blockers().contains("COURSE_HAS_NO_LESSONS"));
            assertEquals(0, evaluation.overallPercent());
            assertFalse(evaluation.courseCompleted());
            assertNull(evaluation.nextLessonId());
            assertFalse(evaluation.warnings().contains("NO_ACCESSIBLE_NEXT_LESSON"));
        }

        @Test
        void overallBelowRequiredBlocksCompletion() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(video), 80, 0, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(evaluation.blockers().contains("OVERALL_PROGRESS_BELOW_REQUIRED"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        void noAccessibleNextLessonWarnsWhenIncompleteLockedCourse() {
            LessonProgress locked = lesson("v1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, false);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(locked), 80, 0, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(evaluation.warnings().contains("NO_ACCESSIBLE_NEXT_LESSON"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        void finalProjectRequiredBlocksUntilProjectApproved() {
            ProgressEvaluation without = service().evaluate(snap(List.of(lesson("v1", LessonType.VIDEO,
                    false, 0, true, true)), 0, 0, false, true, true, true, false, true, null, null,
                    false, null, true, false, false));
            assertTrue(without.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));
            assertFalse(without.courseCompleted());

            LessonProgress project = lesson("p1", LessonType.PROJECT, false, 1, 100, 0, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation with = service().evaluate(snap(List.of(project), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, true, false, false));
            assertFalse(with.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));
            assertTrue(with.courseCompleted());
        }

        @Test
        void finalProjectGateTracksSubmissionAndApprovalStates() {
            LessonProgress untouched = lesson("p1", LessonType.PROJECT, false, 0, 0, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation unsubmitted = service().evaluate(snap(List.of(untouched), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, true, false, false));
            assertTrue(unsubmitted.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));

            LessonProgress pending = lesson("p2", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, false, false, 0, 0, true);
            ProgressEvaluation awaiting = service().evaluate(snap(List.of(pending), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, true, false, false));
            assertTrue(awaiting.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));

            LessonProgress approved = lesson("p3", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(approved), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, true, false, false));
            assertFalse(done.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));

            LessonProgress plagiarized = lesson("p4", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, true, true, 0, 0, true);
            ProgressEvaluation held = service().evaluate(snap(List.of(plagiarized), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, true, false, false));
            assertTrue(held.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));
        }

        @Test
        void unsubmittedProjectEarnsNothing() {
            LessonProgress untouched = lesson("p1", LessonType.PROJECT, false, 0, 0, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(untouched)));
            assertEquals(0, evaluation.overallPercent());
            assertEquals("p1", evaluation.nextLessonId());
            assertTrue(evaluation.warnings().stream().noneMatch(w -> w.startsWith("PROJECT_HELD")));
        }

        @Test
        void enrollmentInFutureBlocksAccess() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, true, true);
            ProgressEvaluation future = service().evaluate(snap(List.of(video), 0, 0, false, true,
                    true, true, false, true, NOW.plusSeconds(3600), null, false, null, false, false, false));
            assertTrue(future.blockers().contains("ENROLLMENT_DATE_IN_FUTURE"));
            assertTrue(future.courseCompleted());
            ProgressEvaluation past = service().evaluate(snap(List.of(video), 0, 0, false, true,
                    true, true, false, true, NOW.minusSeconds(3600), null, false, null, false, false, false));
            assertFalse(past.blockers().contains("ENROLLMENT_DATE_IN_FUTURE"));
        }

        @Test
        void instructorApprovalRequiredChecksMandatoryAssignments() {
            LessonProgress approved = lesson("a1", LessonType.ASSIGNMENT, true, 0, 100, 0, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation ok = service().evaluate(snap(List.of(approved), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, false, false, true));
            assertFalse(ok.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));

            LessonProgress unapproved = lesson("a2", LessonType.ASSIGNMENT, true, 0, 100, 0, 1, 0,
                    true, false, false, 0, 0, true);
            ProgressEvaluation blocked = service().evaluate(snap(List.of(unapproved), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, false, false, true));
            assertTrue(blocked.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));
            assertFalse(blocked.courseCompleted());

            ProgressEvaluation vacuous = service().evaluate(snap(List.of(lesson("v1", LessonType.VIDEO,
                    false, 0, true, true)), 0, 0, false, true, true, true, false, true, null, null,
                    false, null, false, false, true));
            assertFalse(vacuous.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));

            LessonProgress mandatoryVideo = lesson("v2", LessonType.VIDEO, true, 0, true, true);
            ProgressEvaluation videoOk = service().evaluate(snap(List.of(mandatoryVideo), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, false, false, true));
            assertFalse(videoOk.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));
        }

        @Test
        void accountAccessBlockers() {
            CourseProgressSnapshot unpublished = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, false, null, null, false, null, false, false, false);
            assertTrue(service().evaluate(unpublished).blockers().contains("COURSE_NOT_PUBLISHED"));

            CourseProgressSnapshot suspended = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, true, true, null, null, false, null, false, false, false);
            assertTrue(service().evaluate(suspended).blockers().contains("ACCOUNT_SUSPENDED"));

            CourseProgressSnapshot inactive = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, false, false, true, null, null, false, null, false, false, false);
            assertTrue(service().evaluate(inactive).blockers().contains("SUBSCRIPTION_INACTIVE"));

            CourseProgressSnapshot noConduct = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, false, true, false, true, null, null, false, null, false, false, false);
            assertTrue(service().evaluate(noConduct).blockers().contains("CODE_OF_CONDUCT_NOT_ACCEPTED"));
        }

        @Test
        void enrollmentInFutureBlocks() {
            CourseProgressSnapshot future = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, NOW.plusSeconds(60), null, false, null,
                    false, false, false);
            assertTrue(service().evaluate(future).blockers().contains("ENROLLMENT_DATE_IN_FUTURE"));
        }

        @Test
        void accessExpiryBlocksAndWarnsNearExpiry() {
            CourseProgressSnapshot expired = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, NOW.minusSeconds(1), false, null,
                    false, false, false);
            assertTrue(service().evaluate(expired).blockers().contains("COURSE_ACCESS_EXPIRED"));

            CourseProgressSnapshot soon = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, NOW.plusSeconds(7L * 86400), false,
                    null, false, false, false);
            ProgressEvaluation soonEval = service().evaluate(soon);
            assertTrue(soonEval.warnings().contains("COURSE_ACCESS_EXPIRES_SOON"));
            assertFalse(soonEval.blockers().contains("COURSE_ACCESS_EXPIRED"));

            CourseProgressSnapshot later = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, NOW.plusSeconds(8L * 86400), false,
                    null, false, false, false);
            ProgressEvaluation laterEval = service().evaluate(later);
            assertFalse(laterEval.warnings().contains("COURSE_ACCESS_EXPIRES_SOON"));
        }

        @Test
        void identityAndBalanceWarnings() {
            CourseProgressSnapshot noIdentity = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, false, true, true, false, true, null, null, false, null, false, false, false);
            assertTrue(service().evaluate(noIdentity).warnings().contains("IDENTITY_NOT_VERIFIED"));

            CourseProgressSnapshot balance = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, false, null, false, true, false);
            assertTrue(service().evaluate(balance).warnings().contains("OUTSTANDING_BALANCE"));
        }

        @Test
        void certificateDisabledWarnsAndBlocksEligibility() {
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(lesson("v1", LessonType.VIDEO,
                    false, 0, true, true))));
            assertTrue(evaluation.warnings().contains("CERTIFICATE_DISABLED"));
            assertFalse(evaluation.certificateEligible());
        }

        @Test
        void certificateRequiresCompletion() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(video), 80, 0, false, true,
                    true, true, false, true, null, null, true, "AB", false, false, false));
            assertFalse(evaluation.certificateEligible());
            assertTrue(evaluation.blockers().stream().noneMatch(b -> b.startsWith("CERTIFICATE_")));
        }

        @Test
        void certificateRequiresIdentityVerification() {
            CourseProgressSnapshot snapshot = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, false, true, true, false, true, null, null, true, "AB", false, false, false);
            ProgressEvaluation evaluation = service().evaluate(snapshot);
            assertTrue(evaluation.blockers().contains("CERTIFICATE_REQUIRES_IDENTITY_VERIFICATION"));
            assertFalse(evaluation.certificateEligible());
        }

        @Test
        void certificateBlockedByBalance() {
            CourseProgressSnapshot snapshot = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, "AB", false, true, false);
            ProgressEvaluation evaluation = service().evaluate(snapshot);
            assertTrue(evaluation.blockers().contains("CERTIFICATE_BLOCKED_BY_BALANCE"));
            assertFalse(evaluation.certificateEligible());
        }

        @Test
        void certificateRequiresName() {
            CourseProgressSnapshot noName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, null, false, false, false);
            assertTrue(service().evaluate(noName).blockers().contains("CERTIFICATE_NAME_REQUIRED"));
            CourseProgressSnapshot blankName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, "  ", false, false, false);
            assertTrue(service().evaluate(blankName).blockers().contains("CERTIFICATE_NAME_REQUIRED"));
        }

        @Test
        void certificateNameLengthMustBeWithinBounds() {
            CourseProgressSnapshot shortName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, "A", false, false, false);
            assertTrue(service().evaluate(shortName).blockers().contains("CERTIFICATE_NAME_LENGTH_INVALID"));

            CourseProgressSnapshot longName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true,
                    "N".repeat(81), false, false, false);
            assertTrue(service().evaluate(longName).blockers().contains("CERTIFICATE_NAME_LENGTH_INVALID"));
        }

        @Test
        void certificateEligibleHappyPath() {
            CourseProgressSnapshot snapshot = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, "AB", false, false, false);
            ProgressEvaluation evaluation = service().evaluate(snapshot);
            assertTrue(evaluation.certificateEligible());
            assertTrue(evaluation.blockers().isEmpty());
        }

        @Test
        void videoAtExactlyNinetyPercentCompletesWithoutWarning() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, 90, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(video)));
            assertEquals(1, evaluation.completedLessons());
            assertFalse(evaluation.warnings().contains("VIDEO_NEAR_COMPLETION:v1"));
        }

        @Test
        void videoAtSeventyPercentWarnsAboutNearCompletion() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, 70, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(video)));
            assertTrue(evaluation.warnings().contains("VIDEO_NEAR_COMPLETION:v1"));
        }

        @Test
        void articleCompleteCountsAndHasNoNextLesson() {
            LessonProgress complete = lesson("a1", LessonType.ARTICLE, false, 0, true, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(complete)));
            assertEquals(1, done.completedLessons());
            assertNull(done.nextLessonId());
        }

        @Test
        void quizScoreExactlyAtRequiredPasses() {
            LessonProgress quiz = lesson("q1", LessonType.QUIZ, false, 0, 100, 60, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(quiz), 0, 60, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertEquals(1, evaluation.completedLessons());
            assertFalse(evaluation.warnings().contains("QUIZ_SCORE_TOO_LOW:q1"));
        }

        @Test
        void requiredQuizScoreOfHundredIsValid() {
            LessonProgress quiz = lesson("q1", LessonType.QUIZ, false, 0, 100, 100, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(quiz), 0, 100, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertEquals(1, evaluation.completedLessons());
            assertTrue(evaluation.courseCompleted());
        }

        @Test
        void liveSessionZeroRequiredWithAttendanceCompletes() {
            LessonProgress present = lesson("l1", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 1, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(present)));
            assertEquals(1, evaluation.completedLessons());
        }

        @Test
        void liveSessionAtThresholdCompletes() {
            LessonProgress enough = lesson("l1", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 80, 100, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(enough)));
            assertEquals(1, evaluation.completedLessons());
        }

        @Test
        void liveSessionWithoutAttendanceHasNoWarning() {
            LessonProgress absent = lesson("l1", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 0, 100, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(absent)));
            assertFalse(evaluation.warnings().contains("LIVE_ATTENDANCE_TOO_SHORT:l1"));
        }

        @Test
        void lockedLessonWithZeroProgressHasNoWarning() {
            LessonProgress locked = lesson("v1", LessonType.VIDEO, false, 0, 0, 0, 0, 0,
                    false, false, false, 0, 0, false);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(locked)));
            assertFalse(evaluation.warnings().contains("LOCKED_LESSON_HAS_PROGRESS:v1"));
        }

        @Test
        void sequentialHalfCompletedNonMandatoryLessonAllowsNext() {
            LessonProgress first = lesson("v1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress second = lesson("v2", LessonType.VIDEO, false, 1, true, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(first, second), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:v2"));
        }

        @Test
        void overallExactlyAtRequiredIsNotBlocked() {
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(lesson("v1", LessonType.VIDEO,
                    false, 0, true, true)), 100, 0, false, true, true, true, false, true, null, null,
                    false, null, false, false, false));
            assertFalse(evaluation.blockers().contains("OVERALL_PROGRESS_BELOW_REQUIRED"));
            assertTrue(evaluation.courseCompleted());
        }

        @Test
        void certificateNameOfEightyCharsIsValid() {
            CourseProgressSnapshot snapshot = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true,
                    "N".repeat(80), false, false, false);
            assertTrue(service().evaluate(snapshot).certificateEligible());
        }

        @Test
        void certificateBlockingBranchesReturnFalseEligibility() {
            CourseProgressSnapshot noName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, null, false, false, false);
            assertFalse(service().evaluate(noName).certificateEligible());
            CourseProgressSnapshot shortName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, "A", false, false, false);
            assertFalse(service().evaluate(shortName).certificateEligible());
            CourseProgressSnapshot longName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true,
                    "N".repeat(81), false, false, false);
            assertFalse(service().evaluate(longName).certificateEligible());
        }

        // ----- validation of LearningProgressService -----

        @Test
        void evaluateRequiresSnapshot() {
            assertThrows(NullPointerException.class, () -> service().evaluate(null));
        }

        @Test
        void validateRejectsNullStudentCourseAndLessonIds() {
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(
                    new CourseProgressSnapshot(null, "c1", List.of(), false, 0, 0, true, true, true,
                            false, true, null, null, false, null, false, false, false)));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(
                    new CourseProgressSnapshot("s1", null, List.of(), false, 0, 0, true, true, true,
                            false, true, null, null, false, null, false, false, false)));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(
                    new CourseProgressSnapshot("s1", "c1", List.of(
                            new LessonProgress(null, LessonType.VIDEO, false, 0, 0, 0, 0, 0,
                                    false, false, false, 0, 0, true)), false, 0, 0, true, true, true,
                            false, true, null, null, false, null, false, false, false)));
        }

        // ------------------------------------------------------------------
        // 白盒测试（反射）: 仅用于覆盖公开 API 不可达的私有分支，断言真实行为
        // ------------------------------------------------------------------





        @Test
        void validateRejectsBlankStudentAndCourse() {
            CourseProgressSnapshot blankStudent = new CourseProgressSnapshot("", "c1", List.of(), false, 0, 0,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(blankStudent));
            CourseProgressSnapshot blankCourse = new CourseProgressSnapshot("s1", " ", List.of(), false, 0, 0,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(blankCourse));
        }

        @Test
        void validateRejectsOutOfRangeRequirements() {
            CourseProgressSnapshot lowOverall = new CourseProgressSnapshot("s1", "c1", List.of(), false, -1, 0,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(lowOverall));
            CourseProgressSnapshot highOverall = new CourseProgressSnapshot("s1", "c1", List.of(), false, 101, 0,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(highOverall));
            CourseProgressSnapshot lowQuiz = new CourseProgressSnapshot("s1", "c1", List.of(), false, 0, -1,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(lowQuiz));
            CourseProgressSnapshot highQuiz = new CourseProgressSnapshot("s1", "c1", List.of(), false, 0, 101,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(highQuiz));
        }

        @Test
        void validateRejectsMalformedLessons() {
            List<LessonProgress> nullElement = new ArrayList<>();
            nullElement.add(null);
            assertThrows(NullPointerException.class, () -> new CourseProgressSnapshot("s1", "c1", nullElement,
                    false, 0, 0, true, true, true, false, true, null, null, false, null, false, false, false));

            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("", LessonType.VIDEO, false, 0, true, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, true, true),
                    lesson("v1", LessonType.VIDEO, false, 1, true, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 1, true, true),
                    lesson("v2", LessonType.VIDEO, false, 1, true, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", null, false, 0, true, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, -1, true, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, -1, 0, 0, 0, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 101, 0, 0, 0, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, -1, 0, 0, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 101, 0, 0, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 0, -1, 0, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 0, 0, -1, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 0, 4, 3, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 0, 0, 0, false, false, false, -1, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 0, 0, 0, false, false, false, 0, -1, true)))));
        }
    }

    // ------------------------------------------------------------------
    // Progress service: ExpressionExamProgressService
    // ------------------------------------------------------------------


    private static final Instant NOW = Instant.parse("2026-11-16T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static BigDecimal bd(String v) { return new BigDecimal(v); }
    @SuppressWarnings("unchecked")
    private static <T> T with(T source, Object... changes) {
        try {
            var components = source.getClass().getRecordComponents();
            Class<?>[] types = new Class<?>[components.length];
            Object[] values = new Object[components.length];
            for (int i=0;i<components.length;i++) {
                types[i]=components[i].getType(); values[i]=components[i].getAccessor().invoke(source);
                for(int j=0;j<changes.length;j+=2) if(components[i].getName().equals(changes[j])) values[i]=changes[j+1];
            }
            return (T) source.getClass().getConstructor(types).newInstance(values);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static PriceRequest price() { return new PriceRequest("c",bd("100"),1,MembershipLevel.NONE,null,null,false,false,false,false,0,0,"CN","CNY"); }
    private static Promotion promotion() { return new Promotion("Sale",PromotionType.PERCENT,bd("10"),null,null,false,1,null); }
    private static Coupon coupon() { return new Coupon("SAVE",CouponType.PERCENT,bd("10"),null,null,null,1,true,null,null,0); }
    private static PriceBreakdown priced(PriceRequest r) { return new AdvancedPricingService(CLOCK).calculate(r); }
    @Test void priceMembershipAndBulkBoundaries() {
        MembershipLevel[] levels=MembershipLevel.values(); String[] amounts={"0","3","7","12","10","8"};
        for(int i=0;i<levels.length;i++) { var r=priced(with(price(),"membershipLevel",levels[i])); assertBigDecimalEquals(amounts[i],r.membershipDiscount()); assertBigDecimalEquals("100",r.subtotal()); }
        int[] quantities={1,4,5,19,20,49,50,100}; String[] discounts={"0","0","25","95","200","490","750","1500"};
        for(int i=0;i<quantities.length;i++) assertBigDecimalEquals(discounts[i],priced(with(price(),"quantity",quantities[i])).bulkDiscount());
        assertBigDecimalEquals("180",priced(with(price(),"quantity",10,"membershipLevel",MembershipLevel.ENTERPRISE)).membershipDiscount());
        var free=priced(with(price(),"membershipLevel",MembershipLevel.INSTRUCTOR,"instructorOwnedCourse",true)); assertBigDecimalEquals("0",free.total()); assertTrue(free.appliedRules().contains("FREE_ORDER"));
    }
    @Test void priceTaxesAndOtherDiscounts() {
        String[] countries={"CN","US","DE","SG","GB","FR"}; String[] taxes={"6","7.50","19","9","20","0"};
        for(int i=0;i<countries.length;i++) { var r=priced(with(price(),"countryCode",countries[i])); assertBigDecimalEquals(taxes[i],r.tax()); }
        assertBigDecimalEquals("0",priced(with(price(),"taxExempt",true)).tax());
        assertBigDecimalEquals("2.85",priced(with(price(),"nonprofitCustomer",true)).tax());
        assertBigDecimalEquals("5",priced(with(price(),"nonprofitCustomer",true)).nonprofitDiscount());
        assertBigDecimalEquals("8",priced(with(price(),"nonprofitCustomer",true,"membershipLevel",MembershipLevel.ENTERPRISE)).nonprofitDiscount());
        assertBigDecimalEquals("8",priced(with(price(),"firstPurchase",true)).firstPurchaseDiscount());
        assertBigDecimalEquals("100",priced(with(price(),"firstPurchase",true,"unitPrice",bd("2000"))).firstPurchaseDiscount());
        assertBigDecimalEquals("0",priced(with(price(),"firstPurchase",true,"instructorOwnedCourse",true)).firstPurchaseDiscount());
        assertBigDecimalEquals("0",priced(with(price(),"firstPurchase",true,"coupon",with(coupon(),"stackable",false))).firstPurchaseDiscount());
        for(int points:new int[]{0,99,100,1000,2000,2001}) { String expected=points<100?"0":points==100?"1":points==1000?"10":"20"; assertBigDecimalEquals(expected,priced(with(price(),"loyaltyPointsToUse",points,"availableLoyaltyPoints",10000)).loyaltyDiscount()); }
        assertBigDecimalEquals("1",priced(with(price(),"loyaltyPointsToUse",1000,"availableLoyaltyPoints",100)).loyaltyDiscount());
        var zero=priced(with(price(),"unitPrice",BigDecimal.ZERO,"loyaltyPointsToUse",100,"availableLoyaltyPoints",100)); assertBigDecimalEquals("0",zero.total()); assertEquals(1,zero.warnings().size());
    }
    @Test void pricePromotionsCouponsCapsAndStacking() {
        assertBigDecimalEquals("10",priced(with(price(),"promotion",promotion())).promotionDiscount());
        assertBigDecimalEquals("20",priced(with(price(),"promotion",with(promotion(),"type",PromotionType.FIXED,"value",bd("20")))).promotionDiscount());
        for(int q:new int[]{1,10,20}) assertBigDecimalEquals(q==1?"4":q==10?"80":"240",priced(with(price(),"quantity",q,"promotion",with(promotion(),"type",PromotionType.BUY_MORE_SAVE_MORE))).promotionDiscount());
        assertBigDecimalEquals("5",priced(with(price(),"promotion",with(promotion(),"maximumDiscount",bd("5")))).promotionDiscount());
        for(CouponType t:CouponType.values()) assertBigDecimalEquals(t==CouponType.CASHBACK?"0":"10",priced(with(price(),"coupon",with(coupon(),"type",t))).couponDiscount());
        var cap=priced(with(price(),"promotion",with(promotion(),"value",bd("100")),"coupon",with(coupon(),"value",bd("100")),"firstPurchase",true,"nonprofitCustomer",true));
        assertBigDecimalEquals("70",cap.promotionDiscount()); assertBigDecimalEquals("0",cap.couponDiscount()); assertTrue(cap.appliedRules().contains("DISCOUNT_CAP"));
        var a=priced(with(price(),"promotion",promotion(),"coupon",with(coupon(),"stackable",false))); assertBigDecimalEquals("10",a.promotionDiscount()); assertBigDecimalEquals("0",a.couponDiscount()); assertFalse(a.appliedRules().contains("COUPON_PERCENT"));
        var b=priced(with(price(),"promotion",promotion(),"coupon",with(coupon(),"value",bd("20"),"stackable",false))); assertBigDecimalEquals("0",b.promotionDiscount()); assertBigDecimalEquals("20",b.couponDiscount()); assertFalse(b.appliedRules().contains("PROMOTION_PERCENT"));
    }
    @Test void invalidDiscountConfigurationIsIgnoredWithReason() {
        Object[][] promotions={{"name",null},{"name"," "},{"type",null},{"value",null},{"value",BigDecimal.ZERO},{"value",bd("-1")},{"startsOn",LocalDate.of(2026,11,17)},{"endsOn",LocalDate.of(2026,11,15)},{"membersOnly",true},{"minimumQuantity",2}};
        for(Object[] change:promotions) { var r=priced(with(price(),"promotion",with(promotion(),change))); assertBigDecimalEquals("0",r.promotionDiscount()); assertFalse(r.warnings().isEmpty()); }
        Object[][] coupons={{"code",null},{"code"," "},{"type",null},{"value",null},{"value",BigDecimal.ZERO},{"value",bd("-1")},{"remainingUses",0},{"remainingUses",-1},{"validFrom",LocalDate.of(2026,11,17)},{"validUntil",LocalDate.of(2026,11,15)},{"minimumSubtotal",bd("101")},{"eligibleCourseIds",Set.of("other")},{"maximumQuantity",1},{"minimumMembership",MembershipLevel.BASIC}};
        for(Object[] change:coupons) { var r=priced(with(price(),"quantity",2,"coupon",with(coupon(),change[0].equals("minimumSubtotal")?new Object[]{"minimumSubtotal",bd("201")}:change))); assertBigDecimalEquals("0",r.couponDiscount()); assertFalse(r.warnings().isEmpty()); }
        var today=LocalDate.of(2026,11,16); assertBigDecimalEquals("10",priced(with(price(),"coupon",with(coupon(),"validFrom",today,"validUntil",today,"minimumSubtotal",bd("100"),"maximumQuantity",1,"minimumMembership",MembershipLevel.NONE,"eligibleCourseIds",Set.of("c")))).couponDiscount());
    }
    @Test void priceRejectsMalformedRequests() {
        assertThrows(NullPointerException.class,()->priced(null));
        Object[][] bad={{"courseId",null},{"courseId"," "},{"unitPrice",null},{"unitPrice",bd("-0.01")},{"unitPrice",bd("100000")},{"quantity",0},{"quantity",101},{"membershipLevel",null},{"currency",null},{"currency"," "},{"currency","JPY"},{"countryCode",null},{"countryCode"," "},{"loyaltyPointsToUse",-1},{"availableLoyaltyPoints",-1}};
        for(Object[] c:bad) assertThrows(RuntimeException.class,()->priced(with(price(),c)));
        assertBigDecimalEquals("99999.99",priced(with(price(),"unitPrice",bd("99999.99"))).subtotal());
    }


    private static PurchaseSnapshot purchase() { return new PurchaseSnapshot("p","s","c",bd("100"),bd("6"),BigDecimal.ZERO,NOW.minusSeconds(3600),PaymentMethod.CARD,0,0,false,0,0,false,false,0,false,false,false,false,null); }
    private static RefundRequest refund() { return new RefundRequest(purchase(),RefundReason.ACCIDENTAL_PURCHASE,true,0,false,false,false,false,false,false,false,false); }
    private static RefundDecision refunded(RefundRequest r) { return new RefundPolicyService(CLOCK).evaluate(r); }
    @Test void refundTimeAndUsageBoundaries() {
        int[][] cases={{2,0,100},{3,0,100},{24,5,100},{25,5,100},{168,10,100},{169,10,50},{168,11,70},{168,30,70},{169,30,50},{336,30,50},{337,15,25},{720,15,25},{721,15,0},{720,16,0},{100,80,0},{100,79,0}};
        for(int[] c:cases) { var result=refunded(with(refund(),"purchase",with(purchase(),"paidAt",NOW.minusSeconds(c[0]*3600L),"progressPercent",c[1],"paymentMethod",PaymentMethod.WALLET))); assertBigDecimalEquals(String.valueOf(c[2]),result.refundableAmount()); assertEquals(c[2]>0,result.approved()); assertFalse(result.manualReview()); }
        var reduced=refunded(with(refund(),"fullRefundRequested",false,"requestedPercentage",25)); assertBigDecimalEquals("25",reduced.refundableAmount());
        assertBigDecimalEquals("100",refunded(with(refund(),"fullRefundRequested",false,"requestedPercentage",0)).refundableAmount());
    }
    @Test void refundSpecialReasonsAndEarlyExits() {
        Object[][] states={{"previouslyRefunded",true,"PURCHASE_ALREADY_REFUNDED"},{"paidAt",NOW.plusSeconds(3600),"PURCHASE_DATE_IN_FUTURE"}};
        for(Object[] c:states) { var r=refunded(with(refund(),"purchase",with(purchase(),c[0],c[1]))); assertFalse(r.approved()); assertEquals(List.of(c[2]),r.reasons()); assertEquals("NONE",r.refundRoute()); }
        assertTrue(refunded(with(refund(),"purchase",with(purchase(),"chargebackOpen",true))).manualReview());
        assertTrue(refunded(with(refund(),"fraudSuspected",true)).manualReview());
        assertTrue(refunded(with(refund(),"reason",RefundReason.FRAUD)).manualReview());
        for(String flag:new String[]{"instructorCancelled","courseRemoved"}) assertBigDecimalEquals("100",refunded(with(refund(),"purchase",with(purchase(),flag,true,"certificateIssued",true))).refundableAmount());
        for(RefundReason reason:new RefundReason[]{RefundReason.DUPLICATE_PAYMENT,RefundReason.TECHNICAL_ISSUE,RefundReason.MEDICAL}) assertFalse(refunded(with(refund(),"reason",reason)).approved());
        for(RefundReason reason:new RefundReason[]{RefundReason.DUPLICATE_PAYMENT,RefundReason.MEDICAL}) assertBigDecimalEquals("50",refunded(with(refund(),"reason",reason,"evidenceProvided",true)).refundableAmount());
        assertBigDecimalEquals("100",refunded(with(refund(),"reason",RefundReason.TECHNICAL_ISSUE,"evidenceProvided",true)).refundableAmount());
        Object[][] exceptional={{RefundReason.TECHNICAL_ISSUE,"technicalIssueConfirmed",50,"100"},{RefundReason.TECHNICAL_ISSUE,"technicalIssueConfirmed",51,"75"},{RefundReason.MEDICAL,"medicalDocumentVerified",79,"100"},{RefundReason.MEDICAL,"medicalDocumentVerified",80,"40"},{RefundReason.CONTENT_QUALITY,"contentComplaintUpheld",59,"100"},{RefundReason.CONTENT_QUALITY,"contentComplaintUpheld",60,"60"},{RefundReason.DUPLICATE_PAYMENT,"duplicatePaymentConfirmed",100,"100"}};
        for(Object[] c:exceptional) assertBigDecimalEquals((String)c[3],refunded(with(refund(),"reason",c[0],c[1],true,"purchase",with(purchase(),"progressPercent",c[2]))).refundableAmount());
        for(Object[] c:new Object[][]{{"certificateIssued",true},{"downloadedResources",10},{"progressPercent",80}}) assertFalse(refunded(with(refund(),"purchase",with(purchase(),c))).approved());
        var override=refunded(with(refund(),"adminOverride",true,"fullRefundRequested",false,"purchase",with(purchase(),"certificateIssued",true))); assertBigDecimalEquals("50",override.refundableAmount()); assertTrue(override.warnings().contains("ADMIN_OVERRIDE_APPLIED"));
        assertBigDecimalEquals("100",refunded(with(refund(),"adminOverride",true,"purchase",with(purchase(),"certificateIssued",true))).refundableAmount());
    }
    @Test void refundFeesRoutesCouponsAndPoints() {
        PaymentMethod[] methods=PaymentMethod.values(); String[] fees={"3","1","1","2","0","0","5"}; String[] routes={"ORIGINAL_PAYMENT_METHOD","ORIGINAL_PAYMENT_METHOD","ORIGINAL_PAYMENT_METHOD","BANK_TRANSFER","WALLET_CREDIT","WALLET_CREDIT","MANUAL_REVIEW"};
        for(int i=0;i<methods.length;i++) { var r=refunded(with(refund(),"bankDetailsPresent",true,"purchase",with(purchase(),"paymentMethod",methods[i],"paidAt",NOW.minusSeconds(25*3600)))); assertBigDecimalEquals(fees[i],r.processingFee()); assertEquals(routes[i],r.refundRoute()); assertEquals(methods[i]==PaymentMethod.CRYPTO,r.manualReview()); }
        assertTrue(refunded(with(refund(),"purchase",with(purchase(),"paymentMethod",PaymentMethod.BANK_TRANSFER))).manualReview());
        for(int attended:new int[]{0,1,2,4,5,10}) { var r=refunded(with(refund(),"purchase",with(purchase(),"paidAt",NOW.minusSeconds(25*3600),"liveSessionsAttended",attended,"totalLiveSessions",10,"downloadedResources",1))); assertBigDecimalEquals(attended<2?"5":attended<5?"10":"15",r.processingFee()); }
        var couponResult=refunded(with(refund(),"purchase",with(purchase(),"couponUsed",true,"discountAmount",bd("10"),"loyaltyPointsUsed",101))); assertBigDecimalEquals("90",couponResult.refundableAmount()); assertEquals(50,couponResult.loyaltyPointsReturned());
        var provider=refunded(with(refund(),"reason",RefundReason.COURSE_CANCELLED,"purchase",with(purchase(),"couponUsed",true,"discountAmount",bd("200"),"loyaltyPointsUsed",101))); assertBigDecimalEquals("100",provider.refundableAmount()); assertEquals(101,provider.loyaltyPointsReturned());
        assertFalse(refunded(with(refund(),"purchase",with(purchase(),"couponUsed",true,"discountAmount",bd("100")))).approved());
        var proportional=refunded(with(refund(),"fullRefundRequested",false,"requestedPercentage",33,"purchase",with(purchase(),"loyaltyPointsUsed",101))); assertEquals(33,proportional.loyaltyPointsReturned());
        assertFalse(refunded(with(refund(),"purchase",with(purchase(),"paidAmount",bd("0.99")))).approved());
        assertBigDecimalEquals("0.99",refunded(with(refund(),"purchase",with(purchase(),"paidAmount",bd("0.99"),"paymentMethod",PaymentMethod.WALLET))).refundableAmount());
        assertBigDecimalEquals("1",refunded(with(refund(),"purchase",with(purchase(),"paidAmount",bd("1")))).refundableAmount());
        assertTrue(refunded(with(refund(),"purchase",with(purchase(),"accessExpiresAt",NOW.minusSeconds(1)))).warnings().contains("COURSE_ACCESS_EXPIRED"));
    }
    @Test void refundValidationMatrix() {
        assertThrows(NullPointerException.class,()->refunded(null)); assertThrows(NullPointerException.class,()->refunded(with(refund(),"purchase",null))); assertThrows(NullPointerException.class,()->refunded(with(refund(),"reason",null)));
        Object[][] bad={{"purchaseId",null},{"purchaseId"," "},{"studentId",null},{"studentId"," "},{"courseId",null},{"courseId"," "},{"paidAmount",null},{"paidAmount",bd("-1")},{"taxAmount",null},{"taxAmount",bd("-1")},{"discountAmount",null},{"discountAmount",bd("-1")},{"paidAt",null},{"paymentMethod",null},{"progressPercent",-1},{"progressPercent",101},{"downloadedResources",-1},{"liveSessionsAttended",-1},{"totalLiveSessions",-1},{"liveSessionsAttended",1},{"loyaltyPointsUsed",-1}};
        for(Object[] c:bad) assertThrows(RuntimeException.class,()->refunded(with(refund(),"purchase",with(purchase(),c))));
        for(int n:new int[]{-1,101}) assertThrows(IllegalArgumentException.class,()->refunded(with(refund(),"requestedPercentage",n)));
    }
    private static StudentProfile student() { return new StudentProfile("s",18,"CN","zh",MembershipLevel.NONE,true,false,true,true,true,false,false,true,true,0); }
    private static CourseOffering offering() { return new CourseOffering("c","i",EnrollmentCourseStatus.PUBLISHED,null,null,null,10,0,false,0,0,0,0,null,null,null,0,0,0,false,null,false,false,null,0,0,bd("100")); }
    private static EnrollmentRequest enrollment() { return new EnrollmentRequest(student(),offering(),1,true,false,false,false,null,false,false); }
    private static EnrollmentDecision enrolled(EnrollmentRequest r) { return new EnrollmentEligibilityService(new EmptyEnrollmentHistory(),CLOCK).evaluate(r); }
    @Test void enrollmentBaselinePaymentsAndRisks() {
        var normal=enrolled(enrollment()); assertTrue(normal.accepted()); assertEquals(EnrollmentOutcome.ENROLLED,normal.outcome()); assertEquals(10,normal.availableSeats()); assertTrue(normal.paymentRequired()); assertEquals(List.of(),normal.reasons()); assertEquals(List.of(),normal.warnings());
        assertEquals(List.of("PAYMENT_NOT_AUTHORIZED"),enrolled(with(enrollment(),"paymentAuthorized",false)).reasons());
        for(String flag:new String[]{"auditMode","scholarshipApproved","organizationSponsored"}) assertFalse(enrolled(with(enrollment(),flag,true)).paymentRequired());
        for(BigDecimal amount:new BigDecimal[]{null,BigDecimal.ZERO,bd("-1")}) assertFalse(enrolled(with(enrollment(),"course",with(offering(),"price",amount))).paymentRequired());
        int[] risks={0,49,50,69,70,89,90,100};
        for(int risk:risks) { var r=enrolled(with(enrollment(),"student",with(student(),"riskScore",risk))); assertEquals(risk>=90?EnrollmentOutcome.REJECTED:risk>=70?EnrollmentOutcome.MANUAL_REVIEW:EnrollmentOutcome.ENROLLED,r.outcome()); if(risk>=50&&risk<70) assertTrue(r.warnings().contains("ELEVATED_RISK_MONITORING")); }
    }
    @Test void enrollmentStudentAndCourseBlockers() {
        Object[][] cases={{"accountActive",false,"ACCOUNT_INACTIVE"},{"suspended",true,"ACCOUNT_SUSPENDED"},{"emailVerified",false,"EMAIL_NOT_VERIFIED"},{"profileComplete",false,"PROFILE_INCOMPLETE"},{"termsAccepted",false,"TERMS_NOT_ACCEPTED"},{"age",17,"PARENTAL_CONSENT_REQUIRED"},{"age",-1,"INVALID_STUDENT_AGE"},{"age",121,"INVALID_STUDENT_AGE"},{"countryCode",null,"COUNTRY_MISSING"},{"countryCode"," ","COUNTRY_MISSING"}};
        for(Object[] c:cases) { var r=enrolled(with(enrollment(),"student",with(student(),c[0],c[1]))); assertEquals(EnrollmentOutcome.REJECTED,r.outcome()); assertTrue(r.reasons().contains(c[2])); }
        Object[][] courses={{"status",null,"COURSE_STATUS_MISSING"},{"status",EnrollmentCourseStatus.DRAFT,"COURSE_NOT_PUBLISHED"},{"status",EnrollmentCourseStatus.CLOSED,"COURSE_CLOSED"},{"status",EnrollmentCourseStatus.ARCHIVED,"COURSE_ARCHIVED"},{"enrollmentOpensAt",NOW.plusSeconds(1),"ENROLLMENT_NOT_OPEN"},{"enrollmentClosesAt",NOW.minusSeconds(1),"ENROLLMENT_CLOSED"},{"startsAt",NOW.minusSeconds(1),"COURSE_ALREADY_STARTED"},{"minimumAge",19,"BELOW_MINIMUM_AGE"},{"maximumAge",17,"ABOVE_MAXIMUM_AGE"},{"allowedCountries",Set.of("US"),"COUNTRY_NOT_ALLOWED"},{"requiredLanguage","en","LANGUAGE_REQUIREMENT_NOT_MET"},{"requiredOrganizationId","org","ORGANIZATION_NOT_ALLOWED"},{"liveCourse",true,"LIVE_COURSE_WITHOUT_SCHEDULE"}};
        for(Object[] c:courses) assertTrue(enrolled(with(enrollment(),"course",with(offering(),c[0],c[1]))).reasons().contains(c[2]));
        assertEquals(EnrollmentOutcome.ENROLLED,enrolled(with(enrollment(),"course",with(offering(),"enrollmentOpensAt",NOW,"enrollmentClosesAt",NOW,"startsAt",NOW,"minimumAge",18,"maximumAge",18,"allowedCountries",Set.of("CN"),"requiredLanguage","ZH"))).outcome());
        assertTrue(enrolled(with(enrollment(),"auditMode",true,"course",with(offering(),"status",EnrollmentCourseStatus.ARCHIVED,"startsAt",NOW.minusSeconds(1)))).warnings().containsAll(List.of("ARCHIVED_COURSE_AUDIT_ONLY","LATE_AUDIT_ENROLLMENT")));
        assertTrue(enrolled(with(enrollment(),"student",with(student(),"identityVerified",false))).warnings().contains("IDENTITY_NOT_VERIFIED"));
        assertTrue(enrolled(with(enrollment(),"student",with(student(),"identityVerified",false),"course",with(offering(),"liveCourse",true))).reasons().contains("IDENTITY_REQUIRED_FOR_LIVE_COURSE"));
        assertTrue(enrolled(with(enrollment(),"student",with(student(),"instructor",true),"course",with(offering(),"instructorId","s"))).reasons().contains("INSTRUCTOR_CANNOT_ENROLL_IN_OWN_COURSE"));
    }
    @Test void enrollmentCapacityWaitlistAndAccessibility() {
        var full=with(offering(),"enrolledCount",10); assertEquals(List.of("COURSE_FULL"),enrolled(with(enrollment(),"course",full)).reasons());
        var wait=with(full,"waitlistEnabled",true,"waitlistCapacity",2);
        assertEquals(List.of("WAITLIST_NOT_ACCEPTED"),enrolled(with(enrollment(),"course",wait)).reasons());
        var req=with(enrollment(),"course",wait,"acceptWaitlist",true); var r=enrolled(req); assertEquals(EnrollmentOutcome.WAITLISTED,r.outcome()); assertTrue(r.accepted()); assertFalse(r.paymentRequired());
        assertEquals(List.of("WAITLIST_DISABLED_BY_CAPACITY"),enrolled(with(req,"course",with(wait,"waitlistCapacity",0))).reasons());
        assertEquals(List.of("WAITLIST_FULL"),enrolled(with(req,"course",with(wait,"waitlistedCount",2))).reasons());
        assertEquals(List.of("GROUP_ENROLLMENT_CANNOT_WAITLIST"),enrolled(with(req,"requestedSeats",2,"organizationSponsored",true)).reasons());
        assertEquals(EnrollmentOutcome.MANUAL_REVIEW,enrolled(with(req,"student",with(student(),"riskScore",70))).outcome());
        assertTrue(enrolled(with(enrollment(),"requestedSeats",2)).reasons().contains("GROUP_ENROLLMENT_REQUIRES_ENTERPRISE"));
        assertTrue(enrolled(with(enrollment(),"accessibilitySeatRequired",true)).reasons().contains("NO_ACCESSIBILITY_SEATS"));
        assertTrue(enrolled(with(enrollment(),"accessibilitySeatRequired",true,"course",with(offering(),"accessibilitySeatCapacity",1,"accessibilitySeatsUsed",1))).reasons().contains("ACCESSIBILITY_SEATS_FULL"));
        assertTrue(enrolled(with(enrollment(),"accessibilitySeatRequired",true,"course",with(offering(),"accessibilitySeatCapacity",1))).warnings().contains("ACCESSIBILITY_SEAT_RESERVED"));
        assertTrue(enrolled(with(enrollment(),"course",with(offering(),"accessibilitySeatsUsed",1))).warnings().contains("ACCESSIBILITY_CAPACITY_CONFIGURATION_INVALID"));
        for(int seats:new int[]{2,3,4}) assertEquals(seats<=3,enrolled(with(enrollment(),"course",with(offering(),"capacity",seats))).warnings().contains("COURSE_ALMOST_FULL"));
    }
    @Test void enrollmentExternalHistoryAndPrerequisiteScores() {
        EnrollmentHistory history=mock(EnrollmentHistory.class);
        var service=new EnrollmentEligibilityService(history,CLOCK);
        when(history.alreadyEnrolled("s","c")).thenReturn(true); assertTrue(service.evaluate(enrollment()).reasons().contains("ALREADY_ENROLLED")); when(history.alreadyEnrolled("s","c")).thenReturn(false);
        when(history.hasRefundAbuseFlag("s")).thenReturn(true); assertEquals(EnrollmentOutcome.MANUAL_REVIEW,service.evaluate(enrollment()).outcome()); when(history.hasRefundAbuseFlag("s")).thenReturn(false);
        var prereq=with(enrollment(),"course",with(offering(),"prerequisiteCourseIds",Set.of("pre"),"minimumPrerequisiteScore",60));
        assertTrue(service.evaluate(prereq).reasons().contains("MISSING_PREREQUISITE:pre")); when(history.completedCourse("s","pre")).thenReturn(true);
        for(int score:new int[]{-1,0,59,60,94,95,100,101}) { when(history.completionScore("s","pre")).thenReturn(score); var r=service.evaluate(prereq); assertEquals(score>=60&&score<=100,r.accepted()); assertEquals(score>=95&&score<=100,r.warnings().contains("EXCELLENT_PREREQUISITE_SCORE:pre")); }
        for(int attempts:new int[]{-1,0,1,2}) { when(history.failedEnrollmentAttempts("s","c")).thenReturn(attempts); var req=with(enrollment(),"course",with(offering(),"maximumFailedAttempts",2)); if(attempts<0) assertThrows(IllegalStateException.class,()->service.evaluate(req)); else assertEquals(attempts<2,service.evaluate(req).accepted()); } when(history.failedEnrollmentAttempts("s","c")).thenReturn(0);
        for(int active:new int[]{-1,0,1,2,3}) { when(history.activeCourseCountThisWeek("s")).thenReturn(active); var req=with(enrollment(),"course",with(offering(),"maximumWeeklyCourses",2)); if(active<0) assertThrows(IllegalStateException.class,()->service.evaluate(req)); else { assertEquals(active<2,service.evaluate(req).accepted()); assertEquals(active<=2,service.evaluate(with(req,"student",with(student(),"membershipLevel",MembershipLevel.PREMIUM))).accepted()); assertTrue(service.evaluate(with(req,"student",with(student(),"membershipLevel",MembershipLevel.ENTERPRISE),"organizationSponsored",true)).accepted()); } }
        when(history.activeCourseCountThisWeek("s")).thenReturn(0); when(history.hasScheduleConflict("s","slot")).thenReturn(true);
        var live=with(enrollment(),"course",with(offering(),"liveCourse",true,"scheduleKey","slot")); assertTrue(service.evaluate(live).reasons().contains("SCHEDULE_CONFLICT")); assertTrue(service.evaluate(with(live,"auditMode",true)).accepted());
    }
    @Test void enrollmentOrganizationsAndValidation() {
        var req=with(enrollment(),"course",with(offering(),"organizationOnly",true,"subscriptionRequired",true),"student",with(student(),"activeSubscription",false));
        assertTrue(enrolled(req).reasons().containsAll(List.of("ORGANIZATION_SPONSOR_REQUIRED","ORGANIZATION_ID_REQUIRED","ACTIVE_SUBSCRIPTION_REQUIRED")));
        assertTrue(enrolled(with(req,"organizationSponsored",true,"organizationId","org")).accepted());
        assertTrue(enrolled(with(enrollment(),"scholarshipApproved",true,"organizationSponsored",true)).warnings().contains("SCHOLARSHIP_AND_SPONSORSHIP_BOTH_PRESENT"));
        assertTrue(enrolled(with(enrollment(),"student",with(student(),"activeSubscription",false),"course",with(offering(),"subscriptionRequired",true),"scholarshipApproved",true)).warnings().contains("SUBSCRIPTION_COVERED_BY_SCHOLARSHIP"));
        for(Object[] c:new Object[][]{{"studentId",null},{"studentId"," "},{"riskScore",-1},{"riskScore",101}}) assertThrows(IllegalArgumentException.class,()->enrolled(with(enrollment(),"student",with(student(),c))));
        for(Object[] c:new Object[][]{{"courseId",null},{"courseId"," "},{"capacity",-1},{"enrolledCount",-1},{"enrollmentOpensAt",NOW.plusSeconds(1),"enrollmentClosesAt",NOW},{"minimumAge",20,"maximumAge",10}}) assertThrows(IllegalArgumentException.class,()->enrolled(with(enrollment(),"course",with(offering(),c))));
        for(int seats:new int[]{0,51}) assertThrows(IllegalArgumentException.class,()->enrolled(with(enrollment(),"requestedSeats",seats)));
        assertThrows(NullPointerException.class,()->enrolled(null)); assertThrows(NullPointerException.class,()->enrolled(with(enrollment(),"student",null))); assertThrows(NullPointerException.class,()->enrolled(with(enrollment(),"course",null)));
    }

    @Test
    void coursePriceBoundariesAndDiscountRounding() {
        var policy = new net.mooctest.course.service.PricePolicy();
        for (BigDecimal invalid : new BigDecimal[]{null, new BigDecimal("-0.01"), new BigDecimal("100000"), new BigDecimal("99999.991")}) {
            var ex = assertThrows(net.mooctest.error.BusinessException.class, () -> policy.normalizePrice(invalid));
            assertEquals(net.mooctest.error.ErrorCode.INVALID_PRICE, ex.code());
            assertEquals("Price must be between 0 and 99999.99", ex.getMessage());
        }
        assertEquals(new BigDecimal("0.00"), policy.normalizePrice(BigDecimal.ZERO));
        assertEquals(new BigDecimal("99999.99"), policy.normalizePrice(new BigDecimal("99999.99")));
        assertEquals(new BigDecimal("1.24"), policy.normalizePrice(new BigDecimal("1.235")));
        assertEquals(new BigDecimal("12.34"), policy.discountedPrice(new BigDecimal("12.34"), 0));
        assertEquals(new BigDecimal("2.47"), policy.discountedPrice(new BigDecimal("12.34"), 80));
        assertEquals(new BigDecimal("9.26"), policy.discountedPrice(new BigDecimal("12.34"), 25));
        for (int invalid : new int[]{-1, 81}) {
            var ex = assertThrows(net.mooctest.error.BusinessException.class, () -> policy.discountedPrice(BigDecimal.ONE, invalid));
            assertEquals(net.mooctest.error.ErrorCode.INVALID_DISCOUNT, ex.code());
            assertEquals("Discount percent must be between 0 and 80", ex.getMessage());
        }
    }

    @Test
    void courseRecordNormalizesAndPublishesOnlyDraft() {
        var time = Instant.parse("2026-01-01T00:00:00Z");
        var course = new net.mooctest.course.domain.Course(" id ", " Title ", new BigDecimal("1.235"), net.mooctest.course.domain.CourseStatus.DRAFT, time);
        assertEquals("id", course.id());
        assertEquals("Title", course.title());
        assertEquals(new BigDecimal("1.24"), course.price());
        assertEquals(time, course.createdAt());
        var published = course.publish();
        assertEquals(net.mooctest.course.domain.CourseStatus.DRAFT, course.status());
        assertEquals(net.mooctest.course.domain.CourseStatus.PUBLISHED, published.status());
        assertEquals(course.id(), published.id());
        assertEquals(course.title(), published.title());
        assertEquals(course.price(), published.price());
        assertEquals(time, published.createdAt());
        for (var status : new net.mooctest.course.domain.CourseStatus[]{net.mooctest.course.domain.CourseStatus.PUBLISHED, net.mooctest.course.domain.CourseStatus.ARCHIVED}) {
            var other = new net.mooctest.course.domain.Course("a", "title", BigDecimal.ZERO, status, time);
            var ex = assertThrows(net.mooctest.error.BusinessException.class, other::publish);
            assertEquals(net.mooctest.error.ErrorCode.COURSE_ALREADY_PUBLISHED, ex.code());
            assertEquals("Only a draft course can be published", ex.getMessage());
        }
        for (String invalid : new String[]{null, "", " \t "}) {
            Class<? extends Throwable> expected = invalid == null ? NullPointerException.class : IllegalArgumentException.class;
            assertThrows(expected, () -> new net.mooctest.course.domain.Course(invalid, "title", BigDecimal.ZERO, net.mooctest.course.domain.CourseStatus.DRAFT, time));
            assertThrows(expected, () -> new net.mooctest.course.domain.Course("id", invalid, BigDecimal.ZERO, net.mooctest.course.domain.CourseStatus.DRAFT, time));
        }
        assertThrows(NullPointerException.class, () -> new net.mooctest.course.domain.Course("id", "title", null, net.mooctest.course.domain.CourseStatus.DRAFT, time));
        assertThrows(NullPointerException.class, () -> new net.mooctest.course.domain.Course("id", "title", BigDecimal.ZERO, null, time));
        assertThrows(NullPointerException.class, () -> new net.mooctest.course.domain.Course("id", "title", BigDecimal.ZERO, net.mooctest.course.domain.CourseStatus.DRAFT, null));
    }

    @Test
    void courseCreationRejectsDuplicateAndInvalidTitles() {
        var repository = new net.mooctest.course.adapter.InMemoryCourseRepository();
        var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        var service = new net.mooctest.course.service.CourseService(repository, id -> List.of(), () -> "course-1", new net.mooctest.course.service.PricePolicy(), clock);
        assertFalse(repository.existsByTitleIgnoreCase("absent"));
        var course = service.createCourse("  Linear   Algebra  ", new BigDecimal("25.125"));
        assertEquals("Linear Algebra", course.title());
        assertEquals("course-1", course.id());
        assertEquals(new BigDecimal("25.13"), course.price());
        assertEquals(clock.instant(), course.createdAt());
        assertSame(course, service.getCourse("course-1"));
        assertTrue(repository.existsByTitleIgnoreCase("LINEAR ALGEBRA"));
        assertFalse(repository.existsByTitleIgnoreCase("geometry"));
        assertEquals(net.mooctest.error.ErrorCode.DUPLICATE_COURSE_TITLE, assertThrows(net.mooctest.error.BusinessException.class, () -> service.createCourse("linear algebra", BigDecimal.ONE)).code());
        var missing = assertThrows(net.mooctest.error.BusinessException.class, () -> service.getCourse("absent"));
        assertEquals(net.mooctest.error.ErrorCode.COURSE_NOT_FOUND, missing.code());
        assertEquals("Course not found: absent", missing.getMessage());
        assertEquals(net.mooctest.error.ErrorCode.COURSE_HAS_NO_CHAPTERS, assertThrows(net.mooctest.error.BusinessException.class, () -> service.publishCourse("course-1")).code());
        assertEquals(net.mooctest.course.domain.CourseStatus.DRAFT, service.getCourse("course-1").status());
        for (String invalid : new String[]{null, "", "  ", "ab", "a".repeat(81)}) {
            var ex = assertThrows(net.mooctest.error.BusinessException.class, () -> service.createCourse(invalid, BigDecimal.ONE));
            assertEquals(net.mooctest.error.ErrorCode.INVALID_COURSE_TITLE, ex.code());
            assertEquals("Course title length must be between 3 and 80 characters", ex.getMessage());
        }
        assertEquals("abc", service.createCourse("abc", BigDecimal.ZERO).title());
        assertEquals("a".repeat(80), service.createCourse("a".repeat(80), BigDecimal.ZERO).title());
    }

    @Test
    void courseControllerAndCatalogPreserveSortedContent() {
        var repository = new net.mooctest.course.adapter.InMemoryCourseRepository();
        net.mooctest.course.port.ChapterRepository chapters = id -> List.of(
                new net.mooctest.course.domain.Chapter("c3", id, "zeta", 1),
                new net.mooctest.course.domain.Chapter("c2", id, "Alpha", 1),
                new net.mooctest.course.domain.Chapter("c1", id, "first", 0));
        net.mooctest.course.port.VideoRepository videos = ids -> {
            assertEquals(java.util.Set.of("c1", "c2", "c3"), ids);
            return List.of(new net.mooctest.course.domain.Video("v3", "c1", "zeta", 30, 1),
                    new net.mooctest.course.domain.Video("v2", "c1", "Alpha", 20, 1),
                    new net.mooctest.course.domain.Video("v1", "c1", "first", 10, 0));
        };
        var service = new net.mooctest.course.service.CourseService(repository, chapters, () -> "course", new net.mooctest.course.service.PricePolicy(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        var catalog = new net.mooctest.course.service.CatalogService(service, chapters, videos);
        var controller = new net.mooctest.course.web.CourseController(service, catalog);
        var response = controller.create(new net.mooctest.course.web.CreateCourseRequest("Algebra", new BigDecimal("10")));
        assertEquals(201, response.getStatusCode().value());
        assertEquals(java.net.URI.create("/api/courses/course"), response.getHeaders().getLocation());
        assertTrue(response.getBody().success());
        assertNull(response.getBody().error());
        assertEquals("Algebra", response.getBody().data().title());
        assertEquals(response.getBody(), controller.get("course"));
        var published = controller.publish("course");
        assertTrue(published.success());
        assertEquals(net.mooctest.course.domain.CourseStatus.PUBLISHED, published.data().status());
        assertEquals(published.data(), service.getCourse("course"));
        var result = controller.catalog("course");
        assertTrue(result.success());
        assertNull(result.error());
        assertEquals("course", result.data().courseId());
        assertEquals(List.of("c1", "c2", "c3"), result.data().chapters().stream().map(net.mooctest.course.domain.ChapterView::id).toList());
        assertEquals("first", result.data().chapters().get(0).title());
        assertEquals(List.of(new net.mooctest.course.domain.VideoView("v1", "first", 10), new net.mooctest.course.domain.VideoView("v2", "Alpha", 20), new net.mooctest.course.domain.VideoView("v3", "zeta", 30)), result.data().chapters().get(0).videos());
        assertEquals(List.of(), result.data().chapters().get(1).videos());
        assertThrows(UnsupportedOperationException.class, () -> result.data().chapters().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.data().chapters().get(0).videos().clear());
        var emptyCatalog = new net.mooctest.course.service.CatalogService(service, new net.mooctest.course.adapter.InMemoryChapterRepository(), new net.mooctest.course.adapter.InMemoryVideoRepository());
        assertEquals(List.of(), emptyCatalog.getCatalog("course").chapters());
        assertEquals(List.of(), new net.mooctest.course.adapter.InMemoryVideoRepository().findByChapterIds(java.util.Set.of("c")));
        var ids = new net.mooctest.course.adapter.UuidGenerator();
        assertEquals(4, java.util.UUID.fromString(ids.nextId()).version());
    }

    @Test
    void restErrorsExposeStatusCodeAndMessage() throws Exception {
        var handler = new net.mooctest.course.web.RestExceptionHandler();
        var codes = net.mooctest.error.ErrorCode.values();
        int[] statuses = {404, 409, 400, 400, 400, 409, 422};
        for (int i = 0; i < codes.length; i++) {
            var response = handler.handleBusinessException(new net.mooctest.error.BusinessException(codes[i], "detail"));
            assertEquals(statuses[i], response.getStatusCode().value());
            assertFalse(response.getBody().success());
            assertNull(response.getBody().data());
            assertEquals(codes[i].name(), response.getBody().error().code());
            assertEquals("detail", response.getBody().error().message());
        }
        assertThrows(NullPointerException.class, () -> new net.mooctest.error.BusinessException(null, "detail"));
        var binding = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "request");
        var parameter = new org.springframework.core.MethodParameter(net.mooctest.course.web.CourseController.class.getMethod("create", net.mooctest.course.web.CreateCourseRequest.class), 0);
        var exception = new org.springframework.web.bind.MethodArgumentNotValidException(parameter, binding);
        var empty = handler.handleValidation(exception);
        assertEquals(400, empty.getStatusCode().value());
        assertEquals("Request validation failed", empty.getBody().error().message());
        binding.addError(new org.springframework.validation.FieldError("request", "title", "required"));
        binding.addError(new org.springframework.validation.FieldError("request", "price", "negative"));
        var error = handler.handleValidation(exception);
        assertEquals(400, error.getStatusCode().value());
        assertFalse(error.getBody().success());
        assertNull(error.getBody().data());
        assertEquals("VALIDATION_ERROR", error.getBody().error().code());
        assertEquals("title: required", error.getBody().error().message());
        var method = net.mooctest.config.TimeConfiguration.class.getDeclaredMethod("systemClock");
        method.setAccessible(true);
        var clock = (Clock) method.invoke(new net.mooctest.config.TimeConfiguration());
        assertEquals(ZoneOffset.UTC, clock.getZone());
    }


    private static ScheduleRequest schedule() { return new ScheduleRequest("c","co","i",DeliveryMode.ONLINE,CourseKind.LECTURE,NOW.plusSeconds(34*3600),NOW.plusSeconds(35*3600),"UTC",null,"https://meet",20,20,30,null,null,false,true,false,false,false,false,1,0,5,null,null,null,null,false,false,false); }
    private static SchedulingDecision scheduled(ScheduleRequest r) { return new SchedulingService(CLOCK).evaluate(r); }
    @Test void scheduleBaselineDurationAndCourseKindBoundaries() {
        var r=scheduled(schedule()); assertTrue(r.approved()); assertFalse(r.manualReview()); assertEquals("APPROVED",r.status()); assertEquals(60,r.durationMinutes()); assertEquals(List.of(),r.conflicts()); assertEquals(List.of("INSTRUCTOR_AVAILABILITY_NOT_PROVIDED"),r.warnings());
        CourseKind[] kinds=CourseKind.values(); int[] minimum={30,60,20,45,15}; int[] maximum={180,240,300,360,120}; String[] shortCodes={"LECTURE_TOO_SHORT","LAB_TOO_SHORT","EXAM_TOO_SHORT","WORKSHOP_TOO_SHORT","WEBINAR_TOO_SHORT"}; String[] longCodes={"LECTURE_LONGER_THAN_RECOMMENDED","LAB_TOO_LONG","EXAM_TOO_LONG","WORKSHOP_REQUIRES_BREAK_PLAN","WEBINAR_LONGER_THAN_RECOMMENDED"};
        for(int i=0;i<kinds.length;i++) for(int minutes:new int[]{minimum[i]-1,minimum[i],maximum[i],maximum[i]+1}) { var req=with(schedule(),"courseKind",kinds[i],"end",schedule().start().plusSeconds(minutes*60L),"lunchTimeAllowed",true,"availableEquipment",Set.of("LAB_SAFETY_KIT","PROCTORING")); var decision=scheduled(req); assertEquals(minutes<minimum[i],decision.conflicts().contains(shortCodes[i])); assertEquals(minutes>maximum[i],(i==1||i==2?decision.conflicts():decision.warnings()).contains(longCodes[i])); assertEquals(minutes,decision.durationMinutes()); }
    }
    @Test void schedulingTimingBoundaryMatrix() {
        for(int lead:new int[]{-1,0,1,2,23,24}) { var start=NOW.plusSeconds(lead*3600L); var r=scheduled(with(schedule(),"start",start,"end",start.plusSeconds(3600),"weekendAllowed",true,"afterHoursAllowed",true,"lunchTimeAllowed",true)); assertEquals(lead<0,r.conflicts().contains("START_TIME_IN_PAST")); assertEquals(lead>=0&&lead<2,r.conflicts().contains("INSUFFICIENT_BOOKING_NOTICE")); assertEquals(lead>=2&&lead<24,r.warnings().contains("SHORT_BOOKING_NOTICE")); }
        for(int day:new int[]{5,6}) { var start=NOW.plusSeconds(day*86400L+10*3600); assertTrue(scheduled(with(schedule(),"start",start,"end",start.plusSeconds(3600))).conflicts().contains("WEEKEND_NOT_ALLOWED")); assertFalse(scheduled(with(schedule(),"start",start,"end",start.plusSeconds(3600),"weekendAllowed",true)).conflicts().contains("WEEKEND_NOT_ALLOWED")); }
        assertTrue(scheduled(with(schedule(),"holidays",Set.of(LocalDate.of(2026,11,17)))).conflicts().contains("HOLIDAY_NOT_AVAILABLE"));
        for(int hour:new int[]{7,8,20,21}) { var start=NOW.plusSeconds(86400+hour*3600L); var req=with(schedule(),"start",start,"end",start.plusSeconds(3600)); assertEquals(hour==7||hour==21,scheduled(req).conflicts().contains("OUTSIDE_OPERATING_HOURS")); assertEquals(hour==7||hour==21,scheduled(with(req,"afterHoursAllowed",true)).warnings().contains("AFTER_HOURS_SESSION")); }
        for(int minute:new int[]{660,661,750,810}) { var start=NOW.plusSeconds(86400+minute*60L); var req=with(schedule(),"start",start,"end",start.plusSeconds(3600)); boolean overlap=minute>660&&minute<810; assertEquals(overlap,scheduled(req).conflicts().contains("LUNCH_BREAK_CONFLICT")); assertEquals(overlap,scheduled(with(req,"lunchTimeAllowed",true)).warnings().contains("LUNCH_TIME_SESSION")); }
        var start=NOW.plusSeconds(47*3600); assertTrue(scheduled(with(schedule(),"start",start,"end",start.plusSeconds(3600))).conflicts().contains("SESSION_CROSSES_LOCAL_DATE"));
    }
    @Test void schedulingDeliveryAndEquipment() {
        for(DeliveryMode mode:DeliveryMode.values()) for(String room:new String[]{null," ","room"}) for(String url:new String[]{null," ","http://meet","https://meet"}) { var r=scheduled(with(schedule(),"deliveryMode",mode,"roomId",room,"meetingUrl",url)); boolean noRoom=room==null||room.isBlank(), noUrl=url==null||url.isBlank(); if(mode==DeliveryMode.CLASSROOM) assertEquals(noRoom,r.conflicts().contains("ROOM_REQUIRED")); else { assertEquals(noUrl,r.conflicts().contains(mode==DeliveryMode.HYBRID?"HYBRID_MEETING_URL_REQUIRED":"MEETING_URL_REQUIRED")); assertEquals(!noUrl&&!url.startsWith("https://"),r.conflicts().contains("SECURE_MEETING_URL_REQUIRED")); } if(mode==DeliveryMode.HYBRID) assertEquals(noRoom,r.conflicts().contains("HYBRID_ROOM_REQUIRED")); }
        assertTrue(scheduled(with(schedule(),"recordingRequired",true)).conflicts().contains("RECORDING_NOT_SUPPORTED"));
        assertTrue(scheduled(with(schedule(),"recordingSupported",true)).warnings().contains("RECORDING_AVAILABLE"));
        assertTrue(scheduled(with(schedule(),"requiredEquipment",Set.of("PROJECTOR"))).conflicts().contains("MISSING_EQUIPMENT:PROJECTOR"));
        assertFalse(scheduled(with(schedule(),"requiredEquipment",Set.of("PROJECTOR"),"availableEquipment",Set.of("PROJECTOR"))).conflicts().contains("MISSING_EQUIPMENT:PROJECTOR"));
        assertTrue(scheduled(with(schedule(),"courseKind",CourseKind.LAB)).conflicts().contains("LAB_SAFETY_KIT_REQUIRED"));
        assertEquals("MANUAL_REVIEW",scheduled(with(schedule(),"courseKind",CourseKind.EXAM)).status());
    }
    @Test void schedulingCapacityAndManualReview() {
        for(int expected:new int[]{500,501,1000,1001}) {var r=scheduled(with(schedule(),"expectedParticipants",expected)); assertEquals(expected>1000,r.conflicts().contains("ONLINE_PLATFORM_CAPACITY_EXCEEDED")); assertEquals(expected>500&&expected<=1000,r.warnings().contains("LARGE_ONLINE_EVENT")); }
        for(int capacity:new int[]{0,19,20,59,60,61}) {var r=scheduled(with(schedule(),"deliveryMode",DeliveryMode.CLASSROOM,"roomId","room","roomCapacity",capacity)); assertEquals(capacity==0,r.conflicts().contains("ROOM_CAPACITY_UNKNOWN")); assertEquals(capacity>0&&capacity<20,r.conflicts().contains("ROOM_TOO_SMALL_FOR_REGISTRATION")); assertEquals(capacity>60,r.warnings().contains("ROOM_SIGNIFICANTLY_OVERSIZED")); }
        assertTrue(scheduled(with(schedule(),"deliveryMode",DeliveryMode.HYBRID,"roomId","room","expectedParticipants",31)).warnings().contains("ROOM_BELOW_EXPECTED_CAPACITY"));
        assertTrue(scheduled(with(schedule(),"expectedParticipants",19)).warnings().contains("REGISTRATION_EXCEEDS_FORECAST"));
        assertTrue(scheduled(with(schedule(),"deliveryMode",DeliveryMode.CLASSROOM,"roomId","room","accessibleRoomRequired",true,"roomAccessible",false)).conflicts().contains("ACCESSIBLE_ROOM_REQUIRED"));
        for(int size:new int[]{30,31}) assertEquals(size==31,scheduled(with(schedule(),"courseKind",CourseKind.LAB,"expectedParticipants",size)).conflicts().contains("LAB_CAPACITY_EXCEEDED"));
        for(int size:new int[]{50,51}) assertEquals(size==51,scheduled(with(schedule(),"courseKind",CourseKind.WORKSHOP,"expectedParticipants",size)).warnings().contains("WORKSHOP_GROUP_TOO_LARGE"));
        for(int size:new int[]{200,201}) assertEquals(size==201,scheduled(with(schedule(),"deliveryMode",DeliveryMode.HYBRID,"roomId","room","expectedParticipants",size,"roomCapacity",size)).manualReview());
        for(int occurrences:new int[]{1,2,20,21,52,53}) {var r=scheduled(with(schedule(),"recurring",true,"occurrenceCount",occurrences)); assertEquals(occurrences==1,r.conflicts().contains("RECURRING_SESSION_REQUIRES_MULTIPLE_OCCURRENCES")); assertEquals(occurrences>52,r.conflicts().contains("TOO_MANY_RECURRENCES")); assertEquals(occurrences>20,r.manualReview());}
        assertTrue(scheduled(with(schedule(),"occurrenceCount",2)).conflicts().contains("OCCURRENCE_COUNT_REQUIRES_RECURRENCE"));
    }
    @Test void schedulingIntervalsBookingsAndOverride() {
        Instant start=schedule().start(),end=schedule().end();
        for(TimeWindow window:List.of(new TimeWindow(start,end),new TimeWindow(start.minusSeconds(1),end.plusSeconds(1)))) assertFalse(scheduled(with(schedule(),"instructorAvailability",List.of(window))).conflicts().contains("INSTRUCTOR_UNAVAILABLE"));
        for(TimeWindow window:List.of(new TimeWindow(start.plusSeconds(1),end),new TimeWindow(start,end.minusSeconds(1)))) assertTrue(scheduled(with(schedule(),"instructorAvailability",List.of(window))).conflicts().contains("INSTRUCTOR_UNAVAILABLE"));
        for(TimeWindow window:List.of(new TimeWindow(start.minusSeconds(3600),start),new TimeWindow(end,end.plusSeconds(3600)))) assertFalse(scheduled(with(schedule(),"maintenanceWindows",List.of(window))).conflicts().contains("MAINTENANCE_WINDOW_CONFLICT"));
        assertTrue(scheduled(with(schedule(),"maintenanceWindows",List.of(new TimeWindow(start,end)))).conflicts().contains("MAINTENANCE_WINDOW_CONFLICT"));
        var booking=new ExistingBooking("b","i","room","co",start,end,false,true,CourseKind.LECTURE);
        var r=scheduled(with(schedule(),"deliveryMode",DeliveryMode.CLASSROOM,"roomId","room","existingBookings",List.of(booking))); assertTrue(r.conflicts().containsAll(List.of("INSTRUCTOR_BOOKING_CONFLICT:b","ROOM_BOOKING_CONFLICT:b","COHORT_BOOKING_CONFLICT:b"))); assertTrue(r.warnings().contains("CONFLICTING_BOOKING_IS_TENTATIVE:b"));
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(with(booking,"cancelled",true)))).approved());
        var prior=with(booking,"start",start.minusSeconds(3600),"end",start,"tentative",false);
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(prior))).approved());
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(prior),"minimumBufferMinutes",1)).conflicts().contains("INSTRUCTOR_BOOKING_CONFLICT:b"));
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(prior),"maximumInstructorSessionsPerDay",1)).conflicts().contains("INSTRUCTOR_DAILY_SESSION_LIMIT"));
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(prior),"maximumInstructorSessionsPerDay",2)).warnings().contains("INSTRUCTOR_REACHES_DAILY_SESSION_LIMIT"));
        assertTrue(scheduled(with(schedule(),"meetingUrl",null,"administratorOverride",true)).approved());
        assertFalse(scheduled(with(schedule(),"existingBookings",List.of(booking),"administratorOverride",true)).approved());
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(booking),"administratorOverride",true)).warnings().contains("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"));
    }
    @Test void schedulingRejectsMalformedRequests() {
        assertThrows(NullPointerException.class,()->scheduled(null));
        Object[][] bad={{"courseId",null},{"courseId"," "},{"cohortId",null},{"cohortId"," "},{"instructorId",null},{"instructorId"," "},{"deliveryMode",null},{"courseKind",null},{"start",null},{"end",null},{"end",schedule().start()},{"end",schedule().start().plusSeconds(86400)},{"zoneId",null},{"zoneId"," "},{"zoneId","Invalid/Zone"},{"expectedParticipants",-1},{"registeredParticipants",-1},{"roomCapacity",-1},{"occurrenceCount",0},{"minimumBufferMinutes",-1},{"minimumBufferMinutes",241},{"maximumInstructorSessionsPerDay",0},{"requiredEquipment",Set.of(" ")},{"availableEquipment",Set.of(" ")}};
        for(Object[] c:bad) assertThrows(IllegalArgumentException.class,()->scheduled(with(schedule(),c)),String.valueOf(c[0]));
        for(TimeWindow w:List.of(new TimeWindow(null,NOW),new TimeWindow(NOW,null),new TimeWindow(NOW,NOW))) for(String field:List.of("instructorAvailability","maintenanceWindows")) assertThrows(IllegalArgumentException.class,()->scheduled(with(schedule(),field,List.of(w))));
        var b=new ExistingBooking("b","i","r","co",NOW,NOW.plusSeconds(60),false,false,CourseKind.EXAM);
        for(Object[] c:new Object[][]{{"bookingId",null},{"bookingId"," "},{"start",null},{"end",null},{"end",NOW}}) assertThrows(IllegalArgumentException.class,()->scheduled(with(schedule(),"existingBookings",List.of(with(b,c)))));
        assertThrows(IllegalArgumentException.class,()->scheduled(with(schedule(),"existingBookings",List.of(b,b))));
    }

    private net.mooctest.expression.engine.EquationSolver equationFor(java.util.function.DoubleUnaryOperator function, double result) throws Exception {
        var constructor = net.mooctest.expression.engine.EquationSolver.class.getDeclaredConstructor(String.class, double.class, String.class, net.mooctest.expression.engine.ManyToOneNumberFunction.class);
        constructor.setAccessible(true);
        var input = new net.mooctest.expression.engine.ManyToOneNumberFunction("testfunction") {
            @Override public double run(double... values) { return function.applyAsDouble(values[0]); }
        };
        return constructor.newInstance("f(x)", result, "equation", input);
    }

    @Test
    void equationNewtonSolvesKnownRootsAndRejectsNonconvergence() throws Exception {
        var solver = equationFor(x -> x * x, 9);
        assertEquals(9, solver.getResult());
        assertFalse(solver.isBigDecimal());
        assertTrue(solver.isUnBigDecimal());
        assertTrue(solver.isUseNewton());
        assertEquals(100, solver.getMaxIter());
        assertEquals(1e-12, solver.getTol());
        assertEquals(2, solver.getNewtonInitialX());
        assertEquals(1, solver.getBisectionLeft());
        assertEquals(3, solver.getBisectionRight());
        assertEquals(3, solver.solveNewton(1e-10, 100)[0], 1e-8);
        solver.setNewtonInitialX(3);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[]{3, 1}, solver.solveNewton(1e-10, 1));
        solver.setNewtonInitialX(-2);
        assertEquals(-2, solver.getNewtonInitialX());
        assertEquals(-3, solver.solveNewton(1e-10, 100)[0], 1e-8);
        assertThrows(ArithmeticException.class, () -> solver.solveNewton(1e-12, 0));
        var constant = equationFor(x -> 4, 3);
        assertTrue(assertThrows(ArithmeticException.class, () -> constant.solveNewton(1e-12, 10)).getMessage().contains("导数过小"));
        var linear = equationFor(x -> 2 * x + 1, 9);
        assertEquals(4, linear.calculation(false).getResult().doubleValue(), 1e-8);
        assertThrows(UnsupportedOperationException.class, linear::convertToMultiPrecisionSupported);
        assertThrows(UnsupportedOperationException.class, () -> linear.calculationBigDecimals(false));
    }

    @Test
    void equationBisectionRespectsBracketsAndSettings() throws Exception {
        var solver = equationFor(x -> x * x, 9);
        solver.setBisectionLeft(0);
        solver.setBisectionRight(8);
        solver.setUseNewton(false);
        solver.setTol(1e-10);
        solver.setMaxIter(80);
        assertEquals(0, solver.getBisectionLeft());
        assertEquals(8, solver.getBisectionRight());
        assertFalse(solver.isUseNewton());
        assertEquals(1e-10, solver.getTol());
        assertEquals(80, solver.getMaxIter());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[]{3, 3}, solver.solveBisection(1e-10, 80));
        assertEquals(3, solver.calculation(true).getResult().doubleValue(), 1e-8);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[]{0, 0}, solver.solveBisection(1e-10, 0));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[]{0, 7}, solver.solveBisection(4, 7));
        solver.setBisectionLeft(4);
        assertThrows(IllegalArgumentException.class, () -> solver.solveBisection(1e-10, 80));
        solver.setBisectionLeft(3);
        solver.setBisectionRight(5);
        assertEquals(3, solver.solveBisection(1e-9, 80)[0], 1e-7);
    }

    @Test
    void equationCompilerRejectsMalformedEquationsAndReportsUnavailableDependency() {
        for (String expression : new String[]{"x", "x=2=3"}) {
            assertThrows(IllegalArgumentException.class, () -> net.mooctest.expression.engine.EquationSolver.compile(expression, "equation"));
            assertThrows(IllegalArgumentException.class, () -> net.mooctest.expression.engine.EquationSolverExpression.compile(expression, "equation"));
        }
        assertThrows(NumberFormatException.class, () -> net.mooctest.expression.engine.EquationSolver.compile("x=abc", "equation"));
        // The bundled compiler generates an import of the original upstream package.
        var failure = assertThrows(RuntimeException.class, () -> net.mooctest.expression.engine.DynamicFunctionCompiler.compile("linear", "x + 1", new String[]{"x"}));
        assertEquals("动态函数生成失败", failure.getMessage());
        assertNotNull(failure.getCause());
        assertTrue(failure.getCause().getMessage().contains("编译错误"));
        new net.mooctest.expression.engine.DynamicFunctionCompiler();
        assertEquals("missing", new net.mooctest.expression.engine.EquationSolver.NoRootFoundException("missing").getMessage());
    }

    @Test
    void equationCalculatorRegistrationChecksTypeAndPrecision() {
        String firstName = "equationRegistryOne";
        String secondName = "equationRegistryTwo";
        net.mooctest.expression.engine.CalculationManagement.unregister(firstName);
        net.mooctest.expression.engine.CalculationManagement.unregister(secondName);
        try {
            var first = net.mooctest.expression.engine.SingletonEquationSolving.getInstance(firstName);
            var second = net.mooctest.expression.engine.SingletonEquationSolvingTwo.getInstance(secondName);
            assertSame(first, net.mooctest.expression.engine.SingletonEquationSolving.getInstance(firstName));
            assertSame(second, net.mooctest.expression.engine.SingletonEquationSolvingTwo.getInstance(secondName));
            assertThrows(net.mooctest.expression.engine.ExtractException.class, () -> net.mooctest.expression.engine.SingletonEquationSolving.getInstance(secondName));
            assertThrows(net.mooctest.expression.engine.ExtractException.class, () -> net.mooctest.expression.engine.SingletonEquationSolvingTwo.getInstance(firstName));
            assertEquals(" x = 3 ", first.formatStr(" x = 3 "));
            assertEquals(" x = 3 ", second.formatStr(" x = 3 "));
            assertThrows(UnsupportedOperationException.class, () -> first.compileBigDecimal("x=3", true));
            assertThrows(UnsupportedOperationException.class, () -> second.compileBigDecimal("x=3", true));
            assertThrows(IllegalArgumentException.class, () -> first.compile("bad", true));
            assertThrows(IllegalArgumentException.class, () -> second.compile("bad", true));
            assertThrows(IllegalArgumentException.class, () -> first.calculation("bad", false));
            assertThrows(IllegalArgumentException.class, () -> second.calculation("bad", false));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(firstName);
            net.mooctest.expression.engine.CalculationManagement.unregister(secondName);
        }
    }

    @Test
    void jvmFunctionSignatureValidationAndRegistration() {
        var methods = net.mooctest.expression.engine.JvmExpressionFunction.getRegisteredMethods();
        var saved = new java.util.LinkedHashMap<>(methods);
        try {
            methods.clear();
            assertEquals("public static double f() { return 2; }", net.mooctest.expression.engine.JvmExpressionFunction.convertToStaticMethodSource("f()=2"));
            assertEquals("public static double sum(double x, double y) { return x+y; }", net.mooctest.expression.engine.JvmExpressionFunction.convertToStaticMethodSource("sum(x, y)=x+y"));
            for (String invalid : new String[]{"f(x)", "f(x)=x=2", "f=x", "f(x=x", "f1(x)=x"}) {
                assertThrows(IllegalArgumentException.class, () -> net.mooctest.expression.engine.JvmExpressionFunction.convertToStaticMethodSource(invalid));
            }
            assertThrows(IllegalArgumentException.class, () -> net.mooctest.expression.engine.JvmExpressionFunction.registerJvmFunction("bad"));
            assertThrows(IllegalArgumentException.class, () -> net.mooctest.expression.engine.JvmExpressionFunction.registerJvmFunction("private double f() {}"));
            net.mooctest.expression.engine.JvmExpressionFunction.registerFunction("square(x)=x*x");
            assertEquals("public static double square(double x) { return x*x; }", methods.get("double square"));
            assertEquals(1, methods.size());
        } finally {
            methods.clear();
            methods.putAll(saved);
        }
    }

    // =================================================================
    // Coverage-gap suite: public portal API, registry bookkeeping, dynamic
    // class loading and the built-in function packages. Every expectation is
    // an independently computed worked example or a documented contract.
    // =================================================================

    /** Function library consumed through the {@code Functions} class overload. */
    @net.mooctest.expression.engine.Functions({"gapjvmtwo(x) = x * 2"})
    static class GapJvmFunctionLibrary { }

    /** Function library consumed through the {@code Functions} class overload. */
    @net.mooctest.expression.engine.Functions({"gapclassfn(e) = e + 6"})
    static class GapPortalFunctionLibrary { }

    /** Deliberately lacks the {@code Functions} annotation. */
    static class GapPlainLibrary { }

    @Test
    void gapPortalResolvesEveryEnumComponentByName() throws Exception {
        for (net.mooctest.expression.engine.Mathematical_Expression kind
                : net.mooctest.expression.engine.Mathematical_Expression.values()) {
            net.mooctest.expression.engine.Calculation component =
                    net.mooctest.expression.engine.Mathematical_Expression.getInstance(kind);
            assertNotNull(component, kind.name());
            assertEquals(kind.toString(), component.getName(), kind.name());
            assertSame(component,
                    net.mooctest.expression.engine.Mathematical_Expression.getInstance(kind, kind.toString()),
                    kind.name());
        }
    }

    @Test
    void gapPortalRegistersJvmFunctionSources() throws Exception {
        net.mooctest.expression.engine.JvmExpressionFunction.getRegisteredMethods().clear();
        net.mooctest.expression.engine.Mathematical_Expression.register_jvm_function("gapjvmone(x) = x + 3");
        assertTrue(net.mooctest.expression.engine.JvmExpressionFunction.getRegisteredMethods()
                .containsKey("double gapjvmone"));
        assertTrue(net.mooctest.expression.engine.Mathematical_Expression
                .register_jvm_function(GapJvmFunctionLibrary.class));
        assertTrue(net.mooctest.expression.engine.JvmExpressionFunction.getRegisteredMethods()
                .containsKey("double gapjvmtwo"));
        assertFalse(net.mooctest.expression.engine.Mathematical_Expression
                .register_jvm_function(GapPlainLibrary.class));
    }

    @Test
    void gapPortalRegistersFunctionsFromEveryOverload() throws Exception {
        assertTrue(net.mooctest.expression.engine.Mathematical_Expression
                .register_function("gapport(a) = a + 1"));
        assertFalse(net.mooctest.expression.engine.Mathematical_Expression
                .register_function("gapport(a) = a + 2"));
        assertTrue(net.mooctest.expression.engine.Mathematical_Expression.getFunctionMap().contains("gapport"));
        assertNotNull(net.mooctest.expression.engine.Mathematical_Expression.getFunction("gapport"));

        assertTrue(net.mooctest.expression.engine.Mathematical_Expression
                .register_function(GapPortalFunctionLibrary.class));
        assertTrue(net.mooctest.expression.engine.Mathematical_Expression.getFunctionMap().contains("gapclassfn"));
        assertFalse(net.mooctest.expression.engine.Mathematical_Expression
                .register_function(GapPlainLibrary.class));

        var direct = net.mooctest.expression.engine.ExpressionFunction.parse("gapdirect(b) = b * 4");
        var again = net.mooctest.expression.engine.ExpressionFunction.parse("gapdirect(b) = b * 4");
        assertTrue(net.mooctest.expression.engine.Mathematical_Expression.register_function(direct));
        assertFalse(net.mooctest.expression.engine.Mathematical_Expression.register_function(again));
        assertTrue(net.mooctest.expression.engine.Mathematical_Expression.unregister_function(direct));
        assertFalse(net.mooctest.expression.engine.Mathematical_Expression.unregister_function(again));

        assertTrue(net.mooctest.expression.engine.Mathematical_Expression.unregister_function("gapport"));
        assertFalse(net.mooctest.expression.engine.Mathematical_Expression.unregister_function("gapport"));
    }

    @Test
    void gapPortalPersistsFunctionsThroughFilesAndStreams() throws Exception {
        net.mooctest.expression.engine.Mathematical_Expression
                .register_function(net.mooctest.expression.engine.FunctionPackage.BRANCH);
        assertTrue(net.mooctest.expression.engine.CalculationManagement.isFunctionExist("ifEq"));
        assertTrue(net.mooctest.expression.engine.Mathematical_Expression.getFunctionMap().contains("ifLe"));

        var sum = (net.mooctest.expression.engine.ManyToOneNumberFunction)
                net.mooctest.expression.engine.FunctionPackage.MATH.getFunctionByNameNoCheck("sum");
        var saved = java.nio.file.Files.createTempFile("gapfunction", ".ser");
        try {
            net.mooctest.expression.engine.Mathematical_Expression.saveFunction(sum, saved.toFile());
            net.mooctest.expression.engine.Mathematical_Expression.unregister_function("sum");
            assertFalse(net.mooctest.expression.engine.CalculationManagement.isFunctionExist("sum"));
            assertTrue(net.mooctest.expression.engine.Mathematical_Expression.register_function(saved.toFile()));
            assertEquals(6.0, net.mooctest.expression.engine.Mathematical_Expression.getFunction("sum").run(1, 2, 3), 1e-12);

            var stray = java.nio.file.Files.createTempFile("gapplain", ".ser");
            try (var out = new java.io.ObjectOutputStream(java.nio.file.Files.newOutputStream(stray))) {
                out.writeObject("gap-not-a-function");
            }
            assertFalse(net.mooctest.expression.engine.Mathematical_Expression.register_function(stray.toFile()));
            java.nio.file.Files.deleteIfExists(stray);
        } finally {
            java.nio.file.Files.deleteIfExists(saved);
        }

        var buffer = new java.io.ByteArrayOutputStream();
        try (var out = new java.io.ObjectOutputStream(buffer)) {
            out.writeObject(net.mooctest.expression.engine.ExpressionFunction.parse("gapstreama(c) = c + 1"));
            out.writeObject(net.mooctest.expression.engine.ExpressionFunction.parse("gapstreamb(d) = d + 2"));
            out.writeObject("gap-stray-object");
        }
        var counted = net.mooctest.expression.engine.Mathematical_Expression
                .register_function(new java.io.ByteArrayInputStream(buffer.toByteArray()));
        assertEquals(2, counted.getKey().intValue());
        assertEquals(1, counted.getValue().intValue());
    }

    @Test
    void gapManagementKeepsRegistryBookkeepingStraight() throws Exception {
        var first = net.mooctest.expression.engine.BracketsCalculation2.getInstance("gapMgmtKnown");
        assertFalse(net.mooctest.expression.engine.CalculationManagement.register(first, true));
        var second = net.mooctest.expression.engine.BracketsCalculation2.getInstance("gapMgmtFresh");
        assertTrue(net.mooctest.expression.engine.CalculationManagement.unregister("gapMgmtFresh"));
        assertTrue(net.mooctest.expression.engine.CalculationManagement.register(second, true));
        assertTrue(net.mooctest.expression.engine.CalculationManagement.register(first, false));
        assertTrue(net.mooctest.expression.engine.CalculationManagement.isRegister("gapMgmtKnown"));
        assertSame(first, net.mooctest.expression.engine.CalculationManagement.getCalculationByName("gapMgmtKnown"));
        assertFalse(net.mooctest.expression.engine.CalculationManagement.isRegister("gapMgmtAbsent"));
        assertTrue(net.mooctest.expression.engine.CalculationManagement.unregister("gapMgmtKnown"));
        assertFalse(net.mooctest.expression.engine.CalculationManagement.unregister("gapMgmtKnown"));

        var function = net.mooctest.expression.engine.ExpressionFunction.parse("gapmgmtf(x) = x + 5");
        assertTrue(net.mooctest.expression.engine.CalculationManagement.register(function));
        assertFalse(net.mooctest.expression.engine.CalculationManagement.register(function));
        assertTrue(net.mooctest.expression.engine.CalculationManagement.isFunctionExist("gapmgmtf"));
        assertFalse(net.mooctest.expression.engine.CalculationManagement.isFunctionExist("gapmgmtMissing"));
        assertTrue(net.mooctest.expression.engine.CalculationManagement.getFunctionMap().contains("gapmgmtf"));
        assertSame(function,
                net.mooctest.expression.engine.CalculationManagement.getFunctionByNameNoCheck("gapmgmtf"));
        assertSame(function, net.mooctest.expression.engine.CalculationManagement.getFunctionByName("gapmgmtf"));
        assertNull(net.mooctest.expression.engine.CalculationManagement.getFunctionByNameNoCheck("gapmgmtMissing"));

        net.mooctest.expression.engine.CalculationManagement
                .register(net.mooctest.expression.engine.FunctionPackage.BRANCH);
        assertTrue(net.mooctest.expression.engine.CalculationManagement.isFunctionExist("ifGt"));
        assertTrue(net.mooctest.expression.engine.CalculationManagement.unregisterF("ifGt"));
        assertFalse(net.mooctest.expression.engine.CalculationManagement.unregisterF("ifGt"));
    }

    @Test
    void gapFunctionPackagesExplainTheirDependencies() throws Exception {
        assertEquals("Math", net.mooctest.expression.engine.FunctionPackage.MATH.getName());
        assertTrue(net.mooctest.expression.engine.FunctionPackage.MATH.getFunctionMap().containsKey("sum"));

        var average = (net.mooctest.expression.engine.ManyToOneNumberFunction)
                net.mooctest.expression.engine.FunctionPackage.MATH.getFunctionByNameNoCheck("avg");
        var moduleLength = (net.mooctest.expression.engine.ManyToOneNumberFunction)
                net.mooctest.expression.engine.FunctionPackage.MATH.getFunctionByNameNoCheck("moduleLength");
        assertEquals("avg", average.toString());
        assertTrue(average.AllowSerialization());

        net.mooctest.expression.engine.CalculationManagement.unregisterF("sum");
        net.mooctest.expression.engine.CalculationManagement.unregisterF("pow");
        net.mooctest.expression.engine.CalculationManagement.unregisterF("sqrt");
        var missingSum = assertThrows(UnsupportedOperationException.class, () -> average.run(1, 2, 3));
        assertTrue(missingSum.getMessage().contains("sum"));
        assertTrue(missingSum.getMessage().contains("Math"), missingSum.getMessage());
        var missingPow = assertThrows(UnsupportedOperationException.class, () -> moduleLength.run());
        assertTrue(missingPow.getMessage().contains("pow"));

        net.mooctest.expression.engine.CalculationManagement
                .register(net.mooctest.expression.engine.FunctionPackage.MATH);
        assertEquals(2.0, average.run(1, 2, 3), 1e-12);
        net.mooctest.expression.engine.CalculationManagement.unregisterF("sqrt");
        var missingSqrt = assertThrows(UnsupportedOperationException.class, () -> moduleLength.run());
        assertTrue(missingSqrt.getMessage().contains("sqrt"));
        net.mooctest.expression.engine.CalculationManagement
                .register(net.mooctest.expression.engine.FunctionPackage.MATH);
        assertEquals(0.0, moduleLength.run(), 1e-12);

        var ifEq = (net.mooctest.expression.engine.ManyToOneNumberFunction)
                net.mooctest.expression.engine.FunctionPackage.BRANCH.getFunctionByNameNoCheck("ifEq");
        var badArity = assertThrows(UnsupportedOperationException.class, () -> ifEq.run(1, 1, 5));
        assertTrue(badArity.getMessage().contains("4"));
    }

    @Test
    void gapUtilitiesExposeTheirContracts() throws Exception {
        assertEquals("", net.mooctest.expression.engine.StrUtils.randomString(0));
        assertEquals(7, net.mooctest.expression.engine.StrUtils.randomString(7).length());
    }

    @Test
    void gapDynamicLoaderDefinesAndRejectsClasses() throws Exception {
        Class<?> loaderType = Class.forName("net.mooctest.expression.engine.DynamicClassLoader");
        var loaderConstructor = loaderType.getDeclaredConstructor();
        loaderConstructor.setAccessible(true);
        ClassLoader loader = (ClassLoader) loaderConstructor.newInstance();

        byte[] bytecode;
        try (var in = net.mooctest.expression.engine.StrUtils.class
                .getResourceAsStream("/net/mooctest/expression/engine/StrUtils.class")) {
            assertNotNull(in);
            bytecode = in.readAllBytes();
        }
        Class<?> holderType = Class.forName("net.mooctest.expression.engine.InMemoryJavaFileObject");
        var holderConstructor = holderType.getDeclaredConstructor(String.class, javax.tools.JavaFileObject.Kind.class);
        holderConstructor.setAccessible(true);
        Object holder = holderConstructor.newInstance("net.mooctest.expression.engine.StrUtils",
                javax.tools.JavaFileObject.Kind.CLASS);
        var openOutputStream = holderType.getMethod("openOutputStream");
        try (var out = (java.io.OutputStream) openOutputStream.invoke(holder)) {
            out.write(bytecode);
        }
        var getBytes = holderType.getDeclaredMethod("getBytes");
        getBytes.setAccessible(true);
        assertArrayEquals(bytecode, (byte[]) getBytes.invoke(holder));

        var addClass = loaderType.getDeclaredMethod("addClass", String.class, holderType);
        addClass.setAccessible(true);
        addClass.invoke(loader, "net.mooctest.expression.engine.StrUtils", holder);
        var findClass = loaderType.getDeclaredMethod("findClass", String.class);
        findClass.setAccessible(true);
        Class<?> defined = (Class<?>) findClass.invoke(loader, "net.mooctest.expression.engine.StrUtils");
        assertNotNull(defined);
        assertEquals("net.mooctest.expression.engine.StrUtils", defined.getName());
        assertNotSame(net.mooctest.expression.engine.StrUtils.class, defined);
        var absent = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> findClass.invoke(loader, "net.mooctest.expression.engine.GapAbsent"));
        assertTrue(absent.getCause() instanceof ClassNotFoundException);
    }

    @Test
    void gapJvmCalculationKeepsNamesAndRejectsForeignComponents() throws Exception {
        var calculator = net.mooctest.expression.engine.JvmCalculation.getInstance("gapJvmOwn");
        assertSame(calculator, net.mooctest.expression.engine.JvmCalculation.getInstance("gapJvmOwn"));
        assertEquals(" keep ", calculator.formatStr(" keep "));

        net.mooctest.expression.engine.BracketsCalculation2.getInstance("gapJvmForeign");
        var clash = assertThrows(net.mooctest.expression.engine.ExtractException.class,
                () -> net.mooctest.expression.engine.JvmCalculation.getInstance("gapJvmForeign"));
        assertTrue(clash.getMessage().contains("gapJvmForeign"));
    }

    @Test
    void gapIntervalComponentsReuseTheirRegistration() throws Exception {
        String sumName = "gapFastSum";
        String productName = "gapFastProduct";
        var sum = net.mooctest.expression.engine.FastSumOfIntervalsBrackets.getInstance(sumName);
        var product = net.mooctest.expression.engine.FastMultiplyOfIntervalsBrackets.getInstance(productName);
        try {
            assertSame(sum, net.mooctest.expression.engine.FastSumOfIntervalsBrackets.getInstance(sumName));
            assertSame(product, net.mooctest.expression.engine.FastMultiplyOfIntervalsBrackets.getInstance(productName));
            assertEquals("3,4", sum.formatStr(" 3,4 "));
            assertEquals(7.0, ((Number) sum.compileBigDecimal("3,4", true)
                    .calculationBigDecimals(true).getResult()).doubleValue(), 1e-10);
            assertEquals(48.0, ((Number) product.compileBigDecimal("2,6", true)
                    .calculationBigDecimals(true).getResult()).doubleValue(), 1e-10);
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(sumName);
            net.mooctest.expression.engine.CalculationManagement.unregister(productName);
        }
    }

    @Test
    void gapBooleanAndFunctionFormulasAnswerLiteralOperands() throws Exception {
        var booleanCalculator = net.mooctest.expression.engine.BooleanCalculation2.getInstance("gapBoolLiteral");
        try {
            for (String literal : new String[]{"null==null", "null==1", "1==null", "1<>null"}) {
                assertThrows(net.mooctest.expression.engine.AbnormalOperation.class,
                        () -> booleanCalculator.calculation(literal, true), literal);
            }
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister("gapBoolLiteral");
        }

        var formula = net.mooctest.expression.engine.FunctionFormulaCalculation.getInstance("gapFormula");
        try {
            assertSame(formula, net.mooctest.expression.engine.FunctionFormulaCalculation.getInstance("gapFormula"));
            assertTrue(net.mooctest.expression.engine.CalculationManagement.isRegister("gapFormula"));
            assertDoesNotThrow(() -> formula.check("2+3*4"));
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> formula.check("gap(2"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister("gapFormula");
        }

        var formula2 = net.mooctest.expression.engine.FunctionFormulaCalculation2.getInstance("gapFormula2");
        try {
            assertNotNull(formula2.compileBigDecimal("3+4", true));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister("gapFormula2");
        }

        var unknown = assertThrows(net.mooctest.expression.engine.WrongFormat.class,
                () -> net.mooctest.expression.engine.ExpressionFunction.parse("gapfn(x) = x + zz1"));
        assertTrue(unknown.getMessage().contains("zz1"));
    }

    // Equation solver additions
    // ==================================================================
    // Equation solver block: EquationSolver / EquationSolverExpression /
    // SingletonEquationSolving / SingletonEquationSolvingTwo.
    //
    // All expected values below are hand computed from the solver algorithm:
    //   autoDetectInterval() expands  exp = 1,2,4,...  in the direction given by
    //   sign(eval(0)-result), and refineInterval() scans a..b with STEP = 1.0,
    //   taking the first x2 whose g(x2) = eval(x2)-result does not keep the sign
    //   of g(x1); it then stores left=x1, right=x2 and newtonInitialX=(x1+x2)/2.
    // ==================================================================

    /**
     * Fake replacement for the JVM dynamic compiler. The real one emits an import of a
     * non existing upstream package, so every legitimate equation compile path is
     * intercepted here. The returned function ignores the generated parameter names and
     * models the compiled body of "10-x*x" (three parameters: constant, x, x).
     */
    private static net.mooctest.expression.engine.ManyToOneNumberFunction eqHelperCompiledFunction(String body) {
        switch (body) {
            // EquationSolver.compile path: the raw "10-x*x" is compiled with the single parameter x
            case "10-x*x":
                return new net.mooctest.expression.engine.ManyToOneNumberFunction("eqFakeCompiledFunction") {
                    @Override
                    public double run(double... values) {
                        return 10 - values[0] * values[0];
                    }
                };
            // EquationSolverExpression.compile path: generated body, parameters [10, x, x]
            case "param0-param1*param2":
                return new net.mooctest.expression.engine.ManyToOneNumberFunction("eqFakeCompiledFunction") {
                    @Override
                    public double run(double... values) {
                        return values[0] - values[1] * values[2];
                    }
                };
            default:
                throw new IllegalArgumentException("unexpected compiled body: " + body);
        }
    }

    private static void eqHelperInstallCompiler(MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked) {
        mocked.when(() -> net.mooctest.expression.engine.DynamicFunctionCompiler.compile(
                        Mockito.anyString(), Mockito.anyString(), Mockito.any(String[].class)))
                .thenAnswer(invocation -> eqHelperCompiledFunction((String) invocation.getArgument(1)));
    }

    /** Builds a solver through the protected function-injection constructor (no interval detection). */
    private static net.mooctest.expression.engine.EquationSolver eqHelperSolver(DoubleUnaryOperator function, double result) throws Exception {
        Constructor<net.mooctest.expression.engine.EquationSolver> constructor =
                net.mooctest.expression.engine.EquationSolver.class.getDeclaredConstructor(
                        String.class, double.class, String.class,
                        net.mooctest.expression.engine.ManyToOneNumberFunction.class);
        constructor.setAccessible(true);
        net.mooctest.expression.engine.ManyToOneNumberFunction injected =
                new net.mooctest.expression.engine.ManyToOneNumberFunction("eqInjectedFunction") {
                    @Override
                    public double run(double... values) {
                        return function.applyAsDouble(values[0]);
                    }
                };
        return constructor.newInstance("f(x)", result, "eqInjected", injected);
    }

    private static Object eqHelperInvoke(net.mooctest.expression.engine.EquationSolver solver, String name,
                                         Class<?>[] types, Object... arguments) throws Exception {
        Method method = net.mooctest.expression.engine.EquationSolver.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        try {
            return method.invoke(solver, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e;
        }
    }

    private static void eqHelperAutoDetect(net.mooctest.expression.engine.EquationSolver solver) throws Exception {
        eqHelperInvoke(solver, "autoDetectInterval", new Class<?>[0]);
    }

    private static void eqHelperRefine(net.mooctest.expression.engine.EquationSolver solver,
                                       double a, double b, double fa, double fb) throws Exception {
        eqHelperInvoke(solver, "refineInterval",
                new Class<?>[]{double.class, double.class, double.class, double.class}, a, b, fa, fb);
    }

    // ---------------------------------------------------------------- EquationSolver

    @Test
    void eqAutoDetectNarrowsPositiveIntervalExponentially() throws Exception {
        // g(x) = eval(x) - 1 = 9 - x*x ; g(0) = 9 > 0 so the search goes to the right.
        // exp=1 -> g(1)=8 (same sign), exp=2 -> g(2)=5 (same sign), exp=4 -> g(4)=-7 (sign flip).
        // refineInterval(2,4,5,-7): g(3)=0 -> left=2, right=3, newtonInitialX=2.5
        var solver = eqHelperSolver(x -> 10 - x * x, 1.0);
        assertEquals(2.0, solver.getNewtonInitialX());
        assertEquals(1.0, solver.getBisectionLeft());
        assertEquals(3.0, solver.getBisectionRight());
        eqHelperAutoDetect(solver);
        assertEquals(2.0, solver.getBisectionLeft());
        assertEquals(3.0, solver.getBisectionRight());
        assertEquals(2.5, solver.getNewtonInitialX());
        // Newton on 9-x*x from 2.5: 2.5 -> 3.05 -> 3.0004098 -> 3.00000004 -> 3.0
        assertEquals(3.0, solver.solveNewton(1e-12, 100)[0], 1e-9);
    }

    @Test
    void eqAutoDetectReportsLinearRefinementFailure() throws Exception {
        // g(x) = x*x - 1 ; g(0) = -1 < 0 so the first probe is x = -1 with g(-1) = 0,
        // a sign flip that hands refineInterval(prevX=0, currX=-1) to a scan whose upper
        // bound lies below its lower bound, hence no step is scanned and the solver reports it.
        var solver = eqHelperSolver(x -> x * x, 1.0);
        var failure = assertThrows(net.mooctest.expression.engine.EquationSolver.NoRootFoundException.class,
                () -> eqHelperAutoDetect(solver));
        assertNotNull(failure.getMessage());
        assertTrue(failure.getMessage().contains("线性细化在"), failure.getMessage());
        assertTrue(failure.getMessage().contains("未找到根"), failure.getMessage());
        // nothing must have been configured by the failed refinement
        assertEquals(1.0, solver.getBisectionLeft());
        assertEquals(3.0, solver.getBisectionRight());
        assertEquals(2.0, solver.getNewtonInitialX());
    }

    @Test
    void eqAutoDetectAcceptsSignFlipOnExpansionStep() throws Exception {
        // g(x) = 4 - x*x ; g(0) = 4 > 0, g(1) = 3 > 0, g(2) = 0 exactly -> refineInterval(1,2,3,0)
        // scans x2 = 2 only: g(2) = 0 -> left=1, right=2, newtonInitialX=1.5
        var solver = eqHelperSolver(x -> 10 - x * x, 6.0);
        eqHelperAutoDetect(solver);
        assertEquals(1.0, solver.getBisectionLeft());
        assertEquals(2.0, solver.getBisectionRight());
        assertEquals(1.5, solver.getNewtonInitialX());
    }

    @Test
    void eqRefineIntervalSelectsFirstSignChangeStep() throws Exception {
        // g(x) = x ; scan [0,3] from x1=0 (g=0): first step x2=1 -> g(1)=1, product 0 <= 0
        var solver = eqHelperSolver(x -> x, 0.0);
        eqHelperRefine(solver, 0.0, 3.0, 0.0, 3.0);
        assertEquals(0.0, solver.getBisectionLeft());
        assertEquals(1.0, solver.getBisectionRight());
        assertEquals(0.5, solver.getNewtonInitialX());
    }

    @Test
    void eqRefineIntervalScansStepThatTouchesRightBoundary() throws Exception {
        // g(x) = x - 2 ; scan [1,2] with g(1) = -1 and g(2) = 0: the only candidate step is
        // exactly x2 = b = 2, so the inclusive bound is what makes the interval be found.
        var solver = eqHelperSolver(x -> x, 2.0);
        eqHelperRefine(solver, 1.0, 2.0, -1.0, 0.0);
        assertEquals(1.0, solver.getBisectionLeft());
        assertEquals(2.0, solver.getBisectionRight());
        assertEquals(1.5, solver.getNewtonInitialX());
    }

    @Test
    void eqRefineIntervalUsesComputedValueOfFirstScannedStep() throws Exception {
        // g(x) = 9 - x*x ; scan [2,4] from x1=2 (g=5): x2=3 gives g(3)=0 (fresh evaluation,
        // not the sign carried by fa), so the interval is [2,3] and not [3,4].
        var solver = eqHelperSolver(x -> 10 - x * x, 1.0);
        eqHelperRefine(solver, 2.0, 4.0, 5.0, -7.0);
        assertEquals(2.0, solver.getBisectionLeft());
        assertEquals(3.0, solver.getBisectionRight());
        assertEquals(2.5, solver.getNewtonInitialX());
    }

    @Test
    void eqRefineIntervalAdvancesScanCursor() throws Exception {
        // g(x) = 9 - x*x ; scan [1,5] from x1=1 (g=8): the step x2=2 gives g(2)=5, still the
        // sign of y1, so the cursor has to move on to x2=3 where g(3)=0 closes the bracket.
        var solver = eqHelperSolver(x -> 10 - x * x, 1.0);
        eqHelperRefine(solver, 1.0, 5.0, 8.0, -16.0);
        assertEquals(2.0, solver.getBisectionLeft());
        assertEquals(3.0, solver.getBisectionRight());
        assertEquals(2.5, solver.getNewtonInitialX());
    }

    @Test
    void eqCompileBuildsSolverWithDetectedInterval() throws Exception {
        try (MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked =
                     Mockito.mockStatic(net.mooctest.expression.engine.DynamicFunctionCompiler.class)) {
            eqHelperInstallCompiler(mocked);
            var solver = net.mooctest.expression.engine.EquationSolver.compile("10-x*x=1", "eqCompiledSolver");
            assertNotNull(solver);
            assertEquals("10-x*x", solver.getExpressionStr());
            assertEquals("eqCompiledSolver", solver.getCalculationName());
            assertEquals(1.0, solver.getResult());
            // the constructor used by compile must have run autoDetectInterval: g(x) = 9 - x*x
            assertEquals(2.0, solver.getBisectionLeft());
            assertEquals(3.0, solver.getBisectionRight());
            assertEquals(2.5, solver.getNewtonInitialX());
            assertEquals(3.0, solver.calculation(false).getResult().doubleValue(), 1e-9);
        }
    }

    @Test
    void eqNewtonStopsWhenResidualHitsTolerance() throws Exception {
        // f(x) = x with target 2.5 and initial x = 2 gives the residual -0.5 exactly.
        // With tol = 0.5 the iteration must not stop on the very first comparison
        // (0.5 < 0.5 is false), it steps to 2.5 first and only stops at the second round.
        var solver = eqHelperSolver(x -> x, 2.5);
        assertEquals(2.0, solver.getNewtonInitialX());
        var results = solver.solveNewton(0.5, 10);
        assertEquals(2.0, results[1]);
        assertEquals(2.5, results[0], 1e-6);
    }

    @Test
    void eqBisectionRejectsSameSignBracketBelowTarget() throws Exception {
        // f(x) = x with target 5 on [4, 4.5]: f(4)-5 = -1 and f(4.5)-5 = -0.5, both below the
        // target, so no sign change exists and the bracket is refused.
        var solver = eqHelperSolver(x -> x, 5.0);
        solver.setBisectionLeft(4.0);
        solver.setBisectionRight(4.5);
        var failure = assertThrows(IllegalArgumentException.class, () -> solver.solveBisection(1e-10, 10));
        assertTrue(failure.getMessage().contains("同号"), failure.getMessage());
    }

    @Test
    void eqBisectionAcceptsRootSittingOnRightBoundary() throws Exception {
        // f(x) = 10 - x with target 5 on [3, 5]: f(3)-5 = 2 and f(5)-5 = 0.
        // A zero at the right end is still a bracket, the scan is skipped because
        // (5-3)/2 = 1 is not greater than tol = 2, so the returned midpoint is the left end.
        var solver = eqHelperSolver(x -> 10 - x, 5.0);
        solver.setBisectionLeft(3.0);
        solver.setBisectionRight(5.0);
        var results = solver.solveBisection(2.0, 7);
        assertEquals(3.0, results[0]);
        assertEquals(7.0, results[1]);
    }

    @Test
    void eqBisectionSkipsScanWhenHalfWidthEqualsTolerance() throws Exception {
        // f(x) = x with target 3 on [2, 4] and tol = 1 shares the same width/direction
        // arithmetic: (4-2)/2 = 1 is not greater than 1, so nothing is scanned at all.
        var solver = eqHelperSolver(x -> x, 3.0);
        solver.setBisectionLeft(2.0);
        solver.setBisectionRight(4.0);
        var results = solver.solveBisection(1.0, 5);
        assertEquals(2.0, results[0]);
        assertEquals(5.0, results[1]);
    }

    @Test
    void eqBisectionHalvesWhenResidualEqualsTolerance() throws Exception {
        // f(x) = x with target 1 on [0, 4]:
        //   step 1 midpoint 2 -> residual 1, which is not below tol = 1, so the scan continues
        //   step 2 half width (2-0)/2 = 1 is not greater than 1 -> stop, reported mid is 2
        var solver = eqHelperSolver(x -> x, 1.0);
        solver.setBisectionLeft(0.0);
        solver.setBisectionRight(4.0);
        var results = solver.solveBisection(1.0, 10);
        assertEquals(2.0, results[0]);
        assertEquals(10.0, results[1]);
    }

    @Test
    void eqBisectionTreatsExactZeroResidualAsBracketHit() throws Exception {
        // f(x) = 10 - x with target 5 on [1, 9] and tol = 0:
        //   step 1 midpoint 5 -> residual 0 (not below tol 0) -> the zero residual must move the
        //   right end to 5, so step 2 midpoint is 3 and the scan ends after maxIter = 2 rounds.
        var solver = eqHelperSolver(x -> 10 - x, 5.0);
        solver.setBisectionLeft(1.0);
        solver.setBisectionRight(9.0);
        var results = solver.solveBisection(0.0, 2);
        assertEquals(3.0, results[0]);
        assertEquals(2.0, results[1]);
    }

    // ------------------------------------------------------ EquationSolverExpression

    @Test
    void eqExpressionCompileParsesParametersAndDetectsInterval() throws Exception {
        try (MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked =
                     Mockito.mockStatic(net.mooctest.expression.engine.DynamicFunctionCompiler.class)) {
            eqHelperInstallCompiler(mocked);
            // "10-x*x" -> numbers [10, x, x], unknown slots {1,2}; compiled body param0-param1*param2
            var expression = net.mooctest.expression.engine.EquationSolverExpression.compile("10-x*x=1", "eqCompiledExpression");
            assertNotNull(expression);
            assertEquals("10-x*x", expression.getExpressionStr());
            assertEquals("eqCompiledExpression", expression.getCalculationName());
            assertEquals(1.0, expression.getResult());
            var jvm = expression.getJvmExpression();
            assertNotNull(jvm);
            assertEquals(3, jvm.getLength());
            assertEquals(10.0, jvm.getParamNumber(0));
            // constructor must have auto detected the interval of g(x) = 9 - x*x
            assertEquals(2.0, expression.getBisectionLeft());
            assertEquals(3.0, expression.getBisectionRight());
            assertEquals(2.5, expression.getNewtonInitialX());
        }
    }

    @Test
    void eqExpressionExplainSubstitutesNumberParameters() throws Exception {
        try (MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked =
                     Mockito.mockStatic(net.mooctest.expression.engine.DynamicFunctionCompiler.class)) {
            eqHelperInstallCompiler(mocked);
            var expression = net.mooctest.expression.engine.EquationSolverExpression.compile("10-x*x=1", "eqExplainExpression");
            assertEquals("10.0-x*x = 1.0", expression.explain());
            // only the known slot 0 may be rewritten, and the compiled parameter array must follow it
            expression.setKnownNumber(0, 20.0);
            assertEquals(20.0, expression.getJvmExpression().getParamNumber(0));
            assertEquals("20.0-x*x = 1.0", expression.explain());
        }
    }

    @Test
    void eqExpressionRefusesToOverwriteUnknownSlotAsKnownNumber() throws Exception {
        try (MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked =
                     Mockito.mockStatic(net.mooctest.expression.engine.DynamicFunctionCompiler.class)) {
            eqHelperInstallCompiler(mocked);
            var expression = net.mooctest.expression.engine.EquationSolverExpression.compile("10-x*x=1", "eqKnownGuardExpression");
            double before = expression.getJvmExpression().getParamNumber(1);
            var failure = assertThrows(IllegalArgumentException.class, () -> expression.setKnownNumber(1, 5.0));
            assertEquals("未知数索引不能被修改！", failure.getMessage());
            assertTrue(expression.indexIsUnKnownNumber(1));
            assertEquals(before, expression.getJvmExpression().getParamNumber(1));
        }
    }

    @Test
    void eqExpressionPromotesUnknownSlotToKnownNumber() throws Exception {
        try (MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked =
                     Mockito.mockStatic(net.mooctest.expression.engine.DynamicFunctionCompiler.class)) {
            eqHelperInstallCompiler(mocked);
            var expression = net.mooctest.expression.engine.EquationSolverExpression.compile("10-x*x=1", "eqPromoteExpression");
            expression.setUnKnownNumber(1, 5.0);
            assertFalse(expression.indexIsUnKnownNumber(1));
            assertTrue(expression.indexIsUnKnownNumber(2));
            // the compiled parameter slot and the explanation copy both have to carry 5.0 now
            assertEquals(5.0, expression.getJvmExpression().getParamNumber(1));
            assertEquals("10.0-5.0*x = 1.0", expression.explain());
        }
    }

    @Test
    void eqExpressionReportsUnknownSlotMembership() throws Exception {
        try (MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked =
                     Mockito.mockStatic(net.mooctest.expression.engine.DynamicFunctionCompiler.class)) {
            eqHelperInstallCompiler(mocked);
            var expression = net.mooctest.expression.engine.EquationSolverExpression.compile("10-x*x=1", "eqMembershipExpression");
            assertFalse(expression.indexIsUnKnownNumber(0));
            assertTrue(expression.indexIsUnKnownNumber(1));
            assertTrue(expression.indexIsUnKnownNumber(2));
            assertFalse(expression.indexIsUnKnownNumber(3));
        }
    }

    @Test
    void eqExpressionNewtonProducesEquationRoot() throws Exception {
        try (MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked =
                     Mockito.mockStatic(net.mooctest.expression.engine.DynamicFunctionCompiler.class)) {
            eqHelperInstallCompiler(mocked);
            var expression = net.mooctest.expression.engine.EquationSolverExpression.compile("10-x*x=1", "eqRootExpression");
            var results = expression.calculation(false);
            assertNotNull(results);
            // 10 - x*x = 1  <=>  x = 3 (positive branch, newton initial guess 2.5)
            assertEquals(3.0, results.getResult().doubleValue(), 1e-9);
            assertEquals("eqRootExpression", results.getCalculationSourceName());
            assertTrue(results.getResultLayers() >= 1);
            assertTrue(results.getResultLayers() <= 100);
        }
    }

    @Test
    void eqExpressionCloneKeepsEquationStateAndSolvesAgain() throws Exception {
        try (MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked =
                     Mockito.mockStatic(net.mooctest.expression.engine.DynamicFunctionCompiler.class)) {
            eqHelperInstallCompiler(mocked);
            var expression = net.mooctest.expression.engine.EquationSolverExpression.compile("10-x*x=1", "eqCloneSource");
            var clone = expression.cloneExpression();
            assertNotNull(clone);
            assertNotSame(expression, clone);
            assertEquals("10-x*x", clone.getExpressionStr());
            assertEquals("eqCloneSource_clone", clone.getCalculationName());
            assertEquals(1.0, clone.getResult());
            assertEquals(2.5, clone.getNewtonInitialX());
            assertEquals(3.0, clone.calculation(false).getResult().doubleValue(), 1e-9);
            assertEquals("10.0-x*x = 1.0", clone.explain());
            // the source keeps working on its own parameter array
            assertEquals(3.0, expression.calculation(false).getResult().doubleValue(), 1e-9);
        }
    }

    // ------------------------------------------------------------- Singleton solvers

    @Test
    void eqSingletonSolvingCompilesAndCalculatesEquation() throws Exception {
        String name = "eqSnippetSingletonSolving";
        net.mooctest.expression.engine.CalculationManagement.unregister(name);
        try (MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked =
                     Mockito.mockStatic(net.mooctest.expression.engine.DynamicFunctionCompiler.class)) {
            eqHelperInstallCompiler(mocked);
            var calculator = net.mooctest.expression.engine.SingletonEquationSolving.getInstance(name);
            var solver = calculator.compile("10-x*x=1", true);
            assertNotNull(solver);
            assertEquals(name, solver.getCalculationName());
            assertEquals(1.0, solver.getResult());
            assertEquals(2.5, solver.getNewtonInitialX());
            var results = calculator.calculation("10-x*x=1", false);
            assertNotNull(results);
            assertEquals(3.0, results.getResult().doubleValue(), 1e-9);
            assertEquals(name, results.getCalculationSourceName());
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(name);
        }
    }

    @Test
    void eqSingletonSolvingTwoCompilesAndCalculatesEquation() throws Exception {
        String name = "eqSnippetSingletonSolvingTwo";
        net.mooctest.expression.engine.CalculationManagement.unregister(name);
        try (MockedStatic<net.mooctest.expression.engine.DynamicFunctionCompiler> mocked =
                     Mockito.mockStatic(net.mooctest.expression.engine.DynamicFunctionCompiler.class)) {
            eqHelperInstallCompiler(mocked);
            var calculator = net.mooctest.expression.engine.SingletonEquationSolvingTwo.getInstance(name);
            var expression = calculator.compile("10-x*x=1", true);
            assertNotNull(expression);
            assertEquals(name, expression.getCalculationName());
            assertEquals("10.0-x*x = 1.0", expression.explain());
            var results = calculator.calculation("10-x*x=1", false);
            assertNotNull(results);
            assertEquals(3.0, results.getResult().doubleValue(), 1e-9);
            assertEquals(name, results.getCalculationSourceName());
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(name);
        }
    }

    // UNREACHABLE-OR-EQUIVALENT:
    // EquationSolver L355 ConditionalsBoundaryMutator (exp <= MAX_RANGE -> exp < MAX_RANGE):
    //   exp only ever holds 1,2,4,...,512 and then 1024 (powers of two) and never equals
    //   MAX_RANGE = 1000.0, so both comparisons select exactly the same iterations -> equivalent.
    // EquationSolver L355 MathMutator (exp *= 2 -> exp /= 2):
    //   the mutated cursor shrinks towards 0 and forever satisfies exp <= 1000, so the only
    //   observable effect is a non terminating search; it is detected as PIT TIMED_OUT, but no
    //   test can kill it through a value/exception assertion without hanging.
    // EquationSolver L120 ConditionalsBoundaryMutator (Math.abs(dfx) < 1e-12 -> <=):
    //   killing it needs the central difference (eval(x+h)-eval(x-h))/(2h) to be exactly 1e-12;
    //   h = 1e-6 is not representable in binary, so the boundary value is not constructible
    //   deterministically -> cannot be observed.

    // Complex / function expression additions
    // =================================================================
    // cx* : complex / function expression engine coverage.
    // Every expected value below is hand-derived from the production logic
    // (never produced by re-calling the method under assertion):
    //   a prefix expression is compiled as "formula+0", so its result carries
    //   one layer per operator; a complex result carries the sum of the layer
    //   counts of its real and imaginary halves.
    // =================================================================

    private static net.mooctest.expression.engine.ParsedSignature cxHelperSignature(String name, String[] params, String expression) throws Exception {
        java.lang.reflect.Constructor<?> constructor = net.mooctest.expression.engine.ParsedSignature.class
                .getDeclaredConstructor(String.class, String[].class, String.class);
        constructor.setAccessible(true);
        return (net.mooctest.expression.engine.ParsedSignature) constructor.newInstance(name, params, expression);
    }

    private static void cxHelperRegisterFunction(String signature) throws Exception {
        assertTrue(net.mooctest.expression.engine.CalculationManagement.register(
                net.mooctest.expression.engine.ExpressionFunction.parse(signature)));
    }

    // ComplexCalculation#getInstance : registered ComplexCalculation is reused,
    // and a foreign Calculation registered under the name is rejected.
    @Test
    void cxComplexGetInstanceReusesRegisteredComponent() {
        String label = "CxComplexRegistered";
        try {
            var created = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
            assertNotNull(created);
            assertEquals(label, created.getName());
            var fetched = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
            assertSame(created, fetched);
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void cxComplexGetInstanceRejectsForeignComponent() {
        String label = "CxComplexForeign";
        try {
            assertNotNull(net.mooctest.expression.engine.BooleanCalculation2.getInstance(label));
            var error = assertThrows(net.mooctest.expression.engine.ExtractException.class,
                    () -> net.mooctest.expression.engine.ComplexCalculation.getInstance(label));
            assertTrue(error.getMessage().contains(label));
            assertTrue(error.getMessage().contains("ComplexCalculation"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // ComplexCalculation#check validates the real half and the imaginary half.
    // "2*+3i"  : the real half "2*" is malformed.
    // "2+3*i"  : the imaginary half "+3*" is malformed.
    // "(2+3)+4i": both halves are valid.
    @Test
    void cxComplexCheckValidatesRealAndImaginaryHalves() {
        String label = "CxComplexCheck";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            assertDoesNotThrow(() -> calc.check("(2+3)+4i"));
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calc.check("2*+3i"));
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calc.check("2+3*i"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // ComplexCalculation#explain : LogResults metadata (setNameJoin / setPrefix /
    // setResult), bracket counting (++count / --count) and the returned value.
    // "(2+3)+4i" => real "(2+3)" = 5 (one operator layer), imaginary "+4" = 4.
    @Test
    void cxComplexExplainRecordsLogMetadata() {
        String label = "CxComplexExplain";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            var log = calc.explain("(2+3)+4i", true);
            assertNotNull(log);
            assertFalse(log.isNameJoin());
            assertEquals(log.getName() + "(\"(2+3)+4i\")", log.getPrefix());
            var result = log.getResult();
            assertTrue(result instanceof net.mooctest.expression.engine.CalculationComplexResults);
            var complex = (net.mooctest.expression.engine.CalculationComplexResults) result;
            assertEquals(5.0, complex.toComplexNumber().getReal(), 1e-12);
            assertEquals(4.0, complex.toComplexNumber().getImaginary(), 1e-12);
            assertEquals(complex.toString(), log.get("result"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // "7+8" has no 'i' : the imaginary part is missing, so explain must reject
    // it with UnsupportedOperationException (not a string-index failure).
    @Test
    void cxComplexExplainRejectsMissingImaginaryPart() {
        String label = "CxComplexNoImaginary";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            var error = assertThrows(UnsupportedOperationException.class, () -> calc.explain("7+8", true));
            assertTrue(error.getMessage().contains("The imaginary part of the complex number is not found"));
            assertTrue(error.getMessage().contains("+8"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // "7" contains no top-level +/- : the scan reaches the end and reports it.
    @Test
    void cxComplexExplainRejectsExpressionWithoutOperator() {
        String label = "CxComplexNoOperator";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            var error = assertThrows(RuntimeException.class, () -> calc.explain("7", true));
            assertEquals("7 is not a complex expression.", error.getMessage());
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // ComplexCalculation#calculation on the default (non BigDecimal) option.
    @Test
    void cxComplexCalculationReturnsComplexNumberResult() {
        String label = "CxComplexCalculation";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            var result = calc.calculation("(2+3)+4i", true);
            assertNotNull(result);
            assertEquals(5.0, result.getResult(), 1e-12);
            assertEquals(2, result.getResultLayers());
            assertTrue(result instanceof net.mooctest.expression.engine.CalculationComplexResults);
            var complex = (net.mooctest.expression.engine.CalculationComplexResults) result;
            assertEquals(4.0, complex.toComplexNumber().getImaginary(), 1e-12);
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // ComplexCalculation#calculation with useBigDecimal enabled must consult the
    // BigDecimal cache path of the compiled expression, not the plain one.
    @Test
    void cxComplexCalculationUsesBigDecimalBranchWhenEnabled() {
        String label = "CxComplexBigBranch";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        Boolean previous = net.mooctest.expression.engine.Mathematical_Expression.Options.isUseBigDecimal();
        try {
            net.mooctest.expression.engine.Mathematical_Expression.Options.setUseBigDecimal(true);
            try (var construction = org.mockito.Mockito.mockConstruction(
                    net.mooctest.expression.engine.ComplexExpression.class, (mock, context) -> {
                        when(mock.calculationBigDecimalsCache(false)).thenReturn(
                                new net.mooctest.expression.engine.CalculationComplexResults(0, 7, 2, "CxBigDecimalBranch"));
                        when(mock.calculationCache(false)).thenReturn(
                                new net.mooctest.expression.engine.CalculationComplexResults(0, -7, -2, "CxPlainBranch"));
                    })) {
                var result = calc.calculation("(2+3)+4i", true);
                assertNotNull(result);
                assertEquals(1, construction.constructed().size());
                assertEquals("CxBigDecimalBranch", result.getCalculationSourceName());
                assertEquals(7.0, result.getResult(), 1e-12);
                verify(construction.constructed().get(0), times(1)).calculationBigDecimalsCache(false);
            }
        } finally {
            net.mooctest.expression.engine.Mathematical_Expression.Options.setUseBigDecimal(previous);
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // ComplexCalculation#compile formats " (2+3) + 4i " into "(2+3)+4i".
    @Test
    void cxComplexCompileBigDecimalFormatsAndReturnsExpression() {
        String label = "CxComplexCompile";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            assertEquals("(2+3)+4i", calc.compile(" (2+3) + 4i ", true).getExpressionStr());
            var compiled = calc.compileBigDecimal(" (2+3) + 4i ", true);
            assertNotNull(compiled);
            assertEquals("(2+3)+4i", compiled.getExpressionStr());
            var result = compiled.calculationBigDecimals(true);
            assertNotNull(result);
            assertEquals(5.0, result.getResult(), 1e-12);
            assertEquals(4.0, result.toComplexNumber().getImaginary(), 1e-12);
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // ComplexCalculation#compileBigDecimal formats the formula itself before it
    // delegates to the overridable compile(Formula, formatRequired).
    @Test
    void cxComplexCompileBigDecimalFormatsBeforeDelegatingToCompile() {
        String label = "CxComplexFormatDelegate";
        String[] captured = new String[1];
        var calc = new net.mooctest.expression.engine.ComplexCalculation(label) {
            @Override
            public net.mooctest.expression.engine.ComplexExpression compile(String Formula, boolean formatRequired) {
                captured[0] = Formula;
                return null;
            }
        };
        calc.compileBigDecimal(" (2+3) + 4i ", true);
        assertEquals("(2+3)+4i", captured[0]);
    }

    // ComplexExpression#convertToMultiPrecisionSupported must forward to BOTH halves.
    @Test
    void cxComplexExpressionConvertsBothHalvesToMultiPrecision() {
        var left = mock(net.mooctest.expression.engine.FunctionExpression.class);
        var right = mock(net.mooctest.expression.engine.FunctionExpression.class);
        var expression = new net.mooctest.expression.engine.ComplexExpression("1+2i", "CxMultiPrecision", left, right, '+');
        expression.convertToMultiPrecisionSupported();
        verify(left, times(1)).convertToMultiPrecisionSupported();
        verify(right, times(1)).convertToMultiPrecisionSupported();
    }

    // ComplexExpression#isBigDecimal / isUnBigDecimal delegate to the real half only.
    @Test
    void cxComplexExpressionDelegatesPrecisionFlagsToRealPart() {
        var left = mock(net.mooctest.expression.engine.FunctionExpression.class);
        when(left.isBigDecimal()).thenReturn(false);
        when(left.isUnBigDecimal()).thenReturn(false);
        var right = mock(net.mooctest.expression.engine.FunctionExpression.class);
        when(right.isBigDecimal()).thenReturn(true);
        when(right.isUnBigDecimal()).thenReturn(true);
        var expression = new net.mooctest.expression.engine.ComplexExpression("1+2i", "CxPrecisionFlags", left, right, '+');
        assertFalse(expression.isBigDecimal());
        assertFalse(expression.isUnBigDecimal());
    }

    // ComplexExpression#calculation : the sign of the imaginary half is applied
    // and the result layers are the SUM of both half layer counts.
    // "(2+3)+4i"      => real 5 (1 layer) + imag +4 (1 layer) => layers 2.
    // "(1+2)-(3*4)i"  => real 3 (1 layer) + imag -(3*4) = -12 (1 layer),
    //                    then the '-' sign negates it => +12, layers 2.
    @Test
    void cxComplexExpressionCalculatesSignedImaginaryPart() {
        String label = "CxComplexExpressionCalc";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            var positive = calc.compile("(2+3)+4i", true).calculation(true);
            assertNotNull(positive);
            assertEquals(5.0, positive.getResult(), 1e-12);
            assertEquals(4.0, positive.toComplexNumber().getImaginary(), 1e-12);
            assertEquals(2, positive.getResultLayers());

            var negative = calc.compile("(1+2)-(3*4)i", true).calculation(true);
            assertNotNull(negative);
            assertEquals(3.0, negative.getResult(), 1e-12);
            assertEquals(12.0, negative.toComplexNumber().getImaginary(), 1e-12);
            assertEquals(2, negative.getResultLayers());
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // ComplexExpression#calculationBigDecimals : same hand-computed values.
    @Test
    void cxComplexExpressionBigDecimalCalculationKeepsSignedImaginaryPart() {
        String label = "CxComplexExpressionBigCalc";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            var positive = calc.compile("(2+3)+4i", true).calculationBigDecimals(true);
            assertNotNull(positive);
            assertEquals(5.0, positive.getResult(), 1e-12);
            assertEquals(4.0, positive.toComplexNumber().getImaginary(), 1e-12);
            assertEquals(2, positive.getResultLayers());

            var negative = calc.compile("(1+2)-(3*4)i", true).calculationBigDecimals(true);
            assertNotNull(negative);
            assertEquals(3.0, negative.getResult(), 1e-12);
            assertEquals(12.0, negative.toComplexNumber().getImaginary(), 1e-12);
            assertEquals(2, negative.getResultLayers());
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // ComplexExpression#calculationCache / calculationBigDecimalsCache.
    @Test
    void cxComplexExpressionCacheMethodsReturnTypedComplexResults() {
        String label = "CxComplexExpressionCache";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            var cached = calc.compile("(2+3)+4i", true).calculationCache(true);
            assertNotNull(cached);
            assertEquals(5.0, cached.getResult(), 1e-12);
            assertEquals(4.0, cached.toComplexNumber().getImaginary(), 1e-12);

            var cachedBig = calc.compile("(2+3)+4i", true).calculationBigDecimalsCache(true);
            assertNotNull(cachedBig);
            assertEquals(5.0, cachedBig.getResult(), 1e-12);
            assertEquals(4.0, cachedBig.toComplexNumber().getImaginary(), 1e-12);
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    // FunctionExpression#subCompile returns the unchanged builder for a plain
    // number and for an expression without function names.
    @Test
    void cxFunctionExpressionStaticSubCompileReturnsInputWhenNothingToReplace() {
        var start = new Stack<Integer>();
        var end = new Stack<Integer>();
        var names = new Stack<String>();
        assertEquals("123", net.mooctest.expression.engine.FunctionExpression.subCompile("123", start, end, names).toString());
        assertEquals("1+2", net.mooctest.expression.engine.FunctionExpression.subCompile("1+2", start, end, names).toString());
    }

    // FunctionExpression#subCompile must not touch the stacks when there is
    // nothing to replace: a numeric formula or an empty funNames stack.
    @Test
    void cxFunctionExpressionStaticSubCompileReturnsEarlyWhenNoWorkToDo() {
        var numericStart = new Stack<Integer>();
        numericStart.push(1);
        var numericEnd = new Stack<Integer>();
        numericEnd.push(2);
        var numericNames = new Stack<String>();
        numericNames.push("cxUnregisteredFunction");
        assertEquals("123", net.mooctest.expression.engine.FunctionExpression.subCompile("123", numericStart, numericEnd, numericNames).toString());
        assertEquals(1, numericStart.size());
        assertEquals(1, numericEnd.size());
        assertEquals(1, numericNames.size());

        var plainStart = new Stack<Integer>();
        plainStart.push(1);
        var plainEnd = new Stack<Integer>();
        plainEnd.push(2);
        var plainNames = new Stack<String>();
        assertEquals("1+2", net.mooctest.expression.engine.FunctionExpression.subCompile("1+2", plainStart, plainEnd, plainNames).toString());
        assertEquals(1, plainStart.size());
    }

    // FunctionExpression#getFunNames exposes a non-null stack of the function names.
    @Test
    void cxFunctionExpressionGetFunNamesExposesFunctionNames() {
        var plain = net.mooctest.expression.engine.FunctionExpression.compile("1+2", "CxPlainNames");
        var plainNames = plain.getFunNames();
        assertNotNull(plainNames);
        assertEquals(0, plainNames.size());

        var withFunction = net.mooctest.expression.engine.FunctionExpression.compile("cxfn(1)+2", "CxFunctionNames");
        var names = withFunction.getFunNames();
        assertNotNull(names);
        assertEquals(1, names.size());
        assertEquals("cxfn", names.peek());
    }

    // FunctionExpression#setFunName rewrites the name inside the stored expression.
    // "cxfn(1)+2" -> "cxrr(1)+2"; cxrr(a)=a+100 => 101+2 = 103.
    @Test
    void cxFunctionExpressionSetFunNameRewritesExpression() throws Exception {
        cxHelperRegisterFunction("cxrr(a)=a+100");
        try {
            var expression = net.mooctest.expression.engine.FunctionExpression.compile("cxfn(1)+2", "CxRename");
            expression.setFunName(0, "cxrr");
            assertEquals("cxrr(1)+2", expression.getExpressionStr());
            assertEquals(103.0, expression.calculationCache(true).getResult(), 1e-12);
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregisterF("cxrr");
        }
    }

    // FunctionExpression#subCompile(boolean) : an expression without functions
    // cannot be deep-compiled again.
    @Test
    void cxFunctionExpressionSubCompileRejectsPlainExpression() {
        var expression = net.mooctest.expression.engine.FunctionExpression.compile("1+2", "CxPlainSubCompile");
        var error = assertThrows(UnsupportedOperationException.class, () -> expression.subCompile(true));
        assertTrue(error.getMessage().contains("此表达式不需要深度编译了"));
        assertTrue(error.getMessage().contains("1+2"));
    }

    // FunctionExpression#subCompileBigDecimals : an expression without functions
    // cannot be deep-compiled again.
    @Test
    void cxFunctionExpressionSubCompileBigDecimalsRejectsPlainExpression() {
        var expression = net.mooctest.expression.engine.FunctionExpression.compile("1+2", "CxPlainBigCompile");
        var error = assertThrows(UnsupportedOperationException.class, () -> expression.subCompileBigDecimals(true));
        assertTrue(error.getMessage().contains("此表达式不需要深度编译了"));
        assertTrue(error.getMessage().contains("1+2"));
    }

    // FunctionExpression#subCompileBigDecimals on a real function expression.
    // cxfn(a)=a*2, cxfn(2)=4 -> "4.0+1" -> 5.
    @Test
    void cxFunctionExpressionSubCompileBigDecimalsBuildsCompiledExpression() throws Exception {
        cxHelperRegisterFunction("cxfn(a)=a*2");
        try {
            var expression = net.mooctest.expression.engine.FunctionExpression.compile("cxfn(2)+1", "CxBigCompile");
            var compiled = expression.subCompileBigDecimals(true);
            assertNotNull(compiled);
            assertEquals("4.0+1", compiled.getExpressionStr());
            var result = compiled.calculationBigDecimalsCache(true);
            assertNotNull(result);
            assertEquals(5.0, ((Number) result.getResult()).doubleValue(), 1e-12);
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregisterF("cxfn");
        }
    }

    // FunctionExpression#calculationCache / calculationBigDecimalsCache tag the
    // result with the expression's own calculation name.
    @Test
    void cxFunctionExpressionCachesTagResultSource() throws Exception {
        cxHelperRegisterFunction("cxfn(a)=a*2");
        try {
            var expression = net.mooctest.expression.engine.FunctionExpression.compile("cxfn(2)+1", "CxSourceTag");
            var plain = expression.calculationCache(true);
            assertNotNull(plain);
            assertEquals(5.0, plain.getResult(), 1e-12);
            assertEquals("CxSourceTag", plain.getCalculationSourceName());

            var another = net.mooctest.expression.engine.FunctionExpression.compile("cxfn(2)+1", "CxBigSourceTag");
            var big = another.calculationBigDecimalsCache(true);
            assertNotNull(big);
            assertEquals(5.0, big.getResult(), 1e-12);
            assertEquals("CxBigSourceTag", big.getCalculationSourceName());
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregisterF("cxfn");
        }
    }

    // FunctionExpression keeps copies of start / end / funNames when isCopy is
    // true, so callers of the copy getters cannot damage the expression.
    @Test
    void cxFunctionExpressionCopiedStacksAreIndependent() throws Exception {
        var expression = net.mooctest.expression.engine.FunctionExpression.compile("cxfn(2)+1", "CxCopiedStacks");

        var startGetter = net.mooctest.expression.engine.FunctionExpression.class.getDeclaredMethod("getStart", boolean.class);
        startGetter.setAccessible(true);
        @SuppressWarnings("unchecked")
        var firstStart = (Stack<Integer>) startGetter.invoke(expression, true);
        assertEquals(1, firstStart.size());
        firstStart.clear();
        @SuppressWarnings("unchecked")
        var secondStart = (Stack<Integer>) startGetter.invoke(expression, true);
        assertEquals(1, secondStart.size());

        var endGetter = net.mooctest.expression.engine.FunctionExpression.class.getDeclaredMethod("getEnd", boolean.class);
        endGetter.setAccessible(true);
        @SuppressWarnings("unchecked")
        var firstEnd = (Stack<Integer>) endGetter.invoke(expression, true);
        assertEquals(1, firstEnd.size());
        firstEnd.clear();
        @SuppressWarnings("unchecked")
        var secondEnd = (Stack<Integer>) endGetter.invoke(expression, true);
        assertEquals(1, secondEnd.size());

        var firstNames = expression.getFunNames();
        assertEquals(1, firstNames.size());
        firstNames.clear();
        var secondNames = expression.getFunNames();
        assertEquals(1, secondNames.size());
        assertEquals("cxfn", secondNames.peek());
    }

    // ParsedSignature getters and toString.
    @Test
    void cxParsedSignatureExposesParsedMetadata() throws Exception {
        var signature = cxHelperSignature("cxps", new String[]{"a", "b"}, "a+b");
        assertEquals("cxps", signature.getName());
        assertArrayEquals(new String[]{"a", "b"}, signature.getParamNames());
        assertEquals("a+b", signature.getExpression());
        assertEquals("cxps(a,b)=a+b", signature.toString());
    }

    // SubCompileExpression#calculationBigDecimals without functions: the bracket
    // component still evaluates "1+2" directly => 3.
    @Test
    void cxSubCompileExpressionBigDecimalCalculationHandlesPlainExpression() {
        var expression = net.mooctest.expression.engine.FunctionExpression.compile("1+2", "CxSubPlain");
        var result = expression.calculationBigDecimals(true);
        assertNotNull(result);
        assertEquals(3.0, ((Number) result.getResult()).doubleValue(), 1e-12);
    }

    // SubCompileExpression#calculationBigDecimals with a function: deep compile
    // then evaluate. cxfn(a)=a*2, cxfn(2)+1 = 4+1 = 5.
    @Test
    void cxSubCompileExpressionBigDecimalCalculationHandlesFunctionExpression() throws Exception {
        cxHelperRegisterFunction("cxfn(a)=a*2");
        try {
            var expression = net.mooctest.expression.engine.FunctionExpression.compile("cxfn(2)+1", "CxSubFunction");
            var result = expression.calculationBigDecimals(true);
            assertNotNull(result);
            assertEquals(5.0, ((Number) result.getResult()).doubleValue(), 1e-12);
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregisterF("cxfn");
        }
    }

    // UNREACHABLE-OR-EQUIVALENT:
    // net.mooctest.expression.engine.ComplexCalculation line 157 (ConditionalsBoundaryMutator): the guard is
    //   `i == -1 || Formula.length() <= stringBuilder.length()`. Before the guard runs, stringBuilder holds exactly
    //   the characters preceding the current +/- sign, so stringBuilder.length() == signIndex and
    //   Formula.length() >= signIndex + 1; the second comparison is never true, so `<` and `<=` behave identically.
    // net.mooctest.expression.engine.ComplexCalculation line 157 (RemoveConditionalMutator_ORDER_ELSE): same second
    //   comparison of the same guard; replacing it with false changes nothing because it is always false (and it is
    //   short-circuited whenever the first operand is true).

    // Pricing / refund boundary additions
    // =================================================================
    // Mutation-killing suite for AdvancedPricingService / RefundPolicyService
    // Every expectation is derived by hand from the rules in the production code.
    // =================================================================

    private static net.mooctest.pricing.domain.PriceBreakdown prHelperPriced(Object... fields) {
        return priced(changed(price(), fields));
    }

    private static net.mooctest.refund.domain.PurchaseSnapshot prHelperReceipt(Object... fields) {
        return changed(purchase(), fields);
    }

    private static net.mooctest.refund.domain.RefundDecision prHelperEvaluated(Object... fields) {
        return refunded(changed(refund(), fields));
    }

    // -----------------------------------------------------------------
    // AdvancedPricingService
    // -----------------------------------------------------------------

    /** L51/L52: the promotion/coupon mutual-exclusion guard must only fire for strictly positive discounts. */
    @Test void prPricingStackingBoundaryAtZeroDiscounts() {
        var zeroValuePromotion = prHelperPriced(
                "promotion", changed(promotion(), "value", BigDecimal.ZERO),
                "coupon", changed(coupon(), "type", CouponType.FIXED, "value", bd("15"), "stackable", false));
        amount("0", zeroValuePromotion.promotionDiscount());
        amount("15", zeroValuePromotion.couponDiscount());
        amount("90.10", zeroValuePromotion.total());
        assertTrue(zeroValuePromotion.warnings().contains("Promotion value must be positive"));
        assertFalse(zeroValuePromotion.warnings().contains("The promotion was ignored because the coupon gives a greater discount"));
        assertTrue(zeroValuePromotion.appliedRules().contains("COUPON_FIXED"));

        var zeroCoupon = prHelperPriced(
                "promotion", promotion(),
                "coupon", changed(coupon(), "type", CouponType.CASHBACK, "stackable", false));
        amount("10", zeroCoupon.promotionDiscount());
        amount("0", zeroCoupon.couponDiscount());
        amount("95.40", zeroCoupon.total());
        assertTrue(zeroCoupon.appliedRules().contains("COUPON_CASHBACK_FUTURE"));
        assertTrue(zeroCoupon.appliedRules().contains("PROMOTION_PERCENT"));
        assertTrue(zeroCoupon.warnings().contains("Cashback is credited after payment and does not reduce this order"));
        assertFalse(zeroCoupon.warnings().contains("The coupon was ignored because the promotion gives a greater discount"));
    }

    /** L57/L61: dropping the losing discount must remove only the losing rule family, never every rule. */
    @Test void prPricingMutualExclusionRuleBookkeeping() {
        var promotionWins = prHelperPriced(
                "membershipLevel", MembershipLevel.PREMIUM,
                "promotion", changed(promotion(), "value", bd("80")),
                "coupon", changed(coupon(), "type", CouponType.FIXED, "value", bd("5"), "stackable", false));
        amount("12", promotionWins.membershipDiscount());
        amount("58", promotionWins.promotionDiscount());
        amount("0", promotionWins.couponDiscount());
        amount("31.80", promotionWins.total());
        assertTrue(promotionWins.appliedRules().containsAll(List.of("MEMBERSHIP_PREMIUM", "PROMOTION_PERCENT", "DISCOUNT_CAP")));
        assertFalse(promotionWins.appliedRules().contains("COUPON_FIXED"));
        assertTrue(promotionWins.warnings().contains("The coupon was ignored because the promotion gives a greater discount"));

        var couponWins = prHelperPriced(
                "membershipLevel", MembershipLevel.PREMIUM,
                "promotion", changed(promotion(), "type", PromotionType.FIXED, "value", bd("5")),
                "coupon", changed(coupon(), "type", CouponType.FIXED, "value", bd("40"), "stackable", false));
        amount("12", couponWins.membershipDiscount());
        amount("0", couponWins.promotionDiscount());
        amount("40", couponWins.couponDiscount());
        amount("50.88", couponWins.total());
        assertTrue(couponWins.appliedRules().containsAll(List.of("MEMBERSHIP_PREMIUM", "COUPON_FIXED")));
        assertFalse(couponWins.appliedRules().contains("PROMOTION_FIXED"));
        assertTrue(couponWins.warnings().contains("The promotion was ignored because the coupon gives a greater discount"));
    }

    /** L189: the enterprise "below ten seats" warning needs quantity 9/10 to separate < from <=. */
    @Test void prPricingEnterpriseMembershipSeatBoundary() {
        var belowTen = prHelperPriced("quantity", 5, "membershipLevel", MembershipLevel.ENTERPRISE);
        amount("500", belowTen.subtotal());
        amount("40", belowTen.membershipDiscount());
        amount("25", belowTen.bulkDiscount());
        assertTrue(belowTen.warnings().contains("Enterprise discount is reduced for orders below ten seats"));

        var qtyTen = prHelperPriced("quantity", 10, "membershipLevel", MembershipLevel.ENTERPRISE);
        amount("180", qtyTen.membershipDiscount());
        amount("50", qtyTen.bulkDiscount());
        assertFalse(qtyTen.warnings().contains("Enterprise discount is reduced for orders below ten seats"));
        amount("198", prHelperPriced("quantity", 11, "membershipLevel", MembershipLevel.ENTERPRISE).membershipDiscount());

        for (int seats : new int[]{1, 9}) {
            var r = prHelperPriced("quantity", seats, "membershipLevel", MembershipLevel.ENTERPRISE);
            amount(seats == 1 ? "8" : "72", r.membershipDiscount());
            assertTrue(r.warnings().contains("Enterprise discount is reduced for orders below ten seats"));
        }
    }

    /** L271/L272/L276: promotion maximum-discount caps, equality at the cap, and the zero cap window. */
    @Test void prPricingPromotionMaximumDiscountBoundaries() {
        var cappedToZero = prHelperPriced("promotion", changed(promotion(), "maximumDiscount", BigDecimal.ZERO));
        amount("0", cappedToZero.promotionDiscount());
        amount("6", cappedToZero.tax());
        assertTrue(cappedToZero.warnings().contains("Promotion discount reached its configured cap"));
        assertFalse(cappedToZero.appliedRules().contains("PROMOTION_PERCENT"));

        var equalCap = prHelperPriced("promotion", changed(promotion(), "maximumDiscount", bd("10")));
        amount("10", equalCap.promotionDiscount());
        assertFalse(equalCap.warnings().contains("Promotion discount reached its configured cap"));
        assertTrue(equalCap.appliedRules().contains("PROMOTION_PERCENT"));

        var roomyCap = prHelperPriced("promotion", changed(promotion(), "maximumDiscount", bd("40")));
        amount("10", roomyCap.promotionDiscount());
        assertFalse(roomyCap.warnings().contains("Promotion discount reached its configured cap"));
    }

    /** L260/L344: a percent value above the hard ceiling is clamped, and coupon/promotion precedence follows the raw values. */
    @Test void prPricingPercentCeilingAndCouponPrecedence() {
        var cappedPercent = prHelperPriced("promotion", changed(promotion(), "value", bd("90"), "maximumDiscount", bd("85")));
        amount("70", cappedPercent.promotionDiscount());
        assertFalse(cappedPercent.warnings().contains("Promotion discount reached its configured cap"));
        assertTrue(cappedPercent.appliedRules().contains("DISCOUNT_CAP"));

        var couponHigher = prHelperPriced(
                "promotion", changed(promotion(), "value", bd("80")),
                "coupon", changed(coupon(), "value", bd("90"), "stackable", false));
        amount("70", couponHigher.promotionDiscount());
        amount("0", couponHigher.couponDiscount());
        assertTrue(couponHigher.warnings().contains("The coupon was ignored because the promotion gives a greater discount"));
        assertFalse(couponHigher.warnings().contains("The promotion was ignored because the coupon gives a greater discount"));
        assertFalse(couponHigher.appliedRules().contains("COUPON_PERCENT"));
    }

    /** L350/L353: cashback rule bookkeeping, positive fixed coupons, and the zero-discount window of a zero subtotal. */
    @Test void prPricingCouponCashbackAndFixedRuleBookkeeping() {
        var cashback = prHelperPriced("coupon", changed(coupon(), "type", CouponType.CASHBACK));
        amount("0", cashback.couponDiscount());
        assertTrue(cashback.appliedRules().contains("COUPON_CASHBACK_FUTURE"));
        assertTrue(cashback.warnings().contains("Cashback is credited after payment and does not reduce this order"));

        var fixed = prHelperPriced("coupon", changed(coupon(), "type", CouponType.FIXED, "value", bd("15")));
        amount("15", fixed.couponDiscount());
        assertTrue(fixed.appliedRules().contains("COUPON_FIXED"));

        var zeroBase = prHelperPriced("unitPrice", BigDecimal.ZERO,
                "coupon", changed(coupon(), "type", CouponType.FIXED, "value", bd("15")));
        amount("0", zeroBase.couponDiscount());
        amount("0", zeroBase.total());
        assertFalse(zeroBase.appliedRules().contains("COUPON_FIXED"));
        assertTrue(zeroBase.appliedRules().contains("FREE_ORDER"));
        assertTrue(zeroBase.warnings().contains("No discount value is generated for a free course"));
    }

    /** L402/L412/L416: points-vs-balance, points-vs-cap equality, and a cap that rounds the value down to zero. */
    @Test void prPricingLoyaltyDiscountBoundariesAndRuleBookkeeping() {
        var exactlyAvailable = prHelperPriced("loyaltyPointsToUse", 500, "availableLoyaltyPoints", 500);
        amount("5", exactlyAvailable.loyaltyDiscount());
        assertFalse(exactlyAvailable.warnings().contains("Requested loyalty points were reduced to the available balance"));
        assertTrue(exactlyAvailable.appliedRules().contains("LOYALTY_POINTS"));

        var aboveAvailable = prHelperPriced("loyaltyPointsToUse", 600, "availableLoyaltyPoints", 500);
        amount("5", aboveAvailable.loyaltyDiscount());
        assertTrue(aboveAvailable.warnings().contains("Requested loyalty points were reduced to the available balance"));

        var exactlyCapped = prHelperPriced("loyaltyPointsToUse", 2000, "availableLoyaltyPoints", 2000);
        amount("20", exactlyCapped.loyaltyDiscount());
        assertFalse(exactlyCapped.warnings().contains("Loyalty discount was limited to twenty percent"));

        var aboveCap = prHelperPriced("loyaltyPointsToUse", 2001, "availableLoyaltyPoints", 2001);
        amount("20", aboveCap.loyaltyDiscount());
        assertTrue(aboveCap.warnings().contains("Loyalty discount was limited to twenty percent"));

        var tinyOrder = prHelperPriced("unitPrice", bd("0.01"), "loyaltyPointsToUse", 100, "availableLoyaltyPoints", 100);
        amount("0", tinyOrder.loyaltyDiscount());
        assertTrue(tinyOrder.warnings().contains("Loyalty discount was limited to twenty percent"));
        assertFalse(tinyOrder.appliedRules().contains("LOYALTY_POINTS"));
    }

    /** L432/L448: a zero taxable amount and an unknown country must both return early without registering a tax rule. */
    @Test void prPricingZeroTaxableAmountSkipsTaxRules() {
        var freeOrder = prHelperPriced("unitPrice", BigDecimal.ZERO);
        amount("0", freeOrder.tax());
        amount("0", freeOrder.total());
        assertFalse(freeOrder.appliedRules().contains("TAX_CN"));
        for (String rule : freeOrder.appliedRules()) assertFalse(rule.startsWith("TAX_"));
        assertTrue(freeOrder.warnings().contains("No discount value is generated for a free course"));

        var unknownCountry = prHelperPriced("countryCode", "nz");
        amount("0", unknownCountry.tax());
        assertTrue(unknownCountry.warnings().contains("No tax rule is configured for country NZ"));
        assertFalse(unknownCountry.appliedRules().contains("TAX_NZ"));
    }

    /** L444: the non-profit tax halving only applies when a positive tax rate exists. */
    @Test void prPricingNonprofitTaxReductionBoundary() {
        var nonprofit = prHelperPriced("nonprofitCustomer", true);
        amount("2.85", nonprofit.tax());
        assertTrue(nonprofit.appliedRules().containsAll(List.of("NONPROFIT_TAX_REDUCTION", "TAX_CN")));

        var nonprofitWithoutRule = prHelperPriced("nonprofitCustomer", true, "countryCode", "nz");
        amount("0", nonprofitWithoutRule.tax());
        amount("5", nonprofitWithoutRule.nonprofitDiscount());
        assertFalse(nonprofitWithoutRule.appliedRules().contains("NONPROFIT_TAX_REDUCTION"));
        assertTrue(nonprofitWithoutRule.warnings().contains("No tax rule is configured for country NZ"));
    }

    /** L452/L455/L458: the currency-mismatch warnings exist only for a mismatching currency. */
    @Test void prPricingCurrencyMismatchTaxWarnings() {
        String chinese = "Chinese tax is calculated for a non-CNY order";
        String unitedStates = "United States tax is calculated for a non-USD order";
        String german = "German tax is calculated for a non-EUR order";

        assertFalse(prHelperPriced("countryCode", "cn", "currency", "cny").warnings().contains(chinese));
        assertTrue(prHelperPriced("countryCode", "cn", "currency", "USD").warnings().contains(chinese));
        amount("6", prHelperPriced("countryCode", "cn", "currency", "USD").tax());

        assertFalse(prHelperPriced("countryCode", "US", "currency", "USD").warnings().contains(unitedStates));
        assertTrue(prHelperPriced("countryCode", "US").warnings().contains(unitedStates));
        amount("7.50", prHelperPriced("countryCode", "US").tax());

        assertFalse(prHelperPriced("countryCode", "DE", "currency", "EUR").warnings().contains(german));
        assertTrue(prHelperPriced("countryCode", "DE").warnings().contains(german));
        amount("19", prHelperPriced("countryCode", "DE").tax());
    }

    // -----------------------------------------------------------------
    // RefundPolicyService
    // -----------------------------------------------------------------

    /** L38: exactly zero elapsed hours is a valid purchase instant, only a future instant is rejected. */
    @Test void prRefundZeroHourBoundary() {
        var zeroHours = prHelperEvaluated("purchase", prHelperReceipt("paidAt", NOW));
        assertTrue(zeroHours.approved());
        assertFalse(zeroHours.manualReview());
        amount("100", zeroHours.refundableAmount());
        amount("0", zeroHours.processingFee());
        assertEquals(List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), zeroHours.reasons());

        var futureHours = prHelperEvaluated("purchase", prHelperReceipt("paidAt", NOW.plusSeconds(3600)));
        assertFalse(futureHours.approved());
        assertEquals(List.of("PURCHASE_DATE_IN_FUTURE"), futureHours.reasons());
    }

    /** L74/L341/L391: requested rate at the policy limit, plus percentage 0 falling back to the eligible rate. */
    @Test void prRefundRequestedRateBoundaries() {
        var fullRate = prHelperEvaluated();
        assertFalse(fullRate.warnings().contains("REQUESTED_PERCENTAGE_REDUCED_TO_POLICY_LIMIT"));
        amount("100", fullRate.refundableAmount());

        var fullByPercentage = prHelperEvaluated("fullRefundRequested", false, "requestedPercentage", 100);
        assertFalse(fullByPercentage.warnings().contains("REQUESTED_PERCENTAGE_REDUCED_TO_POLICY_LIMIT"));
        amount("100", fullByPercentage.refundableAmount());

        var defaultedPercentage = prHelperEvaluated("fullRefundRequested", false, "requestedPercentage", 0);
        assertTrue(defaultedPercentage.approved());
        amount("100", defaultedPercentage.refundableAmount());
        assertFalse(defaultedPercentage.warnings().contains("REQUESTED_PERCENTAGE_REDUCED_TO_POLICY_LIMIT"));

        var limitedToPolicy = prHelperEvaluated(
                "fullRefundRequested", false, "requestedPercentage", 80,
                "purchase", prHelperReceipt("paidAt", NOW.minusSeconds(300 * 3600L), "progressPercent", 20, "paymentMethod", PaymentMethod.WALLET));
        amount("50", limitedToPolicy.refundableAmount());
        assertTrue(limitedToPolicy.warnings().contains("REQUESTED_PERCENTAGE_REDUCED_TO_POLICY_LIMIT"));
    }

    /** L84/L361/L364: zero paid amount, zero tax and zero discount are all legal and the fee never exceeds a zero refund. */
    @Test void prRefundZeroMoneyAmountsAreValid() {
        var zeroPaid = prHelperEvaluated("purchase", prHelperReceipt("paidAmount", BigDecimal.ZERO));
        amount("0", zeroPaid.refundableAmount());
        amount("0", zeroPaid.processingFee());
        assertFalse(zeroPaid.warnings().contains("PROCESSING_FEE_LIMITED_TO_REFUND_AMOUNT"));
        assertFalse(zeroPaid.warnings().contains("REFUND_BELOW_MINIMUM_PAYOUT"));
        assertEquals(List.of("NO_REFUNDABLE_VALUE"), zeroPaid.reasons());
        assertFalse(zeroPaid.approved());

        var zeroPaidWallet = prHelperEvaluated("purchase",
                prHelperReceipt("paidAmount", BigDecimal.ZERO, "paymentMethod", PaymentMethod.WALLET));
        amount("0", zeroPaidWallet.refundableAmount());
        assertFalse(zeroPaidWallet.warnings().contains("SMALL_REFUND_RETURNED_TO_WALLET"));
        assertEquals(List.of("NO_REFUNDABLE_VALUE"), zeroPaidWallet.reasons());

        var zeroTax = prHelperEvaluated("purchase", prHelperReceipt("taxAmount", BigDecimal.ZERO));
        amount("100", zeroTax.refundableAmount());
        assertTrue(zeroTax.approved());

        var zeroDiscount = prHelperEvaluated("purchase", prHelperReceipt("discountAmount", BigDecimal.ZERO));
        amount("100", zeroDiscount.refundableAmount());
        assertTrue(zeroDiscount.approved());
    }

    /** L105: a refund of exactly 1.00 is a normal payout; 0.99 is not. */
    @Test void prRefundSmallPayoutBoundary() {
        var exactlyOne = prHelperEvaluated("purchase", prHelperReceipt("paidAmount", bd("1"), "paymentMethod", PaymentMethod.WALLET));
        amount("1", exactlyOne.refundableAmount());
        assertTrue(exactlyOne.approved());
        assertFalse(exactlyOne.warnings().contains("SMALL_REFUND_RETURNED_TO_WALLET"));
        assertFalse(exactlyOne.warnings().contains("REFUND_BELOW_MINIMUM_PAYOUT"));

        var belowOne = prHelperEvaluated("purchase", prHelperReceipt("paidAmount", bd("0.99"), "paymentMethod", PaymentMethod.CARD));
        amount("0", belowOne.refundableAmount());
        assertTrue(belowOne.warnings().contains("REFUND_BELOW_MINIMUM_PAYOUT"));
        assertEquals(List.of("NO_REFUNDABLE_VALUE"), belowOne.reasons());
        assertFalse(belowOne.approved());

        var belowOneWallet = prHelperEvaluated("purchase", prHelperReceipt("paidAmount", bd("0.99"), "paymentMethod", PaymentMethod.WALLET));
        amount("0.99", belowOneWallet.refundableAmount());
        assertTrue(belowOneWallet.warnings().contains("SMALL_REFUND_RETURNED_TO_WALLET"));
    }

    /** L92: a coupon adjustment equal to the refundable amount consumes it instead of being deducted. */
    @Test void prRefundCouponAdjustmentEqualityBoundary() {
        var consumed = prHelperEvaluated("purchase", prHelperReceipt("couponUsed", true, "discountAmount", bd("100")));
        amount("0", consumed.refundableAmount());
        assertTrue(consumed.warnings().contains("COUPON_VALUE_CONSUMED_REFUND"));
        assertFalse(consumed.warnings().contains("COUPON_VALUE_NOT_REFUNDED"));
        assertFalse(consumed.approved());

        var justBelow = prHelperEvaluated("purchase",
                prHelperReceipt("couponUsed", true, "discountAmount", bd("99.99"), "paymentMethod", PaymentMethod.WALLET));
        amount("0.01", justBelow.refundableAmount());
        assertTrue(justBelow.warnings().contains("COUPON_VALUE_NOT_REFUNDED"));
        assertFalse(justBelow.warnings().contains("COUPON_VALUE_CONSUMED_REFUND"));
    }

    /** L141/L239/L334: every provider-fault trigger must waive the processing fee. */
    @Test void prRefundProviderFaultSuppressesFee() {
        net.mooctest.refund.domain.PurchaseSnapshot billed =
                prHelperReceipt("paidAt", NOW.minusSeconds(25 * 3600L), "progressPercent", 50);

        var cancelled = prHelperEvaluated("purchase", changed(billed, "instructorCancelled", true, "certificateIssued", true));
        amount("100", cancelled.refundableAmount());
        amount("0", cancelled.processingFee());
        assertTrue(cancelled.reasons().contains("PROVIDER_CANCELLED_COURSE"));

        var removed = prHelperEvaluated("purchase", changed(billed, "courseRemoved", true, "certificateIssued", true));
        amount("100", removed.refundableAmount());
        amount("0", removed.processingFee());
        assertTrue(removed.reasons().contains("PROVIDER_CANCELLED_COURSE"));

        var cancelledReason = prHelperEvaluated("reason", RefundReason.COURSE_CANCELLED,
                "purchase", changed(billed, "certificateIssued", true));
        amount("100", cancelledReason.refundableAmount());
        amount("0", cancelledReason.processingFee());
        assertTrue(cancelledReason.reasons().contains("PROVIDER_CANCELLED_COURSE"));

        var technical = prHelperEvaluated("reason", RefundReason.TECHNICAL_ISSUE, "technicalIssueConfirmed", true,
                "purchase", billed);
        amount("100", technical.refundableAmount());
        amount("0", technical.processingFee());

        var content = prHelperEvaluated("reason", RefundReason.CONTENT_QUALITY, "contentComplaintUpheld", true,
                "purchase", billed);
        amount("100", content.refundableAmount());
        amount("0", content.processingFee());

        var duplicate = prHelperEvaluated("reason", RefundReason.DUPLICATE_PAYMENT, "duplicatePaymentConfirmed", true,
                "purchase", billed);
        amount("100", duplicate.refundableAmount());
        amount("0", duplicate.processingFee());

        var control = prHelperEvaluated("purchase", changed(billed, "progressPercent", 5));
        amount("3", control.processingFee());
        amount("97", control.refundableAmount());
    }

    /** L242: an admin override never pays a processing fee. */
    @Test void prRefundAdminOverrideSuppressesFee() {
        var overridden = prHelperEvaluated("adminOverride", true,
                "purchase", prHelperReceipt("paidAt", NOW.minusSeconds(25 * 3600L), "certificateIssued", true));
        amount("100", overridden.refundableAmount());
        amount("0", overridden.processingFee());
        assertTrue(overridden.approved());
        assertTrue(overridden.warnings().contains("ADMIN_OVERRIDE_APPLIED"));

        var control = prHelperEvaluated("purchase",
                prHelperReceipt("paidAt", NOW.minusSeconds(25 * 3600L), "progressPercent", 5));
        amount("3", control.processingFee());
        amount("97", control.refundableAmount());
    }

    /** L253: the free-of-charge window ends after 24 full hours. */
    @Test void prRefundProcessingFeeTwentyFourHourBoundary() {
        var atTwentyFour = prHelperEvaluated("purchase",
                prHelperReceipt("paidAt", NOW.minusSeconds(24 * 3600L), "progressPercent", 5));
        amount("0", atTwentyFour.processingFee());
        amount("100", atTwentyFour.refundableAmount());
        assertTrue(atTwentyFour.approved());

        var atTwentyFive = prHelperEvaluated("purchase",
                prHelperReceipt("paidAt", NOW.minusSeconds(25 * 3600L), "progressPercent", 5));
        amount("3", atTwentyFive.processingFee());
        amount("97", atTwentyFive.refundableAmount());
    }

    /** L197/L201/L221: the exact reason string produced by every time/usage tier boundary. */
    @Test void prRefundEligibilityReasonMatrix() {
        int[] hours = {1, 2, 3, 24, 25, 168, 168, 169, 336, 337, 720};
        int[] progress = {0, 0, 0, 5, 5, 10, 11, 30, 30, 15, 15};
        String[] rules = {
                "COOLING_OFF_PERIOD", "COOLING_OFF_PERIOD",
                "ONE_DAY_LOW_USAGE", "ONE_DAY_LOW_USAGE",
                "SEVEN_DAY_GUARANTEE", "SEVEN_DAY_GUARANTEE",
                "SEVEN_DAY_PARTIAL_REFUND", "FOURTEEN_DAY_PARTIAL_REFUND",
                "FOURTEEN_DAY_PARTIAL_REFUND", "THIRTY_DAY_EXCEPTION", "THIRTY_DAY_EXCEPTION"};
        String[] refundables = {"100", "100", "100", "100", "100", "100", "70", "50", "50", "25", "25"};
        for (int i = 0; i < hours.length; i++) {
            var r = prHelperEvaluated("purchase", prHelperReceipt(
                    "paidAt", NOW.minusSeconds(hours[i] * 3600L),
                    "progressPercent", progress[i],
                    "paymentMethod", PaymentMethod.WALLET));
            assertEquals(List.of(rules[i], "REFUND_APPROVED"), r.reasons());
            amount(refundables[i], r.refundableAmount());
            amount("0", r.processingFee());
            assertTrue(r.approved());
            assertFalse(r.manualReview());
            assertEquals("WALLET_CREDIT", r.refundRoute());
        }

        var noTier = prHelperEvaluated("purchase", prHelperReceipt(
                "paidAt", NOW.minusSeconds(721 * 3600L), "progressPercent", 15, "paymentMethod", PaymentMethod.WALLET));
        assertEquals(List.of("REFUND_POLICY_NOT_SATISFIED"), noTier.reasons());
        amount("0", noTier.refundableAmount());
        assertFalse(noTier.approved());

        var afterDownload = prHelperEvaluated("purchase", prHelperReceipt(
                "paidAt", NOW.minusSeconds(800 * 3600L), "progressPercent", 50,
                "downloadedResources", 1, "paymentMethod", PaymentMethod.WALLET));
        assertEquals(List.of("ACCIDENTAL_PURCHASE_AFTER_DOWNLOAD"), afterDownload.reasons());
        amount("0", afterDownload.refundableAmount());
    }

    /** L284/L293: zero points never return a coupon-reduced warning; a whole-rate refund is not "proportional". */
    @Test void prRefundPointsZeroAndProportionalBoundaries() {
        var zeroPoints = prHelperEvaluated("purchase",
                prHelperReceipt("loyaltyPointsUsed", 0, "couponUsed", true, "discountAmount", bd("10")));
        assertEquals(0, zeroPoints.loyaltyPointsReturned());
        assertTrue(zeroPoints.warnings().contains("COUPON_VALUE_NOT_REFUNDED"));
        assertFalse(zeroPoints.warnings().contains("POINT_RETURN_REDUCED_FOR_COUPON_ORDER"));
        assertFalse(zeroPoints.warnings().contains("LOYALTY_POINTS_RETURNED_PROPORTIONALLY"));

        var fullPoints = prHelperEvaluated("purchase", prHelperReceipt("loyaltyPointsUsed", 100));
        assertEquals(100, fullPoints.loyaltyPointsReturned());
        assertFalse(fullPoints.warnings().contains("LOYALTY_POINTS_RETURNED_PROPORTIONALLY"));

        var halfPoints = prHelperEvaluated("fullRefundRequested", false, "requestedPercentage", 50,
                "purchase", prHelperReceipt("loyaltyPointsUsed", 103, "paymentMethod", PaymentMethod.WALLET));
        assertEquals(51, halfPoints.loyaltyPointsReturned());
        assertTrue(halfPoints.warnings().contains("LOYALTY_POINTS_RETURNED_PROPORTIONALLY"));
        assertFalse(halfPoints.warnings().contains("POINT_RETURN_REDUCED_FOR_COUPON_ORDER"));
        amount("50", halfPoints.refundableAmount());
    }

    /** L305: gift purchases and bank transfers without details route away from the original payment method. */
    @Test void prRefundRoutesForGiftAndManualReview() {
        var giftFlag = prHelperEvaluated("purchase", prHelperReceipt("giftPurchase", true, "paymentMethod", PaymentMethod.CARD));
        assertEquals("WALLET_CREDIT", giftFlag.refundRoute());
        assertTrue(giftFlag.warnings().contains("GIFT_REFUND_ISSUED_AS_CREDIT"));
        assertFalse(giftFlag.manualReview());
        assertTrue(giftFlag.approved());

        var giftMethod = prHelperEvaluated("purchase", prHelperReceipt("giftPurchase", false, "paymentMethod", PaymentMethod.GIFT));
        assertEquals("WALLET_CREDIT", giftMethod.refundRoute());
        assertTrue(giftMethod.warnings().contains("GIFT_REFUND_ISSUED_AS_CREDIT"));

        var bankWithoutDetails = prHelperEvaluated("purchase", prHelperReceipt("paymentMethod", PaymentMethod.BANK_TRANSFER));
        assertEquals("MANUAL_REVIEW", bankWithoutDetails.refundRoute());
        assertTrue(bankWithoutDetails.manualReview());
        assertFalse(bankWithoutDetails.approved());
        assertTrue(bankWithoutDetails.warnings().contains("BANK_DETAILS_REQUIRED"));

        var bankWithDetails = prHelperEvaluated("bankDetailsPresent", true,
                "purchase", prHelperReceipt("paymentMethod", PaymentMethod.BANK_TRANSFER));
        assertEquals("BANK_TRANSFER", bankWithDetails.refundRoute());

        var crypto = prHelperEvaluated("purchase", prHelperReceipt("paymentMethod", PaymentMethod.CRYPTO));
        assertEquals("MANUAL_REVIEW", crypto.refundRoute());
        assertTrue(crypto.manualReview());
    }

    // Scheduling / enrollment / grading boundary additions

    // =================================================================
    // fl suite: exact-boundary flow tests for the three services whose
    // mutants survived the previous PIT run.
    //   * SchedulingService            (16 surviving mutants)
    //   * EnrollmentEligibilityService (15 surviving mutants)
    //   * ExamGradingService           (14 surviving mutants)
    // Every expectation is derived by hand from the production source.
    // =================================================================

    private static EnrollmentHistory flHelperHistory() {
        return mock(EnrollmentHistory.class);
    }

    private static EnrollmentDecision flHelperEnrolled(EnrollmentRequest request, EnrollmentHistory history) {
        return new net.mooctest.enrollment.service.EnrollmentEligibilityService(history, CLOCK).evaluate(request);
    }

    private static GradeReport flHelperGrade(List<AnswerResult> answers, GradingPolicy policy) {
        return new net.mooctest.grading.service.ExamGradingService().grade(exam("answers", answers), policy);
    }

    private static ScheduleRequest flHelperScheduleAt(Instant start, long minutes) {
        return with(schedule(), "start", start, "end", start.plusSeconds(minutes * 60L));
    }

    // -----------------------------------------------------------------
    // SchedulingService
    // -----------------------------------------------------------------

    /**
     * evaluateTiming: an EXAM booked with exactly 48 hours of notice must not be
     * flagged; 47h59m59s must be flagged (the code tests {@code toHours() < 48}).
     */
    @Test void flSchedulingExamShortNoticeBoundary() {
        Instant at48 = NOW.plusSeconds(48 * 3600L);
        var exactly = scheduled(with(flHelperScheduleAt(at48, 60), "courseKind", CourseKind.EXAM, "afterHoursAllowed", true));
        assertEquals(60, exactly.durationMinutes());
        assertFalse(exactly.warnings().contains("EXAM_SCHEDULED_WITH_SHORT_NOTICE"));
        assertTrue(exactly.warnings().contains("AFTER_HOURS_SESSION"));
        var justUnder = scheduled(with(flHelperScheduleAt(at48.minusSeconds(1), 60), "courseKind", CourseKind.EXAM, "afterHoursAllowed", true));
        assertTrue(justUnder.warnings().contains("EXAM_SCHEDULED_WITH_SHORT_NOTICE"));
        var farAhead = scheduled(with(flHelperScheduleAt(NOW.plusSeconds(72 * 3600L), 60), "courseKind", CourseKind.EXAM, "afterHoursAllowed", true));
        assertFalse(farAhead.warnings().contains("EXAM_SCHEDULED_WITH_SHORT_NOTICE"));
    }

    /**
     * evaluateDeliveryResources: the ONLINE "room is ignored" and the CLASSROOM
     * "meeting url is optional" advisories are only emitted when the optional
     * resource is actually present.
     */
    @Test void flSchedulingDeliveryResourceAdvisories() {
        var online = scheduled(with(schedule(), "roomId", "room-1"));
        assertTrue(online.warnings().contains("ROOM_IGNORED_FOR_ONLINE_SESSION"));
        assertEquals(List.of(), online.conflicts());
        var onlineBlankRoom = scheduled(with(schedule(), "roomId", " "));
        assertFalse(onlineBlankRoom.warnings().contains("ROOM_IGNORED_FOR_ONLINE_SESSION"));
        var classroom = scheduled(with(schedule(), "deliveryMode", DeliveryMode.CLASSROOM, "roomId", "room-1"));
        assertTrue(classroom.warnings().contains("MEETING_URL_OPTIONAL_FOR_CLASSROOM"));
        assertEquals(List.of(), classroom.conflicts());
        var classroomNoUrl = scheduled(with(schedule(), "deliveryMode", DeliveryMode.CLASSROOM, "roomId", "room-1", "meetingUrl", null));
        assertFalse(classroomNoUrl.warnings().contains("MEETING_URL_OPTIONAL_FOR_CLASSROOM"));
    }

    /**
     * evaluateDeliveryResources: a remote EXAM without the PROCTORING equipment must
     * carry the REMOTE_PROCTORING_NOT_CONFIGURED advisory (classroom exams and
     * proctored exams must not).
     */
    @Test void flSchedulingRemoteExamProctoringAdvisory() {
        var remoteExam = scheduled(with(schedule(), "courseKind", CourseKind.EXAM));
        assertTrue(remoteExam.warnings().contains("REMOTE_PROCTORING_NOT_CONFIGURED"));
        assertTrue(remoteExam.manualReview());
        assertEquals("MANUAL_REVIEW", remoteExam.status());
        var classroomExam = scheduled(with(schedule(), "courseKind", CourseKind.EXAM, "deliveryMode", DeliveryMode.CLASSROOM, "roomId", "room-1"));
        assertFalse(classroomExam.warnings().contains("REMOTE_PROCTORING_NOT_CONFIGURED"));
        assertTrue(classroomExam.approved());
        var proctoredExam = scheduled(with(schedule(), "courseKind", CourseKind.EXAM, "availableEquipment", Set.of("PROCTORING")));
        assertFalse(proctoredExam.warnings().contains("REMOTE_PROCTORING_NOT_CONFIGURED"));
        assertTrue(proctoredExam.approved());
    }

    /**
     * evaluateExistingBookings: the buffer window is {@code minimumBufferMinutes * 60}
     * seconds, so one minute extends the buffered end by exactly 60 seconds.
     */
    @Test void flSchedulingMinimumBufferConvertsMinutesToSeconds() {
        Instant start = schedule().start();
        Instant end = schedule().end();
        var afterEnd = new ExistingBooking("buf-after", "i", "other-room", "other-cohort",
                end.plusSeconds(45), end.plusSeconds(3645), false, false, CourseKind.LECTURE);
        var buffered = scheduled(with(schedule(), "minimumBufferMinutes", 1, "existingBookings", List.of(afterEnd)));
        assertTrue(buffered.conflicts().contains("INSTRUCTOR_BOOKING_CONFLICT:buf-after"));
        assertEquals("REJECTED", buffered.status());
        var unbuffered = scheduled(with(schedule(), "existingBookings", List.of(afterEnd)));
        assertFalse(unbuffered.conflicts().contains("INSTRUCTOR_BOOKING_CONFLICT:buf-after"));
        assertTrue(unbuffered.approved());
        var beforeStart = new ExistingBooking("buf-before", "i", "other-room", "other-cohort",
                start.minusSeconds(3750), start.minusSeconds(45), false, false, CourseKind.LECTURE);
        assertTrue(scheduled(with(schedule(), "minimumBufferMinutes", 1, "existingBookings", List.of(beforeStart)))
                .conflicts().contains("INSTRUCTOR_BOOKING_CONFLICT:buf-before"));
        assertFalse(scheduled(with(schedule(), "existingBookings", List.of(beforeStart)))
                .conflicts().contains("INSTRUCTOR_BOOKING_CONFLICT:buf-before"));
    }

    /**
     * evaluateRecurrence/requiresManualReview: a recurring series starting on a
     * Saturday carries RECURRING_WEEKEND_SESSION; a hybrid session above 200
     * participants and a workshop of exactly six hours carry their review advisories.
     */
    @Test void flSchedulingRecurrenceAndManualReviewAdvisories() {
        Instant saturday = LocalDate.of(2026, 11, 21).atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(10 * 3600L);
        var weekendSeries = scheduled(with(flHelperScheduleAt(saturday, 60), "recurring", true, "occurrenceCount", 2, "weekendAllowed", true));
        assertTrue(weekendSeries.warnings().contains("RECURRING_WEEKEND_SESSION"));
        assertFalse(weekendSeries.conflicts().contains("WEEKEND_NOT_ALLOWED"));
        var weekdaySeries = scheduled(with(schedule(), "recurring", true, "occurrenceCount", 2));
        assertFalse(weekdaySeries.warnings().contains("RECURRING_WEEKEND_SESSION"));
        var largeHybrid = scheduled(with(schedule(), "deliveryMode", DeliveryMode.HYBRID, "roomId", "room-1",
                "expectedParticipants", 201, "roomCapacity", 400));
        assertTrue(largeHybrid.warnings().contains("LARGE_HYBRID_EVENT_REQUIRES_REVIEW"));
        assertTrue(largeHybrid.manualReview());
        var justUnder = scheduled(with(schedule(), "deliveryMode", DeliveryMode.HYBRID, "roomId", "room-1",
                "expectedParticipants", 200, "roomCapacity", 400));
        assertFalse(justUnder.warnings().contains("LARGE_HYBRID_EVENT_REQUIRES_REVIEW"));
        var sixHourWorkshop = scheduled(with(schedule(), "courseKind", CourseKind.WORKSHOP,
                "start", NOW.plusSeconds(40 * 3600L), "end", NOW.plusSeconds(46 * 3600L), "afterHoursAllowed", true));
        assertEquals(360, sixHourWorkshop.durationMinutes());
        assertTrue(sixHourWorkshop.warnings().contains("EXTENDED_WORKSHOP_REQUIRES_REVIEW"));
        assertTrue(sixHourWorkshop.manualReview());
        var fiveHourWorkshop = scheduled(with(schedule(), "courseKind", CourseKind.WORKSHOP,
                "start", NOW.plusSeconds(40 * 3600L), "end", NOW.plusSeconds(45 * 3600L)));
        assertFalse(fiveHourWorkshop.warnings().contains("EXTENDED_WORKSHOP_REQUIRES_REVIEW"));
        assertFalse(fiveHourWorkshop.manualReview());
    }

    /**
     * isNonOverridable: a cohort clash alone is not overridable, so the administrator
     * override must be partly rejected and the conflict must survive.
     */
    @Test void flSchedulingAdministratorOverrideKeepsCohortConflict() {
        Instant start = schedule().start();
        Instant end = schedule().end();
        var cohortBooking = new ExistingBooking("cohort-clash", "another-teacher", "another-room", "co",
                start, end, false, false, CourseKind.LECTURE);
        var decision = scheduled(with(schedule(), "existingBookings", List.of(cohortBooking), "administratorOverride", true));
        assertTrue(decision.warnings().contains("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"));
        assertFalse(decision.warnings().contains("ADMINISTRATOR_OVERRIDE_APPLIED"));
        assertTrue(decision.conflicts().contains("COHORT_BOOKING_CONFLICT:cohort-clash"));
        assertFalse(decision.approved());
        assertEquals("REJECTED", decision.status());
    }

    /**
     * validate(): zero participants/capacity and a 240 minute buffer are legal
     * (the guards are strict comparisons).
     */
    @Test void flSchedulingValidationAcceptsZeroValuesAndExactBufferLimit() {
        var zeroValues = scheduled(with(schedule(), "expectedParticipants", 0, "registeredParticipants", 0, "roomCapacity", 0));
        assertTrue(zeroValues.approved());
        assertEquals(60, zeroValues.durationMinutes());
        assertEquals(List.of(), zeroValues.conflicts());
        assertEquals(List.of("INSTRUCTOR_AVAILABILITY_NOT_PROVIDED"), zeroValues.warnings());
        var maxBuffer = scheduled(with(schedule(), "minimumBufferMinutes", 240));
        assertTrue(maxBuffer.approved());
        assertEquals(List.of(), maxBuffer.conflicts());
    }

    // -----------------------------------------------------------------
    // EnrollmentEligibilityService
    // -----------------------------------------------------------------

    /** A previous failed attempt (without a configured limit) is only a warning. */
    @Test void flEnrollmentPriorFailedAttemptsWarning() {
        EnrollmentHistory history = flHelperHistory();
        when(history.failedEnrollmentAttempts("s", "c")).thenReturn(1);
        var decision = flHelperEnrolled(enrollment(), history);
        assertEquals(EnrollmentOutcome.ENROLLED, decision.outcome());
        assertEquals(List.of("PREVIOUS_FAILED_ATTEMPTS"), decision.warnings());
        assertEquals(List.of(), decision.reasons());
        when(history.failedEnrollmentAttempts("s", "c")).thenReturn(2);
        var limited = flHelperEnrolled(with(enrollment(), "course", with(offering(), "maximumFailedAttempts", 2)), history);
        assertEquals(EnrollmentOutcome.REJECTED, limited.outcome());
        assertEquals(List.of("TOO_MANY_FAILED_ATTEMPTS"), limited.reasons());
    }

    /** A live course with a blank (not only null) schedule key is blocked. */
    @Test void flEnrollmentLiveCourseBlankScheduleKeyBlocked() {
        var blank = enrolled(with(enrollment(), "course", with(offering(), "liveCourse", true, "scheduleKey", " ")));
        assertEquals(EnrollmentOutcome.REJECTED, blank.outcome());
        assertEquals(List.of("LIVE_COURSE_WITHOUT_SCHEDULE"), blank.reasons());
        var nullKey = enrolled(with(enrollment(), "course", with(offering(), "liveCourse", true, "scheduleKey", null)));
        assertEquals(List.of("LIVE_COURSE_WITHOUT_SCHEDULE"), nullKey.reasons());
        var booked = enrolled(with(enrollment(), "course", with(offering(), "liveCourse", true, "scheduleKey", "slot")));
        assertEquals(EnrollmentOutcome.ENROLLED, booked.outcome());
        assertEquals(List.of(), booked.reasons());
    }

    /** Fifty seats in one request is still legal; 51 and 0 are rejected by validation. */
    @Test void flEnrollmentAcceptsFiftySeatsExactly() {
        var request = with(enrollment(), "course", with(offering(), "capacity", 60), "requestedSeats", 50,
                "organizationSponsored", true, "student", with(student(), "membershipLevel", MembershipLevel.ENTERPRISE));
        var decision = enrolled(request);
        assertEquals(EnrollmentOutcome.ENROLLED, decision.outcome());
        assertEquals(60, decision.availableSeats());
        assertFalse(decision.paymentRequired());
        assertEquals(List.of(), decision.reasons());
        assertTrue(decision.warnings().containsAll(List.of("OPTIONAL_ORGANIZATION_SPONSORSHIP", "PAYMENT_AUTHORIZATION_NOT_REQUIRED")));
        var tooMany = assertThrows(IllegalArgumentException.class, () -> enrolled(with(request, "requestedSeats", 51)));
        assertEquals("requestedSeats must not exceed 50", tooMany.getMessage());
        var none = assertThrows(IllegalArgumentException.class, () -> enrolled(with(request, "requestedSeats", 0)));
        assertEquals("requestedSeats must be positive", none.getMessage());
    }

    /** An adult with parental consent gets the advisory at the exact age of 18. */
    @Test void flEnrollmentAdultParentalConsentBoundary() {
        var adult = enrolled(with(enrollment(), "student", with(student(), "age", 18, "parentalConsent", true)));
        assertEquals(EnrollmentOutcome.ENROLLED, adult.outcome());
        assertEquals(List.of("UNNECESSARY_PARENTAL_CONSENT"), adult.warnings());
        var teen = enrolled(with(enrollment(), "student", with(student(), "age", 17, "parentalConsent", true)));
        assertEquals(EnrollmentOutcome.ENROLLED, teen.outcome());
        assertEquals(List.of(), teen.warnings());
    }

    /** Zero capacity is legal (not a validation error) and overbooking only warns above capacity. */
    @Test void flEnrollmentZeroCapacityAndOverbookingBoundary() {
        var zeroCapacity = enrolled(with(enrollment(), "course", with(offering(), "capacity", 0)));
        assertEquals(EnrollmentOutcome.REJECTED, zeroCapacity.outcome());
        assertEquals(0, zeroCapacity.availableSeats());
        assertEquals(List.of("COURSE_FULL"), zeroCapacity.reasons());
        assertEquals(List.of(), zeroCapacity.warnings());
        var exactlyFull = enrolled(with(enrollment(), "course", with(offering(), "enrolledCount", 10)));
        assertEquals(EnrollmentOutcome.REJECTED, exactlyFull.outcome());
        assertEquals(List.of("COURSE_FULL"), exactlyFull.reasons());
        assertEquals(List.of(), exactlyFull.warnings());
        var overbooked = enrolled(with(enrollment(), "course", with(offering(), "enrolledCount", 11)));
        assertEquals(List.of("COURSE_FULL"), overbooked.reasons());
        assertEquals(List.of("COURSE_IS_OVERBOOKED"), overbooked.warnings());
    }

    /** The valid age window is 0..120 inclusive; outside values are blocked. */
    @Test void flEnrollmentStudentAgeWindowBoundaries() {
        var newborn = enrolled(with(enrollment(), "student", with(student(), "age", 0)));
        assertEquals(EnrollmentOutcome.REJECTED, newborn.outcome());
        assertEquals(List.of("PARENTAL_CONSENT_REQUIRED"), newborn.reasons());
        var oldest = enrolled(with(enrollment(), "student", with(student(), "age", 120)));
        assertEquals(EnrollmentOutcome.ENROLLED, oldest.outcome());
        assertEquals(List.of(), oldest.reasons());
        var negative = enrolled(with(enrollment(), "student", with(student(), "age", -1)));
        assertEquals(List.of("PARENTAL_CONSENT_REQUIRED", "INVALID_STUDENT_AGE"), negative.reasons());
        var tooOld = enrolled(with(enrollment(), "student", with(student(), "age", 121)));
        assertEquals(List.of("INVALID_STUDENT_AGE"), tooOld.reasons());
    }

    /** A student exactly at the course minimum age is allowed, one year below is not. */
    @Test void flEnrollmentCourseMinimumAgeBoundaries() {
        var exact = enrolled(with(enrollment(), "course", with(offering(), "minimumAge", 18)));
        assertEquals(EnrollmentOutcome.ENROLLED, exact.outcome());
        assertEquals(List.of(), exact.reasons());
        var below = enrolled(with(enrollment(), "course", with(offering(), "minimumAge", 19)));
        assertEquals(EnrollmentOutcome.REJECTED, below.outcome());
        assertEquals(List.of("BELOW_MINIMUM_AGE"), below.reasons());
        var above = enrolled(with(enrollment(), "course", with(offering(), "maximumAge", 17)));
        assertEquals(List.of("ABOVE_MAXIMUM_AGE"), above.reasons());
    }

    /** Equal minimum/maximum ages are fine, a zero maximum means "unbounded", and min>max is a hard error. */
    @Test void flEnrollmentCourseAgeRangeBoundaries() {
        var equal = enrolled(with(enrollment(), "student", with(student(), "age", 19),
                "course", with(offering(), "minimumAge", 19, "maximumAge", 19)));
        assertEquals(EnrollmentOutcome.ENROLLED, equal.outcome());
        assertEquals(List.of(), equal.reasons());
        var openMaximum = enrolled(with(enrollment(), "student", with(student(), "age", 19),
                "course", with(offering(), "minimumAge", 19, "maximumAge", 0)));
        assertEquals(EnrollmentOutcome.ENROLLED, openMaximum.outcome());
        var invalid = assertThrows(IllegalArgumentException.class,
                () -> enrolled(with(enrollment(), "course", with(offering(), "minimumAge", 20, "maximumAge", 19))));
        assertEquals("course age range is invalid", invalid.getMessage());
    }

    /** A blank organization id is as invalid as a missing one for an organization-only course. */
    @Test void flEnrollmentOrganizationIdBlankRejected() {
        var blank = enrolled(with(enrollment(), "course", with(offering(), "organizationOnly", true),
                "organizationSponsored", true, "organizationId", " "));
        assertEquals(EnrollmentOutcome.REJECTED, blank.outcome());
        assertEquals(List.of("ORGANIZATION_ID_REQUIRED"), blank.reasons());
        var named = enrolled(with(enrollment(), "course", with(offering(), "organizationOnly", true),
                "organizationSponsored", true, "organizationId", "org"));
        assertEquals(EnrollmentOutcome.ENROLLED, named.outcome());
        assertEquals(List.of(), named.reasons());
    }

    /** Sponsoring an optional-organization course only produces an advisory. */
    @Test void flEnrollmentOptionalSponsorshipAdvisory() {
        var optional = enrolled(with(enrollment(), "organizationSponsored", true));
        assertEquals(EnrollmentOutcome.ENROLLED, optional.outcome());
        assertTrue(optional.warnings().contains("OPTIONAL_ORGANIZATION_SPONSORSHIP"));
        assertFalse(optional.paymentRequired());
        var required = enrolled(with(enrollment(), "course", with(offering(), "organizationOnly", true),
                "organizationSponsored", true, "organizationId", "org"));
        assertFalse(required.warnings().contains("OPTIONAL_ORGANIZATION_SPONSORSHIP"));
        var unsponsored = enrolled(with(enrollment(), "organizationId", "org"));
        assertFalse(unsponsored.warnings().contains("OPTIONAL_ORGANIZATION_SPONSORSHIP"));
    }

    /** Prerequisite scores: 0 is a low score (not invalid), exactly the minimum passes, 95+ is excellent. */
    @Test void flEnrollmentPrerequisiteScoreBoundaries() {
        EnrollmentHistory history = flHelperHistory();
        when(history.completedCourse("s", "pre")).thenReturn(true);
        var course = with(offering(), "prerequisiteCourseIds", Set.of("pre"), "minimumPrerequisiteScore", 60);
        var request = with(enrollment(), "course", course);
        when(history.completionScore("s", "pre")).thenReturn(0);
        assertEquals(List.of("PREREQUISITE_SCORE_TOO_LOW:pre"), flHelperEnrolled(request, history).reasons());
        when(history.completionScore("s", "pre")).thenReturn(60);
        var exact = flHelperEnrolled(request, history);
        assertEquals(EnrollmentOutcome.ENROLLED, exact.outcome());
        assertEquals(List.of(), exact.reasons());
        assertEquals(List.of("ALL_PREREQUISITES_COMPLETED"), exact.warnings());
        when(history.completionScore("s", "pre")).thenReturn(100);
        var perfect = flHelperEnrolled(request, history);
        assertEquals(List.of(), perfect.reasons());
        assertTrue(perfect.warnings().contains("EXCELLENT_PREREQUISITE_SCORE:pre"));
        var zeroMinimum = with(request, "course", with(course, "minimumPrerequisiteScore", 0));
        when(history.completionScore("s", "pre")).thenReturn(0);
        assertEquals(List.of(), flHelperEnrolled(zeroMinimum, history).reasons());
    }

    // -----------------------------------------------------------------
    // ExamGradingService
    // -----------------------------------------------------------------

    /**
     * scoreAnswer: with negative marking disabled a negative partial-credit score is
     * clamped to zero, so it cannot cancel the credit earned by another answer.
     * With negative marking enabled the very same answer deducts points.
     */
    @Test void flGradingNegativeScoreClampedWhenNegativeMarkingDisabled() {
        var correct = answer("questionId", "ok");
        var partial = answer("questionId", "partial", "type", QuestionType.MULTIPLE_CHOICE, "exactMatch", false,
                "correctOptionCount", 1, "selectedCorrectOptions", 0, "selectedIncorrectOptions", 1);
        var lenient = flHelperGrade(List.of(correct, partial), policy("negativeMarkingEnabled", false));
        amount("80", lenient.rawPoints());
        amount("80", lenient.finalPoints());
        assertEquals(50, lenient.percentage());
        assertEquals("F", lenient.letterGrade());
        assertFalse(lenient.passed());
        var strict = flHelperGrade(List.of(correct, partial), policy("wrongAnswerPenaltyRate", decimal("0.5")));
        amount("40", strict.rawPoints());
        assertEquals(25, strict.percentage());
    }

    /**
     * scoreNumeric: partial credit covers errors up to ten tolerances; at exactly ten
     * tolerances the computed credit is zero, one epsilon beyond it falls through to
     * the negative-marking path (which costs a quarter of the maximum).
     */
    @Test void flGradingNumericPartialCreditStopsAtTenTolerances() {
        var correct = answer("questionId", "ok");
        var boundary = answer("questionId", "num", "type", QuestionType.NUMERIC, "exactMatch", false,
                "numericError", decimal("10"), "numericTolerance", BigDecimal.ONE);
        var inside = flHelperGrade(List.of(correct, boundary), policy());
        amount("80", inside.rawPoints());
        amount("80", inside.finalPoints());
        assertEquals(50, inside.percentage());
        var outside = answer("questionId", "num", "type", QuestionType.NUMERIC, "exactMatch", false,
                "numericError", decimal("10.000001"), "numericTolerance", BigDecimal.ONE);
        var penalised = flHelperGrade(List.of(correct, outside), policy());
        amount("60", penalised.rawPoints());
        assertEquals(38, penalised.percentage());
    }

    // Small utility additions
    @Test
    void gapUtilityCharacterBoundariesAndCotangent() {
        assertEquals(Character.valueOf('A'), net.mooctest.expression.engine.StrUtils.containsEnChar("@A"));
        assertEquals(Character.valueOf('Z'), net.mooctest.expression.engine.StrUtils.containsEnChar("[Z"));
        assertEquals(Character.valueOf('a'), net.mooctest.expression.engine.StrUtils.containsEnChar("`a"));
        assertEquals(Character.valueOf('z'), net.mooctest.expression.engine.StrUtils.containsEnChar("{z"));
        assertNull(net.mooctest.expression.engine.StrUtils.containsEnChar("@[`{"));
        assertNull(net.mooctest.expression.engine.StrUtils.containsEnChar(""));

        var math = net.mooctest.expression.engine.FunctionPackage.MATH;
        var cotangent = (net.mooctest.expression.engine.ManyToOneNumberFunction) math.getFunctionByNameNoCheck("cot");
        var tangent = (net.mooctest.expression.engine.ManyToOneNumberFunction) math.getFunctionByNameNoCheck("tan");
        assertEquals(1.0, tangent.run(Math.PI / 4), 1e-12);
        assertEquals(1.0, cotangent.run(Math.PI / 4), 1e-12);
        assertEquals(1.0 / Math.tan(0.5), cotangent.run(0.5), 1e-12);
        assertEquals(1.0 / Math.tan(1.2), cotangent.run(1.2), 1e-12);
    }

    @Test
    void gapIntervalComponentsRejectForeignNames() {
        net.mooctest.expression.engine.BracketsCalculation2.getInstance("gapForeignName");
        try {
            var sumClash = assertThrows(net.mooctest.expression.engine.ExtractException.class,
                    () -> net.mooctest.expression.engine.FastSumOfIntervalsBrackets.getInstance("gapForeignName"));
            assertTrue(sumClash.getMessage().contains("gapForeignName"));
            var productClash = assertThrows(net.mooctest.expression.engine.ExtractException.class,
                    () -> net.mooctest.expression.engine.FastMultiplyOfIntervalsBrackets.getInstance("gapForeignName"));
            assertTrue(productClash.getMessage().contains("gapForeignName"));
            var formulaClash = assertThrows(net.mooctest.expression.engine.ExtractException.class,
                    () -> net.mooctest.expression.engine.FunctionFormulaCalculation.getInstance("gapForeignName"));
            assertTrue(formulaClash.getMessage().contains("gapForeignName"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister("gapForeignName");
        }
    }

    @Test
    void gapJvmCalculationRefusesDecimalCompilation() {
        var calculator = net.mooctest.expression.engine.JvmCalculation.getInstance("gapJvmDecimal");
        try {
            var unsupported = assertThrows(UnsupportedOperationException.class,
                    () -> calculator.compileBigDecimal("1+1", true));
            assertTrue(unsupported.getMessage().contains("BigDecimal"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister("gapJvmDecimal");
        }
    }

    @Test
    void gapCatalogSkipsVideoLookupWithoutChapters() {
        var repository = new net.mooctest.course.adapter.InMemoryCourseRepository();
        var clock = java.time.Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC);
        var chapters = (net.mooctest.course.port.ChapterRepository) id -> java.util.List.of();
        var service = new net.mooctest.course.service.CourseService(repository,
                id -> java.util.List.of(new net.mooctest.course.domain.Chapter("intro", id, "First", 1)),
                () -> "linear", new net.mooctest.course.service.PricePolicy(), clock);
        service.createCourse("Empty Chapters", new java.math.BigDecimal("9.99"));
        var videoLookups = new java.util.concurrent.atomic.AtomicInteger();
        var catalog = new net.mooctest.course.service.CatalogService(service, chapters, ids -> {
            videoLookups.incrementAndGet();
            return java.util.List.of();
        });
        assertEquals(new net.mooctest.course.domain.CourseCatalog("linear", java.util.List.of()),
                catalog.getCatalog("linear"));
        assertEquals(0, videoLookups.get());
    }

    @Test
    void gapEvaluatorRejectsBlankAndMissingNumericResults() {
        var calculator = org.mockito.Mockito.mock(net.mooctest.expression.engine.BracketsCalculation2.class);
        var expression = org.mockito.Mockito.mock(net.mooctest.expression.engine.PackExpression.class);
        var results = org.mockito.Mockito.mock(net.mooctest.expression.engine.CalculationResults.class);
        org.mockito.Mockito.when(calculator.compileBigDecimal("1+1", true)).thenReturn(expression);
        org.mockito.Mockito.when(expression.calculationBigDecimals(false)).thenReturn(results);
        org.mockito.Mockito.when(results.getBigDecimalResult()).thenReturn(null);
        var evaluator = new net.mooctest.grading.expression.MathematicalExpressionEvaluator(calculator);

        var blank = assertThrows(net.mooctest.grading.expression.ExpressionEvaluationException.class,
                () -> evaluator.evaluate("   "));
        assertEquals("expression must not be blank", blank.getMessage());
        var nullValue = assertThrows(net.mooctest.grading.expression.ExpressionEvaluationException.class,
                () -> evaluator.evaluate("1+1"));
        assertEquals("expression produced no numeric result", nullValue.getMessage());

        org.mockito.Mockito.when(results.getBigDecimalResult()).thenReturn(new java.math.BigDecimal("2"));
        assertEquals(new java.math.BigDecimal("2"), evaluator.evaluate("1+1"));
    }
}
