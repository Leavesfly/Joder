package io.leavesfly.joder.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * 数组工具类
 * 对应 Kode 的 array.ts
 */
public class ArrayUtils {
    
    /**
     * 在数组元素之间插入分隔符
     * 
     * @param <T> 元素类型
     * @param list 原始列表
     * @param separator 分隔符生成函数（接收索引）
     * @return 插入分隔符后的新列表
     */
    public static <T> List<T> intersperse(List<T> list, IntFunction<T> separator) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<T> result = new ArrayList<>(list.size() * 2 - 1);
        
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                result.add(separator.apply(i));
            }
            result.add(list.get(i));
        }
        
        return result;
    }
    
    /**
     * 在数组元素之间插入固定分隔符
     * 
     * @param <T> 元素类型
     * @param list 原始列表
     * @param separator 固定分隔符
     * @return 插入分隔符后的新列表
     */
    public static <T> List<T> intersperseWithValue(List<T> list, T separator) {
        return intersperse(list, (IntFunction<T>) i -> separator);
    }
}
