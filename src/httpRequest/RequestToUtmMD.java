package httpRequest;

import javax.net.ssl.*;
import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class RequestToUtmMD {

    public static void main(String[] args) throws Exception {

        String serverResponse = "";
        try {
            SSLSocketFactory factory =
                    (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket =
                    (SSLSocket) factory.createSocket("utm.md", 443);
            socket.startHandshake();

            PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream())));

            StringBuilder dataRequest = new StringBuilder();
            dataRequest
                    .append("GET " + "/" + " HTTP/1.1\r\n")
                    .append("Host: " + "utm.md" + "\r\n")
                    .append("Content-Type: text/html;charset=utf-8 \r\n")
                    .append("Cache-Control: immutable \r\n")
                    .append("Content-Language: en, ase, ru \r\n")
                    .append("Access-Control-Allow-Credentials: true \r\n")
                    .append("Age: 24 \r\n")
                    .append("\r\n");

            out.println(dataRequest);
            out.flush(); //clean what is left of the request

            if (out.checkError())
                System.out.println(
                        "SSLSocketClient:  java.io.PrintWriter error");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));  //rs from server

            String inputLine;
            while ((inputLine = in.readLine()) != null) {   //read each line
                serverResponse += inputLine + "\n";
                System.out.println(serverResponse);      //displaying
            }
            in.close();
            out.close();
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Secured connection performed successfully");

        List<String> listOfImgSecurised = ImageWorker.getPics(serverResponse);
        listOfImgSecurised.remove(0);
        listOfImgSecurised.remove(0);
        listOfImgSecurised.remove(0);
        System.out.println("List of images from site UTM.md :" + listOfImgSecurised);

        Semaphore semaphore = new Semaphore(2);
        ExecutorService exec = Executors.newFixedThreadPool(4);
        boolean status = true;
        while (status) {
            for (String element : listOfImgSecurised) {
                semaphore.acquire();
                exec.execute(() -> {
                    try {
                        getImgUTM(ImageWorker.splitNameOfPicture(element, "https://utm.md"));
                        semaphore.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName());
                });
                if (element.equals(listOfImgSecurised.get(listOfImgSecurised.size() - 1))) {
                    status = false;
                    break;
                }
            }
        }
        exec.shutdown();     //disconnection
        exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);  //wait a few seconds
    }

    private static void getImgUTM(String NameImg) {
        try {
            SSLSocketFactory factory =
                    (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket =
                    (SSLSocket) factory.createSocket("utm.md", 443);
            socket.startHandshake();

            PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream())));

            out.println("GET " + NameImg + " HTTP/1.1\r\nHost: " + "utm.md" + " \r\n\r\n");
            out.flush();

            if (out.checkError())
                System.out.println(
                        "SSLSocketClient:  java.io.PrintWriter error");

            String[] tokens = NameImg.split("/");
            DataInputStream in = new DataInputStream(socket.getInputStream());

            OutputStream dos = new FileOutputStream("images/" + tokens[tokens.length - 1]);

            int count, offset;
            byte[] buffer = new byte[2048];
            boolean eohFound = false;
            while ((count = in.read(buffer)) != -1) {
                offset = 0;
                if (!eohFound) {
                    String string = new String(buffer, 0, count);
                    int indexOfEOH = string.indexOf("\r\n\r\n");
                    if (indexOfEOH != -1) {
                        count = count - indexOfEOH - 4;
                        offset = indexOfEOH + 4;
                        eohFound = true;
                    } else {
                        count = 0;
                    }
                }
                dos.write(buffer, offset, count);
                dos.flush();
            }
            in.close();
            dos.close();
            System.out.println("image transfer done");
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
