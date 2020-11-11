package com.cic.offer.facade.xsteam;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * 让类型转换不为null的转换器，null转换为空串
 * lhx
 * 20201110
 */
@Slf4j
public class NotNullConverter implements Converter {
    private Class currentType;

    /**
     * 能够转换的类型
     * @param type
     * @return
     */
    public boolean canConvert(Class type) {
        currentType = type;
        return true;
    }

    /**
     * 序列化
     * @param source
     * @param writer
     * @param context
     */
    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer,MarshallingContext context) {
        try {
            marshalSuper(source, writer, context, currentType);
        } catch (Exception e) {
            log.error("Xsteam Error 转换器转换出错",e);
            e.printStackTrace();
        }
    }

    /**
     * 根据fildid获取值
     * @param clazz
     * @param field
     * @param source
     * @return
     * @throws Exception
     */
    private Object getObj(Class clazz, Field field, Object source)
            throws Exception {
        if(null==source){
            return null;
        }
        Object obj = null;
        obj = field.get(source);
        return obj;
    }

    /**
     * 对象转换方法
     * @param source
     * @param writer
     * @param context
     * @param clazz
     * @param field
     * @param fieldClazz
     * @throws Exception
     */
    private void objConverter(Object source, HierarchicalStreamWriter writer,
                              MarshallingContext context, Class clazz, Field field,
                              Class fieldClazz) throws Exception {
        Object obj = getObj(clazz, field, source);
        if(null==obj){
            return;
        }
        String nodeName=null;
        //优先使用filedid上的注解，次而使用javabean上的注解，fildidname兜底
        String nodeName1=getFildIdAnnotationXStreamAlias(field);
        String nodeName2=getClassAnnotationXStreamAlias(clazz);
        if(!StringUtils.isEmpty(nodeName1)){
            nodeName=nodeName1;
        }else if(!StringUtils.isEmpty(nodeName2)){
            nodeName=nodeName2;
        }else{
            nodeName=field.getName();
        }

        writer.startNode(nodeName);
        marshalSuper(obj, writer, context, fieldClazz);
        writer.endNode();
    }

    /**
     * 集合转换方法
     * @param source
     * @param writer
     * @param context
     * @param clazz
     * @param field
     * @throws Exception
     */
    private void collectionConverter(Object source,
                                     HierarchicalStreamWriter writer, MarshallingContext context,
                                     Class clazz, Field field) throws Exception {
        Type types[] = ((ParameterizedType) field.getGenericType())
                .getActualTypeArguments();
        Object obj = getObj(clazz, field, source);
        Collection collection = null;
        if (field.getType().equals(List.class)) {
            collection = (List) obj;
        } else if (field.getType().equals(Set.class)) {
            collection = (Set) obj;
        }
        //判断是否有忽略节点的注解
        boolean flag=isXStreamImplicit(field);
        String nodeName=getFildIdAnnotationXStreamAlias(field);
        if(StringUtils.isEmpty(nodeName)){
            nodeName=field.getName();
        }
        //忽略节点不进行开启节点
        if(!flag){
            writer.startNode(nodeName);
        }
        if(null!=collection){
            for (Object object : collection) {
                if(null==object){
                    continue;
                }
                //集合元素转换时，先准备javabean的注解节点名称，fildidname兜底
                String objNodNm=getClassAnnotationXStreamAlias((Class) types[0]);
                if(StringUtils.isEmpty(objNodNm)){
                    String clazzName = ((Class) types[0]).getSimpleName();
                    objNodNm= Character.toLowerCase(clazzName.substring(0, 1)
                            .toCharArray()[0]) + clazzName.substring(1);
                }
                writer.startNode(objNodNm);
                marshalSuper(object, writer, context, (Class) types[0]);
                writer.endNode();
            }
        }
        if(!flag) {
            writer.endNode();
        }
    }

    /**
     * 基础元素转换方法
     * @param source
     * @param writer
     * @param context
     * @param clazz
     * @param field
     * @throws Exception
     */
    private void basicTypeConverter(Object source,
                                    HierarchicalStreamWriter writer, MarshallingContext context,
                                    Class clazz, Field field) throws Exception {
        Object obj = getObj(clazz, field, source);
        //该fildid是否是属性，属性就不增加节点
        boolean flag=isXStreamAsAttribute(field);
        String nodeName=getFildIdAnnotationXStreamAlias(field);
        if(StringUtils.isEmpty(nodeName)){
            nodeName=field.getName();
        }
        String value=getConverterValue(field,obj);
        if(StringUtils.isEmpty(value)){
            value=obj == null ? "" :obj.toString();
        }
        //是元素属性就不增减节点
        if(flag){
            writer.addAttribute(nodeName,value);
        }else{
            writer.startNode(nodeName);
            writer.setValue(value);
            writer.endNode();
        }

    }

    /**
     * 获取XStreamAlias注解值
     * @param field
     * @return
     */
    private String getFildIdAnnotationXStreamAlias(Field field){
        XStreamAlias xStreamAlias=field.getAnnotation(XStreamAlias.class);
        if(null==xStreamAlias){
            return null;
        }else{
            return xStreamAlias.value();
        }
    }

    /**
     * 获取转换器的类
     * @param field
     * @return
     */
    private Class<?> getFildIdAnnotationXStreamConverter(Field field){
        XStreamConverter xStreamConverter=field.getAnnotation(XStreamConverter.class);
        if(null==xStreamConverter){
            return null;
        }else{
            return xStreamConverter.value();
        }
    }

    /**
     * 获取类上的XStreamAlias注解值
     * @param clazz
     * @return
     */
    private String getClassAnnotationXStreamAlias(Class clazz) {
        XStreamAlias classXsa = (XStreamAlias)clazz.getAnnotation(XStreamAlias.class);
        if(null==classXsa){
            return null;
        }else{
            return classXsa.value();
        }
    }
    /**
     * 是否忽略
     * @param field
     * @return
     */
    private boolean isXStreamImplicit(Field field){
        XStreamImplicit xStreamImplicit=field.getAnnotation(XStreamImplicit.class);
        if(null==xStreamImplicit){
            return false;
        }else{
            return true;
        }
    }

    /**
     * 是否为节点属性
     * @param field
     * @return
     */
    private boolean isXStreamAsAttribute(Field field){
        XStreamAsAttribute xStreamAlias=field.getAnnotation(XStreamAsAttribute.class);
        if(null==xStreamAlias){
            return false;
        }else{
            return true;
        }
    }

    /**
     * 获取转换器转换后的值
     * @param field
     * @param obj
     * @return
     */
    private String getConverterValue(Field field,Object obj){
        String str=null;
        if(null==obj){
            return "";
        }
        Class fildConverterClass=getFildIdAnnotationXStreamConverter(field);
        if(null==fildConverterClass){
            return str;
        }
        try{
            Method method=fildConverterClass.getMethod("toString",Object.class);
            str=(String)method.invoke(fildConverterClass.newInstance(),obj);
        }catch (NoSuchMethodException e){
            log.error("Xsteam Error 转换器转换出错",e);
        } catch (IllegalAccessException e) {
            log.error("Xsteam Error 转换器非法访问",e);
        } catch (InvocationTargetException e) {
            log.error("Xsteam Error 转换器调用异常{}|{}",fildConverterClass.toString(),field.toString(),e);
        } catch (InstantiationException e) {
            log.error("Xsteam Error 转换器实例化异常",e);
        }
        return str;
    }

    /**
     * 转换对象，集合，对象，属性
     * @param source
     * @param writer
     * @param context
     * @param clazz
     * @throws Exception
     */
    private void marshalSuper(Object source, HierarchicalStreamWriter writer,
                              MarshallingContext context, Class clazz) throws Exception {
        Field fields[] = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Class fieldClazz = field.getType();
            if (Arrays.asList(fieldClazz.getInterfaces()).contains(
                    Collection.class)) {
                collectionConverter(source, writer, context, clazz,
                        field);
            } else if(fieldClazz.getName().contains("com.cic.offer")){
                objConverter(source, writer, context, clazz, field,
                        fieldClazz);
            }
            else {
                basicTypeConverter(source, writer, context, clazz, field);
            }
        }
    }

    /**
     * 未实现反序列化，所以不能反序列化
     * @param reader
     * @param context
     * @return
     */
    @Override
    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
        // TODO Auto-generated method stub
        return null;
    }

}
