# Utils 自己写的一些工具类，希望能够帮到大家
--CicBeanUtil 基于spring BeanUtils.copyProperties 的java Bean 拷贝工具类，可以拷贝javabean中的基本属性，对象属性，List
  --使用方法： CicBeanUtil.orgData(source,target);或者CicBeanUtil.copyBean(source,target);即可拷贝javabean中的同名属性（包含基本属性，对象属性，List）
  --例子：
  public class A {
      private String a;
      private B b;
      private List<B> c;
  }
  public class A1 {
      private String a;
      private B1 b;
      private List<B1> c;
  }
  public class B {
      private String a;
  }
  public class B1 {
      private String a;
  }
  
  A a=new A();
  a.setA("test");
  B b=new B();
  b.setA("I'm  B");
  a.setB(b);
  List<B> bList=new ArrayList<>();
  bList.add(B);
  bList.add(B);
  a.setC(bList);
  A1 a1=new A1();
  CicBeanUtil.orgData(a,a1);
