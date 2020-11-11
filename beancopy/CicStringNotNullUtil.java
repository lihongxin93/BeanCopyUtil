package com.cic.offer.biz.utils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 把bean里string类型为null的都转换为空串""
 * lhx
 * add by 20201030
 */
public class CicStringNotNullUtil {

    public static void StringIsNotNull(Object o){
        try{
            BeanStringNotNull(o);
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    /**
     * 转换bean
     * @param o
     * @throws Exception
     */
    private static void BeanStringNotNull(Object o) throws Exception{
        if(null!=o){
            //获取该class下所有属性 取目标所需要的list
            Field[] tFields=o.getClass().getDeclaredFields();
            //循环所有属性
            for(Field tField:tFields){
                //获取值
                tField.setAccessible(true);
                Object value=tField.get(o);
                //如果是String 类型的
                if(null==value&&String.class.getTypeName().equals(tField.getType().getName())){
                    //String 类型
                    PropertyStringNotNull(o,tField);
                }else if(isList(tField.getType().getTypeName())){
                    ListStringNotNull(o,tField);
                }else if(tField.getType().getName().contains("com.cic.offer")){
                    BeanStringNotNull(value);
                }

            }
        }
    }

    /**
     * string 转空串
     * @param o
     * @param field
     * @throws Exception
     */
    private static void PropertyStringNotNull(Object o,Field field) throws Exception{
        //给属性赋值为空串
        field.set(o,"");
    }

    /**
     * 转换list
     * @param o
     * @param field
     * @throws Exception
     */
    private static void ListStringNotNull(Object o,Field field) throws Exception{
        Type tType=field.getGenericType();
        //必须是参数化的类型
        if (tType instanceof ParameterizedType) {
            // 参数值为true，禁止访问控制检查
            field.setAccessible(true);
            //获取属性值
            List sValue = (List)field.get(o);
            if(null!=sValue&&sValue.size()>0){
                for(int i=0;i<sValue.size();i++){
                    //转换bean下的String
                    BeanStringNotNull(sValue.get(i));
                }
            }
        }
    }
    /**
     * 判断是否list
     * @param type
     * @return
     */
    private static boolean isList (String type){
        if (List.class.getTypeName().equals(type) || ArrayList.class.getTypeName().equals(type)) {
            return true;
        } else {
            return false;
        }
    }
}
