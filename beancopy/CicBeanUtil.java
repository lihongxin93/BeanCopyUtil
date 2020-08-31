package com.cic.offer.biz.utils;

import com.cic.offer.biz.constants.ChannelCodes;
import com.cic.offer.facade.vo.offer.b201.req.B201ReqBasePartVo;
import com.cic.offer.facade.vo.offer.b201.req.B201ReqBodyVo;
import com.cic.offer.facade.vo.offer.b201.req.B201ReqCvrgInfoVo;
import com.cic.offer.facade.vo.offer.b201.rsp.prod0360.B201RspBodyVo;
import com.cic.offer.facade.vo.offer.b201.vhlplat.OfferClaimCoversRspVo;
import com.cic.offer.facade.vo.offer.b201.vhlplat.OfferClaimRspVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 反射调用bean copy工具类
 * lihongxin
 * 20200807
 */
@Slf4j
public class CicBeanUtil {

    /**
     * 根据路径获取bean实例
     *
     * @param classPath bean 路径
     * @return
     * @throws Exception
     */
    public static Object getBean(String classPath) throws Exception {
        try {
            // 根据给定的类名初始化类
            Class catClass = Class.forName(classPath);
            // 实例化这个类
            Object obj = catClass.newInstance();
            return obj;
        } catch (Exception e) {
            log.error(ChannelCodes.ERROR,"CicBeanUtil Init Bean Error");
            throw e;
        }

    }


    /**
     * copy 数据 包含list
     * @param source 源
     * @param target 目标
     * @throws Exception
     */
    public static void copyBean(@NonNull Object source,@NonNull Object target) throws Exception{
        //普通属性用beancopy
        BeanUtils.copyProperties(source, target);
        //copy bean中的bean
        copyBeanProperties(source, target);
        //copy bean 中list
        copyListProperty(source, target);
    }

    /**
     * copy bean下的bean
     * @param source
     * @param target
     * @throws Exception
     */
    private static void copyBeanProperties(Object source,Object target) throws Exception{
        String path=target.getClass().getName();
        path=path.substring(0,path.lastIndexOf("."));
        //获取该class下所有属性 取目标所需要的list
        Field[] tFields=target.getClass().getDeclaredFields();
        //循环所有属性
        for(Field tField:tFields){
            //根据目标bean的路径是否包含是否相同去判断是不是属于子bean
            if(tField.getType().getName().contains(path)){
                //属性名称
                String name=tField.getName();
                //取源数据的同名bean 如果没有该字段跳过，不阻断流程
                Field sField=null;
                try{
                    sField=source.getClass().getDeclaredField(name);
                }catch (NoSuchFieldException e){
                    continue;
                }
                if(null!=sField){
                    sField.setAccessible(true);
                    tField.setAccessible(true);
                    //获取源数据
                    Object s=sField.get(source);
                    if(null!=s){
                        //实例化目标bean
                        Object t=getBean(tField.getType().getName());
                        //bean拷贝
                        copyBean(s,t);
                        tField.set(target,t);
                    }
                }
            }
        }
    }

    /**
     * 根据结构和配置信息组织数据
     * 李红欣
     * @param source 源数据
     * @param target 目标数据
     */
    public static void orgData(@NonNull Object source,@NonNull Object target) throws Exception{
        try{
            copyBean(source,target);
        }catch (Exception e){
            log.error(ChannelCodes.ERROR,e);
            throw new Exception("CicBeanUtil组织数据出错",e);
        }
    }

