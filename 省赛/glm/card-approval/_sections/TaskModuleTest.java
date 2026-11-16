/*
 * Task subsystem: full-branch tests for task/ (RecurrenceRule, Task, TaskAction, TaskContext,
 * TaskEvaluation, TaskPolicy, TaskPriority, TaskStateMachine, TaskStatus) and util/DateTextParser.
 * Every expected value is hand-computed against fixed Instants; "now" is fixed at 2026-06-15T02:00:00Z.
 */
package net.mooctest;

import net.mooctest.task.RecurrenceRule;
import net.mooctest.task.Task;
import net.mooctest.task.TaskAction;
import net.mooctest.task.TaskContext;
import net.mooctest.task.TaskEvaluation;
import net.mooctest.task.TaskPolicy;
import net.mooctest.task.TaskPriority;
import net.mooctest.task.TaskStateMachine;
import net.mooctest.task.TaskStatus;
import net.mooctest.util.DateTextParser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskModuleTest {

    // Fixed reference instants. "Now" is 2026-06-15T02:00:00Z; the sample task was created 2026-01-05T09:00:00Z.
    private static final Instant CREATED_AT = Instant.parse("2026-01-05T09:00:00Z");
    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");
    private static final Instant NOW_PLUS_1H = Instant.parse("2026-06-15T03:00:00Z");
    private static final Instant NOW_PLUS_2H = Instant.parse("2026-06-15T04:00:00Z");
    private static final Instant NOW_PLUS_4H = Instant.parse("2026-06-15T06:00:00Z");
    private static final Instant NOW_PLUS_5H = Instant.parse("2026-06-15T07:00:00Z");
    private static final Instant NOW_PLUS_24H = Instant.parse("2026-06-16T02:00:00Z");
    private static final Instant NOW_PLUS_25H = Instant.parse("2026-06-16T03:00:00Z");
    private static final Instant NOW_PLUS_48H = Instant.parse("2026-06-17T02:00:00Z");
    private static final Instant NOW_PLUS_72H = Instant.parse("2026-06-18T02:00:00Z");
    private static final Instant NOW_PLUS_73H = Instant.parse("2026-06-18T03:00:00Z");
    private static final Instant NOW_MINUS_1H = Instant.parse("2026-06-15T01:00:00Z");
    private static final Instant DAY_BEFORE = Instant.parse("2026-06-14T02:00:00Z");
    private static final Instant TWO_DAYS_BEFORE = Instant.parse("2026-06-13T02:00:00Z");
    private static final Instant THREE_DAYS_BEFORE = Instant.parse("2026-06-12T02:00:00Z");
    private static final Instant FOUR_DAYS_BEFORE = Instant.parse("2026-06-11T02:00:00Z");
    private static final Instant SIX_DAYS_BEFORE = Instant.parse("2026-06-09T02:00:00Z");
    private static final Instant SEVEN_DAYS_BEFORE = Instant.parse("2026-06-08T02:00:00Z");

    // Both collaborators are stateless, so one instance per suite is safe; every test builds fresh tasks/contexts.
    private static final TaskPolicy POLICY = new TaskPolicy();
    private static final TaskStateMachine MACHINE = new TaskStateMachine();

    private static TaskBuilder task() {
        return new TaskBuilder();
    }

    private static CtxBuilder ctx() {
        return new CtxBuilder();
    }

    /** Default task in the given status, with the timestamps that status demands. */
    private static TaskBuilder taskIn(TaskStatus status) {
        TaskBuilder builder = task().status(status);
        if (status == TaskStatus.IN_PROGRESS || status == TaskStatus.BLOCKED || status == TaskStatus.IN_REVIEW) {
            builder.startedAt(DAY_BEFORE);
        } else if (status == TaskStatus.COMPLETED) {
            builder.startedAt(DAY_BEFORE).completedAt(DAY_BEFORE);
        }
        return builder;
    }

    /** Fluent builder for the 18-component Task record; defaults describe a valid DRAFT task. */
    private static final class TaskBuilder {
        private String id = "T-100";
        private String title = "Prepare quarterly report";
        private String description;
        private String creatorId = "creator-1";
        private String assigneeId = "assignee-1";
        private TaskStatus status = TaskStatus.DRAFT;
        private TaskPriority priority = TaskPriority.NORMAL;
        private RecurrenceRule recurrence = RecurrenceRule.NONE;
        private Instant createdAt = CREATED_AT;
        private Instant dueAt;
        private Instant startedAt;
        private Instant completedAt;
        private int estimatedMinutes = 90;
        private int actualMinutes;
        private List<String> dependencyIds = List.of();
        private Set<String> tags = Set.of("ops");
        private boolean confidential;
        private boolean reviewRequired;

        TaskBuilder id(String value) { this.id = value; return this; }

        TaskBuilder title(String value) { this.title = value; return this; }

        TaskBuilder description(String value) { this.description = value; return this; }

        TaskBuilder creatorId(String value) { this.creatorId = value; return this; }

        TaskBuilder assigneeId(String value) { this.assigneeId = value; return this; }

        TaskBuilder status(TaskStatus value) { this.status = value; return this; }

        TaskBuilder priority(TaskPriority value) { this.priority = value; return this; }

        TaskBuilder recurrence(RecurrenceRule value) { this.recurrence = value; return this; }

        TaskBuilder createdAt(Instant value) { this.createdAt = value; return this; }

        TaskBuilder dueAt(Instant value) { this.dueAt = value; return this; }

        TaskBuilder startedAt(Instant value) { this.startedAt = value; return this; }

        TaskBuilder completedAt(Instant value) { this.completedAt = value; return this; }

        TaskBuilder estimatedMinutes(int value) { this.estimatedMinutes = value; return this; }

        TaskBuilder actualMinutes(int value) { this.actualMinutes = value; return this; }

        TaskBuilder dependencyIds(List<String> value) { this.dependencyIds = value; return this; }

        TaskBuilder tags(Set<String> value) { this.tags = value; return this; }

        TaskBuilder confidential(boolean value) { this.confidential = value; return this; }

        TaskBuilder reviewRequired(boolean value) { this.reviewRequired = value; return this; }

        Task build() {
            return new Task(id, title, description, creatorId, assigneeId, status, priority, recurrence,
                    createdAt, dueAt, startedAt, completedAt, estimatedMinutes, actualMinutes,
                    dependencyIds, tags, confidential, reviewRequired);
        }
    }

    /** Fluent builder for the 12-component TaskContext record; defaults: creator-1 at NOW, everything active. */
    private static final class CtxBuilder {
        private String actorId = "creator-1";
        private Instant now = NOW;
        private boolean manager;
        private boolean administrator;
        private boolean dependenciesComplete = true;
        private boolean assigneeActive = true;
        private boolean projectActive = true;
        private boolean withinWorkingHours = true;
        private boolean notificationsEnabled = true;
        private int previousReopens;
        private int assigneeOpenTaskCount;
        private int overdueTaskCount;

        CtxBuilder actorId(String value) { this.actorId = value; return this; }

        CtxBuilder now(Instant value) { this.now = value; return this; }

        CtxBuilder manager(boolean value) { this.manager = value; return this; }

        CtxBuilder administrator(boolean value) { this.administrator = value; return this; }

        CtxBuilder dependenciesComplete(boolean value) { this.dependenciesComplete = value; return this; }

        CtxBuilder assigneeActive(boolean value) { this.assigneeActive = value; return this; }

        CtxBuilder projectActive(boolean value) { this.projectActive = value; return this; }

        CtxBuilder withinWorkingHours(boolean value) { this.withinWorkingHours = value; return this; }

        CtxBuilder notificationsEnabled(boolean value) { this.notificationsEnabled = value; return this; }

        CtxBuilder previousReopens(int value) { this.previousReopens = value; return this; }

        CtxBuilder assigneeOpenTaskCount(int value) { this.assigneeOpenTaskCount = value; return this; }

        CtxBuilder overdueTaskCount(int value) { this.overdueTaskCount = value; return this; }

        TaskContext build() {
            return new TaskContext(actorId, now, manager, administrator, dependenciesComplete, assigneeActive,
                    projectActive, withinWorkingHours, notificationsEnabled, previousReopens,
                    assigneeOpenTaskCount, overdueTaskCount);
        }
    }

    @Nested
    class TaskValidationTests {

        @Test
        void rejectsNullId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().id(null).build());
            assertEquals("id must not be blank", ex.getMessage());
        }

        @Test
        void rejectsBlankId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().id("   ").build());
            assertEquals("id must not be blank", ex.getMessage());
        }

        @Test
        void rejectsNullTitle() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().title(null).build());
            assertEquals("title must not be blank", ex.getMessage());
        }

        @Test
        void rejectsBlankTitle() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().title("\t ").build());
            assertEquals("title must not be blank", ex.getMessage());
        }

        @Test
        void rejectsNullCreatorId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().creatorId(null).build());
            assertEquals("creatorId must not be blank", ex.getMessage());
        }

        @Test
        void rejectsBlankCreatorId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().creatorId("  ").build());
            assertEquals("creatorId must not be blank", ex.getMessage());
        }

        @Test
        void rejectsNullStatus() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> task().status(null).build());
            assertEquals("status must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullPriority() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> task().priority(null).build());
            assertEquals("priority must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullRecurrence() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> task().recurrence(null).build());
            assertEquals("recurrence must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullCreatedAt() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> task().createdAt(null).build());
            assertEquals("createdAt must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullDependencyIds() {
            // Objects.requireNonNull without a message.
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> task().dependencyIds(null).build());
            assertNull(ex.getMessage());
        }

        @Test
        void rejectsNullTags() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> task().tags(null).build());
            assertNull(ex.getMessage());
        }

        @Test
        void acceptsTitleOf120CharactersAndRejects121() {
            String longest = "T".repeat(120);
            assertEquals(longest, task().title(longest).build().title());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().title("T".repeat(121)).build());
            assertEquals("title cannot exceed 120 characters", ex.getMessage());
        }

        @Test
        void acceptsDescriptionUpTo5000CharactersAndRejects5001() {
            assertEquals(5000, task().description("D".repeat(5000)).build().description().length());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().description("D".repeat(5001)).build());
            assertEquals("description cannot exceed 5000 characters", ex.getMessage());
        }

        @Test
        void acceptsNullDescription() {
            assertNull(task().description(null).build().description());
        }

        @Test
        void acceptsDueAtEqualToCreatedAtAndRejectsEarlierDueAt() {
            assertEquals(CREATED_AT, task().dueAt(CREATED_AT).build().dueAt());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().dueAt(CREATED_AT.minusSeconds(1)).build());
            assertEquals("dueAt must not be before createdAt", ex.getMessage());
        }

        @Test
        void estimatedMinutesAcceptsZeroAnd100000RejectsMinusOneAnd100001() {
            assertEquals(0, task().estimatedMinutes(0).build().estimatedMinutes());
            assertEquals(100_000, task().estimatedMinutes(100_000).build().estimatedMinutes());
            IllegalArgumentException low = assertThrows(IllegalArgumentException.class,
                    () -> task().estimatedMinutes(-1).build());
            assertEquals("estimatedMinutes is outside the supported range", low.getMessage());
            IllegalArgumentException high = assertThrows(IllegalArgumentException.class,
                    () -> task().estimatedMinutes(100_001).build());
            assertEquals("estimatedMinutes is outside the supported range", high.getMessage());
        }

        @Test
        void actualMinutesAcceptsZeroAnd200000RejectsMinusOneAnd200001() {
            assertEquals(0, task().actualMinutes(0).build().actualMinutes());
            assertEquals(200_000, task().actualMinutes(200_000).build().actualMinutes());
            IllegalArgumentException low = assertThrows(IllegalArgumentException.class,
                    () -> task().actualMinutes(-1).build());
            assertEquals("actualMinutes is outside the supported range", low.getMessage());
            IllegalArgumentException high = assertThrows(IllegalArgumentException.class,
                    () -> task().actualMinutes(200_001).build());
            assertEquals("actualMinutes is outside the supported range", high.getMessage());
        }

        @Test
        void rejectsTaskDependingOnItself() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().id("T-100").dependencyIds(List.of("T-100")).build());
            assertEquals("task cannot depend on itself", ex.getMessage());
        }

        @Test
        void rejectsDuplicateDependencies() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().dependencyIds(Arrays.asList("d1", "d1")).build());
            assertEquals("dependencies must be unique", ex.getMessage());
        }

        @Test
        void requiresStartedAtOnceWorkBegins() {
            for (TaskStatus active : List.of(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED, TaskStatus.IN_REVIEW)) {
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> taskIn(active).startedAt(null).build());
                assertEquals("startedAt is required after work begins", ex.getMessage());
            }
            IllegalArgumentException completed = assertThrows(IllegalArgumentException.class,
                    () -> task().status(TaskStatus.COMPLETED).completedAt(NOW).build());
            assertEquals("startedAt is required after work begins", completed.getMessage());
        }

        @Test
        void requiresCompletedAtForCompletedTask() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> taskIn(TaskStatus.COMPLETED).completedAt(null).build());
            assertEquals("completedAt is required for completed task", ex.getMessage());
        }

        @Test
        void rejectsCompletedAtOutsideCompletedStatus() {
            IllegalArgumentException draft = assertThrows(IllegalArgumentException.class,
                    () -> task().status(TaskStatus.DRAFT).completedAt(NOW).build());
            assertEquals("completedAt is only valid for completed task", draft.getMessage());
            IllegalArgumentException cancelled = assertThrows(IllegalArgumentException.class,
                    () -> task().status(TaskStatus.CANCELLED).completedAt(NOW).build());
            assertEquals("completedAt is only valid for completed task", cancelled.getMessage());
        }

        @Test
        void trimsIdTitleCreatorAndAssignee() {
            Task trimmed = task().id("  T-7  ").title("  Fix login bug  ").creatorId("  u9  ")
                    .assigneeId("  a5  ").build();
            assertEquals("T-7", trimmed.id());
            assertEquals("Fix login bug", trimmed.title());
            assertEquals("u9", trimmed.creatorId());
            assertEquals("a5", trimmed.assigneeId());
            assertEquals("", task().assigneeId("   ").build().assigneeId());
            assertNull(task().assigneeId(null).build().assigneeId());
        }

        @Test
        void isTerminalMatchesOnlyEndStates() {
            assertTrue(taskIn(TaskStatus.COMPLETED).build().isTerminal());
            assertTrue(task().status(TaskStatus.CANCELLED).build().isTerminal());
            assertTrue(task().status(TaskStatus.ARCHIVED).build().isTerminal());
            assertFalse(task().status(TaskStatus.DRAFT).build().isTerminal());
            assertFalse(task().status(TaskStatus.OPEN).build().isTerminal());
            assertFalse(taskIn(TaskStatus.IN_PROGRESS).build().isTerminal());
            assertFalse(taskIn(TaskStatus.BLOCKED).build().isTerminal());
            assertFalse(taskIn(TaskStatus.IN_REVIEW).build().isTerminal());
        }

        @Test
        void exposesEveryComponent() {
            Task t = task().id("T-1").title("Ship release").description("Notes").creatorId("c1").assigneeId("a1")
                    .status(TaskStatus.OPEN).priority(TaskPriority.HIGH).recurrence(RecurrenceRule.WEEKLY)
                    .createdAt(CREATED_AT).dueAt(NOW_PLUS_24H).startedAt(DAY_BEFORE).completedAt(null)
                    .estimatedMinutes(30).actualMinutes(45)
                    .dependencyIds(List.of("d1", "d2")).tags(Set.of("x", "y"))
                    .confidential(true).reviewRequired(true).build();
            assertEquals("T-1", t.id());
            assertEquals("Ship release", t.title());
            assertEquals("Notes", t.description());
            assertEquals("c1", t.creatorId());
            assertEquals("a1", t.assigneeId());
            assertEquals(TaskStatus.OPEN, t.status());
            assertEquals(TaskPriority.HIGH, t.priority());
            assertEquals(RecurrenceRule.WEEKLY, t.recurrence());
            assertEquals(CREATED_AT, t.createdAt());
            assertEquals(NOW_PLUS_24H, t.dueAt());
            assertEquals(DAY_BEFORE, t.startedAt());
            assertNull(t.completedAt());
            assertEquals(30, t.estimatedMinutes());
            assertEquals(45, t.actualMinutes());
            assertEquals(List.of("d1", "d2"), t.dependencyIds());
            assertEquals(Set.of("x", "y"), t.tags());
            assertTrue(t.confidential());
            assertTrue(t.reviewRequired());
        }

        @Test
        void defensivelyCopiesDependencyAndTagCollections() {
            List<String> deps = new ArrayList<>(List.of("d1"));
            Set<String> tags = new HashSet<>(Set.of("t1"));
            Task t = task().dependencyIds(deps).tags(tags).build();
            deps.add("d2");
            tags.add("t2");
            assertEquals(List.of("d1"), t.dependencyIds());
            assertEquals(Set.of("t1"), t.tags());
        }
    }

    @Nested
    class TaskContextTests {

        @Test
        void rejectsNullActorId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ctx().actorId(null).build());
            assertEquals("actorId must not be blank", ex.getMessage());
        }

        @Test
        void rejectsBlankActorId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ctx().actorId("   ").build());
            assertEquals("actorId must not be blank", ex.getMessage());
        }

        @Test
        void trimsActorId() {
            assertEquals("a7", ctx().actorId("  a7  ").build().actorId());
        }

        @Test
        void rejectsNullNow() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> ctx().now(null).build());
            assertEquals("now must not be null", ex.getMessage());
        }

        @Test
        void rejectsNegativePreviousReopens() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ctx().previousReopens(-1).build());
            assertEquals("task counters must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativeAssigneeOpenTaskCount() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ctx().assigneeOpenTaskCount(-1).build());
            assertEquals("task counters must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativeOverdueTaskCount() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ctx().overdueTaskCount(-1).build());
            assertEquals("task counters must not be negative", ex.getMessage());
        }

        @Test
        void exposesEveryField() {
            TaskContext c = ctx().actorId("mgr-2").now(NOW_PLUS_1H).manager(true).administrator(true)
                    .dependenciesComplete(false).assigneeActive(false).projectActive(false)
                    .withinWorkingHours(false).notificationsEnabled(false)
                    .previousReopens(3).assigneeOpenTaskCount(4).overdueTaskCount(5).build();
            assertEquals("mgr-2", c.actorId());
            assertEquals(NOW_PLUS_1H, c.now());
            assertTrue(c.actorIsManager());
            assertTrue(c.actorIsAdministrator());
            assertFalse(c.dependenciesComplete());
            assertFalse(c.assigneeActive());
            assertFalse(c.projectActive());
            assertFalse(c.withinWorkingHours());
            assertFalse(c.notificationsEnabled());
            assertEquals(3, c.previousReopens());
            assertEquals(4, c.assigneeOpenTaskCount());
            assertEquals(5, c.overdueTaskCount());
        }
    }

    @Nested
    class TaskEvaluationTests {

        @Test
        void rejectsNullEffectivePriority() {
            assertThrows(NullPointerException.class,
                    () -> new TaskEvaluation(null, false, false, false, Set.of(), List.of()));
        }

        @Test
        void rejectsNullAllowedActions() {
            assertThrows(NullPointerException.class,
                    () -> new TaskEvaluation(TaskPriority.LOW, false, false, false, null, List.of()));
        }

        @Test
        void rejectsNullWarnings() {
            assertThrows(NullPointerException.class,
                    () -> new TaskEvaluation(TaskPriority.LOW, false, false, false, Set.of(), null));
        }

        @Test
        void copiesCollectionsAndExposesEveryField() {
            EnumSet<TaskAction> actions = EnumSet.of(TaskAction.PUBLISH, TaskAction.CANCEL);
            List<String> warnings = new ArrayList<>(List.of("w1"));
            TaskEvaluation evaluation = new TaskEvaluation(
                    TaskPriority.HIGH, true, false, true, actions, warnings);
            actions.remove(TaskAction.PUBLISH);
            warnings.add("w2");
            assertEquals(TaskPriority.HIGH, evaluation.effectivePriority());
            assertTrue(evaluation.overdue());
            assertFalse(evaluation.escalationRequired());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL), evaluation.allowedActions());
            assertEquals(List.of("w1"), evaluation.warnings());
        }
    }

    @Nested
    class EnumCoverageTests {

        @Test
        void taskActionExposesExactlyTwelveValuesInOrder() {
            assertArrayEquals(new TaskAction[] {TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.UNBLOCK, TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE,
                    TaskAction.REJECT_REVIEW, TaskAction.COMPLETE, TaskAction.REOPEN,
                    TaskAction.CANCEL, TaskAction.ARCHIVE, TaskAction.RESTORE}, TaskAction.values());
            for (TaskAction action : TaskAction.values()) {
                assertSame(action, TaskAction.valueOf(action.name()));
            }
        }

        @Test
        void taskStatusExposesExactlyEightValuesInOrder() {
            assertArrayEquals(new TaskStatus[] {TaskStatus.DRAFT, TaskStatus.OPEN, TaskStatus.IN_PROGRESS,
                    TaskStatus.BLOCKED, TaskStatus.IN_REVIEW, TaskStatus.COMPLETED,
                    TaskStatus.CANCELLED, TaskStatus.ARCHIVED}, TaskStatus.values());
            for (TaskStatus status : TaskStatus.values()) {
                assertSame(status, TaskStatus.valueOf(status.name()));
            }
        }

        @Test
        void taskPriorityExposesExactlyFiveValuesInOrder() {
            assertArrayEquals(new TaskPriority[] {TaskPriority.LOW, TaskPriority.NORMAL, TaskPriority.HIGH,
                    TaskPriority.URGENT, TaskPriority.CRITICAL}, TaskPriority.values());
            for (TaskPriority priority : TaskPriority.values()) {
                assertSame(priority, TaskPriority.valueOf(priority.name()));
            }
        }

        @Test
        void recurrenceRuleExposesExactlySixValuesInOrder() {
            assertArrayEquals(new RecurrenceRule[] {RecurrenceRule.NONE, RecurrenceRule.DAILY,
                    RecurrenceRule.WEEKLY, RecurrenceRule.MONTHLY, RecurrenceRule.QUARTERLY,
                    RecurrenceRule.YEARLY}, RecurrenceRule.values());
            for (RecurrenceRule rule : RecurrenceRule.values()) {
                assertSame(rule, RecurrenceRule.valueOf(rule.name()));
            }
        }
    }

    @Nested
    class TaskPolicyPriorityTests {

        private List<String> warnings() {
            return new ArrayList<>();
        }

        @Test
        void baseScoresMapToStartingEffectivePriorities() {
            // Hand-computed: LOW=0, NORMAL=10, HIGH=20, URGENT=30, CRITICAL=40 against thresholds 10/25/40/60.
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.HIGH).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.URGENT).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.URGENT, POLICY.calculatePriority(
                    task().priority(TaskPriority.CRITICAL).build(), ctx().build(), false, warnings()));
        }

        @Test
        void noWarningsWhenNothingSpecialApplies() {
            List<String> warnings = warnings();
            POLICY.calculatePriority(task().build(), ctx().build(), false, warnings);
            assertTrue(warnings.isEmpty());
        }

        @Test
        void dueAtWithinZeroHoursAdds30() {
            // toHours() truncates: NOW -> 0h (+30) gives LOW+30=30 HIGH; NOW+1h (+20) gives 20 NORMAL.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_1H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void negativeHoursAlsoTakeTheUrgentWindow() {
            // dueAt one hour before now -> Duration.toHours() = -1, which is <= 0, so +30.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void dueAtWithinFourHoursAdds20() {
            // 4h -> +20 => NORMAL+20=30 HIGH; 5h falls into the <=24h window (+12) => 22 NORMAL.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_4H).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_5H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void dueAtWithin24HoursAdds12() {
            // 24h -> +12 => LOW+12=12 NORMAL; 25h -> +5 => 5 LOW.
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_24H).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_25H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void dueAtWithin72HoursAdds5() {
            // HIGH base 20: 72h -> +5 => 25 HIGH; 73h -> +0 => 20 NORMAL.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.HIGH).dueAt(NOW_PLUS_72H).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.HIGH).dueAt(NOW_PLUS_73H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void overdueSevenDaysAdds25AndSixDaysAdds15() {
            // A past dueAt also takes the hours<=0 window (+30). NORMAL base:
            // -7d -> 10+30+25 = 65 CRITICAL; -6d -> 10+30+15 = 55 URGENT.
            assertEquals(TaskPriority.CRITICAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(SEVEN_DAYS_BEFORE).build(),
                    ctx().build(), true, warnings()));
            assertEquals(TaskPriority.URGENT, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(SIX_DAYS_BEFORE).build(),
                    ctx().build(), true, warnings()));
        }

        @Test
        void overdueThreeDaysAdds15AndTwoDaysAdds8() {
            // LOW base: -3d -> 0+30+15 = 45 URGENT; -2d -> 0+30+8 = 38 HIGH.
            assertEquals(TaskPriority.URGENT, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(THREE_DAYS_BEFORE).build(),
                    ctx().build(), true, warnings()));
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(TWO_DAYS_BEFORE).build(),
                    ctx().build(), true, warnings()));
        }

        @Test
        void overdueLessThanThreeDaysAdds8AndWarnsOnce() {
            // -1h -> 0 overdue days -> +8 => LOW 0+30+8 = 38 HIGH, plus exactly one "task is overdue" warning.
            List<String> warnings = warnings();
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(),
                    ctx().build(), true, warnings));
            assertEquals(List.of("task is overdue"), warnings);
        }

        @Test
        void blockedStatusAdds10() {
            // BLOCKED LOW -> 0+10=10 NORMAL; DRAFT LOW -> 0 LOW.
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    taskIn(TaskStatus.BLOCKED).priority(TaskPriority.LOW).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).build(), ctx().build(), false, warnings()));
        }

        @Test
        void confidentialAdds3() {
            // NORMAL(10) + 24h window(+12) = 22 NORMAL; +confidential(3) = 25 HIGH.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).confidential(true).build(),
                    ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).build(),
                    ctx().build(), false, warnings()));
        }

        @Test
        void fiveDependenciesAdd5() {
            // NORMAL(10) + 24h window(+12) + 5 deps(+5) = 27 HIGH; 4 deps => 22 NORMAL.
            List<String> five = List.of("d1", "d2", "d3", "d4", "d5");
            List<String> four = List.of("d1", "d2", "d3", "d4");
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).dependencyIds(five).build(),
                    ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).dependencyIds(four).build(),
                    ctx().build(), false, warnings()));
        }

        @Test
        void threeAssigneeOverdueTasksAdd5() {
            // LOW(0) + 72h window(+5) + count 3(+5) = 10 NORMAL; count 2 => 5 LOW.
            CtxBuilder base = ctx().overdueTaskCount(3);
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_72H).build(), base.build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_72H).build(),
                    base.overdueTaskCount(2).build(), false, warnings()));
        }

        @Test
        void tenAssigneeOverdueTasksAdd15Total() {
            // NORMAL(10): count 9 -> +5 => 15 NORMAL; count 10 -> +5+10 => 25 HIGH.
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).build(),
                    ctx().overdueTaskCount(9).build(), false, warnings()));
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).build(),
                    ctx().overdueTaskCount(10).build(), false, warnings()));
        }

        @Test
        void inactiveProjectCapsScoreAtTenAndWarns() {
            // HIGH(20) capped to 10 => NORMAL; LOW(0) stays 0 => LOW. One warning either way.
            List<String> warnings = warnings();
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.HIGH).build(), ctx().projectActive(false).build(),
                    false, warnings));
            assertEquals(List.of("inactive project suppresses task escalation"), warnings);
            List<String> lowWarnings = warnings();
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).build(), ctx().projectActive(false).build(),
                    false, lowWarnings));
            assertEquals(List.of("inactive project suppresses task escalation"), lowWarnings);
        }

        @Test
        void terminalStatusesForceLowPriority() {
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    taskIn(TaskStatus.COMPLETED).priority(TaskPriority.CRITICAL).build(),
                    ctx().build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().status(TaskStatus.CANCELLED).priority(TaskPriority.CRITICAL).build(),
                    ctx().build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().status(TaskStatus.ARCHIVED).priority(TaskPriority.CRITICAL).build(),
                    ctx().build(), false, warnings()));
        }

        @Test
        void criticalThresholdIsReachedAtExactly60() {
            // CRITICAL(40) + 24h window(+12) + 5 deps(+5) + confidential(+3) = 60 CRITICAL.
            assertEquals(TaskPriority.CRITICAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.CRITICAL).dueAt(NOW_PLUS_24H).confidential(true)
                            .dependencyIds(List.of("d1", "d2", "d3", "d4", "d5")).build(),
                    ctx().build(), false, warnings()));
            // Same score minus the confidential 3 = 57 URGENT.
            assertEquals(TaskPriority.URGENT, POLICY.calculatePriority(
                    task().priority(TaskPriority.CRITICAL).dueAt(NOW_PLUS_24H)
                            .dependencyIds(List.of("d1", "d2", "d3", "d4", "d5")).build(),
                    ctx().build(), false, warnings()));
        }

        @Test
        void urgentThresholdIsReachedAtExactly40() {
            // NORMAL(10) + due now(+30) = 40 URGENT; one hour out = 30 HIGH.
            assertEquals(TaskPriority.URGENT, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_1H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void highThresholdIsReachedAtExactly25() {
            // NORMAL(10) + 24h window(+12) + confidential(+3) = 25 HIGH; without confidential 22 NORMAL.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).confidential(true).build(),
                    ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).build(),
                    ctx().build(), false, warnings()));
        }

        @Test
        void normalThresholdIsReachedAtExactly10() {
            // NORMAL base alone = 10 NORMAL; LOW + 72h window(+5) = 5 LOW.
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_72H).build(), ctx().build(), false, warnings()));
        }
    }

    @Nested
    class TaskPolicyEscalationTests {

        @Test
        void terminalTasksNeverEscalateEvenForInactiveAssignees() {
            for (TaskStatus terminal : List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED)) {
                TaskEvaluation evaluation = POLICY.evaluate(
                        taskIn(terminal).priority(TaskPriority.CRITICAL).build(), ctx().assigneeActive(false).build());
                assertFalse(evaluation.escalationRequired(), terminal.name());
                assertFalse(evaluation.reassignmentRecommended(), terminal.name());
            }
        }

        @Test
        void criticalEffectivePriorityEscalatesWithWarning() {
            // CRITICAL(40) + due now(+30) = 70 CRITICAL -> escalation warning, no other warnings.
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().priority(TaskPriority.CRITICAL).dueAt(NOW).build(), ctx().build());
            assertTrue(evaluation.escalationRequired());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(List.of("critical task requires escalation"), evaluation.warnings());
        }

        @Test
        void overdueUrgentTaskEscalatesAsCriticalWithBothWarnings() {
            // URGENT(30) + past dueAt hours<=0 window(+30) + overdue 0 days(+8) = 68 CRITICAL.
            // The CRITICAL rule fires before the overdue+URGENT rule can, and both warnings accumulate.
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().priority(TaskPriority.URGENT).dueAt(NOW_MINUS_1H).build(), ctx().build());
            assertEquals(TaskPriority.CRITICAL, evaluation.effectivePriority());
            assertTrue(evaluation.escalationRequired());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(List.of("task is overdue", "critical task requires escalation"), evaluation.warnings());
        }

        @Test
        void overdueWithFiveAssigneeOverdueTasksEscalates() {
            // LOW(0) + past dueAt window(+30) + overdue 0 days(+8) = 38 URGENT; count 5 trips the
            // overdue-count escalation rule (no warning), count 4 leaves every escalation rule false.
            TaskEvaluation five = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(),
                    ctx().overdueTaskCount(5).build());
            assertTrue(five.escalationRequired());
            assertEquals(TaskPriority.URGENT, five.effectivePriority());
            assertEquals(List.of("task is overdue"), five.warnings());
            TaskEvaluation four = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(),
                    ctx().overdueTaskCount(4).build());
            assertFalse(four.escalationRequired());
            assertEquals(List.of("task is overdue"), four.warnings());
        }

        @Test
        void blockedMoreThanThreeDaysEscalatesWithWarning() {
            // startedAt four days ago is strictly before now-3d -> escalation warning, no other warnings.
            TaskEvaluation evaluation = POLICY.evaluate(
                    taskIn(TaskStatus.BLOCKED).priority(TaskPriority.LOW).startedAt(FOUR_DAYS_BEFORE).build(),
                    ctx().build());
            assertTrue(evaluation.escalationRequired());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(List.of("task has been blocked for more than three days"), evaluation.warnings());
        }

        @Test
        void blockedExactlyThreeDaysDoesNotEscalate() {
            // isBefore(now-3d) is false at exactly three days, so no escalation and no warnings.
            TaskEvaluation evaluation = POLICY.evaluate(
                    taskIn(TaskStatus.BLOCKED).priority(TaskPriority.LOW).startedAt(THREE_DAYS_BEFORE).build(),
                    ctx().build());
            assertFalse(evaluation.escalationRequired());
            assertEquals(List.of(), evaluation.warnings());
        }

        @Test
        void reviewDueWithinFourHoursEscalatesSilently() {
            // IN_REVIEW + reviewRequired + dueAt two hours out (before now+4h) -> escalation, no warnings.
            TaskEvaluation evaluation = POLICY.evaluate(
                    taskIn(TaskStatus.IN_REVIEW).reviewRequired(true).dueAt(NOW_PLUS_2H).build(), ctx().build());
            assertTrue(evaluation.escalationRequired());
            assertEquals(List.of(), evaluation.warnings());
        }

        @Test
        void reviewDueExactlyAtFourHourBoundaryDoesNotEscalate() {
            // isBefore(now+4h) is false when dueAt equals the boundary.
            TaskEvaluation evaluation = POLICY.evaluate(
                    taskIn(TaskStatus.IN_REVIEW).reviewRequired(true).dueAt(NOW_PLUS_4H).build(), ctx().build());
            assertFalse(evaluation.escalationRequired());
        }

        @Test
        void reviewWithoutDeadlineOrWithoutFlagDoesNotEscalate() {
            TaskEvaluation noFlag = POLICY.evaluate(
                    taskIn(TaskStatus.IN_REVIEW).reviewRequired(false).dueAt(NOW_PLUS_2H).build(), ctx().build());
            assertFalse(noFlag.escalationRequired());
            TaskEvaluation noDeadline = POLICY.evaluate(
                    taskIn(TaskStatus.IN_REVIEW).reviewRequired(true).build(), ctx().build());
            assertFalse(noDeadline.escalationRequired());
        }

        @Test
        void inactiveAssigneeEscalatesAndReassignmentWarns() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).build(), ctx().assigneeActive(false).build());
            assertTrue(evaluation.escalationRequired());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(List.of("assignee is inactive"), evaluation.warnings());
        }

        @Test
        void inactiveAssigneeIgnoredWhenTaskHasNoAssignee() {
            // Last escalation rule needs a non-null assignee; reassignment still flags the missing assignee.
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).assigneeId(null).build(),
                    ctx().assigneeActive(false).build());
            assertFalse(evaluation.escalationRequired());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(List.of("task has no assignee"), evaluation.warnings());
        }
    }

    @Nested
    class TaskPolicyReassignmentTests {

        @Test
        void terminalTasksNeverRecommendReassignment() {
            // Terminal short-circuit runs before the missing-assignee rule, so no warning either.
            TaskEvaluation evaluation = POLICY.evaluate(
                    taskIn(TaskStatus.COMPLETED).assigneeId(null).build(), ctx().build());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(List.of(), evaluation.warnings());
        }

        @Test
        void unassignedTaskRecommendsReassignment() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).assigneeId(null).build(), ctx().build());
            assertTrue(evaluation.reassignmentRecommended());
            assertFalse(evaluation.escalationRequired());
            assertEquals(List.of("task has no assignee"), evaluation.warnings());
        }

        @Test
        void blankAssigneeCountsAsUnassigned() {
            // Task trims "   " to "", which isBlank() catches.
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).assigneeId("   ").build(), ctx().build());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(List.of("task has no assignee"), evaluation.warnings());
        }

        @Test
        void inactiveAssigneeRecommendsReassignmentWithWarning() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).build(), ctx().assigneeActive(false).build());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(List.of("assignee is inactive"), evaluation.warnings());
        }

        @Test
        void workloadAboveTwentyRecommendsReassignment() {
            // Boundary: 21 > 20 trips the rule with a warning; 20 does not.
            TaskEvaluation over = POLICY.evaluate(task().build(), ctx().assigneeOpenTaskCount(21).build());
            assertTrue(over.reassignmentRecommended());
            assertEquals(List.of("assignee workload is very high"), over.warnings());
            TaskEvaluation at = POLICY.evaluate(task().build(), ctx().assigneeOpenTaskCount(20).build());
            assertFalse(at.reassignmentRecommended());
            assertEquals(List.of(), at.warnings());
        }

        @Test
        void highWorkloadWithLowPriorityRecommendsReassignmentSilently() {
            // 13 > 12 and LOW priority -> true without warning; NORMAL priority or count 12 -> false.
            TaskEvaluation low = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).build(), ctx().assigneeOpenTaskCount(13).build());
            assertTrue(low.reassignmentRecommended());
            assertEquals(List.of(), low.warnings());
            TaskEvaluation normal = POLICY.evaluate(
                    task().priority(TaskPriority.NORMAL).build(), ctx().assigneeOpenTaskCount(13).build());
            assertFalse(normal.reassignmentRecommended());
            TaskEvaluation boundary = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).build(), ctx().assigneeOpenTaskCount(12).build());
            assertFalse(boundary.reassignmentRecommended());
        }

        @Test
        void overdueWithMoreThanTenOpenTasksRecommendsReassignment() {
            // assigneeOpenTaskCount feeds reassignment only (overdueTaskCount feeds priority/escalation):
            // LOW(0) + past dueAt window(+30) + overdue 0 days(+8) = 38 HIGH; count 11 > 10 -> reassignment.
            TaskEvaluation over = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(),
                    ctx().assigneeOpenTaskCount(11).build());
            assertTrue(over.reassignmentRecommended());
            assertFalse(over.escalationRequired());
            assertEquals(TaskPriority.HIGH, over.effectivePriority());
            assertEquals(List.of("task is overdue"), over.warnings());
            TaskEvaluation at = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(),
                    ctx().assigneeOpenTaskCount(10).build());
            assertFalse(at.reassignmentRecommended());
            assertFalse(at.escalationRequired());
            assertEquals(List.of("task is overdue"), at.warnings());
        }

        @Test
        void blockedAfterTwoReopensRecommendsReassignment() {
            // Boundary: previousReopens 2 trips the rule, 1 does not.
            TaskEvaluation two = POLICY.evaluate(
                    taskIn(TaskStatus.BLOCKED).priority(TaskPriority.LOW).build(), ctx().previousReopens(2).build());
            assertTrue(two.reassignmentRecommended());
            assertEquals(List.of(), two.warnings());
            TaskEvaluation one = POLICY.evaluate(
                    taskIn(TaskStatus.BLOCKED).priority(TaskPriority.LOW).build(), ctx().previousReopens(1).build());
            assertFalse(one.reassignmentRecommended());
        }

        @Test
        void balancedTaskNeedsNoReassignment() {
            TaskEvaluation evaluation = POLICY.evaluate(task().build(), ctx().build());
            assertFalse(evaluation.reassignmentRecommended());
            assertFalse(evaluation.escalationRequired());
            assertEquals(List.of(), evaluation.warnings());
        }
    }

    @Nested
    class TaskPolicyActionTests {

        @Test
        void draftCreatorAndAdministratorMayPublishOrCancel() {
            assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL),
                    POLICY.evaluate(task().build(), ctx().build()).allowedActions());
            assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL),
                    POLICY.evaluate(task().build(), ctx().actorId("admin-1").administrator(true).build())
                            .allowedActions());
        }

        @Test
        void draftManagerAndAssigneeGetNoActions() {
            assertEquals(Set.of(), POLICY.evaluate(task().build(), ctx().actorId("mgr-1").manager(true).build())
                    .allowedActions());
            assertEquals(Set.of(), POLICY.evaluate(task().build(), ctx().actorId("assignee-1").build())
                    .allowedActions());
        }

        @Test
        void inactiveProjectKeepsDraftCancelButDropsPublish() {
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(task().build(), ctx().projectActive(false).build()).allowedActions());
        }

        @Test
        void outsideWorkingHoursDropsDraftPublishExceptForAdministrator() {
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(task().build(), ctx().withinWorkingHours(false).build()).allowedActions());
            assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL),
                    POLICY.evaluate(task().build(),
                            ctx().actorId("admin-1").administrator(true).withinWorkingHours(false).build())
                            .allowedActions());
        }

        @Test
        void openAssigneeMayStartWhenDependenciesComplete() {
            assertEquals(Set.of(TaskAction.START),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(), ctx().actorId("assignee-1").build())
                            .allowedActions());
        }

        @Test
        void openAssigneeWithIncompleteDependenciesGetsNothingAndWarning() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).build(),
                    ctx().actorId("assignee-1").dependenciesComplete(false).build());
            assertEquals(Set.of(), evaluation.allowedActions());
            assertEquals(List.of("dependencies prevent task start"), evaluation.warnings());
        }

        @Test
        void openManagerAndAdministratorMayStartAndCancel() {
            assertEquals(Set.of(TaskAction.START, TaskAction.CANCEL),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(), ctx().actorId("mgr-1").manager(true).build())
                            .allowedActions());
            assertEquals(Set.of(TaskAction.START, TaskAction.CANCEL),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void openCreatorMayCancelButNotStart() {
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(), ctx().build()).allowedActions());
        }

        @Test
        void inactiveProjectDropsStartFromOpenTasks() {
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(), ctx().actorId("mgr-1").manager(true)
                            .projectActive(false).build()).allowedActions());
        }

        @Test
        void inactiveAssigneeDropsStartFromOpenTasks() {
            assertEquals(Set.of(),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(),
                            ctx().actorId("assignee-1").assigneeActive(false).build()).allowedActions());
        }

        @Test
        void outsideWorkingHoursKeepsOpenStart() {
            assertEquals(Set.of(TaskAction.START),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(),
                            ctx().actorId("assignee-1").withinWorkingHours(false).build()).allowedActions());
        }

        @Test
        void allThreeFiltersCombinedLeaveOnlyCancelForManager() {
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(),
                            ctx().actorId("mgr-1").manager(true).projectActive(false)
                                    .assigneeActive(false).withinWorkingHours(false).build()).allowedActions());
        }

        @Test
        void workInProgressAssigneeBlocksAndCompletesWithoutReview() {
            assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).reviewRequired(false).build(),
                            ctx().actorId("assignee-1").build()).allowedActions());
        }

        @Test
        void workInProgressAssigneeBlocksAndSubmitsWhenReviewRequired() {
            assertEquals(Set.of(TaskAction.BLOCK, TaskAction.SUBMIT_FOR_REVIEW),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).reviewRequired(true).build(),
                            ctx().actorId("assignee-1").build()).allowedActions());
        }

        @Test
        void workInProgressManagerAndAdministratorAlsoCancel() {
            assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).build(),
                            ctx().actorId("mgr-1").manager(true).build()).allowedActions());
            assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void unrelatedActorGetsNothingFromWorkInProgress() {
            assertEquals(Set.of(),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).build(), ctx().actorId("nobody").build())
                            .allowedActions());
        }

        @Test
        void outsideWorkingHoursKeepsBlockAndComplete() {
            assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).build(),
                            ctx().actorId("assignee-1").withinWorkingHours(false).build()).allowedActions());
        }

        @Test
        void blockedAssigneeMayUnblock() {
            assertEquals(Set.of(TaskAction.UNBLOCK),
                    POLICY.evaluate(taskIn(TaskStatus.BLOCKED).build(), ctx().actorId("assignee-1").build())
                            .allowedActions());
        }

        @Test
        void blockedManagerAndAdministratorMayUnblockAndCancel() {
            assertEquals(Set.of(TaskAction.UNBLOCK, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.BLOCKED).build(), ctx().actorId("mgr-1").manager(true).build())
                            .allowedActions());
            assertEquals(Set.of(TaskAction.UNBLOCK, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.BLOCKED).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void inactiveAssigneeDropsUnblock() {
            assertEquals(Set.of(),
                    POLICY.evaluate(taskIn(TaskStatus.BLOCKED).build(),
                            ctx().actorId("assignee-1").assigneeActive(false).build()).allowedActions());
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.BLOCKED).build(),
                            ctx().actorId("mgr-1").manager(true).assigneeActive(false).build()).allowedActions());
        }

        @Test
        void reviewManagerAndAdministratorApproveRejectCancel() {
            assertEquals(Set.of(TaskAction.APPROVE, TaskAction.REJECT_REVIEW, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_REVIEW).build(), ctx().actorId("mgr-1").manager(true).build())
                            .allowedActions());
            assertEquals(Set.of(TaskAction.APPROVE, TaskAction.REJECT_REVIEW, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_REVIEW).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void reviewAssigneeGetsNoActions() {
            assertEquals(Set.of(),
                    POLICY.evaluate(taskIn(TaskStatus.IN_REVIEW).build(), ctx().actorId("assignee-1").build())
                            .allowedActions());
        }

        @Test
        void outsideWorkingHoursDropsApproveExceptForAdministrator() {
            assertEquals(Set.of(TaskAction.REJECT_REVIEW, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_REVIEW).build(),
                            ctx().actorId("mgr-1").manager(true).withinWorkingHours(false).build())
                            .allowedActions());
            assertEquals(Set.of(TaskAction.APPROVE, TaskAction.REJECT_REVIEW, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_REVIEW).build(),
                            ctx().actorId("admin-1").administrator(true).withinWorkingHours(false).build())
                            .allowedActions());
        }

        @Test
        void completedManagerAndAdministratorMayReopenOrArchive() {
            assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE),
                    POLICY.evaluate(taskIn(TaskStatus.COMPLETED).build(), ctx().actorId("mgr-1").manager(true).build())
                            .allowedActions());
            assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE),
                    POLICY.evaluate(taskIn(TaskStatus.COMPLETED).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void completedCreatorAloneGetsNoActions() {
            assertEquals(Set.of(),
                    POLICY.evaluate(taskIn(TaskStatus.COMPLETED).build(), ctx().build()).allowedActions());
        }

        @Test
        void inactiveProjectDropsReopenFromCompletedTasks() {
            assertEquals(Set.of(TaskAction.ARCHIVE),
                    POLICY.evaluate(taskIn(TaskStatus.COMPLETED).build(),
                            ctx().actorId("mgr-1").manager(true).projectActive(false).build()).allowedActions());
        }

        @Test
        void cancelledCreatorManagerAdministratorMayReopenOrArchive() {
            Set<TaskAction> expected = Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE);
            assertEquals(expected, POLICY.evaluate(task().status(TaskStatus.CANCELLED).build(), ctx().build())
                    .allowedActions());
            assertEquals(expected, POLICY.evaluate(task().status(TaskStatus.CANCELLED).build(),
                    ctx().actorId("mgr-1").manager(true).build()).allowedActions());
            assertEquals(expected, POLICY.evaluate(task().status(TaskStatus.CANCELLED).build(),
                    ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void cancelledAssigneeGetsNoActions() {
            assertEquals(Set.of(),
                    POLICY.evaluate(task().status(TaskStatus.CANCELLED).build(), ctx().actorId("assignee-1").build())
                            .allowedActions());
        }

        @Test
        void inactiveProjectDropsReopenFromCancelledTasks() {
            assertEquals(Set.of(TaskAction.ARCHIVE),
                    POLICY.evaluate(task().status(TaskStatus.CANCELLED).build(),
                            ctx().projectActive(false).build()).allowedActions());
        }

        @Test
        void archivedAdministratorAndManagerMayRestore() {
            assertEquals(Set.of(TaskAction.RESTORE),
                    POLICY.evaluate(task().status(TaskStatus.ARCHIVED).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
            assertEquals(Set.of(TaskAction.RESTORE),
                    POLICY.evaluate(task().status(TaskStatus.ARCHIVED).build(),
                            ctx().actorId("mgr-1").manager(true).build()).allowedActions());
        }

        @Test
        void archivedCreatorAndAssigneeGetNoActions() {
            assertEquals(Set.of(),
                    POLICY.evaluate(task().status(TaskStatus.ARCHIVED).build(), ctx().build()).allowedActions());
            assertEquals(Set.of(),
                    POLICY.evaluate(task().status(TaskStatus.ARCHIVED).build(), ctx().actorId("assignee-1").build())
                            .allowedActions());
        }

        @Test
        void outsideWorkingHoursKeepsRestore() {
            assertEquals(Set.of(TaskAction.RESTORE),
                    POLICY.evaluate(task().status(TaskStatus.ARCHIVED).build(),
                            ctx().actorId("admin-1").administrator(true).withinWorkingHours(false).build())
                            .allowedActions());
        }
    }

    @Nested
    class TaskPolicyOverdueTests {

        @Test
        void nullDueAtIsNeverOverdue() {
            assertFalse(POLICY.evaluate(task().build(), ctx().build()).overdue());
        }

        @Test
        void dueAtEqualToNowIsNotOverdue() {
            assertFalse(POLICY.evaluate(task().dueAt(NOW).build(), ctx().build()).overdue());
        }

        @Test
        void pastDueAtOnOpenWorkIsOverdue() {
            assertTrue(POLICY.evaluate(task().dueAt(NOW_MINUS_1H).build(), ctx().build()).overdue());
        }

        @Test
        void futureDueAtIsNotOverdue() {
            assertFalse(POLICY.evaluate(task().dueAt(NOW_PLUS_1H).build(), ctx().build()).overdue());
        }

        @Test
        void terminalStatusesSuppressOverdue() {
            for (TaskStatus terminal : List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED)) {
                assertFalse(POLICY.evaluate(
                        taskIn(terminal).dueAt(SEVEN_DAYS_BEFORE).build(), ctx().build()).overdue(),
                        terminal.name());
            }
        }
    }

    @Nested
    class TaskPolicyEvaluationTests {

        @Test
        void fullEvaluationForBlockedStartAttempt() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).build(),
                    ctx().actorId("assignee-1").dependenciesComplete(false).build());
            assertEquals(TaskPriority.NORMAL, evaluation.effectivePriority());
            assertFalse(evaluation.overdue());
            assertFalse(evaluation.escalationRequired());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(Set.of(), evaluation.allowedActions());
            assertEquals(List.of("dependencies prevent task start"), evaluation.warnings());
        }

        @Test
        void fullEvaluationForOverdueUrgentDraft() {
            // URGENT(30) + past dueAt window(+30) + overdue 0 days(+8) = 68 CRITICAL; creator may publish/cancel.
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().priority(TaskPriority.URGENT).dueAt(NOW_MINUS_1H).build(), ctx().build());
            assertEquals(TaskPriority.CRITICAL, evaluation.effectivePriority());
            assertTrue(evaluation.overdue());
            assertTrue(evaluation.escalationRequired());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL), evaluation.allowedActions());
            assertEquals(List.of("task is overdue", "critical task requires escalation"), evaluation.warnings());
        }

        @Test
        void fullEvaluationForUnassignedOpenTask() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).assigneeId(null).build(), ctx().build());
            assertEquals(TaskPriority.NORMAL, evaluation.effectivePriority());
            assertFalse(evaluation.overdue());
            assertFalse(evaluation.escalationRequired());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(Set.of(TaskAction.CANCEL), evaluation.allowedActions());
            assertEquals(List.of("task has no assignee"), evaluation.warnings());
        }

        @Test
        void evaluateRejectsNullTask() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> POLICY.evaluate(null, ctx().build()));
            assertEquals("task must not be null", ex.getMessage());
        }

        @Test
        void evaluateRejectsNullContext() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> POLICY.evaluate(task().build(), null));
            assertEquals("context must not be null", ex.getMessage());
        }
    }

    /** Every field the state machine is supposed to carry over unchanged. */
    private static void assertCarriedOver(Task original, Task result) {
        assertEquals(original.id(), result.id());
        assertEquals(original.title(), result.title());
        assertEquals(original.description(), result.description());
        assertEquals(original.creatorId(), result.creatorId());
        assertEquals(original.assigneeId(), result.assigneeId());
        assertEquals(original.priority(), result.priority());
        assertEquals(original.recurrence(), result.recurrence());
        assertEquals(original.createdAt(), result.createdAt());
        assertEquals(original.dueAt(), result.dueAt());
        assertEquals(original.estimatedMinutes(), result.estimatedMinutes());
        assertEquals(original.actualMinutes(), result.actualMinutes());
        assertEquals(original.dependencyIds(), result.dependencyIds());
        assertEquals(original.tags(), result.tags());
        assertEquals(original.confidential(), result.confidential());
        assertEquals(original.reviewRequired(), result.reviewRequired());
    }

    @Nested
    class TaskStateMachineTests {

        @Test
        void applyRejectsNullTaskActionAndContext() {
            Task task = task().build();
            assertEquals("task must not be null",
                    assertThrows(NullPointerException.class, () -> MACHINE.apply(null, TaskAction.PUBLISH, ctx().build()))
                            .getMessage());
            assertEquals("action must not be null",
                    assertThrows(NullPointerException.class, () -> MACHINE.apply(task, null, ctx().build()))
                            .getMessage());
            assertEquals("context must not be null",
                    assertThrows(NullPointerException.class, () -> MACHINE.apply(task, TaskAction.PUBLISH, null))
                            .getMessage());
        }

        @Test
        void publishMovesDraftToOpen() {
            Task original = task().build();
            Task result = MACHINE.apply(original, TaskAction.PUBLISH, ctx().build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.OPEN, result.status());
            assertNull(result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void creatorCancelsDraft() {
            Task original = task().build();
            Task result = MACHINE.apply(original, TaskAction.CANCEL, ctx().build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.CANCELLED, result.status());
            assertNull(result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void startBeginsWorkAndStampsStartedAtFromContext() {
            Task original = task().status(TaskStatus.OPEN).build();
            Task result = MACHINE.apply(original, TaskAction.START, ctx().actorId("assignee-1").build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.IN_PROGRESS, result.status());
            assertEquals(NOW, result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void startKeepsAnExistingStartedAt() {
            Task original = task().status(TaskStatus.OPEN).startedAt(DAY_BEFORE).build();
            Task result = MACHINE.apply(original, TaskAction.START, ctx().actorId("assignee-1").build());
            assertEquals(TaskStatus.IN_PROGRESS, result.status());
            assertEquals(DAY_BEFORE, result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void managerCancelsOpenTask() {
            Task original = task().status(TaskStatus.OPEN).build();
            Task result = MACHINE.apply(original, TaskAction.CANCEL, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.CANCELLED, result.status());
            assertNull(result.completedAt());
        }

        @Test
        void assigneeBlocksWorkInProgress() {
            Task original = taskIn(TaskStatus.IN_PROGRESS).build();
            Task result = MACHINE.apply(original, TaskAction.BLOCK, ctx().actorId("assignee-1").build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.BLOCKED, result.status());
            assertEquals(DAY_BEFORE, result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void assigneeCompletesWorkWithoutReview() {
            Task original = taskIn(TaskStatus.IN_PROGRESS).reviewRequired(false).build();
            Task result = MACHINE.apply(original, TaskAction.COMPLETE, ctx().actorId("assignee-1").build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.COMPLETED, result.status());
            assertEquals(NOW, result.completedAt());
            assertEquals(DAY_BEFORE, result.startedAt());
        }

        @Test
        void assigneeSubmitsReviewedWork() {
            Task original = taskIn(TaskStatus.IN_PROGRESS).reviewRequired(true).build();
            Task result = MACHINE.apply(original, TaskAction.SUBMIT_FOR_REVIEW, ctx().actorId("assignee-1").build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.IN_REVIEW, result.status());
            assertNull(result.completedAt());
            assertEquals(DAY_BEFORE, result.startedAt());
        }

        @Test
        void managerCancelsWorkInProgress() {
            Task original = taskIn(TaskStatus.IN_PROGRESS).build();
            Task result = MACHINE.apply(original, TaskAction.CANCEL, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.CANCELLED, result.status());
            assertNull(result.completedAt());
        }

        @Test
        void assigneeUnblocksBlockedTask() {
            Task original = taskIn(TaskStatus.BLOCKED).build();
            Task result = MACHINE.apply(original, TaskAction.UNBLOCK, ctx().actorId("assignee-1").build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.IN_PROGRESS, result.status());
            assertEquals(DAY_BEFORE, result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void managerCancelsBlockedTask() {
            Task original = taskIn(TaskStatus.BLOCKED).build();
            Task result = MACHINE.apply(original, TaskAction.CANCEL, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.CANCELLED, result.status());
        }

        @Test
        void managerApprovesReview() {
            Task original = taskIn(TaskStatus.IN_REVIEW).build();
            Task result = MACHINE.apply(original, TaskAction.APPROVE, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.COMPLETED, result.status());
            assertEquals(NOW, result.completedAt());
            assertEquals(DAY_BEFORE, result.startedAt());
        }

        @Test
        void managerRejectsReviewBackToWork() {
            Task original = taskIn(TaskStatus.IN_REVIEW).build();
            Task result = MACHINE.apply(original, TaskAction.REJECT_REVIEW, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.IN_PROGRESS, result.status());
            assertNull(result.completedAt());
            assertEquals(DAY_BEFORE, result.startedAt());
        }

        @Test
        void managerCancelsTaskInReview() {
            Task original = taskIn(TaskStatus.IN_REVIEW).build();
            Task result = MACHINE.apply(original, TaskAction.CANCEL, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.CANCELLED, result.status());
            assertNull(result.completedAt());
        }

        @Test
        void managerReopensCompletedTaskClearingTimestamps() {
            Task original = taskIn(TaskStatus.COMPLETED).build();
            Task result = MACHINE.apply(original, TaskAction.REOPEN, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.OPEN, result.status());
            assertNull(result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void managerArchivesCompletedTaskDroppingCompletedAt() {
            Task original = taskIn(TaskStatus.COMPLETED).build();
            Task result = MACHINE.apply(original, TaskAction.ARCHIVE, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.ARCHIVED, result.status());
            assertNull(result.completedAt());
            assertEquals(DAY_BEFORE, result.startedAt());
        }

        @Test
        void creatorReopensCancelledTaskClearingStartedAt() {
            Task original = task().status(TaskStatus.CANCELLED).startedAt(DAY_BEFORE).build();
            Task result = MACHINE.apply(original, TaskAction.REOPEN, ctx().build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.OPEN, result.status());
            assertNull(result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void creatorArchivesCancelledTask() {
            Task original = task().status(TaskStatus.CANCELLED).build();
            Task result = MACHINE.apply(original, TaskAction.ARCHIVE, ctx().build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.ARCHIVED, result.status());
            assertNull(result.completedAt());
        }

        @Test
        void restoreFromArchivedAlwaysFailsBecauseInProgressRequiresStartedAt() {
            // RESTORE is policy-legal from ARCHIVED, but the machine nulls startedAt while targeting
            // IN_PROGRESS, so Task's constructor always rejects the result. Verified production behavior.
            Task withHistory = task().status(TaskStatus.ARCHIVED).startedAt(DAY_BEFORE).build();
            IllegalArgumentException withHistoryEx = assertThrows(IllegalArgumentException.class,
                    () -> MACHINE.apply(withHistory, TaskAction.RESTORE, ctx().actorId("admin-1").administrator(true).build()));
            assertEquals("startedAt is required after work begins", withHistoryEx.getMessage());
            Task fresh = task().status(TaskStatus.ARCHIVED).build();
            IllegalArgumentException freshEx = assertThrows(IllegalArgumentException.class,
                    () -> MACHINE.apply(fresh, TaskAction.RESTORE, ctx().actorId("admin-1").administrator(true).build()));
            assertEquals("startedAt is required after work begins", freshEx.getMessage());
        }

        @Test
        void draftRejectsEveryActionExceptPublishAndCancel() {
            Task original = task().build();
            TaskContext context = ctx().build();
            assertEquals(TaskStatus.OPEN, MACHINE.apply(original, TaskAction.PUBLISH, context).status());
            assertEquals(TaskStatus.CANCELLED, MACHINE.apply(original, TaskAction.CANCEL, context).status());
            for (TaskAction action : List.of(TaskAction.START, TaskAction.BLOCK, TaskAction.UNBLOCK,
                    TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE, TaskAction.REJECT_REVIEW,
                    TaskAction.COMPLETE, TaskAction.REOPEN, TaskAction.ARCHIVE, TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.DRAFT, ex.getMessage());
            }
        }

        @Test
        void openRejectsEveryActionExceptStartForAssignee() {
            Task original = task().status(TaskStatus.OPEN).build();
            TaskContext context = ctx().actorId("assignee-1").build();
            assertEquals(TaskStatus.IN_PROGRESS, MACHINE.apply(original, TaskAction.START, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.BLOCK, TaskAction.UNBLOCK,
                    TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE, TaskAction.REJECT_REVIEW,
                    TaskAction.COMPLETE, TaskAction.CANCEL, TaskAction.REOPEN, TaskAction.ARCHIVE,
                    TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.OPEN, ex.getMessage());
            }
        }

        @Test
        void workInProgressRejectsEveryActionExceptBlockAndCompleteForAssignee() {
            Task original = taskIn(TaskStatus.IN_PROGRESS).reviewRequired(false).build();
            TaskContext context = ctx().actorId("assignee-1").build();
            assertEquals(TaskStatus.BLOCKED, MACHINE.apply(original, TaskAction.BLOCK, context).status());
            assertEquals(TaskStatus.COMPLETED, MACHINE.apply(original, TaskAction.COMPLETE, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.UNBLOCK,
                    TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE, TaskAction.REJECT_REVIEW,
                    TaskAction.CANCEL, TaskAction.REOPEN, TaskAction.ARCHIVE, TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.IN_PROGRESS, ex.getMessage());
            }
        }

        @Test
        void blockedRejectsEveryActionExceptUnblockForAssignee() {
            Task original = taskIn(TaskStatus.BLOCKED).build();
            TaskContext context = ctx().actorId("assignee-1").build();
            assertEquals(TaskStatus.IN_PROGRESS, MACHINE.apply(original, TaskAction.UNBLOCK, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE, TaskAction.REJECT_REVIEW,
                    TaskAction.COMPLETE, TaskAction.CANCEL, TaskAction.REOPEN, TaskAction.ARCHIVE,
                    TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.BLOCKED, ex.getMessage());
            }
        }

        @Test
        void inReviewRejectsEveryActionExceptApproveRejectAndCancelForManager() {
            Task original = taskIn(TaskStatus.IN_REVIEW).build();
            TaskContext context = ctx().actorId("mgr-1").manager(true).build();
            assertEquals(TaskStatus.COMPLETED, MACHINE.apply(original, TaskAction.APPROVE, context).status());
            assertEquals(TaskStatus.IN_PROGRESS, MACHINE.apply(original, TaskAction.REJECT_REVIEW, context).status());
            assertEquals(TaskStatus.CANCELLED, MACHINE.apply(original, TaskAction.CANCEL, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.UNBLOCK, TaskAction.SUBMIT_FOR_REVIEW, TaskAction.COMPLETE,
                    TaskAction.REOPEN, TaskAction.ARCHIVE, TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.IN_REVIEW, ex.getMessage());
            }
        }

        @Test
        void completedRejectsEveryActionExceptReopenAndArchiveForManager() {
            Task original = taskIn(TaskStatus.COMPLETED).build();
            TaskContext context = ctx().actorId("mgr-1").manager(true).build();
            assertEquals(TaskStatus.OPEN, MACHINE.apply(original, TaskAction.REOPEN, context).status());
            assertEquals(TaskStatus.ARCHIVED, MACHINE.apply(original, TaskAction.ARCHIVE, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.UNBLOCK, TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE,
                    TaskAction.REJECT_REVIEW, TaskAction.COMPLETE, TaskAction.CANCEL, TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.COMPLETED, ex.getMessage());
            }
        }

        @Test
        void cancelledRejectsEveryActionExceptReopenAndArchiveForCreator() {
            Task original = task().status(TaskStatus.CANCELLED).build();
            TaskContext context = ctx().build();
            assertEquals(TaskStatus.OPEN, MACHINE.apply(original, TaskAction.REOPEN, context).status());
            assertEquals(TaskStatus.ARCHIVED, MACHINE.apply(original, TaskAction.ARCHIVE, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.UNBLOCK, TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE,
                    TaskAction.REJECT_REVIEW, TaskAction.COMPLETE, TaskAction.CANCEL, TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.CANCELLED, ex.getMessage());
            }
        }

        @Test
        void archivedRejectsEveryActionIncludingRestoreForAdministrator() {
            // RESTORE is in the policy's allowed set but always fails in the Task constructor (tested above);
            // every other action must be rejected by the policy gate with IllegalStateException.
            Task original = task().status(TaskStatus.ARCHIVED).build();
            TaskContext context = ctx().actorId("admin-1").administrator(true).build();
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.UNBLOCK, TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE,
                    TaskAction.REJECT_REVIEW, TaskAction.COMPLETE, TaskAction.REOPEN, TaskAction.CANCEL,
                    TaskAction.ARCHIVE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.ARCHIVED, ex.getMessage());
            }
        }
    }

    @Nested
    class DateTextParserTests {

        @Test
        void rejectsNullText() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateTextParser.parse(null));
            assertEquals("date text must not be blank", ex.getMessage());
        }

        @Test
        void rejectsEmptyText() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateTextParser.parse(""));
            assertEquals("date text must not be blank", ex.getMessage());
        }

        @Test
        void rejectsWhitespaceOnlyText() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("   "));
            assertEquals("date text must not be blank", ex.getMessage());
        }

        @Test
        void parsesDashSeparatedFormat() {
            assertEquals(LocalDate.of(2026, 6, 15), DateTextParser.parse("2026-06-15"));
        }

        @Test
        void parsesSlashSeparatedFormat() {
            assertEquals(LocalDate.of(2026, 6, 15), DateTextParser.parse("2026/06/15"));
        }

        @Test
        void parsesCompactFormat() {
            assertEquals(LocalDate.of(2026, 6, 15), DateTextParser.parse("20260615"));
        }

        @Test
        void trimsSurroundingWhitespaceBeforeParsing() {
            assertEquals(LocalDate.of(2026, 6, 15), DateTextParser.parse("  2026-06-15\t"));
        }

        @Test
        void parsesLeapDayInLeapYear() {
            assertEquals(LocalDate.of(2024, 2, 29), DateTextParser.parse("2024-02-29"));
            assertEquals(LocalDate.of(2024, 2, 29), DateTextParser.parse("2024/02/29"));
        }

        @Test
        void rejectsImpossibleDayWithStrictResolving() {
            // LENIENT would roll 2026-02-30 into March; STRICT must refuse.
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("2026-02-30"));
            assertEquals("unsupported date: 2026-02-30", ex.getMessage());
        }

        @Test
        void rejectsLeapDayInNonLeapYear() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("2025-02-29"));
            assertEquals("unsupported date: 2025-02-29", ex.getMessage());
        }

        @Test
        void rejectsMonthThirteen() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("2026-13-01"));
            assertEquals("unsupported date: 2026-13-01", ex.getMessage());
        }

        @Test
        void rejectsDayFirstOrdering() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("15-06-2026"));
            assertEquals("unsupported date: 15-06-2026", ex.getMessage());
        }

        @Test
        void rejectsSingleDigitMonthForTwoDigitPattern() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("2026-6-15"));
            assertEquals("unsupported date: 2026-6-15", ex.getMessage());
        }

        @Test
        void rejectsDateTimeText() {
            // Time components are not part of any accepted exam date format.
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("2026-06-15T10:00"));
            assertEquals("unsupported date: 2026-06-15T10:00", ex.getMessage());
        }

        @Test
        void reportsTheTrimmedValueInUnsupportedDateErrors() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("   2026-99-99  "));
            assertEquals("unsupported date: 2026-99-99", ex.getMessage());
        }
    }
}
