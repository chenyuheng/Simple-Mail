package co.yuheng.simplemail;

import java.util.List;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;

public class MimePart {
    boolean isMainPart;
    String contentType;
    String contentTransferEncoding;
    String charset;
    String contentId;
    String content;

    String[] rawLines;
    int startIndex;
    int contentStartIndex;
    int endIndex;

    static Pattern boundaryPattern = Pattern.compile("boundary=\\\"(.+)\\\"");
    static Pattern contentTypePattern = Pattern.compile("Content-Type: (.+);");
    static Pattern charsetPattern = Pattern.compile("charset=\\\"(.+)\\\"");
    static Pattern mimePattern = Pattern.compile("(.*)=\\?([a-zA-Z0-9-]+)\\?([BbQq])\\?([^\\?]+)\\?=(.*)");

    static QuotedPrintableCodec quotedPrintable = new QuotedPrintableCodec();

    public static Mail parse(String raw, int mailId) {
        String[] lines = raw.split("\r\n");
        Mail mail = parseBasic(lines, mailId);
        MimePart body = new MimePart();
        String relatedBoundary = getBoundary(lines, "Content-Type: multipart/related;");
        HashMap<String, String> contentIdMap = new HashMap<>();
        if (relatedBoundary == null) {
            int startIndex = 0;
            while (startIndex < lines.length && !lines[startIndex].trim().startsWith("Content-Type: multipart/alternative;")) {
                startIndex++;
            }
            if (startIndex >= lines.length) {
                startIndex = 0;
                while (startIndex < lines.length && !lines[startIndex].trim().startsWith("Content-Type:")) {
                    startIndex++;
                }
            }
            body.startIndex = startIndex;
            body.rawLines = lines;
            body.endIndex = lines.length - 1;
            body.parseMimePart();
        } else {
            List<MimePart> mimeParts = spliteMimePart(lines, relatedBoundary);
            for (MimePart part : mimeParts) {
                part.parseMimePart();
                if (part.isMainPart) {
                    body = part;
                } else if (part.contentId != null && part.contentType.startsWith("image")) {
                    if (part.contentId.startsWith("<") && part.contentId.endsWith(">")) {
                        part.contentId = part.contentId.substring(1, part.contentId.length() - 1);
                    }
                    part.decodeContent();
                    contentIdMap.put(part.contentId, "data:" + part.contentType + ";" + part.contentTransferEncoding + "," + part.content);
                }
            }
        }
        body.decodeContent();
        body.replaceContent(contentIdMap);
        decodeMail(mail);
        if (body.content != null) {
            mail.setContent(body.content);
        }
        return mail;
    }

