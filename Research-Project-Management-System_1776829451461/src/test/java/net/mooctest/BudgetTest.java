package net.mooctest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 
 * 开发者做题前，请仔细阅读以下说明：
 * 
 * 1、该测试类为测试类示例，不要求完全按照该示例类的格式；
 *	    考生可自行创建测试类，类名可自行定义，但需遵循JUnit命名规范，格式为xxxTest.java，提交类似test.java的文件名将因不符合语法而判0分！
 * 
 * 2、所有测试方法放在该顶层类中，不建议再创建内部类。若必需创建内部类，则需检查JUnit对于内部测试类的要求，并添加相关注释，否则将因无法执行而判0分！
 * 
 * 3、本比赛使用jdk21+JUnit5.10.2+Maven 版本：3.9.6 及以上，未使用以上版本编写测试用例者，不再接受低分申诉；
 * 
 * 4、不要修改被测代码；
 * 
 * 5、建议尽量避免卡点提交答案，尤其是两份报告的zip包。
 * 
 * */
public class BudgetTest {

	@Test
	public void testItemConstructorWithValidParameters() {
		Budget.Item item = new Budget.Item("Test", 100.0, 0.5, "CATEGORY");
		assertEquals("Test", item.getName());
		assertEquals(100.0, item.getCost(), 0.001);
		assertEquals(0.5, item.getValue(), 0.001);
		assertEquals("CATEGORY", item.getCategory());
	}
	
	@Test
    public void testItemConstructorWithNullName() {
        Budget.Item item = new Budget.Item(null, 100.0, 0.5, "CATEGORY");
        assertEquals("", item.getName());
    }

}