    /**
     * 拷贝对象中的list属性
     * 必要条件1、都是list，2、属性名称必须相同
     * @param source 源对象
     * @param target 目标对象
     * @throws Exception
     */
    public static void copyListProperty(Object source,Object target) throws Exception {
        //获取该class下所有属性 取目标所需要的list
        Field[] tFields=target.getClass().getDeclaredFields();
        //循环所有属性
        for(Field tField:tFields){
            //如果是list
            if(isList(tField.getType().getTypeName())){
                Type tType=tField.getGenericType();
                try{
                    //取同名属性
                    Field sField=source.getClass().getDeclaredField(tField.getName());
                    //同样为list
                    if(isList(sField.getType().getTypeName())){
                        //必须是参数化的类型
                        if (tType instanceof ParameterizedType) {
                            ParameterizedType t = (ParameterizedType) tType;
                            String listType = t.getActualTypeArguments()[0].getTypeName();
                            // 参数值为true，禁止访问控制检查
                            sField.setAccessible(true);
                            tField.setAccessible(true);
                            //获取属性值
                            List sValue = (List)sField.get(source);
                            if(null!=sValue&&sValue.size()>0){
                                List tValue =new ArrayList();
                                for(int i=0;i<sValue.size();i++){
                                    //实例化
                                    Object data=getBean(listType);
                                    //拷贝
                                    copyBean(sValue.get(i),data);
                                    tValue.add(data);
                                }
                                tField.set(target,tValue);
                            }
                        }
                    }

                }catch (NoSuchFieldException e){
                    log.error(ChannelCodes.ERROR,"CicBeanUtil Not Found {} : ",source.getClass().getName(),tField.getName());
                }

            }
        }
    }

    /**
     * 判断是否list
     * @param type
     * @return
     */
    private static boolean isList(String type){
        if(List.class.getTypeName().equals(type)||ArrayList.class.getTypeName().equals(type)){
            return true;
        }else{
            return false;
        }
    }

    public static void main(String[] args) throws Exception{

        B201ReqBodyVo reqb=new B201ReqBodyVo();
        B201ReqBasePartVo base=new B201ReqBasePartVo();
        base.setCDptCde("xxx");
        base.setTOprTm(new Date());
        reqb.setBase(base);
        List<B201ReqCvrgInfoVo> cvrgList=new ArrayList<>();
        for(int i=0 ;i<3;i++){
            B201ReqCvrgInfoVo cvrg=new B201ReqCvrgInfoVo();
            cvrg.setCCvrgNo("12345");
            cvrg.setNAmt(0.0);
            cvrgList.add(cvrg);
        }
        reqb.setCvrg(cvrgList);
        List<OfferClaimRspVO> flatClaimVo=new ArrayList<>();
        for(int i=0 ;i<3;i++){
            OfferClaimRspVO fc=new OfferClaimRspVO();
            List<OfferClaimCoversRspVo> claimCovers=new ArrayList<>();
            for(int j=0 ;j<3;j++){
                OfferClaimCoversRspVo d=new OfferClaimCoversRspVo();
                d.setClaimAmount(0.0);
                d.setPolicyNo("保单号123");
                claimCovers.add(d);
            }
            fc.setPolicyNo("policyno123");
            fc.setClaimCovers(claimCovers);
            flatClaimVo.add(fc);
        }
        reqb.setFlatClaimVo(flatClaimVo);
        B201RspBodyVo res=new B201RspBodyVo();
        CicBeanUtil.orgData(reqb,res);
        System.out.println("xxxx");

//        CicBeanUtil.orgData(B201ResConstans.class,"0360",reqb,res);
//        B201RspVo pp=new B201RspVo();
//        pp.setBody(res);
//        String x=XsteamUtil.beanToXml(pp);
//        System.out.println(x);

//        B201ReqBodyVo s=new B201ReqBodyVo();
//        Field[] f=s.getClass().getDeclaredFields();
//        String path=s.getClass().getName();
//        path=path.substring(0,path.lastIndexOf("."));
//        System.out.println(path);
//        for(Field xx:f){
//            Class c=xx.getType();
//            String name=xx.getName();
//            Type type=xx.getGenericType();
//            System.out.println("class:"+c.getName()+"|type:"+type+"|name:"+name);
//
//
//        }
//        System.out.println(List.class.getTypeName());



    }
}
