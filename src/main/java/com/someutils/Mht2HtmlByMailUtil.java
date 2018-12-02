package com.someutils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Mht2HtmlByMailUtil {
	/*ʹ��ʵ��
	public static void main(String[] args) throws IOException {
        mht2html("E:\\webdb\\mystruts\\src\\main\\webapp\\mhtfile\\123,321.mht", "E:\\webdb\\mystruts\\src\\main\\webapp\\mhtfile\\123,321.htm","http://192.168.0.1/stars/mhtfile/");   
    }*/

    /**
     * �� mht�ļ�ת���� html�ļ�
     * 
     * @param srcMht      // mht �ļ���λ��
     * @param destHtml    // ת���������HTML��λ��
     */
    public static void mht2html(String srcMht, String destHtml, String serverPath) {
        try {
            InputStream fis = new FileInputStream(srcMht);
            Session mailSession = Session.getDefaultInstance(System.getProperties(), null);
            MimeMessage msg = new MimeMessage(mailSession, fis);
            Object content = msg.getContent();
            if (content instanceof Multipart) {
                MimeMultipart mp = (MimeMultipart) content;

                // ��һ������text/html�����������⣩
                MimeBodyPart mbp = (MimeBodyPart) mp.getBodyPart(0);
                // ��ȡmht�ļ����ݴ���ı���
                String strEncodng = getEncoding(mbp);

                // ��ȡmht�ļ�������
                String strText = getHtmlText(mbp, strEncodng);
                if (strText == null)
                    return;

                // ������mht�ļ����Ƶ��ļ��У���Ҫ����������Դ�ļ���  ���ﲻ��Ҫ����ע�͵���
                File parent = null; 
                if (mp.getCount() > 1) {
                    String sourcePath = destHtml.substring(0, destHtml.lastIndexOf("."));
                    parent = new File(new File(sourcePath).getAbsolutePath()+ ".files");
                    parent.mkdirs();
                    if (!parent.exists()) { // �����ļ���ʧ�ܵĻ����˳�
                        return;
                    }
                }

                //FOR�д��� ��Ҫ�Ǳ�����Դ�ļ����滻·��   �ڶ����ֿ�ʼΪ��Դ�ļ�
                for (int i = 1; i < mp.getCount(); ++i) {
                    MimeBodyPart bp = (MimeBodyPart) mp.getBodyPart(i);
                    // ��ȡ��Դ�ļ���·��
                    // ������ȡ�� http://xxx.com/abc.jpg��
                    String strUrl = getResourcesUrl(bp);
                    if (strUrl == null || strUrl.length() == 0)
                        continue;

                    // ��ȡ��Դ�ļ��ľ���·��
                    String FilePath = parent.getAbsolutePath() + File.separator + getName(strUrl, i);
                    File resources = new File(FilePath);

                    // ������Դ�ļ�
                    if (saveResourcesFile(resources, bp.getInputStream())) {
                        // ��Զ�̵�ַ�滻Ϊ���ص�ַ ��ͼƬ��JS��CSS��ʽ�ȵ�
                        String replacePath = strUrl;
                        if(strUrl.startsWith("file://")){
                            replacePath = getRelativePath(strUrl);
                        }
                        String relativePath = getRelativePath(FilePath);
                        strText = strText.replace(replacePath, serverPath + relativePath);
                    }
                }

                // ��󱣴�HTML�ļ�
                saveHtml(strText, destHtml, strEncodng);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * ȡ���·��
     * @param filePath
     * @return
     */
    public static String getRelativePath(String filePath) {
        char separator = '/';

        if (filePath.lastIndexOf(separator) < 0) {
            separator = '\\';
        }
        if(filePath.lastIndexOf(separator) < 0){
            return "";
        }
        String partStr = filePath.substring(0, filePath.lastIndexOf(separator));
        partStr = partStr.substring(0, partStr.lastIndexOf(separator));
        return filePath.substring(partStr.length() + 1);

    }

    /**
     * ��ȡmht�ļ���������Դ�ļ�������
     * 
     * @param strName
     * @param ID
     * @return
     */
    public static String getName(String strName, int ID) {
        char separator1 = '/';
        char separator2 = '\\';
        // �������滻
        strName = strName.replaceAll("\r\n", "");

        // ��ȡ�ļ�����
        if (strName.lastIndexOf(separator1) >= 0) {
            return strName.substring(strName.lastIndexOf(separator1) + 1);
        }
        if (strName.lastIndexOf(separator2) >= 0) {
            return strName.substring(strName.lastIndexOf(separator2) + 1);
        }
        return "";
    }

    /**
     * ����ȡ������html����д�뱣���·���С�
     * 
     * @param htmlTxt
     * @param htmlPath
     * @param encode
     */
    public static boolean saveHtml(String htmlTxt, String htmlPath, String encode) {
        //�������ڸ�Ŀ¼���򴴽�
        File htmlFile = new File(htmlPath);
        if(!htmlFile.getParentFile().exists()){
            htmlFile.getParentFile().mkdirs();
        }
        try {
            Writer out = null;
            out = new OutputStreamWriter(new FileOutputStream(htmlPath, false), encode);
            out.write(htmlTxt);
            out.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * ������ҳ�е�JS��ͼƬ��CSS��ʽ����Դ�ļ�
     * 
     * @param srcFile Դ�ļ�
     * @param inputStream ������
     * @return
     */
    private static boolean saveResourcesFile(File srcFile, InputStream inputStream) {
        if (srcFile == null || inputStream == null) {
            return false;
        }

        BufferedInputStream in = null;
        FileOutputStream fio = null;
        BufferedOutputStream osw = null;
        try {
            in = new BufferedInputStream(inputStream);
            fio = new FileOutputStream(srcFile);
            osw = new BufferedOutputStream(new DataOutputStream(fio));
            int index = 0;
            byte[] a = new byte[1024];
            while ((index = in.read(a)) != -1) {
                osw.write(a, 0, index);
            }
            osw.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (osw != null)
                    osw.close();
                if (fio != null)
                    fio.close();
                if (in != null)
                    in.close();
                if (inputStream != null)
                    inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * ��ȡmht�ļ�����Դ�ļ���URL·��
     * 
     * @param bp
     * @return
     */
    private static String getResourcesUrl(MimeBodyPart bp) {
        if (bp == null) {
            return null;
        }
        try {
            Enumeration list = bp.getAllHeaders();
            while (list.hasMoreElements()) {
                javax.mail.Header head = (javax.mail.Header) list.nextElement();
                if (head.getName().compareTo("Content-Location") == 0) {
                    return head.getValue();
                }
            }
            return null;
        } catch (MessagingException e) {
            return null;
        }
    }

    /**
     * ��ȡmht�ļ��е����ݴ���
     * 
     * @param bp
     * @param strEncodin  ��mht�ļ��ı���
     * @return
     */
    private static String getHtmlText(MimeBodyPart bp, String strEncoding) {
        InputStream textStream = null;
        BufferedInputStream buff = null;
        BufferedReader br = null;
        Reader r = null;
        try {
            textStream = bp.getInputStream();
            buff = new BufferedInputStream(textStream);
            r = new InputStreamReader(buff, strEncoding);
            br = new BufferedReader(r);
            StringBuffer strHtml = new StringBuffer("");
            String strLine = null;
            while ((strLine = br.readLine()) != null) {
                //System.out.println(strLine);
                strHtml.append(strLine + "\r\n");
            }
            br.close();
            r.close();
            textStream.close();
            return strHtml.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
                if (buff != null)
                    buff.close();
                if (textStream != null)
                    textStream.close();
            } catch (Exception e) {
            }
        }
        return null;
    }

    /**
     * ��ȡmht��ҳ�ļ������ݴ���ı���
     * 
     * @param bp
     * @return
     */
    private static String getEncoding(MimeBodyPart bp) {
        if (bp == null) {
            return null;
        }
        try {
            Enumeration list = bp.getAllHeaders();
            while (list.hasMoreElements()) {
                javax.mail.Header head = (javax.mail.Header) list.nextElement();
                if (head.getName().equalsIgnoreCase("Content-Type")) {
                    String strType = head.getValue();
                    int pos = strType.indexOf("charset=");
                    if (pos >= 0 && strType.indexOf("text/html") >= 0) {
                        String strEncoding = strType.substring(pos + 8, strType.length());
                        if (strEncoding.startsWith("\"") || strEncoding.startsWith("\'")) {
                            strEncoding = strEncoding.substring(1, strEncoding.length());
                        }
                        if (strEncoding.endsWith("\"") || strEncoding.endsWith("\'")) {
                            strEncoding = strEncoding.substring(0, strEncoding.length() - 1);
                        }
                        if (strEncoding.toLowerCase().compareTo("gb2312") == 0) {
                            //strEncoding = "gbk"; ò�Ʋ��У�����ISO-8859-1
                            strEncoding = "ISO-8859-1";
                        }
                        return strEncoding;
                    }
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
