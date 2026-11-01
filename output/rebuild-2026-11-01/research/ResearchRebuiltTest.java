package net.mooctest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Fresh suite derived from the supplied implementation on 2026-11-01.
 * Characterization cases explicitly document observable defects.
 * White-box fixtures never change production code and restore shared state.
 */
public class ResearchRebuiltTest {
    private static Task task(String label, int hours) {
        return new Task(label, hours, Task.Priority.MEDIUM);
    }
    private static Field field(Class<?> owner, String name) throws Exception {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }
    private static void negativeZero(double value) {
        assertEquals(0x8000000000000000L, Double.doubleToRawLongBits(value));
    }
    private static void statistics(RiskAnalyzer.SimulationResult result, double mean, double p90, double max) {
        assertEquals(mean, result.getMeanImpact(), 1e-12);
        assertEquals(p90, result.getP90Impact(), 1e-12);
        assertEquals(max, result.getWorstCaseImpact(), 1e-12);
    }

    @Test void itemDefaultsAndSignedFloatingBoundaries() {
        Budget.Item blank = new Budget.Item(null, -2, -3, null);
        assertEquals("", blank.getName()); assertEquals("GENERAL", blank.getCategory());
        assertEquals(0.0, blank.getCost()); assertEquals(0.0, blank.getValue());
        Budget.Item named = new Budget.Item("hardware", 8.5, 4.5, "CAPITAL");
        assertEquals("hardware", named.getName()); assertEquals("CAPITAL", named.getCategory());
        assertEquals(8.5, named.getCost()); assertEquals(4.5, named.getValue());
        Budget.Item signed = new Budget.Item("", -0.0, -0.0, "");
        negativeZero(signed.getCost()); negativeZero(signed.getValue());
        Budget.Item tiny = new Budget.Item("", -Double.MIN_VALUE, -Double.MIN_VALUE, "");
        assertEquals(0L, Double.doubleToRawLongBits(tiny.getCost()));
        assertEquals(0L, Double.doubleToRawLongBits(tiny.getValue()));
    }

    @Test void budgetAggregationCopiesAndInflationEndpoints() {
        Budget b = new Budget();
        b.add(null); assertEquals(List.of(), b.getItems());
        assertEquals(0, b.totalCost()); assertEquals(0, b.totalValue());
        assertEquals(1000, b.requiredReserve());
        Budget.Item x = new Budget.Item("x", 9000, 4, null);
        Budget.Item y = new Budget.Item("y", 11000, 6, null);
        b.add(x); b.add(y);
        assertEquals(List.of(x, y), b.getItems()); b.getItems().clear();
        assertEquals(2, b.getItems().size());
        assertEquals(20000, b.totalCost()); assertEquals(10, b.totalValue());
        assertEquals(2000, b.requiredReserve());
        double[] input = {-1, -.5, -.1, 0, .25, 1, 2};
        double[] output = {10000, 10000, 18000, 20000, 25000, 40000, 40000};
        for (int i=0;i<input.length;i++) assertEquals(output[i], b.forecastCost(input[i]),1e-8);
    }

    @Test void reserveRatioClampingAndOverflowAreObservable() throws Exception {
        Budget b = new Budget(); b.add(new Budget.Item("large", 40000, 1, null));
        Field ratio = field(Budget.class, "reserveRatio");
        double[] input = {-.1, 0, .02, .3, .5, .7};
        double[] expected = {1000,1000,1000,12000,20000,20000};
        for (int i=0;i<input.length;i++) { b.setReserveRatio(input[i]); assertEquals(expected[i],b.requiredReserve(),1e-8); }
        b.setReserveRatio(-0.0); negativeZero(ratio.getDouble(b));
        b.setReserveRatio(-Double.MIN_VALUE); assertEquals(0L,Double.doubleToRawLongBits(ratio.getDouble(b)));
        Budget overflow = new Budget();
        overflow.add(new Budget.Item("a",Double.MAX_VALUE,0,null));
        overflow.add(new Budget.Item("b",Double.MAX_VALUE,0,null));
        assertEquals(Double.POSITIVE_INFINITY,overflow.totalCost());
        overflow.setReserveRatio(-1); assertTrue(Double.isNaN(overflow.requiredReserve()));
        overflow.setReserveRatio(.5); assertEquals(Double.POSITIVE_INFINITY,overflow.requiredReserve());
    }

