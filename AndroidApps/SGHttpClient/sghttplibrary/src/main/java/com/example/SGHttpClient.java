package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class SGHttpClient {
    public static final int PORT = 80;
    private SGHttpRequest mRequest;
    private SGHttpResponse mResponse;
    private String mUrl;

    public SGHttpClient() {
        mResponse = new SGHttpResponse();
    }

    public SGHttpResponse execute(String url, SGHttpRequest request) {
        mRequest = request;
        mUrl = url;
          submitRequest();
        return mResponse;
    }

    private void submitRequest() {
        UrlParser.ParsedUrl parsedUrl = new UrlParser().parseUrl(mUrl);
        try {
            Socket socket = new Socket(InetAddress.getByName(parsedUrl.getHostName()), PORT);
            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            pw.println(mRequest.getMethod() + " / HTTP/1.1");
            for (SGHttpRequest.Header header : mRequest.getHeaders()) {
                pw.println(header.mKey + ":" + header.mValue);
            }
            pw.println("Host:" + parsedUrl.getHostName());
            pw.println();
            pw.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String t;
            while ((t = br.readLine()) != null) {
                System.out.println(t);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
