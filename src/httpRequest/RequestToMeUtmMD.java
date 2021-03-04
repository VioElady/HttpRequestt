package httpRequest;
//communication details between the client and the server.
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class RequestToMeUtmMD {

    public static void main(String[] args) throws Exception {

        String serverResponse = null;
        int c;

        Socket socket = new Socket("me.utm.md", 80);
        OutputStream request = socket.getOutputStream();
        InputStream response = socket.getInputStream();

        StringBuilder dataRequest = new StringBuilder();
        dataRequest
                .append("GET " + "/" + " HTTP/1.1\r\n")
                .append("Host: " + "me.utm.md" + "\r\n")
                .append("Content-Type: text/html;charset=utf-8 \r\n")
                .append("Accept-Language: ro \r\n")
                .append("Content-Language: en, ase, ru \r\n")
                .append("User-Agent: Mozilla/5.0 (X11; Linux i686; rv:2.0.1) Gecko/20100101 Firefox/4.0.1 \r\n")
                .append("Vary: Accept-Encoding \r\n")
                .append("\r\n");
         // casting from string in byte
        byte[] data = (dataRequest.toString()).getBytes();
        request.write(data);//write the flux of byte

        while ((c = response.read()) != -1) { //read each letter
            serverResponse += (char) c;
            System.out.println(serverResponse);//displayed on the screen
        }
        socket.close();

         //extract the list of images from the server.
        List<String> listOfImg = ImageWorker.getPics(serverResponse);
        listOfImg.remove(listOfImg.size() - 1);
        listOfImg.remove(listOfImg.size() - 1);
        System.out.println("List of images from site [me.utm.md] :" + listOfImg);

        Semaphore semaphore = new Semaphore(2);
        ExecutorService exec = Executors.newFixedThreadPool(4);
        boolean status = true;

        while (status) {
            for (String element : listOfImg) {
                semaphore.acquire(); //enable
                exec.execute(() -> {
                    try {
                        //if it has http then cut, if not then it allows to download the image.
                        getImgMeUTM(ImageWorker.splitNameOfPicture(element, "http://me.utm.md/"));
                        semaphore.release();//elibereaza
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName());
                });
                if (element.equals(listOfImg.get(listOfImg.size() - 1))) {
                    status = false;
                    break;
                }
            }
        }
        exec.shutdown();
        exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    //extract the image from the server.
    private static void getImgMeUTM(String imgName) throws Exception {
        Socket socket = new Socket("me.utm.md", 80);

        DataOutputStream bw = new DataOutputStream(socket.getOutputStream());

        bw.writeBytes("GET /" + imgName + " HTTP/1.1\r\n");
        bw.writeBytes("Host: " + "me.utm.md" + ":80\r\n");
        bw.writeBytes("Content-Type: text/html;charset=utf-8 \r\n");
        bw.writeBytes("Cache-Control: immutable \r\n");
        bw.writeBytes("Content-Language: en, ase, ru \r\n");
        bw.writeBytes("Access-Control-Allow-Credentials: true \r\n");
        bw.writeBytes("Age: 24 \r\n");
        bw.writeBytes("\r\n");

        bw.flush();
         //divide from a string into an array,extract after the last /
        String[] tokens = imgName.split("/");

        DataInputStream in = new DataInputStream(socket.getInputStream());
        //extract after the last /
        OutputStream dos = new FileOutputStream("images/" + tokens[tokens.length - 1]);
       //transform from byte in images
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
    }
}