    @Test void optimizerMatchesIndependentEnumerationForSmallCapacities() {
        Budget b = new Budget();
        int[] costs={0,1,3,4,6,8}; double[] values={2,1.5,7,8,10,11};
        List<Budget.Item> items = new ArrayList<>();
        for(int i=0;i<costs.length;i++){Budget.Item x=new Budget.Item("I"+i,costs[i],values[i],"C");b.add(x);items.add(x);}
        BudgetOptimizer engine=new BudgetOptimizer();
        for(int cap=0;cap<=24;cap++){
            double best=0;
            for(int subset=0;subset<64;subset++){
                int cost=0;double value=0;
                for(int i=0;i<6;i++) if((subset&(1<<i))!=0){cost+=costs[i];value+=values[i];}
                if(cost<=cap)best=Math.max(best,value);
            }
            BudgetOptimizer.Selection got=engine.optimize(b,cap);
            assertEquals(best,got.getTotalValue(),1e-12,"cap="+cap);
            assertEquals(got.getItems().stream().mapToDouble(Budget.Item::getCost).sum(),got.getTotalCost());
            assertEquals(got.getItems().stream().mapToDouble(Budget.Item::getValue).sum(),got.getTotalValue());
            assertTrue(got.getTotalCost()<=cap);
            assertEquals(got.getItems().size(),new HashSet<>(got.getItems()).size());
            int position=-1;
            for(Budget.Item x:got.getItems()){int next=items.indexOf(x);assertTrue(next>position);position=next;}
        }
    }

    @Test void optimizerEmptyErrorsRoundingAndStableTies() {
        BudgetOptimizer engine=new BudgetOptimizer();
        assertEquals("budget null",assertThrows(DomainException.class,()->engine.optimize(null,1)).getMessage());
        Budget b=new Budget();
        BudgetOptimizer.Selection empty=engine.optimize(b,-1);
        assertTrue(empty.getItems().isEmpty());assertEquals(0,empty.getTotalCost());assertEquals(0,empty.getTotalValue());
        Budget.Item free=new Budget.Item("free",0,2,null); b.add(free);
        assertEquals(List.of(free),engine.optimize(b,-5).getItems());
        Budget.Item first=new Budget.Item("first",2,3,null),tie=new Budget.Item("tie",2,3,null);
        b.add(first);b.add(tie);
        assertEquals(List.of(free,first),engine.optimize(b,2).getItems());
        Budget rounding=new Budget();Budget.Item fraction=new Budget.Item("fraction",2.5,7,null);rounding.add(fraction);
        assertTrue(engine.optimize(rounding,2.49).getItems().isEmpty());
        assertEquals(List.of(fraction),engine.optimize(rounding,2.5).getItems());
        Budget defective=new Budget();Budget.Item cheap=new Budget.Item("rounded",1.49,1,null);defective.add(cheap);
        assertEquals(1.49,engine.optimize(defective,1).getTotalCost()); // Characterizes real cost exceeding limit.
        BudgetOptimizer.Selection explicit=new BudgetOptimizer.Selection(List.of(first),31,47);
        assertEquals(List.of(first),explicit.getItems());assertEquals(31,explicit.getTotalCost());assertEquals(47,explicit.getTotalValue());
    }

    @Test void researcherMetadataAndSkillReplacement() {
        Researcher r=new Researcher(null,-3);assertTrue(r.getId()>0);assertEquals("",r.getName());assertEquals(0,r.getCapacity());assertEquals(0,r.getRating());
        r.setName("Grace");assertEquals("Grace",r.getName());r.setName(null);assertEquals("",r.getName());
        r.addSkill(null,2);r.addSkill("",2);assertTrue(r.getSkills().isEmpty());
        assertEquals(0,r.getSkillLevel("absent"));assertTrue(r.hasSkill("absent",-1));assertFalse(r.hasSkill("absent",1));
        int[] levels={-1,0,3,10,11};int[] expected={0,0,3,10,10};
        for(int i=0;i<levels.length;i++){r.addSkill("analysis",levels[i]);assertEquals(expected[i],r.getSkillLevel("analysis"));}
        r.addSkill("analysis",4);assertTrue(r.hasSkill("analysis",4));assertFalse(r.hasSkill("analysis",5));
        assertEquals(Set.of("analysis"),r.getSkills());r.getSkills().clear();assertEquals(Set.of("analysis"),r.getSkills());
    }

