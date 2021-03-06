package cn.ubibi.jettyboot.framework.rest.impl.base;

import cn.ubibi.jettyboot.framework.commons.FrameworkConfig;
import cn.ubibi.jettyboot.framework.commons.ResponseUtils;
import cn.ubibi.jettyboot.framework.rest.ifs.ResponseRender;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public abstract class TextRespRenderAdapter implements ResponseRender {

    public void doRender(HttpServletRequest request, HttpServletResponse response) throws IOException {

        byte[] contentBytes = getContentBytes();

        response.setContentType(getContentType() + "; charset=" + FrameworkConfig.getInstance().getCharset().name());
        response.setHeader("Content-Length", "" + contentBytes.length);
        response.setHeader("Server", FrameworkConfig.getInstance().getResponseServerName());
        response.setHeader("Date", new Date().toGMTString());
        response.setHeader("connection","close");

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Expires", "Thu, 01 Jan 1970 00:00:00 GMT");
            response.setHeader("Pragma", "no-cache");
        }


        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(contentBytes);
        response.getOutputStream().close();

        ResponseUtils.tryClose(response);
    }


    public abstract byte[] getContentBytes();

    public abstract String getContentType();
}
