package net.mooctest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 项目说明：
 * 构建环境：
 * - Maven 版本：3.9.6 及以上
 * - JDK 版本：21
 * - JUnit 版本：5.10.2
 **/

public class JSONTest {

  @Test
  public void orderedObjectStartsEmptyRegardlessOfInitialCapacity() {
      JSONObject object = new JSONObject(1, true);
      assertEquals(0, object.size());
  }

}