    @Test void researcherHoursRatingAndAssignmentSideEffects() throws Exception {
        Researcher r=new Researcher("Lin",7);
        assertEquals("Lin",r.getName());
        assertFalse(r.allocateHours(-1));assertFalse(r.allocateHours(0));assertFalse(r.allocateHours(8));assertEquals(7,r.getCapacity());
        assertTrue(r.allocateHours(2));assertEquals(5,r.getCapacity());assertTrue(r.allocateHours(5));assertEquals(0,r.getCapacity());
        r.releaseHours(-1);r.releaseHours(0);assertEquals(0,r.getCapacity());r.releaseHours(5);assertEquals(5,r.getCapacity());
        r.releaseHours(35);assertEquals(40,r.getCapacity());r.releaseHours(1);assertEquals(40,r.getCapacity());
        Researcher over=new Researcher("",42);over.releaseHours(0);assertEquals(42,over.getCapacity());over.releaseHours(-1);assertEquals(42,over.getCapacity());
        r.updateRating(100);assertEquals(30,r.getRating(),1e-12);
        r.updateRating(50);assertEquals(36,r.getRating(),1e-12);
        r.updateRating(-1);assertEquals(25.2,r.getRating(),1e-12);
        r.updateRating(120);assertEquals(47.64,r.getRating(),1e-12);
        Researcher worker=new Researcher("worker",5);Task t=task("t",2);
        Field count=field(Researcher.class,"assignedCount");assertEquals(0,count.getInt(worker));
        assertFalse(worker.canAssign(null));assertFalse(worker.assignTask(null));assertFalse(worker.completeTask(null,1));
        assertTrue(worker.canAssign(t));assertTrue(worker.assignTask(t));assertEquals(3,worker.getCapacity());assertEquals(1,count.getInt(worker));
        assertTrue(worker.assignTask(t));assertEquals(1,worker.getCapacity());assertEquals(2,count.getInt(worker));
        assertFalse(worker.canAssign(t));assertFalse(worker.assignTask(t));assertEquals(2,count.getInt(worker));
        assertTrue(worker.completeTask(t,80));assertEquals(3,worker.getCapacity());assertEquals(24,worker.getRating());
        Task zero=task("zero",0);assertTrue(worker.canAssign(zero));assertFalse(worker.assignTask(zero));
    }

    @Test void taskDefaultsSettersAndScheduling() {
        Task t=new Task(null,-1,null);assertTrue(t.getId()>0);assertEquals("",t.getName());assertEquals(0,t.getDuration());
        assertEquals(Task.Priority.MEDIUM,t.getPriority());assertEquals(Task.Status.PLANNED,t.getStatus());assertEquals(0,t.getProgress());assertNull(t.getAssignedResearcherId());
        t.setName("review");t.setDuration(6);t.setPriority(Task.Priority.CRITICAL);t.assignTo(72L);
        assertEquals("review",t.getName());assertEquals(6,t.getDuration());assertEquals(Task.Priority.CRITICAL,t.getPriority());assertEquals(72L,t.getAssignedResearcherId());
        t.setName(null);t.setDuration(-4);t.setPriority(null);t.assignTo(null);
        assertEquals("",t.getName());assertEquals(0,t.getDuration());assertEquals(Task.Priority.MEDIUM,t.getPriority());assertNull(t.getAssignedResearcherId());
        t.setSchedule(2,7,5,11);assertSchedule(t,2,7,5,11);assertEquals(3,t.slack());
        t.setSchedule(4,1,-1,2);assertSchedule(t,4,4,0,4);assertEquals(0,t.slack());
        t.setSchedule(-3,-2,-1,-4);assertSchedule(t,0,0,0,0);
        Task named=new Task("positive",8,Task.Priority.HIGH);assertEquals("positive",named.getName());assertEquals(8,named.getDuration());assertEquals(Task.Priority.HIGH,named.getPriority());
    }

