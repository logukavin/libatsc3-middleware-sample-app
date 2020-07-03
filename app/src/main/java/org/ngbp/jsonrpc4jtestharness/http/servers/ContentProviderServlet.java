package org.ngbp.jsonrpc4jtestharness.http.servers;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public  class ContentProviderServlet extends HttpServlet {

    public ContentProviderServlet(Context context) {

        //        Read web content from assets folder
        StringBuilder sb = new StringBuilder();
        try {
            String content;
            InputStream is = context.getAssets().open("GitHub.htm");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8 ));

            while ((content = br.readLine()) != null) {
                sb.append(content);
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.content = sb.toString();
    }

    private String content;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        PrintWriter out = response.getWriter();
        out.println(content);
    }
}
