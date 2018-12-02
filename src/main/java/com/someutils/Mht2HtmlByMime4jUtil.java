package com.someutils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.Field;

public class Mht2HtmlByMime4jUtil {
	/*ʹ��ʵ��
	public static void main(String[] args) {
        //MessageTree.main(new String[]{"E:\\webdb\\mystruts\\src\\main\\webapp\\mhtfile\\123,123.mht"});
        mht2Html("E:\\webdb\\mystruts\\src\\main\\webapp\\mhtfile\\123,123.mht", "E:\\webdb\\mystruts\\src\\main\\webapp\\mhtfile\\123,123.htm", "");
    }*/

	/**
     * �� mht�ļ�ת���� html�ļ�
     * 
     * @param srcMht      // mht �ļ���λ��
     * @param targetFile    // ת���������HTML��λ��
     */
    public static void mht2Html(String srcMht, String targetFile, String serverPath){
        DefaultMessageBuilder builder = new DefaultMessageBuilder();
        try {
            InputStream inputStream = new FileInputStream(new File(srcMht));
            Message message = builder.parseMessage(inputStream);
            Multipart multipart = (Multipart) message.getBody();
            List<Entity> parts = multipart.getBodyParts();

            //1.ȡ����
            StringBuilder htmlTextStr = new StringBuilder();
            String encoding = null;
            for (Entity entity : parts) {
                if(!"text/html".equals(entity.getMimeType())){
                    continue;
                }
                //ȡ����
                encoding = getEncoding(entity);

                TextBody body = (TextBody)entity.getBody();
                BufferedReader br = new BufferedReader(body.getReader());
                String line = null;
                while((line = br.readLine()) != null){
                    htmlTextStr.append(line);
                }
                break;
            }
            String htmlText = htmlTextStr.toString();

            //2.ȡ��Դ�ļ� 
            //������mht�ļ����Ƶ��ļ��У���Ҫ����������Դ�ļ��� 
            File resourceFile = null; 
            if (parts.size() > 1) {
                String sourcePath = targetFile.substring(0, targetFile.lastIndexOf(".")) + ".files";
                resourceFile = new File(sourcePath);
                if(!resourceFile.exists()){
                    resourceFile.mkdirs();
                }
            }

            for (Entity entity : parts) {
                if("text/html".equals(entity.getMimeType())){
                    continue;
                }
                String strUrl = getResourcesUrl(entity);
                if (strUrl == null || strUrl.length() == 0)
                    continue;

                // ��ȡ��Դ�ļ��ľ���·��
                String filePath = resourceFile.getAbsolutePath() + File.separator + getName(strUrl);
                File resources = new File(filePath);

                // ������Դ�ļ�
                BinaryBody body = (BinaryBody)entity.getBody();
                if (saveResourcesFile(resources, body.getInputStream())) {
                    // ��Զ�̵�ַ�滻Ϊ���ص�ַ ��ͼƬ��JS��CSS��ʽ�ȵ�
                    String replacePath = strUrl;
                    if(strUrl.startsWith("file://")){
                        replacePath = getRelativePath(strUrl);
                    }
                    String relativePath = getRelativePath(filePath);
                    htmlText = htmlText.replaceAll(replacePath, serverPath + relativePath);
                }
            }
            //�����ļ�
            saveHtml(htmlText, targetFile, encoding);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    /**
     * ��ȡmht�ļ�����Դ�ļ���URL·��
     * 
     * @param bp
     * @return
     */
    private static String getResourcesUrl(Entity entity) {
        Header header = entity.getHeader();
        Field field = header.getField("Content-Location");
        if(field == null){
            return null;
        }
        return field.getBody();
    }
    /**
     * ȡ�ļ�����
     * 
     * @param strUrl
     * @return
     */
    private static String getName(String strUrl){
        char separator = '/';

        if (strUrl.lastIndexOf(separator) < 0) {
            separator = '\\';
        }
        if(strUrl.lastIndexOf(separator) < 0){
            return null;
        }
        String filename = strUrl.substring(strUrl.lastIndexOf(separator) + 1);
        if(filename.indexOf("?") > 0){
            return filename.substring(0, filename.indexOf("?"));
        }
        return filename;
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
     * ����ȡ������html����д�뱣���·���С�
     * 
     * @param htmlTxt
     * @param htmlPath
     * @param encoding
     */
    public static boolean saveHtml(String htmlTxt, String htmlPath, String encoding) {
        //�������ڸ�Ŀ¼���򴴽�
        File htmlFile = new File(htmlPath);
        if(!htmlFile.getParentFile().exists()){
            htmlFile.getParentFile().mkdirs();
        }
        try {
            Writer out = null;
            out = new OutputStreamWriter(new FileOutputStream(htmlPath, false), encoding);
            out.write(htmlTxt);
            out.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * ��ȡmht��ҳ�ļ������ݴ���ı���
     * 
     * @param entity
     * @return
     */
    private static String getEncoding(Entity entity) {
        return entity.getCharset();
    }
}