    private static void assertSchedule(Task t,int est,int eft,int lst,int lft){
        assertEquals(est,t.getEst());assertEquals(eft,t.getEft());assertEquals(lst,t.getLst());assertEquals(lft,t.getLft());
    }

    @Test void taskRequirementsDependenciesAndStates() {
        Task t=task("owner",2),d=task("dependency",1);
        t.requireSkill(null,5);t.requireSkill("",5);assertTrue(t.getRequiredSkills().isEmpty());
        t.requireSkill("x",-1);assertEquals(0,t.getRequiredSkills().get("x"));
        t.requireSkill("x",4);t.requireSkill("x",3);assertEquals(4,t.getRequiredSkills().get("x"));
        t.requireSkill("x",11);t.requireSkill("y",10);assertEquals(Map.of("x",10,"y",10),t.getRequiredSkills());
        t.getRequiredSkills().clear();assertEquals(2,t.getRequiredSkills().size());
        assertFalse(t.addDependency(null));assertFalse(t.addDependency(t));assertFalse(t.dependsOn(null));assertFalse(t.dependsOn(d));
        assertTrue(t.addDependency(d));assertFalse(t.addDependency(d));assertTrue(t.dependsOn(d));assertEquals(Set.of(d),t.getDependencies());
        t.getDependencies().clear();assertTrue(t.dependsOn(d));
        t.start();assertEquals(Task.Status.IN_PROGRESS,t.getStatus());t.start();assertEquals(Task.Status.IN_PROGRESS,t.getStatus());
        t.complete();t.start();assertEquals(Task.Status.DONE,t.getStatus());t.cancel();t.start();assertEquals(Task.Status.CANCELLED,t.getStatus());
        double[] p={-1,0,.3,1,2};double[] expected={0,0,.3,1,1};
        for(int i=0;i<p.length;i++){t.updateProgress(p[i]);assertEquals(expected[i],t.getProgress());}
        t.updateProgress(-0.0);negativeZero(t.getProgress());t.updateProgress(-Double.MIN_VALUE);assertEquals(0L,Double.doubleToRawLongBits(t.getProgress()));
    }

    @Test void graphsHandleIndependentNodesSharedDependenciesAndCycles() {
        assertEquals(List.of(),GraphUtils.topologicalSort(List.of()));assertFalse(GraphUtils.hasCycle(List.of()));assertEquals(0,GraphUtils.longestPathDuration(List.of()));
        new Scheduler().schedule(List.of());
        Task a=task("a",4),b=task("b",9),zero=task("zero",0);
        List<Task> independent=List.of(a,b,zero);assertEquals(Set.copyOf(independent),Set.copyOf(GraphUtils.topologicalSort(independent)));
        assertEquals(9,GraphUtils.longestPathDuration(independent));new Scheduler().schedule(independent);
        assertSchedule(a,0,4,5,9);assertSchedule(b,0,9,0,9);assertSchedule(zero,0,0,9,9);
        Task dependency=task("shared",2),left=task("left",3),right=task("right",7);
        left.addDependency(dependency);right.addDependency(dependency);
        List<Task> order=GraphUtils.topologicalSort(List.of(left,right,dependency));
        assertEquals(3,order.size());assertEquals(dependency,order.get(2));assertEquals(Set.of(left,right),Set.copyOf(order.subList(0,2)));
        assertEquals(7,GraphUtils.longestPathDuration(List.of(left,right,dependency)));
        a.addDependency(b);b.addDependency(a);
        assertTrue(GraphUtils.hasCycle(List.of(a,b)));
        assertEquals("cycle detected",assertThrows(DomainException.class,()->GraphUtils.topologicalSort(List.of(a,b))).getMessage());
        assertThrows(DomainException.class,()->GraphUtils.longestPathDuration(List.of(a,b)));
        assertThrows(DomainException.class,()->new Scheduler().schedule(List.of(a,b)));
    }

