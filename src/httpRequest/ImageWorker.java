package httpRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageWorker {
    //return a list of images

    public static List<String> getPics(String text) {
        String img;
        String regex = "<img.*src\\s*=\\s*(.*?)[^>]*?>";
        List<String> pics = new ArrayList<>();

        Pattern pImage = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher mImage = pImage.matcher(text);

        while (mImage.find()) {
            img = mImage.group();
            Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img);
            while (m.find()) {
                pics.add(m.group(1));
            }
        }
        return pics;
    }

    //returns a form valid for images

    public static String splitNameOfPicture(String text, String hostName) {
        String result = null;
        if (text.contains(hostName)) {
            result = text.replace(hostName, "");
            result = result.replace("'", "");
        } else result = text;
        return result;
    }
}
