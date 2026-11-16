package net.mooctest;

import net.mooctest.attendance.*;
import net.mooctest.leave.*;
import net.mooctest.domain.*;
import net.mooctest.service.*;
import net.mooctest.port.*;
import net.mooctest.exception.*;
import net.mooctest.util.DateTextParser;
import java.time.*;
import java.util.*;


import net.mooctest.card.*;
import net.mooctest.expense.*;
import net.mooctest.permission.*;
import net.mooctest.workflow.*;
import net.mooctest.task.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.lang.reflect.*;
import static org.junit.jupiter.api.Assertions.*;

class StudentTest {
    static final Instant MOMENT = Instant.parse("2026-06-08T09:00:00Z");
    static final LocalDate DATE = LocalDate.of(2026,6,8);
    static final Clock CLOCK = Clock.fixed(MOMENT,ZoneOffset.UTC);
    static BigDecimal cash(String n) { return new BigDecimal(n); }
    // Constructs a NEW real record through its canonical constructor; never changes fields or methods.
    @SuppressWarnings("unchecked") static <T> T changed(T value,Object... edits) {
        try {
            RecordComponent[] fields=value.getClass().getRecordComponents();
            Class<?>[] types=new Class<?>[fields.length];Object[] args=new Object[fields.length];
            for(int i=0;i<fields.length;i++) {
                types[i]=fields[i].getType();args[i]=fields[i].getAccessor().invoke(value);
                for(int j=0;j<edits.length;j+=2)if(fields[i].getName().equals(edits[j]))args[i]=edits[j+1];
            }
            return (T)value.getClass().getDeclaredConstructor(types).newInstance(args);
        } catch(InvocationTargetException e) {
            if(e.getCause() instanceof RuntimeException x)throw x;
            throw new AssertionError(e.getCause());
        } catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    static void bad(Object value,String field,Object input) { assertThrows(RuntimeException.class,()->changed(value,field,input),field+"="+input); }
    static void required(Object value,String... names) { for(String name:names) bad(value,name,null); }
    static void textFields(Object value,String... names) {
        for(String name:names) for(String text:new String[]{null,""," \t"})bad(value,name,text);
    }
    static void error(String message,Executable action) { assertEquals(message,assertThrows(RuntimeException.class,action).getMessage()); }
    static String number(String prefix,int length) {return prefix+"0".repeat(length-prefix.length());}
    static CreditCardValidator network(String kind,String n) {
        try{return (CreditCardValidator)Class.forName("net.mooctest.card."+kind+"Validator").getConstructor(String.class).newInstance(n);}
        catch(InvocationTargetException e){throw (RuntimeException)e.getCause();}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    @Test void cardNetworkBoundariesFromIssuerSpecifications() {
        String[][] rows={
            {"Visa","13","19","4","3,5"},
            {"AmericanExpress","15","15","34,35,36,37","33,38"},
            {"MasterCard","16","16","51,55,222100,272099","50,56,222099,272100"},
            {"Discover","16","16","6011,622126,622925,644,649,65","6010,6012,622125,622926,643,66"},
            {"VisaElectron","16","16","417500,4026,4405,4508,4844,4913,4917","417499,417501,4025,4027,4912,4918,04026"},
            {"Maestro","12","19","500000,509999,560000,589999,600000,699999","499999,510000,559999,590000,599999,700000"},
            {"MaestroUK","12","19","6759,676770,676774","6758,6760,676769,676771,676773,676775"},
            {"JCB","16","16","3528,3589","3527,3590"},
            {"ChinaTUnion","19","19","31","30,32"},
            {"ChinaUnionPay","16","19","62","61,63"},
            {"DinersClubInternational","16","19","300,305,3095,38,39","299,306,3094,3096,37,40"},
            {"DinersClub","16","16","54,55","53,56"},
            {"RuPay","16","16","60,6521,6522","59,61,6520,6523"},
            {"InterPayment","16","19","636","635,637"},
            {"InstaPaymentt","16","16","637,638,639","636,640"},
            {"Dankort","16","16","4571,5019","4570,5020"},
            {"MIR","16","16","2200,2204","2199,2205"},
            {"NPS_Pridnestrovie","16","16","6054740,6054744","6054739,6054745"},
            {"Troy","16","16","979200,979289","979199,979290"},
            {"UATP","15","15","1","0,2"},
            {"Verve","16","19","506099,506198","506098,506199,650002,650027"},
            {"LankaPay","16","16","357111","357110,357112"}
        };
        for(String[] row:rows) {
            String name=row[0];int low=Integer.parseInt(row[1]),high=Integer.parseInt(row[2]);
            for(String prefix:row[3].split(",")) for(int len:new int[]{low-1,low,high,high+1})
                assertEquals(len>=low&&len<=high,network(name,number(prefix,len)).validate(),name+"/"+prefix+"/"+len);
            for(String prefix:row[4].split(","))assertFalse(network(name,number(prefix,low)).validate(),name+"/"+prefix);
            assertThrows(NullPointerException.class,()->network(name,null));
            assertThrows(IndexOutOfBoundsException.class,()->network(name,"").validate());
        }
    }
    @Test void explicitIssuerSelectionAndPrecedence() {
        String[][] pairs={{"NPS_PRIDNESTROVIE","6054740000000000"},{"LANKAPAY","3571110000000000"},{"VERVE","5060990000000000"},{"TROY","9792000000000000"},{"VISA_ELECTRON","4175000000000000"},{"MAESTRO_UK","675900000000"},{"DANKORT","4571000000000000"},{"VISA","4111111111111"},{"AMERICAN_EXPRESS","340000000000000"},{"MASTERCARD","5400000000000000"},{"DISCOVER","6011000000000000"},{"JCB","3528000000000000"},{"CHINA_TUNION","3100000000000000000"},{"CHINA_UNIONPAY","6200000000000000"},{"DINERS_CLUB_INTERNATIONAL","3000000000000000"},{"RUPAY","6000000000000000"},{"INTERPAYMENT","6360000000000000"},{"INSTAPAYMENT","6370000000000000"},{"MIR","2200000000000000"},{"UATP","100000000000000"},{"MAESTRO","600000000000"}};
        for(String[] pair:pairs) {
            CreditCardType issuer=CreditCardType.valueOf(pair[0]);assertEquals(issuer,TypeChecker.checkType(pair[1]));assertTrue(TypeChecker.matchesType(pair[1],issuer));
        }
        assertTrue(TypeChecker.matchesType("5400000000000000",CreditCardType.DINERS_CLUB));
        for(CreditCardType issuer:CreditCardType.values()) {
            assertFalse(TypeChecker.matchesType("9999999999999999",issuer));
            for(String syntax:List.of("","4","41111111111","41111111111111111111","4X11111111111111"))assertFalse(TypeChecker.matchesType(syntax,issuer));
        }
        for(String syntax:List.of("","123","9999999999999999","Ａ11111111111111"))assertEquals(CreditCardType.OTHER,TypeChecker.checkType(syntax));
        assertThrows(NullPointerException.class,()->TypeChecker.checkType(null));
        assertThrows(NullPointerException.class,()->TypeChecker.matchesType(null,CreditCardType.VISA));
        assertThrows(NullPointerException.class,()->TypeChecker.matchesType("4111111111111",null));
    }
    @Test void parserChecksumAndDateUtilities() {
        new CreditCardParser();new DateChecker();new DateParser();new DateFormatter();
        assertEquals(List.of(0,9,10,35,40,-1),CreditCardParser.parseNumber("09AZ፵-"));
        assertEquals(List.of(),CreditCardParser.parseNumber(""));
        assertEquals(123,CreditCardParser.parseIIN(List.of(0,1,2,3,9),4));
        assertThrows(NumberFormatException.class,()->CreditCardParser.parseIIN(List.of(1),0));
        assertThrows(IndexOutOfBoundsException.class,()->CreditCardParser.parseIIN(List.of(1),2));
        LuhnValidator luhn=new LuhnValidator();
        for(String ok:List.of("","0","18","26","34","42","59","67","75","83","91","4111111111111111","378282246310005","-2"))assertTrue(luhn.validate(ok),ok);
        for(String bad:List.of("1","11","27","4111111111111112"))assertFalse(luhn.algorithmCheck(bad));
        assertEquals(1,luhn.sumOfDigits(10));assertEquals(9,luhn.sumOfDigits(18));assertEquals(45,luhn.sumOfDigits(123456789));assertEquals(0,luhn.sumOfDigits(0));
        assertThrows(NumberFormatException.class,()->luhn.sumOfDigits(-1));assertThrows(NullPointerException.class,()->luhn.validate(null));
        StringBuilder date=new StringBuilder("12/28");assertSame(date,DateParser.parseDate(date));assertEquals("1228",date.toString());assertEquals(28,DateParser.parseDate(date,2,4));
        assertEquals("12/28",DateChecker.convertDate("12/28").toString());assertSame(date,DateParser.parseDate(date));
        assertFalse(DateChecker.compareDates(5,5));assertFalse(DateChecker.compareDates(4,5));assertTrue(DateChecker.compareDates(6,5));
        assertEquals("yy",((java.text.SimpleDateFormat)DateFormatter.yearFormat()).toPattern());assertEquals("MM",((java.text.SimpleDateFormat)DateFormatter.monthFormat()).toPattern());
        assertEquals(LocalDate.now().getYear()%100,DateChecker.getCurrentYear());assertEquals(LocalDate.now().getMonthValue(),DateChecker.getCurrentMonth());
        for(int month:new int[]{5,6,7})assertEquals(month>=6,DateChecker.isValidExpiration(month,26,CLOCK));
        assertFalse(DateChecker.isValidExpiration(1,0,CLOCK));assertTrue(DateChecker.isValidExpiration(1,99,CLOCK));
        for(int y:new int[]{-1,100})error("year must contain two digits",()->DateChecker.isValidExpiration(1,y,CLOCK));
        assertThrows(NullPointerException.class,()->DateChecker.isValidExpiration((YearMonth)null,CLOCK));assertThrows(NullPointerException.class,()->DateChecker.isValidExpiration(YearMonth.of(2026,6),null));
    }
    @Test void validationFacadeAndEvidence() throws Exception {
        for(String expiry:List.of("0626","06/26","0726")) {
            Validator v=new Validator("4111111111111111",expiry,"123",CLOCK);assertTrue(v.validate());assertTrue(v.checkCVV());assertTrue(v.checkExpirationDate());v.setExpirationMonth();v.setExpirationYear();assertTrue(v.checkExpirationDate());
        }
        assertFalse(new Validator("4111111111111111","0526","123",CLOCK).checkExpirationDate());
        for(String code:new String[]{null,"","12","12345","12A","１２３"})assertFalse(new Validator("4111111111111111","0626",code,CLOCK).checkCVV());
        assertTrue(new Validator("4111111111111111","0626","0123",CLOCK).checkCVV());
        Validator alone=new Validator("4111111111111111");assertTrue(alone.validate());assertFalse(alone.checkCVV());assertFalse(alone.checkExpirationDate());error("expirationDate was not supplied",alone::setExpirationMonth);error("expirationDate was not supplied",alone::setExpirationYear);
        new Validator("4111111111111111","1299","000");
        for(String expiry:List.of("","0026","1326","6/26","06/2026","06-26"))error("expirationDate must use MMyy or MM/yy",()->new Validator("4",expiry,"123",CLOCK));
        for(String n:List.of("","12345678901","12345678901234567890","411111111111111X"))error("Card number must contain 12 to 19 digits",()->new Validator(n).validate());
        error("Card number checksum is invalid",()->new Validator("4111111111111112").validate());
        CardEvidenceValidator validator=new CardEvidenceValidator(CLOCK);
        CardEvidence good=new CardEvidence("  i  ","4111111111111111",YearMonth.of(2026,6));assertEquals("i",good.itemId());
        assertEquals(new CardEvidenceCheck("i",CreditCardType.VISA,true,List.of()),validator.check(good));
        assertEquals(List.of("card number checksum is invalid","card has expired"),validator.check(changed(good,"number","4111111111111112","expires",YearMonth.of(2026,5))).issues());
        assertEquals(List.of("card network is not supported"),validator.check(changed(good,"number","000000000000")).issues());
        assertEquals(List.of("card number must contain 12 to 19 digits"),validator.check(changed(good,"number","1")).issues());
        textFields(good,"itemId");required(good,"number","expires");
        CardEvidenceCheck check=new CardEvidenceCheck("i",CreditCardType.VISA,true,List.of());bad(check,"valid",false);bad(check,"issues",List.of("x"));required(check,"itemId","type","issues");
        assertThrows(NullPointerException.class,()->validator.check(null));assertThrows(NullPointerException.class,()->new CardEvidenceValidator(null));assertThrows(NullPointerException.class,()->new Validator(null));
        assertThrows(NullPointerException.class,()->new Validator("4",null,"123",CLOCK));assertThrows(NullPointerException.class,()->new Validator("4","0626","123",null));
    }

    static Task work() {return new Task("t","Plan","spec","creator","staff",TaskStatus.OPEN,TaskPriority.NORMAL,RecurrenceRule.WEEKLY,MOMENT.minusSeconds(900000),null,null,null,100,50,List.of("dep"),Set.of("blue"),false,false);}
    static TaskContext actor() {return new TaskContext("staff",MOMENT,false,false,true,true,true,true,true,0,0,0);}
    static Task state(TaskStatus status) {
        return changed(work(),"status",status,"startedAt",EnumSet.of(TaskStatus.IN_PROGRESS,TaskStatus.BLOCKED,TaskStatus.IN_REVIEW,TaskStatus.COMPLETED).contains(status)?MOMENT.minusSeconds(100):null,"completedAt",status==TaskStatus.COMPLETED?MOMENT:null);
    }
    @Test void taskRecordValidationAndCollections() {
        Task t=work();textFields(t,"id","title","creatorId");required(t,"status","priority","recurrence","createdAt","dependencyIds","tags");
        assertEquals("t",changed(t,"id"," t ").id());assertEquals("Plan",changed(t,"title"," Plan ").title());assertEquals("creator",changed(t,"creatorId"," creator ").creatorId());assertEquals("staff",changed(t,"assigneeId"," staff ").assigneeId());
        assertNull(changed(t,"assigneeId",null).assigneeId());
        for(String name:List.of("estimatedMinutes","actualMinutes")) {
            int max=name.equals("estimatedMinutes")?100000:200000;bad(t,name,-1);bad(t,name,max+1);assertNotNull(changed(t,name,0));assertNotNull(changed(t,name,max));
        }
        bad(t,"title","x".repeat(121));assertEquals(120,changed(t,"title","x".repeat(120)).title().length());
        bad(t,"description","x".repeat(5001));assertEquals(5000,changed(t,"description","x".repeat(5000)).description().length());assertNull(changed(t,"description",null).description());
        bad(t,"dueAt",t.createdAt().minusSeconds(1));assertEquals(t.createdAt(),changed(t,"dueAt",t.createdAt()).dueAt());
        bad(t,"dependencyIds",List.of("t"));bad(t,"dependencyIds",List.of("a","a"));
        for(TaskStatus st:TaskStatus.values()) {
            Task v=state(st);assertEquals(Set.of(TaskStatus.COMPLETED,TaskStatus.CANCELLED,TaskStatus.ARCHIVED).contains(st),v.isTerminal());
            if(Set.of(TaskStatus.IN_PROGRESS,TaskStatus.BLOCKED,TaskStatus.IN_REVIEW,TaskStatus.COMPLETED).contains(st))bad(v,"startedAt",null);
            if(st==TaskStatus.COMPLETED)bad(v,"completedAt",null);else bad(v,"completedAt",MOMENT);
        }
        TaskContext c=actor();textFields(c,"actorId");required(c,"now");assertEquals("staff",changed(c,"actorId"," staff ").actorId());
        for(String count:List.of("previousReopens","assigneeOpenTaskCount","overdueTaskCount"))bad(c,count,-1);
        TaskEvaluation ev=new TaskEvaluation(TaskPriority.NORMAL,false,false,false,Set.of(TaskAction.START),List.of("note"));required(ev,"effectivePriority","allowedActions","warnings");
        assertThrows(UnsupportedOperationException.class,()->ev.allowedActions().clear());assertThrows(UnsupportedOperationException.class,()->ev.warnings().clear());
    }
    @Test void taskPriorityThresholdTable() {
        TaskPolicy p=new TaskPolicy();
        String[][] table={{"LOW","-168","URGENT"},{"LOW","-72","URGENT"},{"LOW","-71","HIGH"},{"LOW","-1","HIGH"},{"LOW","0","HIGH"},{"LOW","1","NORMAL"},{"LOW","4","NORMAL"},{"LOW","5","NORMAL"},{"LOW","24","NORMAL"},{"LOW","25","LOW"},{"LOW","72","LOW"},{"LOW","73","LOW"},{"NORMAL","4","HIGH"},{"HIGH","0","URGENT"},{"URGENT","0","CRITICAL"},{"CRITICAL","4","CRITICAL"}};
        for(String[] row:table) {
            int hours=Integer.parseInt(row[1]);Task t=changed(work(),"priority",TaskPriority.valueOf(row[0]),"dueAt",MOMENT.plusSeconds(hours*3600L));TaskEvaluation e=p.evaluate(t,actor());
            assertEquals(TaskPriority.valueOf(row[2]),e.effectivePriority(),Arrays.toString(row));assertEquals(hours<0,e.overdue());assertEquals(hours<0,e.warnings().contains("task is overdue"));
        }
        for(TaskPriority priority:TaskPriority.values()) {
            TaskPriority[] expected={TaskPriority.LOW,TaskPriority.NORMAL,TaskPriority.NORMAL,TaskPriority.HIGH,TaskPriority.URGENT};
            assertEquals(expected[priority.ordinal()],p.evaluate(changed(work(),"priority",priority),actor()).effectivePriority());
        }
        Task high=changed(work(),"priority",TaskPriority.HIGH);
        assertEquals(TaskPriority.HIGH,p.evaluate(changed(high,"dependencyIds",List.of("1","2","3","4","5")),actor()).effectivePriority());
        assertEquals(TaskPriority.NORMAL,p.evaluate(changed(high,"dependencyIds",List.of("1","2","3","4")),actor()).effectivePriority());
        for(int n:new int[]{2,3,9,10})assertEquals(n>=3?TaskPriority.HIGH:TaskPriority.NORMAL,p.evaluate(high,changed(actor(),"overdueTaskCount",n)).effectivePriority());
        Task low=changed(work(),"priority",TaskPriority.LOW,"dueAt",MOMENT.plusSeconds(26*3600));
        assertEquals(TaskPriority.NORMAL,p.evaluate(changed(low,"confidential",true),changed(actor(),"overdueTaskCount",3)).effectivePriority());
        assertEquals(TaskPriority.LOW,p.evaluate(changed(low,"confidential",true),actor()).effectivePriority());
        Task blocked=changed(state(TaskStatus.BLOCKED),"priority",TaskPriority.LOW);assertEquals(TaskPriority.NORMAL,p.evaluate(blocked,actor()).effectivePriority());
        Task critical=changed(work(),"priority",TaskPriority.CRITICAL,"dueAt",MOMENT);TaskEvaluation ce=p.evaluate(critical,actor());assertTrue(ce.escalationRequired());assertTrue(ce.warnings().contains("critical task requires escalation"));
        TaskEvaluation suppressed=p.evaluate(critical,changed(actor(),"projectActive",false));assertEquals(TaskPriority.NORMAL,suppressed.effectivePriority());assertTrue(suppressed.warnings().contains("inactive project suppresses task escalation"));
        for(TaskStatus st:List.of(TaskStatus.COMPLETED,TaskStatus.CANCELLED,TaskStatus.ARCHIVED)) {
            TaskEvaluation e=p.evaluate(changed(state(st),"dueAt",MOMENT.minusSeconds(1),"priority",TaskPriority.CRITICAL),changed(actor(),"assigneeActive",false));assertFalse(e.overdue());assertFalse(e.escalationRequired());assertFalse(e.reassignmentRecommended());assertEquals(TaskPriority.LOW,e.effectivePriority());
        }
        assertThrows(NullPointerException.class,()->p.evaluate(null,actor()));assertThrows(NullPointerException.class,()->p.evaluate(work(),null));
    }
    @Test void taskPermissionsAndTransitions() {
        TaskPolicy p=new TaskPolicy();TaskStateMachine machine=new TaskStateMachine();
        TaskContext admin=changed(actor(),"actorId","root","actorIsAdministrator",true);
        String[] permitted={"PUBLISH CANCEL","START CANCEL","BLOCK COMPLETE CANCEL","UNBLOCK CANCEL","APPROVE REJECT_REVIEW CANCEL","REOPEN ARCHIVE","REOPEN ARCHIVE","RESTORE"};
        Map<TaskAction,TaskStatus> destinations=Map.ofEntries(Map.entry(TaskAction.PUBLISH,TaskStatus.OPEN),Map.entry(TaskAction.START,TaskStatus.IN_PROGRESS),Map.entry(TaskAction.BLOCK,TaskStatus.BLOCKED),Map.entry(TaskAction.UNBLOCK,TaskStatus.IN_PROGRESS),Map.entry(TaskAction.SUBMIT_FOR_REVIEW,TaskStatus.IN_REVIEW),Map.entry(TaskAction.APPROVE,TaskStatus.COMPLETED),Map.entry(TaskAction.REJECT_REVIEW,TaskStatus.IN_PROGRESS),Map.entry(TaskAction.COMPLETE,TaskStatus.COMPLETED),Map.entry(TaskAction.REOPEN,TaskStatus.OPEN),Map.entry(TaskAction.CANCEL,TaskStatus.CANCELLED),Map.entry(TaskAction.ARCHIVE,TaskStatus.ARCHIVED));
        for(TaskStatus st:TaskStatus.values()) {
            Task t=state(st);Set<TaskAction> allowed=EnumSet.noneOf(TaskAction.class);for(String n:permitted[st.ordinal()].split(" "))allowed.add(TaskAction.valueOf(n));assertEquals(allowed,p.evaluate(t,admin).allowedActions());
            for(TaskAction action:TaskAction.values()) {
                if(!allowed.contains(action)) {error("action "+action+" is not allowed from "+st,()->machine.apply(t,action,admin));continue;}
                if(action==TaskAction.RESTORE){error("startedAt is required after work begins",()->machine.apply(t,action,admin));continue;}
                Task out=machine.apply(t,action,admin);assertEquals(destinations.get(action),out.status());assertEquals(t.id(),out.id());assertEquals(t.title(),out.title());assertEquals(t.description(),out.description());assertEquals(t.creatorId(),out.creatorId());assertEquals(t.assigneeId(),out.assigneeId());assertEquals(t.priority(),out.priority());assertEquals(t.recurrence(),out.recurrence());assertEquals(t.createdAt(),out.createdAt());assertEquals(t.dueAt(),out.dueAt());assertEquals(t.estimatedMinutes(),out.estimatedMinutes());assertEquals(t.actualMinutes(),out.actualMinutes());assertEquals(t.dependencyIds(),out.dependencyIds());assertEquals(t.tags(),out.tags());assertEquals(t.confidential(),out.confidential());assertEquals(t.reviewRequired(),out.reviewRequired());
                assertEquals(action==TaskAction.START?MOMENT:action==TaskAction.REOPEN?null:t.startedAt(),out.startedAt());assertEquals(out.status()==TaskStatus.COMPLETED?MOMENT:null,out.completedAt());
            }
        }
        Task review=changed(state(TaskStatus.IN_PROGRESS),"reviewRequired",true);assertEquals(TaskStatus.IN_REVIEW,machine.apply(review,TaskAction.SUBMIT_FOR_REVIEW,actor()).status());
        assertEquals(Set.of(TaskAction.START),p.evaluate(work(),actor()).allowedActions());
        Task retained=changed(work(),"startedAt",MOMENT.minusSeconds(10));assertEquals(retained.startedAt(),machine.apply(retained,TaskAction.START,actor()).startedAt());
        String[] staffActions={"","START","BLOCK COMPLETE","UNBLOCK","","","",""};
        for(TaskStatus st:TaskStatus.values()) {
            Set<TaskAction> expected=EnumSet.noneOf(TaskAction.class);if(!staffActions[st.ordinal()].isEmpty())for(String x:staffActions[st.ordinal()].split(" "))expected.add(TaskAction.valueOf(x));
            assertEquals(expected,p.evaluate(state(st),actor()).allowedActions());assertEquals(Set.of(),p.evaluate(state(st),changed(actor(),"actorId","stranger")).allowedActions());
        }
        for(String flag:List.of("projectActive","dependenciesComplete","assigneeActive"))assertTrue(p.evaluate(work(),changed(actor(),flag,false)).allowedActions().isEmpty());
        assertTrue(p.evaluate(work(),changed(actor(),"dependenciesComplete",false)).warnings().contains("dependencies prevent task start"));
        assertEquals(Set.of(TaskAction.CANCEL),p.evaluate(state(TaskStatus.DRAFT),changed(actor(),"actorId","creator","withinWorkingHours",false)).allowedActions());
        assertEquals(Set.of(TaskAction.PUBLISH,TaskAction.CANCEL),p.evaluate(state(TaskStatus.DRAFT),changed(admin,"withinWorkingHours",false)).allowedActions());
        assertEquals(Set.of(TaskAction.ARCHIVE),p.evaluate(state(TaskStatus.COMPLETED),changed(admin,"projectActive",false)).allowedActions());
        assertEquals(Set.of(TaskAction.REJECT_REVIEW,TaskAction.CANCEL),p.evaluate(state(TaskStatus.IN_REVIEW),changed(actor(),"actorIsManager",true,"withinWorkingHours",false)).allowedActions());
        for(TaskStatus st:TaskStatus.values())assertEquals(st==TaskStatus.DRAFT?Set.of():p.evaluate(state(st),admin).allowedActions(),p.evaluate(state(st),changed(actor(),"actorId","boss","actorIsManager",true)).allowedActions());
        assertThrows(NullPointerException.class,()->machine.apply(null,TaskAction.START,actor()));assertThrows(NullPointerException.class,()->machine.apply(work(),null,actor()));assertThrows(NullPointerException.class,()->machine.apply(work(),TaskAction.START,null));
    }
    @Test void taskEscalationAndReassignmentCutoffs() {
        TaskPolicy p=new TaskPolicy();
        for(int load:new int[]{10,11,12,13,20,21}) {
            TaskContext c=changed(actor(),"assigneeOpenTaskCount",load);
            assertEquals(load>20,p.evaluate(work(),c).reassignmentRecommended());assertEquals(load>12,p.evaluate(changed(work(),"priority",TaskPriority.LOW),c).reassignmentRecommended());assertEquals(load>10,p.evaluate(changed(work(),"dueAt",MOMENT.minusSeconds(1)),c).reassignmentRecommended());
        }
        for(String id:new String[]{null,""}){TaskEvaluation e=p.evaluate(changed(work(),"assigneeId",id),actor());assertTrue(e.reassignmentRecommended());assertTrue(e.warnings().contains("task has no assignee"));}
        TaskEvaluation inactive=p.evaluate(work(),changed(actor(),"assigneeActive",false));assertTrue(inactive.escalationRequired());assertTrue(inactive.reassignmentRecommended());assertTrue(inactive.warnings().contains("assignee is inactive"));
        assertFalse(p.evaluate(changed(work(),"assigneeId",null),changed(actor(),"assigneeActive",false)).escalationRequired());
        for(int seconds:new int[]{259200,259201})assertEquals(seconds>259200,p.evaluate(changed(state(TaskStatus.BLOCKED),"startedAt",MOMENT.minusSeconds(seconds)),actor()).escalationRequired());
        for(int reopen:new int[]{1,2})assertEquals(reopen==2,p.evaluate(state(TaskStatus.BLOCKED),changed(actor(),"previousReopens",reopen)).reassignmentRecommended());
        for(int seconds:new int[]{14399,14400})assertEquals(seconds<14400,p.evaluate(changed(state(TaskStatus.IN_REVIEW),"reviewRequired",true,"dueAt",MOMENT.plusSeconds(seconds)),actor()).escalationRequired());
        for(int overdue:new int[]{4,5})assertEquals(overdue==5,p.evaluate(changed(work(),"dueAt",MOMENT.minusSeconds(1)),changed(actor(),"overdueTaskCount",overdue)).escalationRequired());
        Task urgent=changed(work(),"priority",TaskPriority.URGENT,"dueAt",MOMENT.minusSeconds(1));assertTrue(p.evaluate(urgent,changed(actor(),"projectActive",false)).escalationRequired());
    }


    static ExpenseItem purchase() {return new ExpenseItem("item",ExpenseCategory.HOTEL,cash("100.00"),Currency.getInstance("CNY"),DATE,"Hotel","Conference lodging",ReceiptType.TAX_INVOICE,PaymentMethod.PERSONAL_CARD,0,false,false,false);}
    static ExpenseClaim claim() {return new ExpenseClaim("claim","person",EmployeeGrade.STAFF,DATE,"cost",List.of(purchase()),true,false,false,true,0);}
    static ExpenseAssessment assess(ExpenseClaim c) {return new ExpensePolicy().assess(c,(currency,date)->BigDecimal.ONE,DATE);}
    static ExpenseAssessment assessItem(ExpenseItem item) {return assess(changed(claim(),"items",List.of(item)));}
    @Test void expenseValueObjectsEnforceInvariants() {
        ExpenseItem item=purchase();textFields(item,"itemId");required(item,"category","amount","currency","incurredDate","receiptType","paymentMethod");
        for(String zero:List.of("0","-1"))bad(item,"amount",cash(zero));
        assertEquals(cash("1.24"),changed(item,"amount",cash("1.235")).amount());assertEquals(cash("1.23"),changed(item,"amount",cash("1.234")).amount());assertEquals("item",changed(item,"itemId"," item ").itemId());
        bad(item,"attendeeCount",1);bad(item,"attendeeCount",-1);ExpenseItem event=changed(item,"category",ExpenseCategory.CLIENT_ENTERTAINMENT);bad(event,"attendeeCount",1001);assertEquals(1000,changed(event,"attendeeCount",1000).attendeeCount());
        ExpenseClaim c=claim();textFields(c,"claimId","employeeId");required(c,"employeeGrade","submittedDate","items");bad(c,"items",List.of());bad(c,"items",Collections.nCopies(101,item));assertEquals(100,changed(c,"items",Collections.nCopies(100,item)).items().size());bad(c,"previousClaimsThisMonth",-1);
        for(String center:new String[]{null,""})assertThrows(IllegalArgumentException.class,()->changed(c,"clientBillable",true,"costCenter",center));
        assertEquals("claim",changed(c,"claimId"," claim ").claimId());assertEquals("person",changed(c,"employeeId"," person ").employeeId());
        ExpenseAssessment assessment=assess(c);required(assessment,"claimedAmount","reimbursableAmount","approvalTier","violations","warnings");bad(assessment,"claimedAmount",cash("-1"));bad(assessment,"reimbursableAmount",cash("-1"));bad(assessment,"reimbursableAmount",cash("101"));bad(assessment,"rejected",true);
        assertThrows(UnsupportedOperationException.class,()->assessment.warnings().add("x"));assertThrows(UnsupportedOperationException.class,()->c.items().clear());
    }
    @Test void expenseCategoryCapsAndApprovalTotals() {
        // Fixed cap values for STAFF, then separate grade examples; no production calculation in expected values.
        String[] caps={"3000.00","1000.00","160.00","400.00","600.00","1000.00","2000.00","1600.00","300.00","600.00","200.00"};
        for(ExpenseCategory category:ExpenseCategory.values()) {
            ExpenseItem item=changed(purchase(),"category",category,"amount",cash("4000.00"),"attendeeCount",category==ExpenseCategory.CLIENT_ENTERTAINMENT?2:0);
            ExpenseAssessment r=assessItem(item);assertEquals(cash("4000.00"),r.claimedAmount());assertEquals(cash(caps[category.ordinal()]),r.reimbursableAmount());assertFalse(r.rejected());assertEquals(ExpenseApprovalTier.DEPARTMENT_HEAD,r.approvalTier());assertTrue(r.warnings().contains("category cap applied to item item"));assertTrue(r.warnings().contains("one or more items were capped or excluded"));
            ExpenseItem small=changed(item,"amount",cash("1.00"));assertEquals(cash("1.00"),assessItem(small).reimbursableAmount());
        }
        String[] gradeCaps={"500.00","1000.00","1500.00","2000.00","3000.00","5000.00"};
        for(EmployeeGrade grade:EmployeeGrade.values())assertEquals(cash(gradeCaps[grade.ordinal()]),assess(changed(claim(),"employeeGrade",grade,"items",List.of(changed(purchase(),"amount",cash("6000"))))).reimbursableAmount());
        for(ExpenseCategory category:List.of(ExpenseCategory.HOTEL,ExpenseCategory.MEAL,ExpenseCategory.LOCAL_TRANSPORT)) {
            String cap=switch(category){case HOTEL->"1500.00";case MEAL->"240.00";default->"600.00";};
            assertEquals(cash(cap),assess(changed(claim(),"internationalTrip",true,"items",List.of(changed(purchase(),"category",category,"amount",cash("9000"))))).reimbursableAmount());
        }
        ExpenseItem entertainment=changed(purchase(),"category",ExpenseCategory.CLIENT_ENTERTAINMENT,"attendeeCount",2,"amount",cash("9000"));assertEquals(cash("1250.00"),assess(changed(claim(),"clientBillable",true,"items",List.of(entertainment))).reimbursableAmount());
        String[] totals={"200","200.01","2000","2000.01","5000","5000.01","20000","20000.01","50000","50000.01"};
        ExpenseApprovalTier[] tiers={ExpenseApprovalTier.AUTO_APPROVED,ExpenseApprovalTier.LINE_MANAGER,ExpenseApprovalTier.LINE_MANAGER,ExpenseApprovalTier.DEPARTMENT_HEAD,ExpenseApprovalTier.DEPARTMENT_HEAD,ExpenseApprovalTier.FINANCE,ExpenseApprovalTier.FINANCE,ExpenseApprovalTier.FINANCE_DIRECTOR,ExpenseApprovalTier.FINANCE_DIRECTOR,ExpenseApprovalTier.CHIEF_FINANCIAL_OFFICER};
        for(int i=0;i<totals.length;i++) {
            BigDecimal amount=cash(totals[i]);List<ExpenseItem> items=new ArrayList<>();
            // Separate merchants avoid duplicate fingerprints; each item remains below the item-audit threshold.
            while(amount.signum()>0){BigDecimal part=amount.min(cash("5000"));items.add(changed(purchase(),"itemId","i"+items.size(),"merchant","Hotel"+items.size(),"amount",part));amount=amount.subtract(part);}
            assertEquals(tiers[i],assess(changed(claim(),"items",items)).approvalTier(),totals[i]);
        }
        assertEquals(ExpenseApprovalTier.LINE_MANAGER,assess(changed(claim(),"managerPreApproved",false)).approvalTier());assertEquals(ExpenseApprovalTier.DEPARTMENT_HEAD,assess(changed(claim(),"clientBillable",true)).approvalTier());assertEquals(ExpenseApprovalTier.FINANCE_DIRECTOR,assess(changed(claim(),"employeeGrade",EmployeeGrade.EXECUTIVE)).approvalTier());
    }
    @Test void expenseRatesDatesAndAuditBoundaries() {
        ExpensePolicy policy=new ExpensePolicy();
        for(BigDecimal rate:new BigDecimal[]{null,cash("0"),cash("-1"),cash("1000.01")}) {
            ExpenseAssessment a=policy.assess(claim(),(c,d)->rate,DATE);assertTrue(a.rejected());assertEquals(cash("0.00"),a.claimedAmount());assertEquals(cash("0.00"),a.reimbursableAmount());assertEquals(ExpenseApprovalTier.FINANCE,a.approvalTier());assertTrue(a.violations().get(0).startsWith("FATAL:"));
        }
        ExpenseClaim tiny=changed(claim(),"items",List.of(changed(purchase(),"amount",cash("0.01"))));assertEquals(cash("10.00"),policy.assess(tiny,(c,d)->cash("1000"),DATE).claimedAmount());
        List<Object> captured=new ArrayList<>();assertEquals(cash("123.46"),policy.assess(claim(),(c,d)->{captured.add(c);captured.add(d);return cash("1.23456");},DATE).claimedAmount());assertEquals(List.of(Currency.getInstance("CNY"),DATE),captured);
        for(int age:new int[]{30,31,90,91}) {
            ExpenseAssessment a=assessItem(changed(purchase(),"incurredDate",DATE.minusDays(age)));assertEquals(age>90,a.rejected());assertEquals(age>30&&age<=90,a.warnings().contains("late submission for item item"));
        }
        assertTrue(assessItem(changed(purchase(),"incurredDate",DATE.plusDays(1))).violations().contains("FATAL: future expense date for item item"));
        assertTrue(assess(changed(claim(),"submittedDate",DATE.plusDays(1))).rejected());
        for(int age:new int[]{30,31})assertEquals(age>30,assess(changed(claim(),"submittedDate",DATE.minusDays(age))).warnings().contains("claim review is occurring more than thirty days after submission"));
        assertTrue(assess(changed(claim(),"clientBillable",true,"projectActive",false)).rejected());assertTrue(assess(changed(claim(),"employeeGrade",EmployeeGrade.INTERN,"internationalTrip",true)).rejected());
        for(String missing:new String[]{null," "})assertTrue(assess(changed(claim(),"costCenter",missing)).warnings().contains("cost center is missing"));
        for(int n:new int[]{9,10,20,21}) {
            ExpenseAssessment a=assess(changed(claim(),"previousClaimsThisMonth",n));assertEquals(n>=10,a.auditRequired());assertEquals(n>=10,a.warnings().contains("employee has a high monthly claim frequency"));assertEquals(n>20,a.warnings().contains("unusually high claim frequency"));
        }
        for(int n:new int[]{19,20,50,51}) {List<ExpenseItem> list=new ArrayList<>();for(int i=0;i<n;i++)list.add(changed(purchase(),"merchant","M"+i,"amount",cash("1")));ExpenseAssessment a=assess(changed(claim(),"items",list));assertEquals(n>=20,a.auditRequired());assertEquals(n>50,a.warnings().contains("large claim should be split by trip or purpose"));}
        for(String amount:List.of("5000","5000.01"))assertEquals(amount.contains("."),assessItem(changed(purchase(),"amount",cash(amount))).auditRequired());
        for(String amount:List.of("1000","1000.01"))assertEquals(amount.contains("."),assessItem(changed(purchase(),"amount",cash(amount),"paymentMethod",PaymentMethod.CASH)).auditRequired());
        assertTrue(assessItem(changed(purchase(),"receiptType",ReceiptType.BANK_STATEMENT)).auditRequired());
        for(String flag:List.of("personalComponent","suspectedDuplicate")){ExpenseAssessment a=assessItem(changed(purchase(),flag,true));assertTrue(a.auditRequired());assertTrue(a.rejected());assertEquals(cash("0.00"),a.reimbursableAmount());}
        assertFalse(assessItem(changed(purchase(),"amount",cash("50000"))).rejected());assertTrue(assessItem(changed(purchase(),"amount",cash("50000.01"))).rejected());
        assertThrows(NullPointerException.class,()->policy.assess(null,(c,d)->BigDecimal.ONE,DATE));assertThrows(NullPointerException.class,()->policy.assess(claim(),null,DATE));assertThrows(NullPointerException.class,()->policy.assess(claim(),(c,d)->BigDecimal.ONE,null));
    }
    @Test void expenseReceiptsAndCategoryConditions() {
        for(ExpenseCategory category:ExpenseCategory.values())for(int amount:new int[]{10,11,20,21,50,51}) {
            ExpenseItem item=changed(purchase(),"category",category,"receiptType",ReceiptType.NONE,"amount",BigDecimal.valueOf(amount));ExpenseAssessment a=assessItem(item);
            boolean required=switch(category){case MEAL,LOCAL_TRANSPORT->amount>50;case COMMUNICATION->amount>20;case OTHER->amount>10;default->true;};
            assertEquals(required,a.violations().contains("receipt is required for item item"),category+"/"+amount);
        }
        for(String field:List.of("merchant","businessPurpose"))for(String value:new String[]{null,""," "})assertTrue(assessItem(changed(purchase(),field,value)).violations().contains((field.equals("merchant")?"merchant":"business purpose")+" is required for item item"));
        for(String value:List.of("x","xx"))assertEquals(value.length()<2,assessItem(changed(purchase(),"merchant",value)).warnings().contains("merchant name is unusually short for item item"));
        for(String value:List.of("1234567","12345678"))assertEquals(value.length()<8,assessItem(changed(purchase(),"businessPurpose",value)).warnings().contains("business purpose is too brief for item item"));
        ExpenseItem duplicate=changed(purchase(),"itemId","other","merchant"," HOTEL ");assertTrue(assess(changed(claim(),"items",List.of(purchase(),duplicate))).violations().contains("possible duplicate item: other"));
        String[][] checks={{"AIRFARE","1000","airfare over 1000 requires pre-approval"},{"OFFICE_SUPPLIES","100","intern office-supply purchase exceeds delegated limit"},{"OTHER","500","other-category expense exceeds allowed limit"}};
        for(String[] row:checks)for(int delta:new int[]{0,1}) {
            ExpenseItem item=changed(purchase(),"category",ExpenseCategory.valueOf(row[0]),"amount",cash(row[1]).add(BigDecimal.valueOf(delta)));
            ExpenseClaim c=changed(claim(),"managerPreApproved",false,"employeeGrade",EmployeeGrade.INTERN,"items",List.of(item));assertEquals(delta==1,assess(c).violations().contains(row[2]));
        }
        String[][] warningCases={{"HOTEL","2000","domestic hotel amount is unusually high"},{"LOCAL_TRANSPORT","1000","local transport amount needs route details"},{"OFFICE_SUPPLIES","500","large office purchase should use corporate procurement"},{"SOFTWARE","3000","software purchase should use procurement review"},{"COMMUNICATION","500","communication expense exceeds ordinary monthly level"}};
        for(String[] row:warningCases)for(int delta:new int[]{0,1})assertEquals(delta==1,assessItem(changed(purchase(),"category",ExpenseCategory.valueOf(row[0]),"amount",cash(row[1]).add(BigDecimal.valueOf(delta)))).warnings().contains(row[2]));
        assertTrue(assessItem(changed(purchase(),"category",ExpenseCategory.AIRFARE,"paymentMethod",PaymentMethod.CASH)).warnings().contains("cash airfare purchase requires verification"));
        assertTrue(assessItem(changed(purchase(),"category",ExpenseCategory.LOCAL_TRANSPORT,"paymentMethod",PaymentMethod.CASH,"receiptType",ReceiptType.NONE)).warnings().contains("cash transport without receipt will be capped"));
        assertTrue(assessItem(changed(purchase(),"receiptType",ReceiptType.BANK_STATEMENT)).violations().contains("hotel requires an invoice or receipt"));
        assertTrue(assessItem(changed(purchase(),"category",ExpenseCategory.MEAL,"alcoholIncluded",true)).violations().containsAll(List.of("ordinary meal cannot include alcohol","alcohol is only allowed for client entertainment")));
        for(int people:new int[]{0,1,2}) {ExpenseAssessment a=assessItem(changed(purchase(),"category",ExpenseCategory.CLIENT_ENTERTAINMENT,"attendeeCount",people,"alcoholIncluded",true));assertEquals(people==0,a.violations().contains("client entertainment requires attendee count"));assertEquals(people<2,a.violations().contains("alcohol expense requires at least two attendees"));}
        for(ExpenseCategory category:List.of(ExpenseCategory.TRAINING,ExpenseCategory.CLIENT_ENTERTAINMENT,ExpenseCategory.OTHER)) {
            ExpenseAssessment a=assess(changed(claim(),"managerPreApproved",false,"employeeGrade",EmployeeGrade.INTERN,"items",List.of(changed(purchase(),"category",category))));
            if(category==ExpenseCategory.TRAINING){assertTrue(a.violations().contains("training requires pre-approval"));assertTrue(a.warnings().contains("intern training requires department confirmation"));}else assertTrue(a.warnings().contains(category==ExpenseCategory.OTHER?"other-category expense needs manager explanation":"client entertainment lacks pre-approval"));
        }
        assertTrue(assessItem(changed(purchase(),"category",ExpenseCategory.SOFTWARE,"paymentMethod",PaymentMethod.CASH)).violations().contains("software cannot be purchased with cash"));
        for(ReceiptType receipt:ReceiptType.values())assertEquals(receipt!=ReceiptType.TAX_INVOICE&&receipt!=ReceiptType.ELECTRONIC_RECEIPT,assessItem(changed(purchase(),"category",ExpenseCategory.MEDICAL,"receiptType",receipt)).violations().contains("medical expense requires a recognized receipt"));
        assertTrue(assessItem(changed(purchase(),"category",ExpenseCategory.MEDICAL,"paymentMethod",PaymentMethod.CORPORATE_CARD)).violations().contains("corporate card cannot be used for medical expenses"));
        for(int day:new int[]{0,1,2})assertEquals(day>0,assessItem(changed(purchase(),"category",ExpenseCategory.OFFICE_SUPPLIES,"incurredDate",DATE.minusDays(day))).warnings().contains("weekend office-supply purchase needs explanation"));
    }


    private static final LocalDate OA_MONDAY = LocalDate.of(2026, 1, 5);
    private static final Instant OA_INSTANT = Instant.parse("2026-01-05T12:00:00Z");
    private static class OaWorkday {
        LocalDate date=OA_MONDAY;
        Shift shift=new Shift(ShiftType.STANDARD,LocalTime.of(8,0),LocalTime.of(17,0),10,60,480);
        PunchRecord punch=new PunchRecord(date.atTime(8,0),date.atTime(17,0),60,false,true,false,false);
        EmploymentMode mode=EmploymentMode.OFFICE;
        boolean scheduled=true,holiday=false,leave=false,trip=false,weather=false,request=true,approved=true;
        int lateCount=0,consecutive=1;
        AttendanceInput input(){return new AttendanceInput(" worker ",date,shift,punch,mode,scheduled,holiday,leave,trip,weather,request,approved,lateCount,consecutive);}
        AttendanceResult result(){return new AttendancePolicy().evaluate(input());}
        void times(int startDelta,int endDelta,int rest){punch=new PunchRecord(date.atTime(8,0).plusMinutes(startDelta),date.atTime(17,0).plusMinutes(endDelta),rest,false,true,false,false);}
    }
    private static class OaLeaveForm {
        String id=" worker ",reason="family travel";
        EmployeeCategory category=EmployeeCategory.REGULAR;
        LeaveType type=LeaveType.ANNUAL;
        LocalDate first=OA_MONDAY,last=OA_MONDAY;
        LeaveUnit unit=LeaveUnit.FULL_DAY;
        double hours=0,balance=40,carry=0;
        boolean emergency=false,remote=false,handover=true;
        int months=24,notice=10;
        AdvancedLeaveRequest request(){return new AdvancedLeaveRequest(id,category,type,first,last,unit,hours,reason,emergency,remote,handover,months,notice,balance,carry);}
        LeaveAssessment assess(){return new AdvancedLeavePolicy().assess(request(),d->true,OA_MONDAY);}
        void span(int days){last=first.plusDays(days-1);}
    }
    private static void oaHasIssue(OaLeaveForm form,String expected){assertTrue(form.assess().violations().contains(expected),form.assess().violations().toString());}
    private static void oaNoIssue(OaLeaveForm form,String forbidden){assertFalse(form.assess().violations().contains(forbidden),form.assess().violations().toString());}
    @Test void oaDayClassificationPrecedesPunchArithmetic(){
        OaWorkday w=new OaWorkday();AttendanceResult normal=w.result();assertEquals(List.of(AttendanceCode.NORMAL),normal.codes());assertEquals(480,normal.workedMinutes());assertEquals(0,normal.penaltyPoints());assertFalse(normal.requiresManagerReview());assertTrue(normal.warnings().isEmpty());
        w.leave=true;assertEquals(List.of(AttendanceCode.APPROVED_LEAVE),w.result().codes());assertEquals(List.of("punch exists on an approved leave day"),w.result().warnings());assertEquals(0,w.result().workedMinutes());
        w.punch=null;assertTrue(w.result().warnings().isEmpty());w.leave=false;w.trip=true;assertEquals(List.of(AttendanceCode.BUSINESS_TRIP),w.result().codes());assertEquals(480,w.result().workedMinutes());
        w.times(0,0,60);assertEquals(List.of(AttendanceCode.BUSINESS_TRIP),w.result().codes());w.trip=false;w.punch=null;w.scheduled=false;assertEquals(List.of(AttendanceCode.REST_DAY),w.result().codes());assertFalse(w.result().requiresManagerReview());
        w.holiday=true;assertEquals(List.of(AttendanceCode.ABSENT),w.result().codes());assertEquals(5,w.result().penaltyPoints());assertTrue(w.result().requiresManagerReview());w.weather=true;assertEquals(List.of(AttendanceCode.MANUAL_CORRECTION_REQUIRED),w.result().codes());assertEquals(0,w.result().penaltyPoints());
        assertThrows(NullPointerException.class,()->new AttendancePolicy().evaluate(null));assertThrows(NullPointerException.class,()->new AttendancePolicy(null));
    }
    @Test void oaMissingPunchesAndCorrectionAreDistinct(){
        for(int mask=1;mask<=3;mask++)for(int mode=0;mode<3;mode++){
            OaWorkday w=new OaWorkday();w.punch=new PunchRecord((mask&1)>0?null:w.date.atTime(8,0),(mask&2)>0?null:w.date.atTime(17,0),0,false,true,mode>0,mode==2);AttendanceResult r=w.result();
            assertEquals((mask&1)>0,r.codes().contains(AttendanceCode.MISSING_CLOCK_IN));assertEquals((mask&2)>0,r.codes().contains(AttendanceCode.MISSING_CLOCK_OUT));assertEquals(mode!=2,r.codes().contains(AttendanceCode.MANUAL_CORRECTION_REQUIRED));assertEquals(mode==2,r.warnings().contains("approved correction remains incomplete"));assertEquals(2,r.penaltyPoints());assertEquals(0,r.workedMinutes());assertTrue(r.requiresManagerReview());
        }
        for(boolean corrected:new boolean[]{false,true}){OaWorkday w=new OaWorkday();w.punch=new PunchRecord(w.date.atTime(8,0),w.date.atTime(17,0),60,false,true,true,corrected);assertEquals(!corrected,w.result().codes().contains(AttendanceCode.MANUAL_CORRECTION_REQUIRED));assertEquals(!corrected,w.result().requiresManagerReview());}
    }
    @Test void oaLatenessAndEarlyExitHaveIndependentThresholds(){
        int[][] lateRows={{10,0,0},{11,1,1},{25,15,1},{26,16,2},{70,60,2},{71,61,5}};
        for(int[] row:lateRows){OaWorkday w=new OaWorkday();w.times(row[0],row[0],60);AttendanceResult r=w.result();assertEquals(480,r.workedMinutes());assertEquals(row[1],r.lateMinutes());assertEquals(row[2],r.penaltyPoints());assertEquals(row[1]>0,r.codes().contains(AttendanceCode.LATE));assertEquals(row[1]>60,r.codes().contains(AttendanceCode.SERIOUSLY_LATE));}
        int[][] earlyRows={{10,0,2},{11,11,3},{60,60,3},{61,61,5},{240,240,5},{241,241,7}};
        for(int[] row:earlyRows){OaWorkday w=new OaWorkday();w.times(0,-row[0],60);AttendanceResult r=w.result();assertEquals(row[1],r.earlyLeaveMinutes());assertEquals(row[2],r.penaltyPoints());assertEquals(480-row[0],r.workedMinutes());}
        for(int[] row:new int[][]{{2,1},{3,3},{5,3},{6,6}}){OaWorkday w=new OaWorkday();w.times(11,11,60);w.lateCount=row[0];assertEquals(row[1],w.result().penaltyPoints());assertEquals(row[0]>=3,w.result().warnings().contains("repeated lateness requires manager review"));assertEquals(row[0]>=6,w.result().warnings().contains("monthly lateness threshold exceeded"));}
        OaWorkday w=new OaWorkday();w.lateCount=6;assertEquals(3,w.result().penaltyPoints());
        for(int[] row:new int[][]{{70,0},{71,1}}){w=new OaWorkday();w.shift=new Shift(ShiftType.FLEXIBLE,LocalTime.of(8,0),LocalTime.of(17,0),10,60,480);w.times(row[0],row[0],60);assertEquals(row[1],w.result().lateMinutes());}
        for(int[] row:new int[][]{{130,0},{131,121}}){w=new OaWorkday();w.weather=true;w.times(row[0],row[0],60);assertEquals(row[1],w.result().lateMinutes());assertEquals(row[0]==130,w.result().warnings().contains("late arrival waived because of severe weather"));}
        for(int minutes:new int[]{359,360}){w=new OaWorkday();w.times(0,minutes-540,0);assertEquals(minutes,w.result().workedMinutes());assertEquals(minutes==360,w.result().codes().contains(AttendanceCode.INSUFFICIENT_BREAK));}
        w=new OaWorkday();w.times(0,-540,0);assertEquals(0,w.result().workedMinutes());w.times(0,-500,80);assertEquals(0,w.result().workedMinutes());
    }
    @Test void oaLocationDecisionTableAndExtendedWork(){
        for(EmploymentMode mode:EmploymentMode.values())for(boolean remote:new boolean[]{false,true})for(boolean verified:new boolean[]{false,true}){
            OaWorkday w=new OaWorkday();w.mode=mode;w.punch=new PunchRecord(w.date.atTime(8,0),w.date.atTime(17,0),60,remote,verified,false,false);AttendanceResult r=w.result();
            boolean unverified=remote&&!verified||mode==EmploymentMode.OFFICE&&remote||mode==EmploymentMode.FIELD&&!verified;
            assertEquals(unverified,r.codes().contains(AttendanceCode.LOCATION_UNVERIFIED));assertEquals(unverified,r.requiresManagerReview());assertEquals(mode==EmploymentMode.OFFICE&&remote,r.warnings().contains("office employee used a remote punch"));assertEquals(mode==EmploymentMode.REMOTE&&!remote,r.warnings().contains("remote employee used an office punch"));assertEquals(mode==EmploymentMode.HYBRID&&remote&&!verified,r.warnings().contains("hybrid remote location needs review"));
        }
        OaWorkday w=new OaWorkday();w.times(0,240,60);assertEquals(List.of(AttendanceCode.OVERTIME),w.result().codes());assertEquals(240,w.result().overtimeMinutes());assertEquals(240,w.result().payableOvertimeMinutes());assertFalse(w.result().requiresManagerReview());w.times(0,241,60);assertTrue(w.result().codes().contains(AttendanceCode.EXCESSIVE_OVERTIME));assertEquals(List.of("ordinary approved overtime"),w.result().warnings());
    }
    @Test void oaOvertimePayUsesRoundingCapsAndApproval(){
        OvertimePolicy policy=new OvertimePolicy();OaWorkday w=new OaWorkday();
        int[][] cases={{0,0},{1,0},{29,0},{30,30},{31,30},{59,30},{60,60},{239,210},{240,240},{269,240},{270,240},{360,240},{361,240}};
        for(int[] pair:cases){OvertimeResult r=policy.evaluate(w.input(),900,pair[0]);assertEquals(pair[0],r.actualMinutes());assertEquals(pair[1],r.payableMinutes());assertEquals(pair[0]==0?0:1,r.compensationFactor());assertEquals(pair[0]>=270,r.managerReview());assertFalse(r.compensatoryLeave());assertEquals(pair[0]==0?"no overtime":pair[0]<30?"overtime below minimum duration":pair[0]>=270?"daily overtime capped at four hours":"ordinary approved overtime",r.reason());}
        assertTrue(policy.evaluate(w.input(),901,30).managerReview());assertThrows(IllegalArgumentException.class,()->policy.evaluate(new OaWorkday().input(),-1,1));assertThrows(IllegalArgumentException.class,()->policy.evaluate(new OaWorkday().input(),1,-1));assertThrows(NullPointerException.class,()->policy.evaluate(null,0,0));
        w.approved=false;assertEquals(new OvertimeResult(70,0,0,false,true,"overtime is awaiting approval"),policy.evaluate(w.input(),550,70));w.request=false;assertEquals(new OvertimeResult(70,0,0,false,true,"overtime was not requested"),policy.evaluate(w.input(),550,70));
        w=new OaWorkday();w.holiday=true;assertEquals(3,policy.evaluate(w.input(),500,60).compensationFactor());assertEquals("public-holiday overtime",policy.evaluate(w.input(),500,60).reason());w.holiday=false;w.scheduled=false;assertEquals(2,policy.evaluate(w.input(),500,60).compensationFactor());
        for(int weekend:new int[]{5,6}){w=new OaWorkday();w.date=OA_MONDAY.plusDays(weekend);assertEquals(2,policy.evaluate(w.input(),500,60).compensationFactor());}
        for(int days:new int[]{5,6}){w=new OaWorkday();w.consecutive=days;assertEquals(days==6,policy.evaluate(w.input(),500,60).compensatoryLeave());assertEquals(days==6,policy.evaluate(w.input(),500,60).managerReview());}
        w=new OaWorkday();w.shift=new Shift(ShiftType.ON_CALL,LocalTime.of(8,0),LocalTime.of(17,0),0,60,480);assertEquals(0,policy.evaluate(w.input(),500,60).compensationFactor());assertTrue(policy.evaluate(w.input(),500,60).compensatoryLeave());assertEquals("short on-call work grants compensatory leave",policy.evaluate(w.input(),500,60).reason());assertEquals(1,policy.evaluate(w.input(),500,61).compensationFactor());assertFalse(policy.evaluate(w.input(),500,61).compensatoryLeave());
        w=new OaWorkday();w.mode=EmploymentMode.REMOTE;w.punch=null;assertEquals(60,policy.evaluate(w.input(),500,60).payableMinutes());w.punch=new PunchRecord(OA_MONDAY.atTime(8,0),OA_MONDAY.atTime(17,0),60,true,false,false,false);assertEquals(new OvertimeResult(60,0,1,false,true,"remote overtime location is unverified"),policy.evaluate(w.input(),500,60));
    }
    @Test void oaAdvancedLeaveChargesCalendarAndHourUnits(){
        AdvancedLeavePolicy policy=new AdvancedLeavePolicy();OaLeaveForm f=new OaLeaveForm();assertEquals("worker",f.request().employeeId());assertTrue(f.assess().eligible());assertEquals(1,f.assess().chargedDays());assertEquals(ApprovalTier.SUPERVISOR,f.assess().approvalTier());assertFalse(f.assess().handoverRequired());assertFalse(f.assess().medicalCertificateRequired());
        f.span(14);assertEquals(10,policy.calculateChargedDays(f.request(),d->d.getDayOfWeek().getValue()<6));assertEquals(0,policy.calculateChargedDays(f.request(),d->false));
        f=new OaLeaveForm();f.unit=LeaveUnit.HALF_DAY_MORNING;assertEquals(.5,policy.calculateChargedDays(f.request(),d->true));assertEquals(0,policy.calculateChargedDays(f.request(),d->false));f.unit=LeaveUnit.HALF_DAY_AFTERNOON;assertEquals(.5,policy.calculateChargedDays(f.request(),d->true));
        for(double[] sample:new double[][]{{.25,.5},{1,.5},{4,.5},{4.25,1},{8,1}}){f=new OaLeaveForm();f.unit=LeaveUnit.HOURS;f.hours=sample[0];assertEquals(sample[1],policy.calculateChargedDays(f.request(),d->false));}
        assertThrows(NullPointerException.class,()->policy.assess(null,d->true,OA_MONDAY));assertThrows(NullPointerException.class,()->policy.assess(new OaLeaveForm().request(),null,OA_MONDAY));assertThrows(NullPointerException.class,()->policy.assess(new OaLeaveForm().request(),d->true,null));
    }
    @Test void oaAdvancedLeaveApprovalAndHandoverExamples(){
        // Each row is a hand-worked business decision: days, tier ordinal, certificate, handover.
        int[][] annual={{1,0,0,0},{2,0,0,1},{3,0,0,1},{4,1,0,1},{10,1,0,1},{11,3,0,1}};
        for(int[] row:annual){OaLeaveForm f=new OaLeaveForm();f.span(row[0]);LeaveAssessment a=f.assess();assertEquals(ApprovalTier.values()[row[1]],a.approvalTier());assertEquals(row[2]==1,a.medicalCertificateRequired());assertEquals(row[3]==1,a.handoverRequired());}
        OaLeaveForm f=new OaLeaveForm();f.type=LeaveType.PERSONAL;assertEquals(ApprovalTier.MANAGER,f.assess().approvalTier());f.category=EmployeeCategory.PROBATION;assertEquals(ApprovalTier.HUMAN_RESOURCES,f.assess().approvalTier());f.category=EmployeeCategory.CONTRACTOR;f.span(4);assertEquals(ApprovalTier.DIRECTOR,f.assess().approvalTier());
        for(EmployeeCategory category:EmployeeCategory.values())for(int days:new int[]{1,2,3})for(boolean emergency:new boolean[]{false,true}){f=new OaLeaveForm();f.type=LeaveType.SICK;f.category=category;f.span(days);f.emergency=emergency;boolean certificate=days==3||days==2&&(category==EmployeeCategory.PROBATION||emergency);assertEquals(certificate,f.assess().medicalCertificateRequired());}
        for(double hour:new double[]{3.5,4,4.5}){f=new OaLeaveForm();f.type=LeaveType.SICK;f.unit=LeaveUnit.HOURS;f.hours=hour;assertEquals(hour==4,f.assess().medicalCertificateRequired());}
        f=new OaLeaveForm();f.type=LeaveType.SICK;f.category=EmployeeCategory.INTERN;f.span(2);assertFalse(f.assess().handoverRequired());f.first=OA_MONDAY.plusDays(4);f.last=f.first.plusDays(1);assertTrue(f.assess().handoverRequired());f.handover=false;oaHasIssue(f,"handover is required for this request");
    }
    @Test void oaAdvancedLeaveRejectsInvalidReasonsAndDates(){
        String[][] reasons={{"","reason is required"},{"  ","reason is required"},{"abcd","reason must contain at least five characters"},{"<travel","reason contains unsupported markup"},{"travel>","reason contains unsupported markup"},{"123456","reason cannot contain digits only"}};
        for(String[] row:reasons){OaLeaveForm f=new OaLeaveForm();f.reason=row[0];oaHasIssue(f,row[1]);}
        OaLeaveForm f=new OaLeaveForm();f.reason=null;oaHasIssue(f,"reason is required");f.reason="a".repeat(300);assertTrue(f.assess().eligible());f.reason+="a";oaHasIssue(f,"reason cannot exceed 300 characters");f.reason=" abcde ";assertTrue(f.assess().eligible());
        f=new OaLeaveForm();assertEquals(List.of("request contains no working time"),new AdvancedLeavePolicy().assess(f.request(),d->false,OA_MONDAY).violations());
        for(LeaveType type:LeaveType.values())for(boolean emergency:new boolean[]{false,true}){f=new OaLeaveForm();f.type=type;f.emergency=emergency;f.first=OA_MONDAY.minusDays(1);f.last=f.first;assertEquals(!(type==LeaveType.SICK&&emergency),f.assess().violations().contains("backdated leave is only allowed for emergency sick leave"));}
        f=new OaLeaveForm();f.first=OA_MONDAY.plusYears(1);f.last=f.first;oaNoIssue(f,"leave cannot be requested more than one year ahead");f.first=f.first.plusDays(1);f.last=f.first;oaHasIssue(f,"leave cannot be requested more than one year ahead");
        for(int days:new int[]{1,2,5,6,10,11,45,46})for(int notice:new int[]{0,1,2,3,6,7})for(boolean emergency:new boolean[]{false,true}){f=new OaLeaveForm();f.type=LeaveType.SICK;f.span(days);f.notice=notice;f.emergency=emergency;List<String> violations=f.assess().violations();assertEquals(days>1&&notice==0&&!emergency,violations.contains("multi-day leave requires advance notice"));assertEquals(days>5&&notice<3&&!emergency,violations.contains("leave longer than five days requires three days notice"));assertEquals(days>10&&notice<7&&!emergency,violations.contains("leave longer than ten days requires seven days notice"));assertEquals(days==46,violations.contains("a single request cannot span more than 45 calendar days"));}
    }
    @Test void oaEmploymentAndAnnualAllowanceRules(){
        for(EmployeeCategory category:EmployeeCategory.values())for(LeaveType type:LeaveType.values())for(int months:new int[]{0,1,5,6,11,12,600,601}){OaLeaveForm f=new OaLeaveForm();f.category=category;f.type=type;f.months=months;List<String> issues=f.assess().violations();assertEquals(category==EmployeeCategory.INTERN&&type==LeaveType.ANNUAL,issues.contains("interns are not eligible for annual leave"));assertEquals(category==EmployeeCategory.CONTRACTOR&&type==LeaveType.ANNUAL&&months<12,issues.contains("contractors require twelve months service for annual leave"));assertEquals(months<1&&type!=LeaveType.SICK,issues.contains("new employees may only request sick leave in their first month"));assertEquals(months==601,issues.contains("employment duration is outside the supported range"));assertEquals(type==LeaveType.ANNUAL&&months<6,issues.contains("annual leave requires six months service"));}
        OaLeaveForm f=new OaLeaveForm();f.category=EmployeeCategory.PROBATION;f.type=LeaveType.PERSONAL;oaHasIssue(f,"non-emergency personal leave is unavailable during probation");f.emergency=true;oaNoIssue(f,"non-emergency personal leave is unavailable during probation");
        f=new OaLeaveForm();f.balance=.5;f.carry=.5;assertTrue(f.assess().eligible());f.balance=.49;oaHasIssue(f,"insufficient annual leave balance");f.balance=40;f.carry=5;oaNoIssue(f,"carry-over balance cannot exceed five days");f.carry=5.01;oaHasIssue(f,"carry-over balance cannot exceed five days");
        f=new OaLeaveForm();f.carry=1;f.first=LocalDate.of(2026,3,31);f.last=f.first;oaNoIssue(f,"carry-over leave must be used before April");f.first=f.first.plusDays(1);f.last=f.first;oaHasIssue(f,"carry-over leave must be used before April");f.carry=0;oaNoIssue(f,"carry-over leave must be used before April");
        f=new OaLeaveForm();f.span(15);oaNoIssue(f,"annual leave cannot exceed fifteen working days per request");f.span(16);oaHasIssue(f,"annual leave cannot exceed fifteen working days per request");
        f=new OaLeaveForm();f.remote=true;f.notice=0;oaHasIssue(f,"remote-capable employees must consider remote work for urgent one-day leave");f.notice=1;oaNoIssue(f,"remote-capable employees must consider remote work for urgent one-day leave");f.notice=0;f.span(2);oaNoIssue(f,"remote-capable employees must consider remote work for urgent one-day leave");
    }
    @Test void oaSickPersonalAndPartialDayRules(){
        for(LeaveType type:LeaveType.values())for(double hours:new double[]{.5,1,1.1,1.5,4,4.1}){OaLeaveForm f=new OaLeaveForm();f.type=type;f.unit=LeaveUnit.HOURS;f.hours=hours;List<String> issues=f.assess().violations();assertEquals(hours<1,issues.contains("hour leave must be at least one hour"));assertEquals(type==LeaveType.ANNUAL&&(hours==1.1||hours==4.1),issues.contains("annual hour leave must use half-hour increments"));assertEquals(type==LeaveType.SICK&&hours>4,issues.contains("hour-based sick leave cannot exceed four hours"));assertEquals(type==LeaveType.PERSONAL&&hours>4,issues.contains("hour-based personal leave cannot exceed four hours"));}
        for(LeaveUnit unit:new LeaveUnit[]{LeaveUnit.HALF_DAY_MORNING,LeaveUnit.HALF_DAY_AFTERNOON}){OaLeaveForm f=new OaLeaveForm();f.unit=unit;f.span(2);oaHasIssue(f,"half-day leave must start and end on the same date");f.span(1);oaNoIssue(f,"half-day leave must start and end on the same date");f.emergency=true;assertEquals(unit==LeaveUnit.HALF_DAY_MORNING,f.assess().violations().contains("emergency leave cannot be limited to a planned morning slot"));}
        OaLeaveForm f=new OaLeaveForm();f.type=LeaveType.SICK;f.span(30);oaNoIssue(f,"sick leave longer than thirty days needs a separate medical process");f.span(31);oaHasIssue(f,"sick leave longer than thirty days needs a separate medical process");f.span(1);f.balance=365;oaNoIssue(f,"sick leave balance is outside the supported range");f.balance=366;oaHasIssue(f,"sick leave balance is outside the supported range");f.emergency=true;f.notice=30;oaNoIssue(f,"emergency sick leave has inconsistent notice information");f.notice=31;oaHasIssue(f,"emergency sick leave has inconsistent notice information");
        f=new OaLeaveForm();f.type=LeaveType.SICK;f.first=OA_MONDAY.plusDays(6);f.last=f.first;oaHasIssue(f,"planned sick leave cannot start on Sunday");f.emergency=true;oaNoIssue(f,"planned sick leave cannot start on Sunday");
        for(int days:new int[]{1,2,3,4,5,6})for(boolean emergency:new boolean[]{false,true}){f=new OaLeaveForm();f.type=LeaveType.PERSONAL;f.span(days);f.emergency=emergency;assertEquals(days>3&&!emergency,f.assess().violations().contains("personal leave cannot exceed three days without an emergency"));assertEquals(days>5,f.assess().violations().contains("personal leave cannot exceed five days"));f.category=EmployeeCategory.CONTRACTOR;assertEquals(days>1,f.assess().violations().contains("contractor personal leave is limited to one day"));}
        f=new OaLeaveForm();f.type=LeaveType.PERSONAL;f.notice=0;oaHasIssue(f,"personal leave requires one day notice");f.emergency=true;oaNoIssue(f,"personal leave requires one day notice");f.emergency=false;f.notice=1;f.remote=true;oaHasIssue(f,"short personal absence should use remote work when available");f.span(2);oaNoIssue(f,"short personal absence should use remote work when available");
    }
    @Test void oaInputValueObjectsEnforceTheirContracts(){
        LocalTime eight=LocalTime.of(8,0),five=LocalTime.of(17,0);
        Shift across=new Shift(ShiftType.STANDARD,five,eight,120,180,960);assertTrue(across.crossesMidnight());assertEquals(OA_MONDAY.atTime(17,0),across.scheduledStart(OA_MONDAY));assertEquals(OA_MONDAY.plusDays(1).atTime(8,0),across.scheduledEnd(OA_MONDAY));assertEquals(720,across.scheduledMinutes(OA_MONDAY));
        Shift daytime=new Shift(ShiftType.PART_TIME,eight,five,0,0,360);assertFalse(daytime.crossesMidnight());assertEquals(540,daytime.scheduledMinutes(OA_MONDAY));
        assertThrows(IllegalArgumentException.class,()->new Shift(ShiftType.STANDARD,eight,eight,0,0,60));assertThrows(IllegalArgumentException.class,()->new Shift(ShiftType.PART_TIME,eight,five,0,0,361));
        // The provided compact record constructor calls crossesMidnight before assigning fields.
        assertThrows(NullPointerException.class,()->new Shift(ShiftType.NIGHT,five,eight,0,0,480));
        for(int[] input:new int[][]{{-1,0,60},{121,0,60},{0,-1,60},{0,181,60},{0,0,59},{0,0,961}})assertThrows(IllegalArgumentException.class,()->new Shift(ShiftType.STANDARD,eight,five,input[0],input[1],input[2]));
        for(int rest:new int[]{-1,481})assertThrows(IllegalArgumentException.class,()->new PunchRecord(null,null,rest,false,false,false,false));assertEquals(480,new PunchRecord(null,null,480,false,false,false,false).breakMinutes());
        assertThrows(IllegalArgumentException.class,()->new PunchRecord(OA_MONDAY.atTime(five),OA_MONDAY.atTime(eight),0,false,false,false,false));assertThrows(IllegalArgumentException.class,()->new PunchRecord(null,null,0,false,false,false,true));
        for(String id:new String[]{null,"\t"})assertThrows(IllegalArgumentException.class,()->new AttendanceInput(id,OA_MONDAY,daytime,null,EmploymentMode.OFFICE,true,false,false,false,false,false,false,0,0));
        OaWorkday w=new OaWorkday();assertEquals("worker",w.input().employeeId());w.lateCount=-1;assertThrows(IllegalArgumentException.class,w::input);w.lateCount=0;w.consecutive=-1;assertThrows(IllegalArgumentException.class,w::input);w.consecutive=0;w.request=false;assertThrows(IllegalArgumentException.class,w::input);w.request=true;w.leave=true;w.trip=true;assertThrows(IllegalArgumentException.class,w::input);
        for(int n=0;n<6;n++){int[] v=new int[6];v[n]=-1;assertThrows(IllegalArgumentException.class,()->new AttendanceResult(List.of(AttendanceCode.NORMAL),v[0],v[1],v[2],v[3],v[4],v[5],false,List.of()));}
        assertThrows(IllegalArgumentException.class,()->new AttendanceResult(List.of(),0,0,0,0,0,0,false,List.of()));assertThrows(IllegalArgumentException.class,()->new AttendanceResult(List.of(AttendanceCode.NORMAL),0,0,0,0,1,0,false,List.of()));
        ArrayList<AttendanceCode> list=new ArrayList<>(List.of(AttendanceCode.REST_DAY));ArrayList<String> notes=new ArrayList<>(List.of("note"));AttendanceResult result=new AttendanceResult(list,0,0,0,0,0,0,false,notes);list.clear();notes.clear();assertEquals(List.of(AttendanceCode.REST_DAY),result.codes());assertEquals(List.of("note"),result.warnings());assertThrows(UnsupportedOperationException.class,()->result.codes().clear());assertThrows(UnsupportedOperationException.class,()->result.warnings().clear());
        for(int[] v:new int[][]{{-1,0},{0,-1},{0,1}})assertThrows(IllegalArgumentException.class,()->new OvertimeResult(v[0],v[1],1,false,false,"ok"));assertThrows(IllegalArgumentException.class,()->new OvertimeResult(0,0,-.1,false,false,"ok"));for(String reason:new String[]{null," "})assertThrows(IllegalArgumentException.class,()->new OvertimeResult(0,0,0,false,false,reason));
        OaLeaveForm f=new OaLeaveForm();f.id=null;assertThrows(IllegalArgumentException.class,f::request);f.id=" ";assertThrows(IllegalArgumentException.class,f::request);f.id="worker";f.last=f.first.minusDays(1);assertThrows(IllegalArgumentException.class,f::request);f.last=f.first;f.months=-1;assertThrows(IllegalArgumentException.class,f::request);f.months=0;f.notice=-1;assertThrows(IllegalArgumentException.class,f::request);f.notice=0;f.balance=-1;assertThrows(IllegalArgumentException.class,f::request);f.balance=0;f.carry=-1;assertThrows(IllegalArgumentException.class,f::request);f.carry=0;f.hours=1;assertThrows(IllegalArgumentException.class,f::request);f.unit=LeaveUnit.HOURS;
        for(double invalid:new double[]{-1,0,8.01}){f.hours=invalid;assertThrows(IllegalArgumentException.class,f::request);}f.hours=8;assertEquals(8,f.request().requestedHours());
        assertThrows(IllegalArgumentException.class,()->new LeaveAssessment(true,-.1,ApprovalTier.MANAGER,false,false,List.of()));assertThrows(IllegalArgumentException.class,()->new LeaveAssessment(false,0,ApprovalTier.MANAGER,false,false,List.of()));assertThrows(IllegalArgumentException.class,()->new LeaveAssessment(true,0,ApprovalTier.MANAGER,false,false,List.of("no")));
        ArrayList<String> violations=new ArrayList<>(List.of("no"));LeaveAssessment assessment=new LeaveAssessment(false,0,ApprovalTier.DIRECTOR,false,false,violations);violations.clear();assertEquals(List.of("no"),assessment.violations());assertThrows(UnsupportedOperationException.class,()->assessment.violations().clear());
    }
    @Test void oaCalendarParserRangesAndSimplePolicy(){
        for(String spelling:new String[]{"2028-02-29","2028/02/29","20280229"," 2028-02-29 "})assertEquals(LocalDate.of(2028,2,29),DateTextParser.parse(spelling));
        for(String blank:new String[]{null,"\t"})assertEquals("date text must not be blank",assertThrows(IllegalArgumentException.class,()->DateTextParser.parse(blank)).getMessage());
        for(String invalid:new String[]{"2027-02-29","2028/02/30","20281301","2028-2-29"," x "})assertEquals("unsupported date: "+invalid.trim(),assertThrows(IllegalArgumentException.class,()->DateTextParser.parse(invalid)).getMessage());
        DateRange week=new DateRange(OA_MONDAY,OA_MONDAY.plusDays(6));assertEquals(5,week.businessDays());DateRange saturday=new DateRange(OA_MONDAY.plusDays(5),OA_MONDAY.plusDays(5));assertEquals(0,saturday.businessDays());assertEquals(0,new DateRange(OA_MONDAY.plusDays(6),OA_MONDAY.plusDays(6)).businessDays());
        assertTrue(week.overlaps(saturday));assertTrue(week.overlaps(new DateRange(OA_MONDAY.plusDays(6),OA_MONDAY.plusDays(8))));assertTrue(week.overlaps(new DateRange(OA_MONDAY.minusDays(2),OA_MONDAY)));assertFalse(week.overlaps(new DateRange(OA_MONDAY.minusDays(2),OA_MONDAY.minusDays(1))));assertFalse(week.overlaps(new DateRange(OA_MONDAY.plusDays(7),OA_MONDAY.plusDays(8))));assertThrows(NullPointerException.class,()->week.overlaps(null));
        assertThrows(NullPointerException.class,()->new DateRange(null,OA_MONDAY));assertThrows(NullPointerException.class,()->new DateRange(OA_MONDAY,null));assertEquals("end must not be before start",assertThrows(IllegalArgumentException.class,()->new DateRange(OA_MONDAY,OA_MONDAY.minusDays(1))).getMessage());
        LeavePolicy policy=new LeavePolicy();DateRange day=new DateRange(OA_MONDAY,OA_MONDAY);assertEquals(1,policy.validate(LeaveType.ANNUAL,day," valid ",1));assertEquals(5,policy.validate(LeaveType.SICK,week,"doctor appointment",0));assertEquals(3,policy.validate(LeaveType.PERSONAL,new DateRange(OA_MONDAY,OA_MONDAY.plusDays(2)),"family",0));
        assertEquals("leave must contain at least one business day",assertThrows(LeaveValidationException.class,()->policy.validate(LeaveType.SICK,saturday,"reason",0)).getMessage());for(String bad:new String[]{null,"abc"," abcd "})assertEquals("reason must contain at least 5 characters",assertThrows(LeaveValidationException.class,()->policy.validate(LeaveType.SICK,day,bad,0)).getMessage());
        assertEquals("insufficient annual-leave balance",assertThrows(LeaveValidationException.class,()->policy.validate(LeaveType.ANNUAL,day,"reason",0)).getMessage());assertEquals("personal leave cannot exceed 3 business days",assertThrows(LeaveValidationException.class,()->policy.validate(LeaveType.PERSONAL,new DateRange(OA_MONDAY,OA_MONDAY.plusDays(3)),"reason",100)).getMessage());
    }
    private static LeaveRequest oaFreshRequest(LeaveType type){return LeaveRequest.pending(" R9 "," staff ",type,new DateRange(OA_MONDAY,OA_MONDAY.plusDays(2))," planned absence ",OA_INSTANT.minusSeconds(60));}
    @Test void oaLeaveLifecycleIsImmutableAndDecisionsAreFinal(){
        LeaveRequest waiting=oaFreshRequest(LeaveType.PERSONAL);assertEquals("R9",waiting.id());assertEquals("staff",waiting.employeeId());assertEquals("planned absence",waiting.reason());assertEquals(LeaveStatus.PENDING,waiting.status());assertNull(waiting.decidedBy());assertNull(waiting.decidedAt());assertNull(waiting.decisionComment());
        LeaveRequest yes=waiting.approve(" manager ",OA_INSTANT),no=waiting.reject(" manager ",OA_INSTANT," rejected comment ");assertEquals(LeaveStatus.APPROVED,yes.status());assertEquals(LeaveStatus.REJECTED,no.status());assertEquals("rejected comment",no.decisionComment());assertNull(yes.decisionComment());
        for(LeaveRequest decided:List.of(yes,no)){assertEquals("manager",decided.decidedBy());assertEquals(OA_INSTANT,decided.decidedAt());assertEquals(waiting.period(),decided.period());assertEquals(waiting.requestedAt(),decided.requestedAt());assertEquals(waiting.id(),decided.id());assertEquals(waiting.employeeId(),decided.employeeId());assertEquals(waiting.reason(),decided.reason());assertEquals(waiting.type(),decided.type());assertEquals("only a pending request can be decided",assertThrows(IllegalStateException.class,()->decided.approve("other",OA_INSTANT)).getMessage());assertThrows(IllegalStateException.class,()->decided.reject("other",OA_INSTANT,"reason"));}assertEquals(LeaveStatus.PENDING,waiting.status());
        for(String invalid:new String[]{null," "}){assertEquals("id must not be blank",assertThrows(IllegalArgumentException.class,()->LeaveRequest.pending(invalid,"staff",LeaveType.SICK,waiting.period(),"reason",OA_INSTANT)).getMessage());assertEquals("employeeId must not be blank",assertThrows(IllegalArgumentException.class,()->LeaveRequest.pending("id",invalid,LeaveType.SICK,waiting.period(),"reason",OA_INSTANT)).getMessage());assertEquals("reason must not be blank",assertThrows(IllegalArgumentException.class,()->LeaveRequest.pending("id","staff",LeaveType.SICK,waiting.period(),invalid,OA_INSTANT)).getMessage());assertEquals("decidedBy must not be blank",assertThrows(IllegalArgumentException.class,()->waiting.approve(invalid,OA_INSTANT)).getMessage());assertEquals("comment must not be blank",assertThrows(IllegalArgumentException.class,()->waiting.reject("manager",OA_INSTANT,invalid)).getMessage());assertThrows(IllegalArgumentException.class,()->new SubmitLeaveCommand(invalid,LeaveType.SICK,waiting.period(),null));}
        for(int field=0;field<3;field++){final int n=field;assertEquals("pending request must not contain a decision",assertThrows(IllegalArgumentException.class,()->new LeaveRequest("id","staff",LeaveType.SICK,waiting.period(),"reason",LeaveStatus.PENDING,OA_INSTANT,n==0?"manager":null,n==1?OA_INSTANT:null,n==2?"comment":null)).getMessage());}
        assertThrows(NullPointerException.class,()->waiting.approve("manager",null));SubmitLeaveCommand command=new SubmitLeaveCommand(" staff ",LeaveType.SICK,waiting.period(),null);assertEquals("staff",command.employeeId());assertNull(command.reason());
    }
    private static class OaPorts implements LeaveRequestRepository,LeaveBalanceRepository,NotificationGateway,RequestIdGenerator {
        final Map<String,LeaveRequest> entries=new HashMap<>();
        final List<String> events=new ArrayList<>();
        final List<LeaveRequest> announcements=new ArrayList<>();
        boolean overlap=false,failSave=false;
        int balance=3,deducted=0;
        LeaveRequest substitute=null;
        public Optional<LeaveRequest> findById(String id){events.add("find:"+id);return Optional.ofNullable(entries.get(id));}
        public boolean existsActiveOverlap(String employee,DateRange period){events.add("overlap:"+employee+":"+period.businessDays());return overlap;}
        public LeaveRequest save(LeaveRequest request){events.add("save:"+request.status());if(failSave)throw new IllegalStateException("database down");LeaveRequest saved=substitute==null?request:substitute;entries.put(saved.id(),saved);return saved;}
        public int remainingAnnualDays(String employee){events.add("balance:"+employee);return balance;}
        public void deductAnnualDays(String employee,int days){events.add("deduct:"+employee+":"+days);deducted+=days;}
        public String nextId(){events.add("id");return "generated-17";}
        public void requestSubmitted(LeaveRequest request){events.add("submitted");announcements.add(request);}
        public void requestApproved(LeaveRequest request){events.add("approved");announcements.add(request);}
        public void requestRejected(LeaveRequest request){events.add("rejected");announcements.add(request);}
        LeaveRequestService service(){return new LeaveRequestService(this,this,this,this,new LeavePolicy(),Clock.fixed(OA_INSTANT,ZoneOffset.UTC));}
    }
    @Test void oaServiceOrchestratesSubmissionAndDecisionPorts(){
        for(LeaveType type:LeaveType.values()){
            OaPorts ports=new OaPorts();LeaveRequestService service=ports.service();DateRange interval=new DateRange(OA_MONDAY,OA_MONDAY.plusDays(2));
            LeaveRequest created=service.submit(new SubmitLeaveCommand(" staff ",type,interval," planned absence "));
            assertEquals("generated-17",created.id());assertEquals("staff",created.employeeId());assertEquals(type,created.type());assertEquals(interval,created.period());assertEquals("planned absence",created.reason());assertEquals(OA_INSTANT,created.requestedAt());assertEquals(LeaveStatus.PENDING,created.status());assertSame(created,ports.entries.get(created.id()));assertSame(created,ports.announcements.get(0));
            assertEquals(type==LeaveType.ANNUAL?List.of("balance:staff","overlap:staff:3","id","save:PENDING","submitted"):List.of("overlap:staff:3","id","save:PENDING","submitted"),ports.events);
            ports.events.clear();LeaveRequest approved=service.approve(" generated-17 "," manager ");assertEquals(LeaveStatus.APPROVED,approved.status());assertEquals("manager",approved.decidedBy());assertEquals(OA_INSTANT,approved.decidedAt());assertSame(approved,ports.announcements.get(1));assertEquals(type==LeaveType.ANNUAL?3:0,ports.deducted);
            assertEquals(type==LeaveType.ANNUAL?List.of("find:generated-17","deduct:staff:3","save:APPROVED","approved"):List.of("find:generated-17","save:APPROVED","approved"),ports.events);
            ports.entries.put(created.id(),created);ports.events.clear();LeaveRequest rejected=service.reject("generated-17"," manager "," cannot approve ");assertEquals(LeaveStatus.REJECTED,rejected.status());assertEquals("cannot approve",rejected.decisionComment());assertEquals("manager",rejected.decidedBy());assertEquals(OA_INSTANT,rejected.decidedAt());assertSame(rejected,ports.announcements.get(2));assertEquals(List.of("find:generated-17","save:REJECTED","rejected"),ports.events);
        }
    }
    @Test void oaServiceUsesPersistenceReturnAndDoesNotNotifyFailedWrites(){
        OaPorts p=new OaPorts();LeaveRequestService service=p.service();p.substitute=oaFreshRequest(LeaveType.SICK);DateRange dates=p.substitute.period();
        assertSame(p.substitute,service.submit(new SubmitLeaveCommand("staff",LeaveType.SICK,dates,"planned absence")));assertSame(p.substitute,p.announcements.get(0));
        assertSame(p.substitute,service.approve("R9","manager"));assertSame(p.substitute,p.announcements.get(1));assertSame(p.substitute,service.reject("R9","manager","reason"));assertSame(p.substitute,p.announcements.get(2));
        OaPorts failing=new OaPorts();failing.failSave=true;assertEquals("database down",assertThrows(IllegalStateException.class,()->failing.service().submit(new SubmitLeaveCommand("staff",LeaveType.SICK,dates,"planned absence"))).getMessage());assertTrue(failing.announcements.isEmpty());assertEquals(List.of("overlap:staff:3","id","save:PENDING"),failing.events);
    }
    @Test void oaServiceValidationPreventsSideEffects(){
        OaPorts p=new OaPorts();LeaveRequestService service=p.service();LeaveRequest pending=oaFreshRequest(LeaveType.ANNUAL);p.entries.put("R9",pending);
        for(String bad:new String[]{null," "}){
            assertEquals("requestId must not be blank",assertThrows(IllegalArgumentException.class,()->service.approve(bad,"manager")).getMessage());assertEquals("requestId must not be blank",assertThrows(IllegalArgumentException.class,()->service.reject(bad,"manager","reason")).getMessage());
            assertEquals("approverId must not be blank",assertThrows(IllegalArgumentException.class,()->service.approve("R9",bad)).getMessage());assertEquals("approverId must not be blank",assertThrows(IllegalArgumentException.class,()->service.reject("R9",bad,"reason")).getMessage());
        }
        assertEquals("leave request not found: unknown",assertThrows(LeaveRequestNotFoundException.class,()->service.approve(" unknown ","manager")).getMessage());assertEquals("employee cannot approve their own request",assertThrows(LeaveValidationException.class,()->service.approve("R9"," staff ")).getMessage());assertEquals("employee cannot reject their own request",assertThrows(LeaveValidationException.class,()->service.reject("R9"," staff ","reason")).getMessage());assertTrue(p.announcements.isEmpty());assertEquals(0,p.deducted);
        p.events.clear();p.overlap=true;assertEquals("leave period overlaps an active request",assertThrows(LeaveValidationException.class,()->service.submit(new SubmitLeaveCommand("staff",LeaveType.SICK,pending.period(),"planned absence"))).getMessage());assertEquals(List.of("overlap:staff:3"),p.events);
        p.events.clear();p.balance=2;assertEquals("insufficient annual-leave balance",assertThrows(LeaveValidationException.class,()->service.submit(new SubmitLeaveCommand("staff",LeaveType.ANNUAL,pending.period(),"planned absence"))).getMessage());assertEquals(List.of("balance:staff"),p.events);
        p.events.clear();assertThrows(LeaveValidationException.class,()->service.submit(new SubmitLeaveCommand("staff",LeaveType.SICK,pending.period(),"bad")));assertTrue(p.events.isEmpty());assertThrows(NullPointerException.class,()->service.submit(null));
        p.entries.put("R9",pending.approve("manager",OA_INSTANT));assertThrows(IllegalStateException.class,()->service.approve("R9","other"));assertThrows(IllegalStateException.class,()->service.reject("R9","other","reason"));assertEquals(0,p.deducted);assertTrue(p.announcements.isEmpty());
        Clock c=Clock.fixed(OA_INSTANT,ZoneOffset.UTC);LeavePolicy policy=new LeavePolicy();assertThrows(NullPointerException.class,()->new LeaveRequestService(null,p,p,p,policy,c));assertThrows(NullPointerException.class,()->new LeaveRequestService(p,null,p,p,policy,c));assertThrows(NullPointerException.class,()->new LeaveRequestService(p,p,null,p,policy,c));assertThrows(NullPointerException.class,()->new LeaveRequestService(p,p,p,null,policy,c));assertThrows(NullPointerException.class,()->new LeaveRequestService(p,p,p,p,null,c));assertThrows(NullPointerException.class,()->new LeaveRequestService(p,p,p,p,policy,null));
    }


    static AccessContext access() {return new AccessContext("person","person","dept","dept",UserRole.EMPLOYEE,ResourceAction.VIEW,ResourceType.PUBLIC_DOCUMENT,Sensitivity.INTERNAL,AccessScope.OWN,true,false,true,true,true,true,false,false,false,false,true,true,true);}
    static AccessDecision decide(AccessContext c) {return new AccessPolicy().decide(c);}
    @Test void permissionRoleResourceDecisionTable() {
        String[] actionCodes={"V","C","U","D","A","E","S","H","R"};
        String[][] grants={
            {"VU","V","VCU","VCU","VU","VU","V","","VU"},
            {"","","VCA","VCA","VA","","","","V"},
            {"VCUAESHR","","VCUAESHR","VCUAESHR","VCUAESHR","VCUAESHR","","","VCUAESHR"},
            {"VCUAESHR","","","VCUAESHR","VCUAESHR","","","","V"},
            {"","VCUAESHR","VCUAESHR","","","VCUAESHR","","","V"},
            {"VE","VE","VE","VE","VE","VE","VE","","VE"},
            {"CUDSHR","CUDSHR","VCUDAESHR","VCUDAESHR","VCUDAESHR","VCUDESHR","VE","VCUDAESHR","VCUDAESHR"},
            {"","","","","","","","","V"}
        };
        AccessScope[] scopes={AccessScope.OWN,AccessScope.TEAM,AccessScope.DEPARTMENT,AccessScope.ORGANIZATION,AccessScope.ORGANIZATION,AccessScope.ORGANIZATION,AccessScope.ORGANIZATION,AccessScope.OWN};
        for(UserRole role:UserRole.values())for(ResourceType type:ResourceType.values())for(ResourceAction action:ResourceAction.values()) {
            AccessContext c=changed(access(),"role",role,"resourceType",type,"action",action,"requestedScope",scopes[role.ordinal()],"ownerId",role==UserRole.EMPLOYEE||role==UserRole.GUEST?"person":"other","archived",action==ResourceAction.RESTORE,"sensitivity",role==UserRole.GUEST?Sensitivity.PUBLIC:Sensitivity.INTERNAL);
            AccessDecision result=decide(c);boolean expected=grants[role.ordinal()][type.ordinal()].contains(actionCodes[action.ordinal()]);
            assertEquals(expected,result.allowed(),role+"/"+type+"/"+action+" "+result.reasons());assertFalse(result.stepUpAuthenticationRequired());
            assertEquals(Set.of(ResourceAction.EXPORT,ResourceAction.DELETE,ResourceAction.APPROVE).contains(action)||role==UserRole.SYSTEM_ADMINISTRATOR||type==ResourceType.AUDIT_LOG,result.auditRequired());assertFalse(result.reasons().isEmpty());
        }
    }
    @Test void permissionScopeAndContextConstraints() {
        AccessContext own=access();assertTrue(own.ownsResource());assertTrue(own.sameDepartment());
        for(String id:new String[]{null,"other"})assertFalse(changed(own,"ownerId",id).ownsResource());
        assertFalse(changed(own,"actorDepartment",null).sameDepartment());assertFalse(changed(own,"resourceDepartment",null).sameDepartment());assertFalse(changed(own,"resourceDepartment","different").sameDepartment());
        textFields(own,"actorId");required(own,"role","action","resourceType","sensitivity","requestedScope");bad(own,"accountSuspended",true);bad(own,"delegationValid",true);
        assertEquals("person",changed(own,"actorId"," person ").actorId());assertEquals("person",changed(own,"ownerId"," person ").ownerId());assertEquals("dept",changed(own,"actorDepartment"," dept ").actorDepartment());assertEquals("dept",changed(own,"resourceDepartment"," dept ").resourceDepartment());
        AccessDecision yes=new AccessDecision(true,false,false,List.of());bad(yes,"stepUpAuthenticationRequired",true);bad(yes,"allowed",false);required(yes,"reasons");
        for(UserRole role:UserRole.values())for(AccessScope scope:AccessScope.values()) {
            AccessContext c=changed(own,"role",role,"requestedScope",scope,"resourceType",ResourceType.PUBLIC_DOCUMENT,"sensitivity",Sensitivity.PUBLIC);
            boolean allowed=switch(scope){case OWN->true;case TEAM->role!=UserRole.EMPLOYEE&&role!=UserRole.GUEST;case DEPARTMENT->Set.of(UserRole.DEPARTMENT_MANAGER,UserRole.HUMAN_RESOURCES,UserRole.FINANCE,UserRole.AUDITOR,UserRole.SYSTEM_ADMINISTRATOR).contains(role);case ORGANIZATION->Set.of(UserRole.HUMAN_RESOURCES,UserRole.FINANCE,UserRole.AUDITOR,UserRole.SYSTEM_ADMINISTRATOR).contains(role);};
            assertEquals(allowed,decide(c).allowed(),role+"/"+scope);
        }
        assertFalse(decide(changed(own,"ownerId",null)).allowed());assertTrue(decide(changed(own,"ownerId",null,"role",UserRole.SYSTEM_ADMINISTRATOR)).allowed());
        AccessContext manager=changed(own,"role",UserRole.DEPARTMENT_MANAGER,"requestedScope",AccessScope.DEPARTMENT,"ownerId","other");assertFalse(decide(changed(manager,"resourceDepartment","different")).allowed());
        assertTrue(decide(changed(manager,"action",ResourceAction.DELETE,"sensitivity",Sensitivity.PUBLIC)).allowed());
        AccessContext hr=changed(manager,"role",UserRole.HUMAN_RESOURCES,"resourceType",ResourceType.EMPLOYEE_PROFILE,"action",ResourceAction.EXPORT,"requestedScope",AccessScope.ORGANIZATION);
        assertFalse(decide(changed(hr,"projectMember",false)).allowed());assertTrue(decide(changed(hr,"requestedScope",AccessScope.TEAM,"projectMember",false)).allowed());
        assertTrue(decide(changed(hr,"resourceType",ResourceType.PAYROLL,"action",ResourceAction.VIEW,"requestedScope",AccessScope.TEAM)).allowed());
        AccessContext finance=changed(manager,"role",UserRole.FINANCE,"resourceType",ResourceType.CONTRACT,"action",ResourceAction.SHARE);assertTrue(decide(finance).allowed());assertFalse(decide(changed(finance,"sensitivity",Sensitivity.RESTRICTED)).allowed());
        assertThrows(NullPointerException.class,()->new AccessPolicy().decide(null));
    }
    @Test void permissionDenialsMessagesAndAuditTriggers() {
        String[][] falseFlags={{"accountActive","account is inactive"},{"trustedDevice","restricted resource requires a trusted device"},{"securityTrainingCompleted","security training is required"},{"businessHours","restricted resource is unavailable outside business hours"}};
        AccessContext restricted=changed(access(),"sensitivity",Sensitivity.RESTRICTED);
        for(String[] rule:falseFlags){AccessDecision d=decide(changed(restricted,rule[0],false));assertFalse(d.allowed());assertTrue(d.reasons().contains(rule[1]));assertTrue(d.auditRequired());}
        AccessDecision step=decide(changed(restricted,"multiFactorAuthenticated",false));assertTrue(step.stepUpAuthenticationRequired());assertTrue(step.reasons().contains("restricted resource requires multi-factor authentication"));
        assertFalse(decide(changed(access(),"multiFactorAuthenticated",false)).stepUpAuthenticationRequired());
        assertTrue(decide(changed(access(),"trustedDevice",false,"securityTrainingCompleted",false)).allowed());
        assertTrue(decide(changed(restricted,"role",UserRole.SYSTEM_ADMINISTRATOR,"businessHours",false)).allowed());
        assertTrue(decide(changed(access(),"businessHours",false)).auditRequired());
        AccessDecision suspended=decide(changed(access(),"accountActive",false,"accountSuspended",true));assertTrue(suspended.reasons().containsAll(List.of("account is inactive","account is suspended")));
        AccessContext specialist=changed(access(),"role",UserRole.FINANCE,"resourceType",ResourceType.CONTRACT,"requestedScope",AccessScope.ORGANIZATION);
        for(ResourceAction action:List.of(ResourceAction.SHARE,ResourceAction.EXPORT))assertTrue(decide(changed(specialist,"action",action,"trustedNetwork",false)).reasons().contains("export or sharing requires a trusted network"));
        assertTrue(decide(changed(specialist,"action",ResourceAction.EXPORT,"dataRegionMatches",false)).reasons().contains("cross-region export is not allowed"));assertTrue(decide(changed(specialist,"dataRegionMatches",false,"trustedNetwork",false)).allowed());
        AccessDecision invalid=decide(changed(access(),"delegated",true));assertTrue(invalid.reasons().contains("delegation has expired or is invalid"));assertTrue(invalid.auditRequired());assertTrue(decide(changed(access(),"delegated",true,"delegationValid",true)).allowed());
        for(ResourceAction action:List.of(ResourceAction.DELETE,ResourceAction.EXPORT))assertTrue(decide(changed(specialist,"action",action,"delegated",true,"delegationValid",true)).reasons().contains(action==ResourceAction.DELETE?"delegated permission cannot delete resources":"delegated permission cannot export resources"));
        for(ResourceAction action:List.of(ResourceAction.DELETE,ResourceAction.ARCHIVE))assertTrue(decide(changed(specialist,"action",action,"legalHold",true)).reasons().contains("resource is under legal hold"));
        assertTrue(decide(changed(access(),"action",ResourceAction.UPDATE,"archived",true)).reasons().contains("archived resource cannot be updated"));assertTrue(decide(changed(specialist,"action",ResourceAction.RESTORE)).reasons().contains("only archived resources can be restored"));
        assertTrue(decide(changed(specialist,"resourceType",ResourceType.PAYROLL,"sensitivity",Sensitivity.PUBLIC)).reasons().contains("payroll cannot be classified as public"));
        assertTrue(decide(changed(specialist,"role",UserRole.HUMAN_RESOURCES,"resourceType",ResourceType.EMPLOYEE_PROFILE,"action",ResourceAction.SHARE,"projectMember",false)).reasons().contains("profile sharing requires project membership"));
        assertTrue(decide(changed(access(),"sensitivity",Sensitivity.CONFIDENTIAL,"securityTrainingCompleted",false)).reasons().contains("security training is required"));
        assertTrue(decide(changed(access(),"ownerId",null,"actorDepartment",null,"resourceDepartment",null,"requestedScope",AccessScope.TEAM,"role",UserRole.FINANCE)).reasons().contains("access granted by role and scope policy"));
    }
    static CorporateCardExpenseService reviewer() {return new CorporateCardExpenseService(new ExpensePolicy(),new CardEvidenceValidator(CLOCK),new AccessPolicy(),new ApprovalWorkflowEngine());}
    @Test void corporateCardReviewUsesRealPoliciesAndApprovalEngine() {
        CorporateCardExpenseService service=reviewer();AccessContext creator=changed(access(),"action",ResourceAction.CREATE,"resourceType",ResourceType.EXPENSE_CLAIM);
        ExpenseClaim c=changed(claim(),"items",List.of(changed(purchase(),"paymentMethod",PaymentMethod.CORPORATE_CARD)));
        CardEvidence evidence=new CardEvidence("item","4111111111111111",YearMonth.of(2026,6));
        ExpenseCardReview ready=service.review(c,Map.of("item",evidence),(currency,date)->BigDecimal.ONE,DATE,creator);
        assertEquals("claim",ready.claimId());assertTrue(ready.readyForApproval());assertFalse(ready.auditRequired());assertTrue(ready.issues().isEmpty());assertEquals(List.of(new CardEvidenceCheck("item",CreditCardType.VISA,true,List.of())),ready.cards());assertTrue(ready.access().allowed());assertEquals(cash("100.00"),ready.expense().reimbursableAmount());
        ApprovalStep step=new ApprovalStep("step","Manager",Set.of("manager"),StepMode.ANY_ONE,1,false,false,null);
        WorkflowSnapshot started=service.startApproval(c,ready,List.of(step),MOMENT);assertEquals("claim",started.workflowId());assertEquals("person",started.requesterId());assertEquals(WorkflowState.PENDING,started.state());assertFalse(started.urgent());assertTrue(started.withdrawalAllowed());
        for(Map<String,CardEvidence> map:List.of(Map.<String,CardEvidence>of(),Map.of("item",changed(evidence,"itemId","other")),Map.of("item",changed(evidence,"number","4111111111111112")))) {
            ExpenseCardReview blocked=service.review(c,map,(cur,d)->BigDecimal.ONE,DATE,creator);assertFalse(blocked.readyForApproval());assertTrue(blocked.auditRequired());assertEquals(1,blocked.issues().size());error("claim is not ready for approval",()->service.startApproval(c,blocked,List.of(step),MOMENT));
        }
        assertEquals(List.of("corporate card evidence missing for item item"),service.review(c,Map.of(),(cur,d)->BigDecimal.ONE,DATE,creator).issues());
        ExpenseCardReview personal=service.review(claim(),Map.of(),(cur,d)->BigDecimal.ONE,DATE,creator);assertTrue(personal.readyForApproval());assertTrue(personal.cards().isEmpty());
        assertFalse(service.review(c,Map.of("item",evidence),(cur,d)->BigDecimal.ONE,DATE,changed(creator,"accountActive",false)).readyForApproval());
        assertFalse(service.review(changed(c,"submittedDate",DATE.plusDays(1)),Map.of("item",evidence),(cur,d)->BigDecimal.ONE,DATE,creator).readyForApproval());
        ExpenseCardReview audited=service.review(c,Map.of("item",evidence),(cur,d)->BigDecimal.ONE,DATE,changed(creator,"businessHours",false));assertTrue(audited.auditRequired());assertTrue(service.startApproval(c,audited,List.of(step),MOMENT).urgent());
        error("review belongs to a different claim",()->service.startApproval(changed(c,"claimId","other"),ready,List.of(step),MOMENT));
        error("employee may only create their own claim",()->service.review(c,Map.of(),(cur,d)->BigDecimal.ONE,DATE,changed(creator,"actorId","stranger")));
        for(AccessContext wrong:List.of(changed(creator,"action",ResourceAction.VIEW),changed(creator,"resourceType",ResourceType.CONTRACT)))error("actor context must describe creating an expense claim",()->service.review(c,Map.of(),(cur,d)->BigDecimal.ONE,DATE,wrong));
        assertTrue(service.review(c,Map.of("item",evidence),(cur,d)->BigDecimal.ONE,DATE,changed(creator,"role",UserRole.FINANCE,"actorId","finance")).readyForApproval());
        textFields(ready,"claimId");required(ready,"access","expense","cards","issues");bad(ready,"readyForApproval",false);bad(ready,"auditRequired",true);bad(ready,"cards",List.of(new CardEvidenceCheck("item",CreditCardType.OTHER,false,List.of("invalid"))));
        assertEquals("claim",changed(ready,"claimId"," claim ").claimId());assertThrows(UnsupportedOperationException.class,()->ready.cards().clear());
        assertThrows(NullPointerException.class,()->service.review(null,Map.of(),(cur,d)->BigDecimal.ONE,DATE,creator));assertThrows(NullPointerException.class,()->service.review(c,null,(cur,d)->BigDecimal.ONE,DATE,creator));assertThrows(NullPointerException.class,()->service.review(c,Map.of(),(cur,d)->BigDecimal.ONE,DATE,null));assertThrows(NullPointerException.class,()->service.startApproval(null,ready,List.of(step),MOMENT));assertThrows(NullPointerException.class,()->service.startApproval(c,null,List.of(step),MOMENT));
        assertThrows(NullPointerException.class,()->new CorporateCardExpenseService(null,new CardEvidenceValidator(CLOCK),new AccessPolicy(),new ApprovalWorkflowEngine()));assertThrows(NullPointerException.class,()->new CorporateCardExpenseService(new ExpensePolicy(),null,new AccessPolicy(),new ApprovalWorkflowEngine()));assertThrows(NullPointerException.class,()->new CorporateCardExpenseService(new ExpensePolicy(),new CardEvidenceValidator(CLOCK),null,new ApprovalWorkflowEngine()));assertThrows(NullPointerException.class,()->new CorporateCardExpenseService(new ExpensePolicy(),new CardEvidenceValidator(CLOCK),new AccessPolicy(),null));
    }

}