    @Test void schedulingCharacterizesReverseDependencyTraversal() {
        Task origin=task("origin",4),middle=task("middle",7),end=task("end",3);
        middle.addDependency(origin);end.addDependency(middle);
        List<Task> input=List.of(origin,middle,end);
        assertEquals(List.of(end,middle,origin),GraphUtils.topologicalSort(input));
        assertEquals(7,GraphUtils.longestPathDuration(input));
        assertEquals(List.of(end,middle,origin),GraphUtils.topologicalSort(List.of(end,middle)));
        new Scheduler().schedule(input);
        assertSchedule(origin,0,4,3,7);assertSchedule(middle,0,7,0,7);assertSchedule(end,0,3,0,3);
    }

    @Test void matchingScoresPreferenceTiesAndCapacityAccounting() {
        MatchingEngine m=new MatchingEngine();Researcher weak=new Researcher("weak",8),strong=new Researcher("strong",8),tie=new Researcher("tie",8);
        strong.updateRating(60);tie.updateRating(60);
        assertEquals(1.2,m.score(strong,task("3h",3)),1e-12);assertEquals(1.7,m.score(strong,task("10h",10)),1e-12);
        Task t=task("job",3);List<MatchingEngine.Assignment> chosen=m.match(List.of(weak,strong,tie),new ArrayList<>(List.of(t)));
        assertEquals(1,chosen.size());MatchingEngine.Assignment x=chosen.get(0);
        assertSame(t,x.getTask());assertSame(strong,x.getResearcher());assertEquals(1.2,x.getScore(),1e-12);
        assertEquals(strong.getId(),t.getAssignedResearcherId());assertEquals(5,strong.getCapacity());assertEquals(8,weak.getCapacity());assertEquals(8,tie.getCapacity());
        Task next=task("second",2);assertEquals(1,m.match(List.of(weak),new ArrayList<>(List.of(t,next))).size());assertNull(next.getAssignedResearcherId());
        MatchingEngine.Assignment explicit=new MatchingEngine.Assignment(null,weak,4.25);assertNull(explicit.getTask());assertSame(weak,explicit.getResearcher());assertEquals(4.25,explicit.getScore());
    }

    @Test void matchingEmptyResultsRejectionsAndCharacterizedPriorityOrder() {
        MatchingEngine m=new MatchingEngine();
        List<MatchingEngine.Assignment> a=m.match(null,new ArrayList<>()),b=m.match(new ArrayList<>(),null);
        a.add(new MatchingEngine.Assignment(null,null,0));assertTrue(b.isEmpty());assertTrue(m.match(null,null).isEmpty());
        assertTrue(m.match(List.of(),new ArrayList<>()).isEmpty());
        Task tooBig=task("huge",10);Researcher small=new Researcher("small",1);
        assertTrue(m.match(List.of(small),new ArrayList<>(List.of(tooBig))).isEmpty());assertEquals(1,small.getCapacity());assertNull(tooBig.getAssignedResearcherId());
        Task important=new Task("important",2,Task.Priority.CRITICAL),shortLow=new Task("short",1,Task.Priority.LOW),longLow=new Task("long",3,Task.Priority.LOW);
        ArrayList<Task> jobs=new ArrayList<>(List.of(important,shortLow,longLow));Researcher only=new Researcher("only",10);
        List<MatchingEngine.Assignment> result=m.match(List.of(only),jobs);
        assertEquals(List.of(longLow,shortLow,important),jobs);assertEquals(1,result.size());assertSame(longLow,result.get(0).getTask());assertEquals(7,only.getCapacity());
    }

