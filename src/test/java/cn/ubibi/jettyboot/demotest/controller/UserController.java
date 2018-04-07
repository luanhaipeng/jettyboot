package cn.ubibi.jettyboot.demotest.controller;

import cn.ubibi.jettyboot.demotest.dao.UserDAO;
import cn.ubibi.jettyboot.demotest.entity.UserEntity;
import cn.ubibi.jettyboot.framework.commons.PageData;
import cn.ubibi.jettyboot.framework.ioc.Autowired;
import cn.ubibi.jettyboot.framework.jdbc.model.UpdateResult;
import cn.ubibi.jettyboot.framework.rest.*;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


//@RestMapping(path = "/user")
public class UserController {

    private static Logger logger = Log.getLogger(UserController.class);

    @Autowired
    private UserDAO userDAO;


    @RestGetMapping(path = "/test_insert")
    public UpdateResult getmm21(UserInfoParser reqParser, ReqParams reqParams) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("name","name" + System.currentTimeMillis() + "_" + Math.random());
        map.put("yaoli",123);
        map.put("dai",3);
        map.put("fid",3);
        map.put("mid",3);
        map.put("create_time",System.currentTimeMillis());
        map.put("update_time",System.currentTimeMillis());
       return userDAO.insertObject(map);
//        return  "123---" + reqParser.getName() +"=====" +
    }


    @RestGetMapping(path = "/test")
    public String getmm(UserInfoParser reqParser, ReqParams reqParams, CurrentUser currentUser) throws Exception {
        new UserDAO().findAll();
        if(reqParser instanceof IReqParser){
            System.out.println("111");
        }
        return  "123---" + reqParser.getName() +"=====" + currentUser.getName();
    }



    @RestGetMapping(path = "/")
    public PageData<UserEntity> getUserById3(ReqParams reqParams, HttpServletRequest request, HttpServletResponse response) throws Exception {

        Integer pageSize = reqParams.getRequestParam("pageSize","10").toInteger();
        Integer pageNo = reqParams.getRequestParam("pageNo","0").toInteger();




//        logger.info("aaa");
        long t1 = System.currentTimeMillis();



        PageData<UserEntity> result = userDAO.findPage(pageNo, pageSize);
//        return "hello222";

        long t2 = System.currentTimeMillis();

        logger.info("::"+(t2-t1));
        return result;
    }




    @RestGetMapping(path = "/:uid")
    public Object getUserById(ReqParams params, HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uid =  params.getPathVariable("uid").toString();
        String name = params.getRequestParam("name").toString();
        String[] names = request.getParameterValues("name");


//        System.out.println("111");
//        try {
//            Thread.sleep(1000 * 10);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

//        request.getSession(true).setAttribute("uid",uid);
//        Object mm = request.getSession(true).getAttribute("uid");

        Cookie cookie = new Cookie("aaa","a2333");
        response.addCookie(cookie);

        Cookie[] cookies = request.getCookies();

        String aaa = request.getContextPath();

        return new PageRender("getUserById",userDAO.findById(uid));

//        return userDAO.findById(uid);
//        return "hello:" + uid + ":" + name;
    }


    @RestPostMapping(path = "/new/:uid")
    public String getUserById2(ReqParams reqParams, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String aaa = request.getContextPath();
        return "123saaa";
    }



}