    private static Mail parseBasic(String[] lines, int mailId) {
        Mail mail = new Mail(mailId);
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].startsWith("From:")) {
                mail.setFrom(lines[i].substring(6));
            } else if (lines[i].startsWith("To:")) {
                mail.setTo(lines[i].substring(4));
            } else if (lines[i].startsWith("Subject:")) {
                mail.setSubject(lines[i].substring(9));
            } else if (lines[i].startsWith("Date:")) {
                mail.setDate(lines[i].substring(6));
            }
            if (mail.hasBasicInfo()) {
                break;
            }
        }
        return mail;
    }

    private static String getBoundary(String[] lines, String boundaryType) {
        return getBoundary(lines, boundaryType, 0, lines.length);
    }

    private static String getBoundary(String[] lines, String boundaryType, int start, int end) {
        if (start < 0 || start >= end || end > lines.length) {
            return null;
        }
        for (int i = start; i < end; i++) {
            if (!lines[i].trim().startsWith(boundaryType)) {
                continue;
            }
            Matcher matcher = boundaryPattern.matcher(lines[i]);
            while (!matcher.find()) {
                i++;
                if (i >= end) {
                    return null;
                }
                matcher = boundaryPattern.matcher(lines[i]);
            }
            return matcher.group(1);
        }
        return null;
    }

    private static List<MimePart> spliteMimePart(String[] lines, String boundary) {
        return spliteMimePart(lines, boundary, 0, lines.length);
    }

    private static List<MimePart> spliteMimePart(String[] lines, String boundary, int start, int end) {
        List<MimePart> parts = new ArrayList<>();
        MimePart part;
        if (boundary == null) {
            part = new MimePart();
            part.rawLines = lines;
            part.startIndex = 0;
            part.endIndex = end - 1;
            parts.add(part);
            return parts;
        }
        int startIndex = start;
        for (int i = start; i < end; i++) {
            String trimmedLine = lines[i].trim();
            if (!trimmedLine.startsWith("--" + boundary)) {
                continue;
            }
            part = new MimePart();
            part.rawLines = lines;
            part.startIndex = startIndex;
            part.endIndex = i;
            parts.add(part);
            startIndex = i + 1;
            if (trimmedLine.equals("--" + boundary + "--")) {
                break;
            }
        }

        return parts;
    }

    private void parseMimePart() {
        if (this.startIndex < 0 || this.startIndex >= this.rawLines.length) {
            System.out.println(this.toString());
        }
        String firstLine = this.rawLines[this.startIndex];
        Matcher contentTypeMatcher = contentTypePattern.matcher(firstLine);
        if (!contentTypeMatcher.find()) {
            return;
        }
        this.contentType = contentTypeMatcher.group(1).trim();
        for (int i = this.startIndex; ; i++) {
            String trimmedLine = this.rawLines[i].trim();
            if (trimmedLine.startsWith("Content-Transfer-Encoding:")) {
                this.contentTransferEncoding = trimmedLine.substring(26).trim();
            } else if (trimmedLine.startsWith("Content-ID:")) {
                this.contentId = trimmedLine.substring(11).trim();
            }
            Matcher charsetMatcher = charsetPattern.matcher(trimmedLine);
            if (charsetMatcher.find()) {
                this.charset = charsetMatcher.group(1).trim();
            }

            if (this.rawLines[i].equals("")) {
                this.contentStartIndex = i + 1;
                break;
            }
        }
        if (this.contentType.equals("multipart/alternative")) {
            this.isMainPart = true;
            parseMimeBody();
            return;
        }
    }

    private void parseMimeBody() {     
        String alternativeBoundary = getBoundary(this.rawLines, "Content-Type: multipart/alternative;", this.startIndex, this.endIndex);
        List<MimePart> parts = spliteMimePart(this.rawLines, alternativeBoundary, this.contentStartIndex, this.endIndex);
        for (MimePart part : parts) {
            part.parseMimePart();
        }
        MimePart topPart = parts.get(0);
        for (MimePart part : parts) {
            if (part.contentType == null) {
                continue;
            }
            if (part.contentType.equals("text/html")) {
                topPart = part;
                break;
            } else if (part.contentType.equals("text/plain")) {
                topPart = part;
            }
        }
        this.contentType = topPart.contentType;
        this.contentTransferEncoding = topPart.contentTransferEncoding;
        this.charset = topPart.charset;
        this.startIndex = topPart.startIndex;
        this.contentStartIndex = topPart.contentStartIndex;
        this.endIndex = topPart.endIndex;
    }

    public static void decodeMail(Mail mail) {
        if (mail.getTo() != null) {
            mail.setTo(decodeMimeLine(mail.getTo()));
        }
        if (mail.getFrom() != null) {
            mail.setFrom(decodeMimeLine(mail.getFrom()));
        }
        if (mail.getSubject() != null) {
            mail.setSubject(decodeMimeLine(mail.getSubject()));
        }
    }

    public void decodeContent() {
        if (this.contentTransferEncoding == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (this.contentTransferEncoding.equals("base64")) {
            for (int i = this.contentStartIndex; i < this.endIndex; i++) {
                sb.append(this.rawLines[i]);
            }
            this.content = sb.toString();
            if (!this.contentType.startsWith("text")) {
                return;
            }
            this.content = decodeString(this.content, this.contentTransferEncoding, this.charset);
        } else if (this.contentTransferEncoding.equals("quoted-printable")) {
            for (int i = this.contentStartIndex; i < this.endIndex; i++) {
                String line = this.rawLines[i].trim();
                if (line.endsWith("=")) {
                    line = line.substring(0, line.length() - 1);
                }
                sb.append(line);
            }
            this.content = decodeString(sb.toString(), this.contentTransferEncoding, this.charset);
        }
    }

    public static String decodeMimeLine(String name) {
        Matcher matcher = mimePattern.matcher(name);
        if (!matcher.find()) {
            return name;
        }
        String prefix = matcher.group(1);
        String charset = matcher.group(2);
        String encoding = matcher.group(3);
        String content = matcher.group(4);
        String suffix = matcher.group(5);
        if (encoding.equals("B") || encoding.equals("b")) {
            encoding = "base64";
        } else if (encoding.equals("Q") || encoding.equals("q")) {
            encoding = "quoted-printable";
        }
        return prefix + decodeString(content, encoding, charset) + suffix;
    }

    public static String decodeString(String input, String encoding, String charset) {
        if (encoding == null) {
            return input;
        }
        if (encoding.equals("base64")) {
            byte[] content = Base64.decodeBase64(input);
            try {
                return new String(content, charset);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (encoding.equals("quoted-printable")) {
            try {
                return quotedPrintable.decode(input, charset);
            } catch (DecoderException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return input;
    }

    public void replaceContent(HashMap<String, String> map) {
        for (String key : map.keySet()) {
            this.content = this.content.replaceAll("cid:" + key, map.get(key));
        }
    }

    public String toString() {
        String content = "";
        for (int i = contentStartIndex; i < endIndex; i++) {
            content += rawLines[i] + "\n";
        }
        return "contentType: " + contentType + "\n" +
                "contentTransferEncoding: " + contentTransferEncoding + "\n" +
                "charset: " + charset + "\n" +
                "contentId: " + contentId + "\n" +
                "startIndex: " + startIndex + "\n" +
                "contentStartIndex: " + contentStartIndex + "\n" +
                "endIndex: " + endIndex + "\n" +
                "content: \n" + content + "\n";
    }
}