    @Test void resourcesValidateIntervalsAndPreserveBookingState() {
        Resource r=new Resource(null,null);assertTrue(r.getId()>0);assertEquals("",r.getName());assertEquals("GENERIC",r.getType());
        r.setName("bench");r.setType("LAB");assertEquals("bench",r.getName());assertEquals("LAB",r.getType());r.setName(null);r.setType(null);assertEquals("",r.getName());assertEquals("GENERIC",r.getType());
        Resource named=new Resource("room","SPACE");assertEquals("room",named.getName());assertEquals("SPACE",named.getType());
        LocalDateTime start=LocalDateTime.of(2032,4,6,10,0),end=start.plusHours(1);
        assertFalse(r.isAvailable(null,end));assertFalse(r.isAvailable(start,null));assertFalse(r.isAvailable(start,start));assertFalse(r.isAvailable(end,start));
        assertFalse(r.book(start,start));assertTrue(r.listBookings().isEmpty());r.cancel(null);
        assertTrue(r.book(start,end));assertFalse(r.book(start,end));assertFalse(r.isAvailable(start.plusMinutes(15),end));assertFalse(r.isAvailable(end,end.plusHours(1)));
        assertTrue(r.isAvailable(end.plusSeconds(1),end.plusHours(1)));assertTrue(r.book(end.plusHours(1),end.plusHours(2)));
        assertEquals(2,r.listBookings().size());assertEquals(start,r.listBookings().get(0).getKey());assertEquals(end,r.listBookings().get(0).getValue());
        r.listBookings().clear();assertEquals(2,r.listBookings().size());r.cancel(start);assertEquals(1,r.listBookings().size());assertTrue(r.isAvailable(start,end));
        assertTrue(r.isAvailable(start,end.plusHours(3))); // Characterizes ignored future overlaps.
        assertFalse(r.conflicts(end.plusHours(1),end.plusHours(2)));assertFalse(r.conflicts(null,null));
    }

    @Test void riskBoundsScoresPrioritiesAndTieBreaking() {
        Risk blank=new Risk(null,null,-1,2);assertEquals("",blank.getName());assertEquals("GENERAL",blank.getCategory());assertEquals(0,blank.getProbability());assertEquals(1,blank.getImpact());
        Risk sample=new Risk("delay","TIME",.5,.8);assertEquals("delay",sample.getName());assertEquals("TIME",sample.getCategory());assertEquals(.4,sample.score());assertEquals(.5,sample.getProbability());assertEquals(.8,sample.getImpact());
        double[] probabilities={0,.01,.249999,.25,.499999,.5,1};int[] ranks={0,1,1,2,2,3,3};
        for(int i=0;i<probabilities.length;i++)assertEquals(ranks[i],new Risk("","",probabilities[i],1).priority());
        Risk signed=new Risk("","",-0.0,-0.0);negativeZero(signed.getProbability());negativeZero(signed.getImpact());negativeZero(signed.clamp(-0.0));
        assertEquals(0L,Double.doubleToRawLongBits(signed.clamp(-Double.MIN_VALUE)));assertEquals(1,signed.clamp(1));
        Risk high=new Risk("z","",1,1),low=new Risk("z","",.1,.1);
        assertTrue(high.compareTo(low)<0);assertTrue(low.compareTo(high)>0);
        Risk large=new Risk("b","",.1,.9),less=new Risk("b","",.1,.8);
        assertTrue(large.compareTo(less)<0);assertTrue(less.compareTo(large)>0);
        Risk first=new Risk("a","",.1,.9);assertTrue(first.compareTo(large)<0);assertTrue(large.compareTo(first)>0);assertEquals(0,large.compareTo(new Risk("b","other",.1,.9)));
    }

    @Test void simulationHandlesEmptyInputsAndDeterministicPercentiles() {
        RiskAnalyzer engine=new RiskAnalyzer();
        statistics(engine.simulate(null,5),0,0,0);statistics(engine.simulate(List.of(),5),0,0,0);
        List<Risk> risks=List.of(new Risk("certain","",1,.25),new Risk("never","",0,1));
        statistics(engine.simulate(risks,0),0,0,0);statistics(engine.simulate(risks,-1),0,0,0);statistics(engine.simulate(risks,10),.25,.25,.25);
        statistics(new RiskAnalyzer.SimulationResult(.1,.2,.3),.1,.2,.3);
        RiskAnalyzer boundary=new RiskAnalyzer(){private int i;public double rnd(){return new double[]{0,.49,.5,.75}[i++];}};
        statistics(boundary.simulate(List.of(new Risk("half","",.5,.8)),4),.4,.8,.8);
        for(int count:new int[]{10,20}){
            RiskAnalyzer oneEvent=new RiskAnalyzer(){private int i;public double rnd(){return i++==0?0:1;}};
            statistics(oneEvent.simulate(List.of(new Risk("one","",.5,1)),count),1.0/count,count==10?1:0,1);
        }
        RiskAnalyzer allZero=new RiskAnalyzer(){public double rnd(){return 0;}};
        statistics(allZero.simulate(List.of(new Risk("zero","",0,1)),1),0,0,0);
    }

    @Test void randomGeneratorHasKnownFirstTransitionAndReproducibleRange() throws Exception {
        RiskAnalyzer first=new RiskAnalyzer(),second=new RiskAnalyzer();
        for(int i=0;i<20;i++){double v=first.rnd();assertEquals(v,second.rnd());assertTrue(v>=0&&v<1);}
        Field seed=field(RiskAnalyzer.class,"seed");seed.setLong(first,1L);
        assertEquals(1082269761L/9007199254740992.0,first.rnd());
        assertEquals(1082269761L,seed.getLong(first));
        seed.setLong(first,0L);assertEquals(0,first.rnd());assertEquals(0L,seed.getLong(first));
    }

    @Test void projectStoresIdentityViewsAndEveryStatus() {
        Project p=new Project(null);assertTrue(p.getId()>0);assertEquals("",p.getName());assertNotNull(p.getBudget());
        p.setName("Study");assertEquals("Study",p.getName());p.setName(null);assertEquals("",p.getName());
        Budget original=p.getBudget();p.setBudget(null);assertSame(original,p.getBudget());Budget replacement=new Budget();p.setBudget(replacement);assertSame(replacement,p.getBudget());
        assertNull(p.addTask(null));assertNull(p.addResearcher(null));p.addRisk(null);assertTrue(p.getRisks().isEmpty());
        assertNull(p.getTask(0));assertNull(p.getResearcher(0));assertTrue(p.getTasks().isEmpty());assertTrue(p.getResearchers().isEmpty());
        Task planned=task("planned",1),started=task("started",2),done=task("done",3),cancelled=task("cancelled",4);
        started.start();done.complete();cancelled.cancel();
        for(Task t:List.of(planned,started,done,cancelled)){assertSame(t,p.addTask(t));assertSame(t,p.getTask(t.getId()));}
        assertSame(planned,p.addTask(planned));assertEquals(4,p.getTasks().size());
        Map<Task.Status,Long> expected=new EnumMap<>(Task.Status.class);for(Task.Status s:Task.Status.values())expected.put(s,0L);
        expected.put(Task.Status.PLANNED,1L);expected.put(Task.Status.IN_PROGRESS,1L);expected.put(Task.Status.DONE,1L);expected.put(Task.Status.CANCELLED,1L);assertEquals(expected,p.statusCounts());
        Researcher r=new Researcher("r",8);assertSame(r,p.addResearcher(r));assertSame(r,p.getResearcher(r.getId()));assertEquals(List.of(r),new ArrayList<>(p.getResearchers()));
        Risk risk=new Risk("risk","",1,.5);p.addRisk(risk);assertEquals(List.of(risk),p.getRisks());p.getRisks().clear();assertEquals(1,p.getRisks().size());
        assertEquals(4,p.criticalPathDuration());statistics(p.analyzeRisk(3),.5,.5,.5);statistics(p.analyzeRisk(0),0,0,0);
        assertEquals(1,p.planAssignments().size());assertEquals(4,r.getCapacity());
        p.getTasks().clear();assertTrue(p.getTasks().isEmpty());p.getResearchers().clear();assertTrue(p.getResearchers().isEmpty());
    }

    @Test void reportHasExactStableLinesAndDomainValues() {
        ReportGenerator report=new ReportGenerator();assertEquals("",report.generate(null));
        Project p=new Project("Aster");Task t=task("single",6);t.start();p.addTask(t);
        p.getBudget().add(new Budget.Item("unit",12.5,3.5,"x"));p.addRisk(new Risk("certain","",1,.5));
        assertEquals("Project:Aster\nStatus PLANNED:0\nStatus IN_PROGRESS:1\nStatus BLOCKED:0\nStatus DONE:0\nStatus CANCELLED:0\nCriticalPath:6\nBudgetCost:12.5\nBudgetValue:3.5\nRiskMean:0.5\nRiskP90:0.5\nRiskWorst:0.5\n",report.generate(p));
    }

    @Test void idParsingCoversUnsignedBoundariesAndExceptionChains() {
        for(String s:new String[]{null,""})assertEquals("id string invalid",assertThrows(DomainException.class,()->IdGenerator.fromString(s)).getMessage());
        DomainException zero=assertThrows(DomainException.class,()->IdGenerator.fromString("0"));assertEquals("id zero invalid",zero.getMessage());assertNull(zero.getCause());
        for(String s:new String[]{"-1","word","18446744073709551616"}){DomainException e=assertThrows(DomainException.class,()->IdGenerator.fromString(s));assertEquals("id parse failed",e.getMessage());assertInstanceOf(NumberFormatException.class,e.getCause());}
        assertEquals(1,IdGenerator.fromString("1"));assertEquals(Long.MAX_VALUE,IdGenerator.fromString("9223372036854775807"));assertEquals(Long.MIN_VALUE,IdGenerator.fromString("9223372036854775808"));assertEquals(-1,IdGenerator.fromString("18446744073709551615"));
        String text=IdGenerator.nextIdStr();assertEquals(text,Long.toUnsignedString(IdGenerator.fromString(text)));assertTrue(IdGenerator.fromString(text)>0);
        RuntimeException cause=new RuntimeException("underlying");DomainException wrapped=new DomainException("outer",cause);assertEquals("outer",wrapped.getMessage());assertSame(cause,wrapped.getCause());assertEquals("plain",new DomainException("plain").getMessage());
    }

    @Test void idCounterFixturesExerciseBothSignsWithoutLeakingGlobalState() throws Exception {
        AtomicLong count=(AtomicLong)field(IdGenerator.class,"COUNTER").get(null);long saved=count.get();
        try{
            for(long initial:new long[]{0,Long.MIN_VALUE}){
                count.set(initial);long low=System.currentTimeMillis();long id=IdGenerator.nextId();long high=System.currentTimeMillis();
                assertEquals(initial+1,count.get());assertTrue(id>0);
                boolean decoded=false;
                for(long signed:new long[]{id,-id}){
                    long bits=signed^(initial+1);long millis=bits>>>20;
                    decoded|=(bits&1048575L)==0&&millis>=low&&millis<=high;
                }
                assertTrue(decoded,"timestamp must survive ID composition");
            }
        }finally{count.set(saved);}
        for(Class<?> utility:List.of(IdGenerator.class,GraphUtils.class)){
            var ctor=utility.getDeclaredConstructor();ctor.setAccessible(true);assertEquals(utility,ctor.newInstance().getClass());
        }
    }

    @Test void exactAvailableCapacityStillAcceptsTheTask() {
        Researcher researcher = new Researcher("exact fit", 6);
        Task exact = task("six hours", 6);
        assertTrue(researcher.canAssign(exact));
        assertTrue(researcher.assignTask(exact));
        assertEquals(0, researcher.getCapacity());
        assertFalse(researcher.canAssign(exact));
    }

    @Test void riskPriorityOutranksContradictoryImpactOrder() {
        // Higher probability gives the smaller impact a higher overall priority.
        Risk frequent = new Risk("frequent", "COST", 1.0, 0.6);
        Risk rare = new Risk("rare", "COST", 0.1, 0.9);
        assertEquals(3, frequent.priority());
        assertEquals(1, rare.priority());
        assertTrue(frequent.compareTo(rare) < 0);
        assertTrue(rare.compareTo(frequent) > 0);
    }

    @Test void bookingEntrySnapshotCharacterizesLiveValueAliasing() {
        Resource resource = new Resource("lab", "room");
        LocalDateTime begin = LocalDateTime.of(2033, 5, 2, 8, 0);
        LocalDateTime finish = begin.plusHours(1);
        assertTrue(resource.book(begin, finish));
        assertTrue(resource.isAvailable(finish.plusMinutes(30), finish.plusHours(2)));
        List<Map.Entry<LocalDateTime, LocalDateTime>> snapshot = resource.listBookings();
        assertEquals(finish, snapshot.get(0).setValue(finish.plusHours(1)));
        assertEquals(finish.plusHours(1), resource.listBookings().get(0).getValue());
        assertFalse(resource.isAvailable(finish.plusMinutes(30), finish.plusHours(2)));
        snapshot.clear();
        assertEquals(1, resource.listBookings().size());
        // The list is copied, but its Map.Entry objects still update the real bookings.
    }
}
